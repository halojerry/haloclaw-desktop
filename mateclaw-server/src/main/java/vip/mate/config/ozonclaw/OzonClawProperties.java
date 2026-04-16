package vip.mate.config.ozonclaw;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ozon-Claw 客户端业务配置属性
 * 
 * 读取 application-ozonclaw.yml 中的 ozonclaw 配置
 * 
 * @author Ozon-Claw Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "ozonclaw")
public class OzonClawProperties {

    /**
     * 客户端标识配置
     */
    private Client client = new Client();

    /**
     * API 配置
     */
    private Api api = new Api();

    /**
     * New API 配置
     */
    private NewApi newApi = new NewApi();

    /**
     * Ozon 平台配置
     */
    private Ozon ozon = new Ozon();

    @Data
    public static class Client {
        /**
         * 客户端版本号
         */
        private String version = "1.0.0";
        
        /**
         * 构建标识
         */
        private String build = "ozon-claw-desktop";
        
        /**
         * 发布渠道
         */
        private String channel = "production";
    }

    @Data
    public static class Api {
        /**
         * API 基础地址
         */
        private String baseUrl = "https://api.ozon-claw.com";
        
        /**
         * 租户ID
         */
        private String tenantId = "ozon-claw-v1";
    }

    @Data
    public static class NewApi {
        /**
         * New API 基础地址
         */
        private String baseUrl = "https://new-api.ozon-claw.com";
    }

    @Data
    public static class Ozon {
        /**
         * Ozon Seller API 地址
         */
        private String sellerApi = "https://api-seller.ozon.ru";
        
        /**
         * API 版本
         */
        private String apiVersion = "v3";
        
        /**
         * API 限流配置
         */
        private RateLimit rateLimit = new RateLimit();
        
        @Data
        public static class RateLimit {
            /**
             * 每秒最大请求数
             */
            private int maxRequestsPerSecond = 10;
            
            /**
             * 每分钟最大请求数
             */
            private int maxRequestsPerMinute = 100;
            
            /**
             * 重试延迟（毫秒）
             */
            private int retryDelayMs = 5000;
            
            /**
             * 最大重试次数
             */
            private int maxRetries = 3;
        }
    }
}
