package vip.mate.memory.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import vip.mate.memory.MemoryProperties;
import vip.mate.memory.event.ConversationCompletedEvent;
import vip.mate.memory.service.MemorySummarizationService;

/**
 * 对话完成后的记忆提取监听器
 * <p>
 * 异步执行，不阻塞用户响应。
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostConversationMemoryListener {

    private final MemoryProperties properties;
    private final MemorySummarizationService summarizationService;

    @Async
    @EventListener
    public void onConversationCompleted(ConversationCompletedEvent event) {
        if (!properties.isAutoSummarizeEnabled()) {
            return;
        }

        // 跳过 cron 触发的对话
        if (properties.isSkipCronConversations() && "cron".equals(event.triggerSource())) {
            return;
        }

        // 消息数量不足
        if (event.messageCount() < properties.getMinMessagesForSummarize()) {
            return;
        }

        // 用户消息太短
        if (event.userMessage() != null
                && event.userMessage().length() < properties.getMinUserMessageLength()) {
            return;
        }

        try {
            log.debug("[Memory] Triggering post-conversation memory analysis: agent={}, conv={}",
                    event.agentId(), event.conversationId());
            summarizationService.analyzeAndUpdateMemory(event.agentId(), event.conversationId());
        } catch (Exception e) {
            log.warn("[Memory] Post-conversation summarization failed: agent={}, conv={}, error={}",
                    event.agentId(), event.conversationId(), e.getMessage());
        }
    }
}
