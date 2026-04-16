package vip.mate.service.cloud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.mate.client.*;
import vip.mate.config.CloudApiProperties;

/**
 * 云端认证服务
 * 封装认证相关的业务逻辑，包括登录、Token管理等
 *
 * @author MateClaw Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudAuthService {

    private final CloudAuthClient authClient;
    private final CloudTokenStore tokenStore;
    private final CloudApiProperties properties;

    /**
     * 登录并保存Token
     */
    public CloudTokenInfo login(String username, String password) {
        return login(username, password, null);
    }

    /**
     * 登录并保存Token（带租户）
     */
    public CloudTokenInfo login(String username, String password, String tenantId) {
        String effectiveTenantId = tenantId != null ? tenantId : properties.getTenantId();

        log.info("开始云端登录，用户: {}，租户: {}", username, effectiveTenantId);
        ApiResult<AuthResponse> result = authClient.login(username, password, effectiveTenantId);

        if (!result.isSuccess() || result.getData() == null) {
            log.error("云端登录失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "LOGIN_FAILED", result.getMessage());
        }

        AuthResponse authResponse = result.getData();
        CloudTokenInfo tokenInfo = authResponse.toTokenInfo();
        tokenInfo.setTenantId(effectiveTenantId);
        tokenInfo.setExpiresAt(java.time.LocalDateTime.now().plusSeconds(tokenInfo.getExpiresIn()));

        // 保存Token到本地
        tokenStore.saveToken(effectiveTenantId, tokenInfo);

        log.info("云端登录成功，用户: {}", username);
        return tokenInfo;
    }

    /**
     * 注册新用户
     */
    public CloudTokenInfo register(String username, String password, String email) {
        log.info("开始云端注册，用户: {}", username);
        ApiResult<AuthResponse> result = authClient.register(username, password, email);

        if (!result.isSuccess() || result.getData() == null) {
            log.error("云端注册失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "REGISTER_FAILED", result.getMessage());
        }

        AuthResponse authResponse = result.getData();
        CloudTokenInfo tokenInfo = authResponse.toTokenInfo();
        tokenInfo.setTenantId(properties.getTenantId());
        tokenInfo.setExpiresAt(java.time.LocalDateTime.now().plusSeconds(tokenInfo.getExpiresIn()));

        // 保存Token到本地
        tokenStore.saveToken(properties.getTenantId(), tokenInfo);

        log.info("云端注册成功，用户: {}", username);
        return tokenInfo;
    }

    /**
     * 刷新Token
     */
    public CloudTokenInfo refreshToken() {
        return refreshToken(properties.getTenantId());
    }

    /**
     * 刷新指定租户的Token
     */
    public CloudTokenInfo refreshToken(String tenantId) {
        var tokenOpt = tokenStore.getToken(tenantId);
        if (tokenOpt.isEmpty() || tokenOpt.get().getRefreshToken() == null) {
            log.warn("无法刷新Token：本地无refresh_token");
            throw ApiException.unauthorized("无refresh_token，请重新登录");
        }

        CloudTokenInfo oldToken = tokenOpt.get();
        log.info("开始刷新Token，租户: {}", tenantId);

        ApiResult<AuthResponse> result = authClient.refreshToken(oldToken.getRefreshToken());
        if (!result.isSuccess() || result.getData() == null) {
            log.error("刷新Token失败: {}", result.getMessage());
            // 刷新失败，清除本地Token
            tokenStore.deleteToken(tenantId);
            throw ApiException.unauthorized("Token刷新失败，请重新登录");
        }

        AuthResponse authResponse = result.getData();
        CloudTokenInfo newToken = authResponse.toTokenInfo();
        newToken.setTenantId(tenantId);
        newToken.setExpiresAt(java.time.LocalDateTime.now().plusSeconds(newToken.getExpiresIn()));

        // 保存新Token
        tokenStore.saveToken(tenantId, newToken);

        log.info("Token刷新成功");
        return newToken;
    }

    /**
     * 获取当前有效Token（自动刷新）
     */
    public String getValidToken() {
        return getValidToken(properties.getTenantId());
    }

    /**
     * 获取指定租户的有效Token（自动刷新）
     */
    public String getValidToken(String tenantId) {
        var tokenOpt = tokenStore.getToken(tenantId);
        if (tokenOpt.isEmpty()) {
            throw ApiException.unauthorized("未登录，请先登录");
        }

        CloudTokenInfo token = tokenOpt.get();
        if (!token.isValid()) {
            // Token无效，尝试刷新
            if (token.getRefreshToken() != null && properties.isTokenAutoRefresh()) {
                try {
                    CloudTokenInfo newToken = refreshToken(tenantId);
                    return newToken.getAccessToken();
                } catch (Exception e) {
                    log.error("Token刷新失败: {}", e.getMessage());
                    throw ApiException.tokenExpired();
                }
            }
            throw ApiException.tokenExpired();
        }

        // 如果Token即将过期且配置了自动刷新
        if (token.isNearExpiry() && properties.isTokenAutoRefresh()) {
            try {
                CloudTokenInfo newToken = refreshToken(tenantId);
                return newToken.getAccessToken();
            } catch (Exception e) {
                log.warn("Token自动刷新失败，使用当前Token: {}", e.getMessage());
            }
        }

        return token.getAccessToken();
    }

    /**
     * 登出
     */
    public void logout() {
        logout(properties.getTenantId());
    }

    /**
     * 登出指定租户
     */
    public void logout(String tenantId) {
        try {
            var tokenOpt = tokenStore.getToken(tenantId);
            if (tokenOpt.isPresent()) {
                authClient.logout(tokenOpt.get().getAccessToken());
            }
        } catch (Exception e) {
            log.warn("登出请求失败: {}", e.getMessage());
        } finally {
            tokenStore.deleteToken(tenantId);
            log.info("本地Token已清除，租户: {}", tenantId);
        }
    }

    /**
     * 检查是否已登录
     */
    public boolean isLoggedIn() {
        return isLoggedIn(properties.getTenantId());
    }

    /**
     * 检查指定租户是否已登录
     */
    public boolean isLoggedIn(String tenantId) {
        return tokenStore.hasValidToken(tenantId);
    }

    /**
     * 获取当前登录用户信息
     */
    public AuthResponse.UserInfo getCurrentUser() {
        String token = getValidToken();
        var tokenOpt = tokenStore.getToken(properties.getTenantId());
        if (tokenOpt.isPresent()) {
            CloudTokenInfo tokenInfo = tokenOpt.get();
            if (tokenInfo.getUserId() != null) {
                AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
                userInfo.setId(tokenInfo.getUserId());
                userInfo.setUsername(tokenInfo.getUsername());
                return userInfo;
            }
        }
        return null;
    }

    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        ApiResult<Boolean> result = authClient.validateToken(token);
        return result.isSuccess() && Boolean.TRUE.equals(result.getData());
    }
}
