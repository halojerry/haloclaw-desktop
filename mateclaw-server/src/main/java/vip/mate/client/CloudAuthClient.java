package vip.mate.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import vip.mate.config.CloudApiProperties;

/**
 * 云端认证API客户端
 * 处理登录、注册、Token刷新等认证相关接口
 *
 * @author MateClaw Team
 */
@Slf4j
public class CloudAuthClient extends BaseApiClient {

    private final CloudApiProperties properties;

    public CloudAuthClient(
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

    /**
     * 用户登录
     */
    public ApiResult<AuthResponse> login(String username, String password) {
        return login(username, password, null);
    }

    /**
     * 用户登录（带租户）
     */
    public ApiResult<AuthResponse> login(String username, String password, String tenantId) {
        String url = getBaseUrl() + "/auth/login";

        AuthRequest request = new AuthRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setTenantId(tenantId != null ? tenantId : properties.getTenantId());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url,
                    createHttpEntity(request), String.class);

            return parseResponse(response, AuthResponse.class);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            return ApiResult.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 用户注册
     */
    public ApiResult<AuthResponse> register(String username, String password, String email) {
        String url = getBaseUrl() + "/auth/register";

        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email);
        request.setTenantId(properties.getTenantId());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url,
                    createHttpEntity(request), String.class);

            return parseResponse(response, AuthResponse.class);
        } catch (Exception e) {
            log.error("注册失败: {}", e.getMessage());
            return ApiResult.error("注册失败: " + e.getMessage());
        }
    }

    /**
     * 刷新Token
     */
    public ApiResult<AuthResponse> refreshToken(String refreshToken) {
        String url = getBaseUrl() + "/auth/refresh";

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);
        request.setTenantId(properties.getTenantId());

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url,
                    createHttpEntity(request), String.class);

            return parseResponse(response, AuthResponse.class);
        } catch (Exception e) {
            log.error("刷新Token失败: {}", e.getMessage());
            return ApiResult.error("刷新Token失败: " + e.getMessage());
        }
    }

    /**
     * 验证Token有效性
     */
    public ApiResult<Boolean> validateToken(String token) {
        String url = getBaseUrl() + "/auth/validate";

        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(token);

            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url,
                    org.springframework.http.HttpMethod.POST, entity, String.class);

            return parseResponse(response, Boolean.class);
        } catch (Exception e) {
            log.error("验证Token失败: {}", e.getMessage());
            return ApiResult.error("验证Token失败: " + e.getMessage());
        }
    }

    /**
     * 登出
     */
    public ApiResult<Void> logout(String token) {
        String url = getBaseUrl() + "/auth/logout";

        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(token);

            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url,
                    org.springframework.http.HttpMethod.POST, entity, String.class);

            return parseResponse(response, new TypeReference<Void>() {});
        } catch (Exception e) {
            log.error("登出失败: {}", e.getMessage());
            return ApiResult.error("登出失败: " + e.getMessage());
        }
    }

    /**
     * 发送验证码
     */
    public ApiResult<Void> sendCaptcha(String email) {
        String url = getBaseUrl() + "/auth/captcha/send";

        java.util.Map<String, String> request = new java.util.HashMap<>();
        request.put("email", email);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url,
                    createHttpEntity(request), String.class);

            return parseResponse(response, new TypeReference<Void>() {});
        } catch (Exception e) {
            log.error("发送验证码失败: {}", e.getMessage());
            return ApiResult.error("发送验证码失败: " + e.getMessage());
        }
    }

    // ========== 内部请求类 ==========

    @lombok.Data
    private static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String tenantId;
    }

    @lombok.Data
    private static class RefreshTokenRequest {
        private String refreshToken;
        private String tenantId;
    }
}
