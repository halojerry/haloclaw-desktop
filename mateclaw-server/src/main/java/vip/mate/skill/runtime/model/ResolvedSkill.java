package vip.mate.skill.runtime.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 运行时已解析的技能包
 * 包含解析状态、安全扫描结果、依赖检查结果
 */
@Data
@Builder
public class ResolvedSkill {

    // ==================== 基础信息 ====================

    /** 技能名称 */
    private String name;

    /** 技能描述（从 SKILL.md frontmatter 解析） */
    private String description;

    /** SKILL.md 完整内容 */
    private String content;

    /**
     * 解析后的来源类型：directory / database
     */
    private String source;

    /** 技能目录路径（如果是目录型 skill） */
    @JsonIgnore
    private Path skillDir;

    /** configJson 中配置的 skillDir（原始值） */
    private String configuredSkillDir;

    /** 是否运行时可用（综合：解析成功 + 未被安全阻断 + 依赖就绪） */
    private boolean runtimeAvailable;

    /** 解析错误信息 */
    private String resolutionError;

    /** 技能目录路径字符串（用于 JSON 序列化） */
    public String getSkillDirPath() {
        return skillDir != null ? skillDir.toString() : null;
    }

    /** references/ 目录树 */
    private Map<String, Object> references;

    /** scripts/ 目录树 */
    private Map<String, Object> scripts;

    /** 是否启用 */
    private boolean enabled;

    /** 图标 */
    private String icon;

    /** 是否为内置技能 */
    @Builder.Default
    private boolean builtin = false;

    // ==================== 安全扫描状态 ====================

    /** 是否被安全扫描阻断 */
    @Builder.Default
    private boolean securityBlocked = false;

    /** 安全扫描最高严重级别 */
    private String securitySeverity;

    /** 安全扫描发现摘要 */
    private String securitySummary;

    /** 安全扫描发现列表（JSON 友好） */
    private List<SecurityFinding> securityFindings;

    /** 安全警告列表 */
    private List<String> securityWarnings;

    // ==================== 依赖检查状态 ====================

    /** 依赖是否全部就绪 */
    @Builder.Default
    private boolean dependencyReady = true;

    /** 缺失依赖列表 */
    private List<String> missingDependencies;

    /** 依赖状态摘要 */
    private String dependencySummary;

    // ==================== 综合状态 ====================

    /**
     * 综合运行时状态标签
     * 用于前端 badge 显示
     */
    public String getRuntimeStatusLabel() {
        if (!enabled) return "Disabled";
        if (securityBlocked) return "Security Blocked";
        if (!dependencyReady) return "Dependencies Missing";
        if (resolutionError != null && !runtimeAvailable) return "Unresolved";
        if (securityFindings != null && !securityFindings.isEmpty()) return "Security Warning";
        if (runtimeAvailable) return "Ready";
        return "Unknown";
    }

    // ==================== 内部 DTO ====================

    /**
     * 安全发现（前端展示用，SkillValidationResult.Finding 的序列化友好版本）
     */
    @Data
    @Builder
    public static class SecurityFinding {
        private String ruleId;
        private String severity;
        private String category;
        private String title;
        private String description;
        private String filePath;
        private Integer lineNumber;
        private String snippet;
        private String remediation;
    }
}
