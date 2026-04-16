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
 * 云端店铺API客户端
 * 处理店铺信息、Ozon店铺绑定等接口
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
public class CloudStoreClient extends BaseApiClient {

    private final CloudApiProperties properties;

    public CloudStoreClient(
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

    // ========== 店铺管理接口 ==========

    /**
     * 获取店铺列表
     */
    public ApiResult<List<StoreInfo>> getStores(String token) {
        String url = getBaseUrl() + "/api/stores";
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(null, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, String.class);
            return parseResponse(response, new TypeReference<List<StoreInfo>>() {});
        });
    }

    /**
     * 获取店铺详情
     */
    public ApiResult<StoreInfo> getStore(String token, Long storeId) {
        String url = getBaseUrl() + "/api/stores/" + storeId;
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(null, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, String.class);
            return parseResponse(response, StoreInfo.class);
        });
    }

    /**
     * 创建店铺
     */
    public ApiResult<StoreInfo> createStore(String token, StoreCreateRequest request) {
        String url = getBaseUrl() + "/api/stores";
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(request, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.POST, entity, String.class);
            return parseResponse(response, StoreInfo.class);
        });
    }

    /**
     * 更新店铺
     */
    public ApiResult<StoreInfo> updateStore(String token, Long storeId, StoreUpdateRequest request) {
        String url = getBaseUrl() + "/api/stores/" + storeId;
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(request, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.PUT, entity, String.class);
            return parseResponse(response, StoreInfo.class);
        });
    }

    /**
     * 删除店铺
     */
    public ApiResult<Void> deleteStore(String token, Long storeId) {
        String url = getBaseUrl() + "/api/stores/" + storeId;
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(null, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.DELETE, entity, String.class);
            return parseResponse(response, new TypeReference<Void>() {});
        });
    }

    // ========== Ozon店铺绑定接口 ==========

    /**
     * 绑定Ozon店铺
     */
    public ApiResult<StoreInfo> bindOzonStore(String token, OzonBindRequest request) {
        String url = getBaseUrl() + "/api/stores/ozon/bind";
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(request, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.POST, entity, String.class);
            return parseResponse(response, StoreInfo.class);
        });
    }

    /**
     * 获取Ozon店铺凭证
     */
    public ApiResult<OzonCredentials> getOzonCredentials(String token, Long storeId) {
        String url = getBaseUrl() + "/api/stores/" + storeId + "/ozon/credentials";
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(null, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, String.class);
            return parseResponse(response, OzonCredentials.class);
        });
    }

    /**
     * 刷新Ozon凭证
     */
    public ApiResult<OzonCredentials> refreshOzonCredentials(String token, Long storeId) {
        String url = getBaseUrl() + "/api/stores/" + storeId + "/ozon/credentials/refresh";
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(null, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.POST, entity, String.class);
            return parseResponse(response, OzonCredentials.class);
        });
    }

    // ========== 数据模型 ==========

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class StoreInfo {
        private Long id;
        private String name;
        private String type; // ozon, wildberries, aliexpress, etc.
        private String status; // active, inactive, error
        private Map<String, Object> credentials;
        private Map<String, Object> settings;
        private java.time.LocalDateTime createTime;
        private java.time.LocalDateTime updateTime;
    }

    @lombok.Data
    public static class StoreCreateRequest {
        private String name;
        private String type;
        private Map<String, Object> credentials;
        private Map<String, Object> settings;
    }

    @lombok.Data
    public static class StoreUpdateRequest {
        private String name;
        private Map<String, Object> credentials;
        private Map<String, Object> settings;
    }

    @lombok.Data
    public static class OzonBindRequest {
        private Long storeId;
        private String clientId;
        private String apiKey;
        private String name;
    }

    @lombok.Data
    public static class OzonCredentials {
        private String clientId;
        private String apiKey;
        private java.time.LocalDateTime expiresAt;
        private String status;
    }
}
