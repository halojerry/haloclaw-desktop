package vip.mate.skill.cloud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 技能执行结果
 * 统一封装技能执行的返回结果
 *
 * @author MateClaw Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SkillResult {

    /** 执行状态 */
    private Status status;

    /** 技能类型 */
    private String skillType;

    /** 技能名称 */
    private String skillName;

    /** 执行结果码 */
    private String code;

    /** 执行结果消息 */
    private String message;

    /** 执行耗时(毫秒) */
    private long durationMs;

    /** 请求ID */
    private String requestId;

    /** 执行开始时间 */
    private LocalDateTime startTime;

    /** 执行结束时间 */
    private LocalDateTime endTime;

    /** 结果数据 */
    private Object data;

    /** 错误信息 */
    private String error;

    /** 错误堆栈 */
    private String stackTrace;

    /** 警告信息列表 */
    private List<String> warnings;

    /** 执行步骤记录 */
    private List<ExecutionStep> steps;

    /** 附加数据 */
    private Map<String, Object> extra;

    /**
     * 执行状态枚举
     */
    public enum Status {
        /** 执行成功 */
        SUCCESS,
        /** 执行失败 */
        FAILED,
        /** 执行中 */
        RUNNING,
        /** 已取消 */
        CANCELLED,
        /** 超时 */
        TIMEOUT,
        /** 部分成功 */
        PARTIAL_SUCCESS
    }

    /**
     * 创建成功结果
     */
    public static SkillResult success(String skillType, String skillName, Object data) {
        return SkillResult.builder()
                .status(Status.SUCCESS)
                .skillType(skillType)
                .skillName(skillName)
                .code("SUCCESS")
                .message("技能执行成功")
                .data(data)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建失败结果
     */
    public static SkillResult failed(String skillType, String skillName, String error) {
        return SkillResult.builder()
                .status(Status.FAILED)
                .skillType(skillType)
                .skillName(skillName)
                .code("FAILED")
                .message("技能执行失败")
                .error(error)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .build();
    }

    /**
     * 创建执行中结果
     */
    public static SkillResult running(String skillType, String skillName, String message) {
        return SkillResult.builder()
                .status(Status.RUNNING)
                .skillType(skillType)
                .skillName(skillName)
                .code("RUNNING")
                .message(message)
                .startTime(LocalDateTime.now())
                .build();
    }

    /**
     * 添加执行步骤
     */
    public SkillResult addStep(String stepName, String description, boolean success) {
        if (this.steps == null) {
            this.steps = new java.util.ArrayList<>();
        }
        this.steps.add(ExecutionStep.builder()
                .stepName(stepName)
                .description(description)
                .success(success)
                .timestamp(LocalDateTime.now())
                .build());
        return this;
    }

    /**
     * 添加警告
     */
    public SkillResult addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new java.util.ArrayList<>();
        }
        this.warnings.add(warning);
        return this;
    }

    /**
     * 标记执行结束
     */
    public SkillResult finish() {
        this.endTime = LocalDateTime.now();
        this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        return this;
    }

    /**
     * 执行步骤记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionStep {
        /** 步骤名称 */
        private String stepName;

        /** 步骤描述 */
        private String description;

        /** 是否成功 */
        private boolean success;

        /** 时间戳 */
        private LocalDateTime timestamp;

        /** 附加数据 */
        private Map<String, Object> data;
    }
}
