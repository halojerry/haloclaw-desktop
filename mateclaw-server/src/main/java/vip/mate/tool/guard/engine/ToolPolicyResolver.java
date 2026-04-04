package vip.mate.tool.guard.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.mate.tool.guard.model.*;

import java.util.List;
import java.util.Set;

/**
 * 策略解析器
 * <p>
 * 将 Guardian 产出的 findings 映射为最终裁决（GuardDecision）。
 * <ul>
 *   <li>Guardian 只负责发现风险事实</li>
 *   <li>PolicyResolver 负责把事实映射为执行策略</li>
 * </ul>
 * 采用 findings-driven approval 策略，不按工具类型默认审批。
 */
@Slf4j
@Component
public class ToolPolicyResolver {

    /**
     * 根据 findings 和上下文产出最终裁决
     * <p>
     * 策略（findings-driven approval）：
     * <ul>
     *   <li>无 findings → ALLOW（普通命令直接执行）</li>
     *   <li>CRITICAL → BLOCK（极端危险直接阻断）</li>
     *   <li>HIGH → NEEDS_APPROVAL（高风险需审批）</li>
     *   <li>MEDIUM → NEEDS_APPROVAL（中风险需审批）</li>
     * </ul>
     */
    public GuardDecision resolve(List<GuardFinding> findings, ToolInvocationContext context) {
        // 无 findings → 直接允许（不再按工具类型默认审批）
        if (findings == null || findings.isEmpty()) {
            return GuardDecision.ALLOW;
        }

        GuardSeverity maxSeverity = findings.stream()
                .map(GuardFinding::severity)
                .reduce(GuardSeverity.INFO, GuardSeverity::max);

        // CRITICAL → 直接 BLOCK
        if (maxSeverity.isAtLeast(GuardSeverity.CRITICAL)) {
            return GuardDecision.BLOCK;
        }

        // HIGH / MEDIUM → 需要审批
        if (maxSeverity.isAtLeast(GuardSeverity.MEDIUM)) {
            return GuardDecision.NEEDS_APPROVAL;
        }

        // LOW / INFO → 允许
        return GuardDecision.ALLOW;
    }

    /**
     * 构建人类可读的摘要
     */
    public String buildSummary(List<GuardFinding> findings, GuardDecision decision) {
        if (findings == null || findings.isEmpty()) {
            // 无 findings 时不应该有 NEEDS_APPROVAL 或 BLOCK
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("检测到 ").append(findings.size()).append(" 项安全风险");

        // 列出最高风险的发现
        findings.stream()
                .filter(f -> f.severity() != null && f.severity().isAtLeast(GuardSeverity.MEDIUM))
                .limit(3)
                .forEach(f -> sb.append("\n- [").append(f.severity().name()).append("] ").append(f.title()));

        if (findings.size() > 3) {
            sb.append("\n- ... 及其他 ").append(findings.size() - 3).append(" 项");
        }

        return sb.toString();
    }
}
