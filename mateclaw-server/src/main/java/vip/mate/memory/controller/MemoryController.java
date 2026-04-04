package vip.mate.memory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vip.mate.common.result.R;
import vip.mate.memory.service.MemoryEmergenceService;
import vip.mate.memory.service.MemorySummarizationService;

import java.util.Map;

/**
 * 记忆管理接口
 * <p>
 * 提供记忆整合的手动触发和状态查询。
 *
 * @author MateClaw Team
 */
@Tag(name = "记忆管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryEmergenceService emergenceService;
    private final MemorySummarizationService summarizationService;

    @Operation(summary = "手动触发记忆整合（daily notes → MEMORY.md）")
    @PostMapping("/{agentId}/emergence")
    public R<Map<String, String>> triggerEmergence(@PathVariable Long agentId) {
        try {
            emergenceService.consolidate(agentId);
            return R.ok(Map.of("status", "completed"));
        } catch (Exception e) {
            log.error("[Memory] Manual emergence failed for agent={}: {}", agentId, e.getMessage(), e);
            return R.fail("记忆整合失败: " + e.getMessage());
        }
    }

    @Operation(summary = "手动触发对话记忆提取")
    @PostMapping("/{agentId}/summarize/{conversationId}")
    public R<Map<String, String>> triggerSummarize(
            @PathVariable Long agentId,
            @PathVariable String conversationId) {
        try {
            summarizationService.analyzeAndUpdateMemory(agentId, conversationId);
            return R.ok(Map.of("status", "completed"));
        } catch (Exception e) {
            log.error("[Memory] Manual summarization failed for agent={}, conv={}: {}",
                    agentId, conversationId, e.getMessage(), e);
            return R.fail("记忆提取失败: " + e.getMessage());
        }
    }
}
