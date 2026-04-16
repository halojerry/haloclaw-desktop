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

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 云端用户API客户端
 * 处理用户信息、设置等接口
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
public class CloudUserClient extends BaseApiClient {

    private final CloudApiProperties properties;

    public CloudUserClient(
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

    // ========== 用户信息接口 ==========

    /**
     * 获取当前用户信息
     */
    public ApiResult<UserInfo> getCurrentUser(String token) {
        String url = getBaseUrl() + "/api/users/me";
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(null, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, String.class);
            return parseResponse(response, UserInfo.class);
        });
    }

    /**
     * 更新用户信息
     */
    public ApiResult<UserInfo> updateUser(String token, UserUpdateRequest request) {
        String url = getBaseUrl() + "/api/users/me";
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(request, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.PUT, entity, String.class);
            return parseResponse(response, UserInfo.class);
        });
    }

    /**
     * 修改密码
     */
    public ApiResult<Void> changePassword(String token, ChangePasswordRequest request) {
        String url = getBaseUrl() + "/api/users/password";
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(request, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.POST, entity, String.class);
            return parseResponse(response, new TypeReference<Void>() {});
        });
    }

    /**
     * 上传头像
     */
    public ApiResult<String> uploadAvatar(String token, byte[] imageData, String fileName) {
        String url = getBaseUrl() + "/api/users/avatar";
        return execute(() -> {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("multipart/form-data"));
            headers.setBearerAuth(token);

            org.springframework.web.client.RestTemplate multipartTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpEntity<byte[]> entity = new org.springframework.http.HttpEntity<>(imageData, headers);

            org.springframework.http.ResponseEntity<String> response = multipartTemplate.postForEntity(
                    url, entity, String.class);
            return parseResponse(response, String.class);
        });
    }

    // ========== 用户设置接口 ==========

    /**
     * 获取用户设置
     */
    public ApiResult<Map<String, Object>> getSettings(String token) {
        String url = getBaseUrl() + "/api/users/settings";
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(null, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, String.class);
            return parseResponse(response, new TypeReference<Map<String, Object>>() {});
        });
    }

    /**
     * 更新用户设置
     */
    public ApiResult<Void> updateSettings(String token, Map<String, Object> settings) {
        String url = getBaseUrl() + "/api/users/settings";
        return execute(() -> {
            org.springframework.http.HttpEntity<?> entity = createHttpEntityWithToken(settings, token);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.PUT, entity, String.class);
            return parseResponse(response, new TypeReference<Void>() {});
        });
    }

    // ========== 数据模型 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String nickname;
        private String email;
        private String avatar;
        private String role;
        private Map<String, Object> settings;
        private LocalDateTime createTime;
    }

    @Data
    public static class UserUpdateRequest {
        private String nickname;
        private String email;
        private String avatar;
    }

    @lombok.Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
