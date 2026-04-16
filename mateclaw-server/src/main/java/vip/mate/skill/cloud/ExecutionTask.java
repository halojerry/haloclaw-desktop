package vip.mate.skill.cloud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 执行任务
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTask {

    /** 任务ID */
    private String taskId;

    /** 技能类型 */
    private String skillType;

    /** 技能名称 */
    private String skillName;

    /** 执行状态 */
    private Status status;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 执行上下文 */
    private SkillContext context;

    /** 执行结果 */
    private SkillResult result;

    /**
     * 执行状态枚举
     */
    public enum Status {
        /** 运行中 */
        RUNNING,
        /** 成功 */
        SUCCESS,
        /** 失败 */
        FAILED,
        /** 已取消 */
        CANCELLED,
        /** 超时 */
        TIMEOUT
    }
}
