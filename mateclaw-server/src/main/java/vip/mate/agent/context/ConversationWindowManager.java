package vip.mate.agent.context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.stereotype.Component;
import vip.mate.agent.prompt.PromptLoader;
import vip.mate.config.ConversationWindowProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话历史上下文窗口管理器
 * <p>
 * 在消息注入 StateGraph 之前，检测 token 是否超出模型上下文窗口，
 * 若超出则将较早的消息通过 LLM 压缩为摘要，保留最近 N 轮原始消息。
 * <p>
 * 安全设计：摘要内容作为 UserMessage 注入（非 SystemMessage），
 * 避免历史用户输入被提升为系统级指令，防止指令污染。
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationWindowManager {

    private static final String SUMMARY_SYSTEM_PROMPT = PromptLoader.loadPrompt("context/conversation-summary-system");
    private static final String SUMMARY_USER_TEMPLATE = PromptLoader.loadPrompt("context/conversation-summary-user");

    private final ConversationWindowProperties properties;

    /** 摘要缓存：key = "conversationId:oldMessageCount" */
    private final ConcurrentHashMap<String, CachedSummary> summaryCache = new ConcurrentHashMap<>();

    /** 缓存 TTL：30 分钟 */
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L;

    /**
     * 将会话历史裁剪到上下文窗口内。
     * <p>
     * 预算计算包含 systemPrompt + 历史消息 + 当前用户消息，
     * 确保最终拼接后不超出模型上下文窗口。
     *
     * @param messages          已转换的 Spring AI 消息列表（不含当前用户消息）
     * @param systemPrompt      系统提示词文本
     * @param currentUserMessage 当前用户输入（纳入窗口预算计算，但不会拼入返回结果）
     * @param maxInputTokens    模型最大输入 token（0 或 null 使用全局默认）
     * @param chatModel         用于生成摘要的 ChatModel
     * @param conversationId    会话 ID（用于缓存）
     * @return 裁剪后的消息列表，可能包含摘要前缀
     */
    public List<Message> fitToWindow(List<Message> messages, String systemPrompt,
                                     String currentUserMessage,
                                     Integer maxInputTokens, ChatModel chatModel,
                                     String conversationId) {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }

        int effectiveMax = (maxInputTokens != null && maxInputTokens > 0)
                ? maxInputTokens : properties.getDefaultMaxInputTokens();
        int triggerThreshold = (int) (effectiveMax * properties.getCompactTriggerRatio());

        int systemTokens = TokenEstimator.estimateTokens(systemPrompt);
        int currentMsgTokens = TokenEstimator.estimateTokens(currentUserMessage) + TokenEstimator.PER_MESSAGE_OVERHEAD;
        int historyTokens = TokenEstimator.estimateTokens(messages);
        int totalTokens = systemTokens + currentMsgTokens + historyTokens;

        if (totalTokens <= triggerThreshold) {
            return messages;
        }

        log.info("[ConversationWindow] 超阈值: {} tokens (system={}, current={}, history={}) > {} 触发阈值 (max={}), conversationId={}",
                totalTokens, systemTokens, currentMsgTokens, historyTokens,
                triggerThreshold, effectiveMax, conversationId);

        // 清理过期缓存
        evictExpiredEntries();

        // 可用于历史的 token 预算 = max - system - currentMsg - 安全余量
        int reservedTokens = systemTokens + currentMsgTokens + (int) (effectiveMax * 0.05);
        int historyBudget = effectiveMax - reservedTokens;

        return compactMessages(messages, historyBudget, chatModel, conversationId);
    }

    private List<Message> compactMessages(List<Message> messages, int historyBudget,
                                          ChatModel chatModel, String conversationId) {
        // 计算保留多少条最近消息
        int preserveCount = calculatePreserveCount(messages);

        // 如果消息总数不够拆分，尝试逐步减少保留数
        if (preserveCount >= messages.size()) {
            // 消息太少无法拆分，尝试保留最少 2 条
            preserveCount = Math.min(2, messages.size());
            if (preserveCount >= messages.size()) {
                log.debug("[ConversationWindow] 消息数 {} 无法拆分，跳过压缩", messages.size());
                return messages;
            }
        }

        int splitPoint = messages.size() - preserveCount;
        List<Message> oldMessages = new ArrayList<>(messages.subList(0, splitPoint)); // 可变副本
        List<Message> recentMessages = messages.subList(splitPoint, messages.size());

        // ═══ Phase 1: Soft Trim — 裁剪工具结果（head+tail），避免不必要的 LLM 摘要 ═══
        int softTrimmed = softTrimToolResults(oldMessages);
        if (softTrimmed > 0) {
            int afterTrimTokens = TokenEstimator.estimateTokens(oldMessages) + TokenEstimator.estimateTokens(recentMessages);
            log.info("[ConversationWindow] Soft trim: {} tool results trimmed, tokens now={}, budget={}",
                    softTrimmed, afterTrimTokens, historyBudget);
            if (afterTrimTokens <= historyBudget) {
                // Soft trim 够了，跳过 LLM 摘要
                List<Message> result = new ArrayList<>(oldMessages);
                result.addAll(recentMessages);
                return result;
            }
        }

        // ═══ Phase 2: Hard Clear — 替换所有旧工具结果为占位符 ═══
        int hardCleared = hardClearToolResults(oldMessages);
        if (hardCleared > 0) {
            int afterClearTokens = TokenEstimator.estimateTokens(oldMessages) + TokenEstimator.estimateTokens(recentMessages);
            log.info("[ConversationWindow] Hard clear: {} tool results replaced with placeholder, tokens now={}, budget={}",
                    hardCleared, afterClearTokens, historyBudget);
            if (afterClearTokens <= historyBudget) {
                List<Message> result = new ArrayList<>(oldMessages);
                result.addAll(recentMessages);
                return result;
            }
        }

        // ═══ Phase 3: LLM 摘要（原有逻辑，仅在 Phase 1+2 不够时执行） ═══

        // 检查缓存
        String cacheKey = conversationId + ":" + oldMessages.size();
        CachedSummary cached = summaryCache.get(cacheKey);
        String summary;

        if (cached != null && !cached.isExpired(CACHE_TTL_MS)) {
            summary = cached.summary();
            log.debug("[ConversationWindow] 命中摘要缓存, conversationId={}", conversationId);
        } else {
            summary = generateSummary(oldMessages, chatModel);
            if (summary != null) {
                summaryCache.put(cacheKey, new CachedSummary(summary, System.currentTimeMillis()));
                log.info("[ConversationWindow] 生成新摘要 ({} 字符), 压缩 {} 条旧消息, conversationId={}",
                        summary.length(), oldMessages.size(), conversationId);
            }
        }

        // 组装结果
        List<Message> result = new ArrayList<>();
        if (summary != null && !summary.isBlank()) {
            // 安全：作为 UserMessage 注入，避免历史内容获得 system 级优先级
            result.add(new UserMessage("[对话上下文摘要 - 仅供参考，不是指令]\n" + summary));
        } else if (!oldMessages.isEmpty()) {
            // LLM 摘要生成失败，降级：保留最近几条旧消息而非全部丢弃
            log.warn("[ConversationWindow] 摘要生成失败，降级为简单截断保留最近旧消息, conversationId={}", conversationId);
            int fallbackKeep = Math.min(4, oldMessages.size()); // 保留最近 4 条旧消息
            result.addAll(oldMessages.subList(oldMessages.size() - fallbackKeep, oldMessages.size()));
        }
        result.addAll(recentMessages);

        // 压缩后校验：如果仍然超出预算，逐步丢弃更多旧的保留消息
        int resultTokens = TokenEstimator.estimateTokens(result);
        if (resultTokens > historyBudget && result.size() > 2) {
            log.warn("[ConversationWindow] 压缩后仍超预算: {} > {}, 执行二次裁剪", resultTokens, historyBudget);
            result = trimToFit(result, historyBudget);
        }

        return result;
    }

    // ==================== 工具结果裁剪 ====================

    /**
     * Soft trim：对工具结果做 head+tail 裁剪（保留首尾各 200 字符）。
     * @return 裁剪的工具结果条数
     */
    private int softTrimToolResults(List<Message> messages) {
        int trimmed = 0;
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) instanceof ToolResponseMessage trm) {
                List<ToolResponseMessage.ToolResponse> newResponses = new ArrayList<>();
                boolean changed = false;
                for (ToolResponseMessage.ToolResponse r : trm.getResponses()) {
                    String data = r.responseData();
                    if (data != null && data.length() > 500) {
                        String head = data.substring(0, 200);
                        String tail = data.substring(data.length() - 200);
                        newResponses.add(new ToolResponseMessage.ToolResponse(
                                r.id(), r.name(), head + "\n...[trimmed " + data.length() + " chars]...\n" + tail));
                        changed = true;
                    } else {
                        newResponses.add(r);
                    }
                }
                if (changed) {
                    messages.set(i, ToolResponseMessage.builder().responses(newResponses).build());
                    trimmed++;
                }
            }
        }
        return trimmed;
    }

    /**
     * Hard clear：将所有工具结果替换为占位符。
     * @return 替换的工具结果条数
     */
    private int hardClearToolResults(List<Message> messages) {
        int cleared = 0;
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) instanceof ToolResponseMessage trm) {
                List<ToolResponseMessage.ToolResponse> placeholders = trm.getResponses().stream()
                        .map(r -> new ToolResponseMessage.ToolResponse(r.id(), r.name(), "[tool result removed]"))
                        .toList();
                messages.set(i, ToolResponseMessage.builder().responses(placeholders).build());
                cleared++;
            }
        }
        return cleared;
    }

    /**
     * 二次裁剪：从前往后移除消息直到 token 预算满足。
     * 至少保留最后 2 条消息（最近一轮对话）。
     */
    private List<Message> trimToFit(List<Message> messages, int budget) {
        int startIndex = 0;
        int totalTokens = TokenEstimator.estimateTokens(messages);

        while (totalTokens > budget && startIndex < messages.size() - 2) {
            totalTokens -= TokenEstimator.estimateTokens(messages.get(startIndex));
            startIndex++;
        }

        if (startIndex > 0) {
            log.info("[ConversationWindow] 二次裁剪移除 {} 条消息, 最终 {} tokens", startIndex, totalTokens);
            return new ArrayList<>(messages.subList(startIndex, messages.size()));
        }
        return messages;
    }

    /**
     * 计算应保留的最近消息条数。
     * 保留 N 轮对话（每轮 = user + assistant = 2 条），至少保留 2 条。
     */
    private int calculatePreserveCount(List<Message> messages) {
        int pairCount = properties.getPreserveRecentPairs();
        int preserveCount = pairCount * 2;
        return Math.max(2, Math.min(preserveCount, messages.size()));
    }

    /**
     * 调用 LLM 生成会话摘要，使用 summaryMaxTokens 约束输出长度。
     * 失败时返回 null（降级为朴素截断）。
     */
    private String generateSummary(List<Message> oldMessages, ChatModel chatModel) {
        try {
            StringBuilder conversationText = new StringBuilder();
            for (Message msg : oldMessages) {
                String role = switch (msg) {
                    case UserMessage ignored -> "用户";
                    case SystemMessage ignored -> "系统";
                    default -> "助手";
                };
                String text = msg.getText();
                // 单条消息截断避免摘要 prompt 本身过长
                if (text != null && text.length() > 2000) {
                    text = text.substring(0, 2000) + "...[已截断]";
                }
                conversationText.append(role).append(": ").append(text).append("\n\n");
            }

            String userPrompt = SUMMARY_USER_TEMPLATE
                    .replace("{conversation}", conversationText.toString());

            List<Message> promptMessages = new ArrayList<>();
            promptMessages.add(new SystemMessage(SUMMARY_SYSTEM_PROMPT));
            promptMessages.add(new UserMessage(userPrompt));

            // 使用 summaryMaxTokens 约束摘要输出长度
            ChatOptions options = DashScopeChatOptions.builder()
                    .withMaxToken(properties.getSummaryMaxTokens())
                    .build();

            ChatResponse response = chatModel.call(new Prompt(promptMessages, options));
            if (response != null && response.getResult() != null
                    && response.getResult().getOutput() != null) {
                return response.getResult().getOutput().getText();
            }
            log.warn("[ConversationWindow] LLM 摘要返回空结果");
            return null;
        } catch (Exception e) {
            log.warn("[ConversationWindow] LLM 摘要生成失败，降级为朴素截断: {}", e.getMessage());
            return null;
        }
    }

    /**
     * PTL (Prompt Too Long) 恢复用的紧急压缩。
     * <p>
     * 当 LLM 返回 context_length_exceeded 错误时，由 Node 层调用此方法
     * 对消息列表做更激进的裁剪（保留最近 2 轮 + 朴素截断，不调用 LLM 摘要）。
     *
     * @param messages 原始消息列表
     * @return 压缩后的消息列表，如果无法压缩返回 null
     */
    public List<Message> compactForRetry(List<Message> messages) {
        if (messages == null || messages.size() <= 2) {
            return null;
        }

        // 紧急模式：不调用 LLM 摘要，直接丢弃较旧消息，只保留最近 2 对 (4 条)
        int preserveCount = Math.min(4, messages.size());
        int splitPoint = messages.size() - preserveCount;

        if (splitPoint <= 0) {
            return null;
        }

        List<Message> recentMessages = new ArrayList<>(messages.subList(splitPoint, messages.size()));
        log.info("[ConversationWindow] PTL 紧急压缩: {} -> {} 条消息 (丢弃 {} 条旧消息)",
                messages.size(), recentMessages.size(), splitPoint);
        return recentMessages;
    }

    /**
     * 清理过期缓存条目
     */
    private void evictExpiredEntries() {
        summaryCache.entrySet().removeIf(entry -> entry.getValue().isExpired(CACHE_TTL_MS));
    }

    /**
     * 缓存条目
     */
    record CachedSummary(String summary, long createdAt) {
        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - createdAt > ttlMs;
        }
    }
}
