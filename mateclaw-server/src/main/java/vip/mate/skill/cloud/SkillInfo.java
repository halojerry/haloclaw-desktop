package vip.mate.skill.cloud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 技能信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillInfo {

    /** 技能类型 */
    private String skillType;

    /** 技能名称 */
    private String skillName;

    /** 描述 */
    private String description;

    /** 版本 */
    private String version;

    /** 参数规则 */
    private List<Map<String, Object>> paramRules;

    /** 标签 */
    private List<String> tags;

    /** 是否需要认证 */
    private boolean requiresAuth;

    /** 超时时间(毫秒) */
    private long timeout;
}
