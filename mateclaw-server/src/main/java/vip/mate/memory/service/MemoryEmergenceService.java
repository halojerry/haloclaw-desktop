package vip.mate.memory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import vip.mate.agent.AgentGraphBuilder;
import vip.mate.agent.prompt.PromptLoader;
import vip.mate.llm.service.ModelConfigService;
import vip.mate.llm.model.ModelConfigEntity;
import vip.mate.memory.MemoryProperties;
import vip.mate.workspace.document.WorkspaceFileService;
import vip.mate.workspace.document.model.WorkspaceFileEntity;

import java.util.Comparator;
import java.util.List;

/**
 * 记忆整合服务
 * <p>
 * 读取近 N 天的 daily notes，提炼反复出现的模式和重要信息，
 * 合并到 MEMORY.md 中。
 *
 * @author MateClaw Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryEmergenceService {

    private final WorkspaceFileService workspaceFileService;
    private final ModelConfigService modelConfigService;
    private final AgentGraphBuilder agentGraphBuilder;
    private final MemoryProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 执行记忆整合：将 daily notes 中的重复模式提炼到 MEMORY.md
     *
     * @param agentId Agent ID
     */
    public void consolidate(Long agentId) {
        if (!properties.isEmergenceEnabled()) {
            log.debug("[Memory] Emergence is disabled, skipping for agent={}", agentId);
            return;
        }

        // 1. 列出所有 memory/*.md 文件
        List<WorkspaceFileEntity> allFiles = workspaceFileService.listFiles(agentId);
        List<String> dailyFilenames = allFiles.stream()
                .map(WorkspaceFileEntity::getFilename)
                .filter(f -> f.startsWith("memory/") && f.endsWith(".md"))
                .sorted(Comparator.reverseOrder())
                .limit(properties.getEmergenceDayRange())
                .toList();

        if (dailyFilenames.isEmpty()) {
            log.info("[Memory] No daily notes found for agent={}, skipping emergence", agentId);
            return;
        }

        // 2. 读取 daily notes 内容
        StringBuilder dailyNotesBuilder = new StringBuilder();
        for (String filename : dailyFilenames) {
            WorkspaceFileEntity file = workspaceFileService.getFile(agentId, filename);
            if (file != null && file.getContent() != null && !file.getContent().isBlank()) {
                dailyNotesBuilder.append("### ").append(filename).append("\n");
                dailyNotesBuilder.append(file.getContent().trim()).append("\n\n");
            }
        }
        String dailyNotes = dailyNotesBuilder.toString().trim();

        if (dailyNotes.isEmpty()) {
            log.info("[Memory] All daily notes are empty for agent={}, skipping emergence", agentId);
            return;
        }

        // 3. 读取现有 MEMORY.md
        String memoryContent = readFileContentSafe(agentId, "MEMORY.md");

        // 4. 构建 prompt 并调用 LLM
        String systemPrompt = PromptLoader.loadPrompt("memory/emergence-system");
        String userTemplate = PromptLoader.loadPrompt("memory/emergence-user");
        String userPrompt = userTemplate
                .replace("{memory}", memoryContent)
                .replace("{day_range}", String.valueOf(properties.getEmergenceDayRange()))
                .replace("{daily_notes}", dailyNotes);

        String llmResponse;
        try {
            ChatModel chatModel = buildChatModel();
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userPrompt)
            ));
            ChatResponse response = chatModel.call(prompt);
            llmResponse = response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.warn("[Memory] Emergence LLM call failed for agent={}: {}", agentId, e.getMessage());
            return;
        }

        // 5. 解析并应用
        try {
            JsonNode root = parseJsonResponse(llmResponse);
            if (root == null || !root.path("should_update").asBoolean(false)) {
                String reason = root != null ? root.path("reason").asText("") : "parse failed";
                log.info("[Memory] No emergence update needed for agent={}: {}", agentId, reason);
                return;
            }

            JsonNode memoryNode = root.path("memory_content");
            if (!memoryNode.isNull() && memoryNode.isTextual()) {
                String newContent = memoryNode.asText().trim();
                if (!newContent.isEmpty()) {
                    workspaceFileService.saveFile(agentId, "MEMORY.md", newContent);
                    String reason = root.path("reason").asText("");
                    log.info("[Memory] Emergence completed for agent={}: {}", agentId, reason);
                }
            }
        } catch (Exception e) {
            log.warn("[Memory] Failed to parse/apply emergence result for agent={}: {}", agentId, e.getMessage());
        }
    }

    private ChatModel buildChatModel() {
        ModelConfigEntity defaultModel = modelConfigService.getDefaultModel();
        return agentGraphBuilder.buildRuntimeChatModel(defaultModel);
    }

    private JsonNode parseJsonResponse(String response) {
        if (response == null || response.isBlank()) return null;

        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        try {
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.warn("[Memory] Failed to parse emergence JSON response: {}", e.getMessage());
            return null;
        }
    }

    private String readFileContentSafe(Long agentId, String filename) {
        try {
            WorkspaceFileEntity file = workspaceFileService.getFile(agentId, filename);
            return file != null && file.getContent() != null ? file.getContent() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
