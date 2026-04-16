package vip.mate.cron.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务 DTO
 *
 * @author MateClaw Team
 */
@Data
public class CronJobDTO {

    private Long id;
    private String name;
    private String cronExpression;
    private String timezone;
    private Long agentId;
    /** 只读展示字段 */
    private String agentName;
    private String taskType;
    private String triggerMessage;
    private String requestBody;
    private Boolean enabled;
    private LocalDateTime nextRunTime;
    private LocalDateTime lastRunTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static CronJobDTO from(CronJobEntity entity) {
        CronJobDTO dto = new CronJobDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCronExpression(entity.getCronExpression());
        dto.setTimezone(entity.getTimezone());
        dto.setAgentId(entity.getAgentId());
        dto.setTaskType(entity.getTaskType());
        dto.setTriggerMessage(entity.getTriggerMessage());
        dto.setRequestBody(entity.getRequestBody());
        dto.setEnabled(entity.getEnabled());
        dto.setNextRunTime(entity.getNextRunTime());
        dto.setLastRunTime(entity.getLastRunTime());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }

    public static CronJobDTO from(CronJobEntity entity, String agentName) {
        CronJobDTO dto = from(entity);
        dto.setAgentName(agentName);
        return dto;
    }

    public CronJobEntity toEntity() {
        CronJobEntity entity = new CronJobEntity();
        entity.setId(this.id);
        entity.setName(this.name);
        entity.setCronExpression(this.cronExpression);
        entity.setTimezone(this.timezone);
        entity.setAgentId(this.agentId);
        entity.setTaskType(this.taskType);
        entity.setTriggerMessage(this.triggerMessage);
        entity.setRequestBody(this.requestBody);
        entity.setEnabled(this.enabled);
        return entity;
    }
}
