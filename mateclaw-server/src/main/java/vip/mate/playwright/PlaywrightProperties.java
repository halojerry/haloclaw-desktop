package vip.mate.playwright;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Playwright配置属性类
 *
 * @author mate
 */
@Data
@Component
@ConfigurationProperties(prefix = "mateclaw.playwright")
public class PlaywrightProperties {

    /**
     * 是否启用Playwright
     */
    private boolean enabled = true;

    /**
     * 浏览器类型：chromium, firefox, webkit
     */
    private String browserType = "chromium";

    /**
     * 是否无头模式
     */
    private boolean headless = true;

    /**
     * 默认超时时间（毫秒）
     */
    private long timeout = 30000;

    /**
     * 截图保存目录
     */
    private String screenshotDir = "./screenshots";

    /**
     * 浏览器上下文数量限制
     */
    private int maxContexts = 5;

    /**
     * 是否禁用CSP（内容安全策略）
     */
    private boolean disableCSP = false;

    /**
     * 用户数据目录（持久化浏览器会话）
     */
    private String userDataDir;
}
