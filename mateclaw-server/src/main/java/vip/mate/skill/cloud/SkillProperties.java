package vip.mate.skill.cloud;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 技能模块配置属性
 *
 * @author MateClaw Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "mateclaw.skill")
public class SkillProperties {

    /** 是否启用技能模块 */
    private boolean enabled = true;

    /** 技能执行超时时间(毫秒) */
    private long timeout = 60000;

    /** 默认重试次数 */
    private int retryCount = 3;

    /** 技能执行历史最大条数 */
    private int maxHistorySize = 1000;

    /** 工作区根目录 */
    private String workspaceRoot;

    /** 自动初始化工作区 */
    private boolean autoInitWorkspace = true;

    /** 工作区删除策略 */
    private String deletePolicy = "archive";
}
