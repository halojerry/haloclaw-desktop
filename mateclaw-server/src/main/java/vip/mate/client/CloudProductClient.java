package vip.mate.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import vip.mate.config.CloudApiProperties;

import java.util.List;
import java.util.Map;

/**
 * 云端商品API客户端
 * 处理商品同步、上下架等接口
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
public class CloudProductClient extends BaseApiClient {

    private final CloudApiProperties properties;

    public CloudProductClient(
            @Qualifier("cloudApiRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CloudApiProperties properties) {
        super(restTemplate, objectMapper);
        this.properties = properties;
    }

    @Override
    protected String getBaseUrl() {
        return properties.getBaseUrl();
    }

    // ========== 商品管理接口 ==========

    /**
     * 获取商品列表
     */
    public ApiResult<PagedResult<ProductInfo>> getProducts(String token, ProductQuery query) {
        String url = getBaseUrl() + "/api/products";
        Map<String, Object> params = query.toMap();
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(null, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    buildUrl(url, params), org.springframework.http.HttpMethod.GET, entity, String.class);
            return parseResponse(response, new TypeReference<PagedResult<ProductInfo>>() {});
        });
    }

    /**
     * 获取商品详情
     */
    public ApiResult<ProductInfo> getProduct(String token, Long productId) {
        String url = getBaseUrl() + "/api/products/" + productId;
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(null, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, String.class);
            return parseResponse(response, ProductInfo.class);
        });
    }

    /**
     * 同步商品到云端
     */
    public ApiResult<ProductSyncResult> syncProduct(String token, ProductSyncRequest request) {
        String url = getBaseUrl() + "/api/products/sync";
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(request, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.POST, entity, String.class);
            return parseResponse(response, ProductSyncResult.class);
        });
    }

    /**
     * 批量同步商品
     */
    public ApiResult<List<ProductSyncResult>> batchSyncProducts(String token, List<ProductSyncRequest> requests) {
        String url = getBaseUrl() + "/api/products/sync/batch";
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(requests, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.POST, entity, String.class);
            return parseResponse(response, new TypeReference<List<ProductSyncResult>>() {});
        });
    }

    /**
     * 发布商品到Ozon
     */
    public ApiResult<PublishResult> publishToOzon(String token, Long productId, Long storeId) {
        String url = getBaseUrl() + "/api/products/" + productId + "/publish/ozon";
        Map<String, Object> params = Map.of("storeId", storeId);
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(null, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    buildUrl(url, params), org.springframework.http.HttpMethod.POST, entity, String.class);
            return parseResponse(response, PublishResult.class);
        });
    }

    /**
     * 批量发布商品
     */
    public ApiResult<List<PublishResult>> batchPublishToOzon(String token, List<Long> productIds, Long storeId) {
        String url = getBaseUrl() + "/api/products/publish/ozon/batch";
        Map<String, Object> body = Map.of("productIds", productIds, "storeId", storeId);
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(body, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.POST, entity, String.class);
            return parseResponse(response, new TypeReference<List<PublishResult>>() {});
        });
    }

    /**
     * 获取商品发布状态
     */
    public ApiResult<PublishResult> getPublishStatus(String token, String taskId) {
        String url = getBaseUrl() + "/api/products/publish/status/" + taskId;
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(null, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, String.class);
            return parseResponse(response, PublishResult.class);
        });
    }

    /**
     * 删除商品
     */
    public ApiResult<Void> deleteProduct(String token, Long productId) {
        String url = getBaseUrl() + "/api/products/" + productId;
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(null, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.DELETE, entity, String.class);
            return parseResponse(response, new TypeReference<Void>() {});
        });
    }

    // ========== 数据模型 ==========

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProductInfo {
        private Long id;
        private String sku;
        private String name;
        private String description;
        private Double price;
        private String currency;
        private Long storeId;
        private String ozonProductId;
        private String status; // pending, synced, published, failed
        private List<String> images;
        private Map<String, Object> attributes;
        private java.time.LocalDateTime createTime;
        private java.time.LocalDateTime updateTime;
    }

    @lombok.Data
    public static class ProductQuery {
        private Long storeId;
        private String status;
        private String keyword;
        private Integer page = 1;
        private Integer pageSize = 20;

        public Map<String, Object> toMap() {
            java.util.HashMap<String, Object> map = new java.util.HashMap<>();
            if (storeId != null) map.put("storeId", storeId);
            if (status != null) map.put("status", status);
            if (keyword != null) map.put("keyword", keyword);
            map.put("page", page);
            map.put("pageSize", pageSize);
            return map;
        }
    }

    @lombok.Data
    public static class ProductSyncRequest {
        private Long storeId;
        private String sku;
        private String name;
        private String description;
        private Double price;
        private String currency;
        private List<String> images;
        private Map<String, Object> attributes;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProductSyncResult {
        private Long localProductId;
        private Long cloudProductId;
        private String sku;
        private boolean success;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PublishResult {
        private String taskId;
        private Long productId;
        private String ozonProductId;
        private String status; // pending, processing, completed, failed
        private String message;
        private java.time.LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PagedResult<T> {
        private List<T> items;
        private long total;
        private int page;
        private int pageSize;
        private int totalPages;
    }
}
