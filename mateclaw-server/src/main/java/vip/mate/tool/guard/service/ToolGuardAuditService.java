package vip.mate.tool.guard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vip.mate.tool.guard.model.*;
import vip.mate.tool.guard.repository.ToolGuardAuditLogMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具安全审计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolGuardAuditService {

    private final ToolGuardAuditLogMapper auditMapper;
    private final ObjectMapper objectMapper;

    /**
     * 异步记录审计日志
     */
    @Async
    public void record(ToolInvocationContext context, GuardEvaluation evaluation, String pendingId) {
        try {
            ToolGuardAuditLogEntity entity = new ToolGuardAuditLogEntity();
            entity.setConversationId(context.conversationId());
            entity.setAgentId(context.agentId());
            entity.setUserId(context.userId());
            entity.setChannelType(context.channelType());
            entity.setToolName(context.toolName());
            entity.setToolParamsJson(truncate(context.rawArguments(), 2000));
            entity.setDecision(evaluation.decision().name());
            entity.setMaxSeverity(evaluation.maxSeverity() != null ? evaluation.maxSeverity().name() : null);
            entity.setPendingId(pendingId);

            if (evaluation.hasFindings()) {
                entity.setFindingsJson(serializeFindings(evaluation));
            }

            auditMapper.insert(entity);
        } catch (Exception e) {
            log.warn("[ToolGuardAudit] Failed to record audit log: {}", e.getMessage());
        }
    }

    /**
     * 分页查询审计日志
     */
    public IPage<ToolGuardAuditLogEntity> listAll(int page, int size,
                                                    String toolName, String decision,
                                                    String conversationId) {
        LambdaQueryWrapper<ToolGuardAuditLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (toolName != null && !toolName.isBlank()) {
            wrapper.eq(ToolGuardAuditLogEntity::getToolName, toolName);
        }
        if (decision != null && !decision.isBlank()) {
            wrapper.eq(ToolGuardAuditLogEntity::getDecision, decision);
        }
        if (conversationId != null && !conversationId.isBlank()) {
            wrapper.eq(ToolGuardAuditLogEntity::getConversationId, conversationId);
        }
        wrapper.orderByDesc(ToolGuardAuditLogEntity::getCreateTime);
        return auditMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 按会话查询审计日志
     */
    public IPage<ToolGuardAuditLogEntity> listByConversation(String conversationId, int page, int size) {
        return listAll(page, size, null, null, conversationId);
    }

    /**
     * 审计统计
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", auditMapper.selectCount(null));
        stats.put("blocked", auditMapper.selectCount(
                new LambdaQueryWrapper<ToolGuardAuditLogEntity>()
                        .eq(ToolGuardAuditLogEntity::getDecision, "BLOCK")));
        stats.put("needsApproval", auditMapper.selectCount(
                new LambdaQueryWrapper<ToolGuardAuditLogEntity>()
                        .eq(ToolGuardAuditLogEntity::getDecision, "NEEDS_APPROVAL")));
        stats.put("allowed", auditMapper.selectCount(
                new LambdaQueryWrapper<ToolGuardAuditLogEntity>()
                        .eq(ToolGuardAuditLogEntity::getDecision, "ALLOW")));
        return stats;
    }

    private String serializeFindings(GuardEvaluation evaluation) {
        try {
            return objectMapper.writeValueAsString(evaluation.findingsToMapList());
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}
