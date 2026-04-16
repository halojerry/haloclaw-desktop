package vip.mate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 云端API配置属性
 *
 * @author MateClaw Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "mateclaw.cloud-api")
public class CloudApiProperties {

    /** 云端API地址 */
    private String baseUrl = "https://api.ozon-claw.com";

    /** New API地址 */
    private String newApiUrl = "https://new-api.ozon-claw.com";

    /** 默认租户ID */
    private String tenantId = "ozon-claw-v1";

    /** 连接超时（毫秒） */
    private int connectTimeout = 10000;

    /** 读取超时（毫秒） */
    private int readTimeout = 30000;

    /** 写超时（毫秒） */
    private int writeTimeout = 30000;

    /** 最大连接数 */
    private int maxConnections = 100;

    /** 每路由最大连接数 */
    private int maxPerRoute = 20;

    /** 请求重试次数 */
    private int maxRetries = 3;

    /** 启用重试 */
    private boolean retryEnabled = true;

    /** Token自动刷新 */
    private boolean tokenAutoRefresh = true;

    /** Token刷新阈值（秒），默认5分钟 */
    private long tokenRefreshThreshold = 300;
}
