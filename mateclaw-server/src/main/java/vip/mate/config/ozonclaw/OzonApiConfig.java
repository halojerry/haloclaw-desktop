package vip.mate.config.ozonclaw;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ozon API 客户端配置
 * 
 * 管理 Ozon Seller API 的连接参数和限流配置
 * 
 * @author Ozon-Claw Team
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "ozonclaw.ozon")
public class OzonApiConfig {

    /**
     * Ozon Seller API 地址
     */
    private String sellerApi = "https://api-seller.ozon.ru";
    
    /**
     * API 版本
     */
    private String apiVersion = "v3";
    
    /**
     * 限流配置
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

    /**
     * 获取完整的 API 基础 URL
     */
    public String getApiBaseUrl() {
        return sellerApi + "/" + apiVersion;
    }

    /**
     * 获取产品导入端点
     */
    public String getProductImportEndpoint() {
        return getApiBaseUrl() + "/product/import";
    }

    /**
     * 获取产品信息端点
     */
    public String getProductInfoEndpoint() {
        return getApiBaseUrl() + "/product/info/list";
    }

    /**
     * 获取类目树端点
     */
    public String getCategoryTreeEndpoint() {
        return sellerApi + "/v1/description-category/tree";
    }

    /**
     * 获取类目属性端点
     */
    public String getCategoryAttributeEndpoint() {
        return getApiBaseUrl() + "/category/attribute";
    }

    /**
     * 获取图片上传端点
     */
    public String getProductPicturesEndpoint() {
        return sellerApi + "/v1/product/pictures/import";
    }

    /**
     * 获取库存更新端点
     */
    public String getStockUpdateEndpoint() {
        return sellerApi + "/v2/products/stocks";
    }

    /**
     * 获取价格更新端点
     */
    public String getPriceUpdateEndpoint() {
        return sellerApi + "/v1/product/prices/update";
    }
}
