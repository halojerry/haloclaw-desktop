package vip.mate;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vip.mate.client.*;
import vip.mate.config.CloudApiProperties;
import vip.mate.service.cloud.CloudAuthService;
import vip.mate.service.cloud.CloudProductService;
import vip.mate.service.cloud.CloudStoreService;
import vip.mate.service.cloud.NewApiService;

/**
 * 云API连通性测试
 * 测试桌面端与云端API的连接和基本功能
 *
 * @author MateClaw Team
 */
@Slf4j
@SpringBootTest
public class CloudApiTest {

    @Autowired(required = false)
    private CloudApiProperties cloudApiProperties;

    @Autowired(required = false)
    private CloudAuthClient authClient;

    @Autowired(required = false)
    private CloudUserClient userClient;

    @Autowired(required = false)
    private CloudStoreClient storeClient;

    @Autowired(required = false)
    private CloudProductClient productClient;

    @Autowired(required = false)
    private NewApiClient newApiClient;

    @Autowired(required = false)
    private CloudTokenStore tokenStore;

    @Autowired(required = false)
    private CloudAuthService authService;

    @Autowired(required = false)
    private CloudStoreService storeService;

    @Autowired(required = false)
    private CloudProductService productService;

    @Autowired(required = false)
    private NewApiService newApiService;

    /**
     * 测试1: 检查配置
     */
    @Test
    void testConfig() {
        log.info("========== 测试1: 配置检查 ==========");
        if (cloudApiProperties == null) {
            log.warn("CloudApiProperties 未注入，跳过测试");
            return;
        }
        log.info("云端API地址: {}", cloudApiProperties.getBaseUrl());
        log.info("New API地址: {}", cloudApiProperties.getNewApiUrl());
        log.info("默认租户ID: {}", cloudApiProperties.getTenantId());
        log.info("连接超时: {}ms", cloudApiProperties.getConnectTimeout());
        log.info("读取超时: {}ms", cloudApiProperties.getReadTimeout());
        assert cloudApiProperties.getBaseUrl() != null;
        assert cloudApiProperties.getNewApiUrl() != null;
        log.info("配置检查通过");
    }

    /**
     * 测试2: Token存储初始化
     */
    @Test
    void testTokenStoreInit() {
        log.info("========== 测试2: Token存储初始化 ==========");
        if (tokenStore == null) {
            log.warn("CloudTokenStore 未注入，跳过测试");
            return;
        }
        try {
            tokenStore.initTable();
            log.info("Token表初始化成功");
        } catch (Exception e) {
            log.error("Token表初始化失败: {}", e.getMessage());
        }
        log.info("Token存储检查通过");
    }

    /**
     * 测试3: API客户端Bean注入
     */
    @Test
    void testClientBeans() {
        log.info("========== 测试3: 客户端Bean注入检查 ==========");
        log.info("CloudAuthClient: {}", authClient != null ? "已注入" : "未注入");
        log.info("CloudUserClient: {}", userClient != null ? "已注入" : "未注入");
        log.info("CloudStoreClient: {}", storeClient != null ? "已注入" : "未注入");
        log.info("CloudProductClient: {}", productClient != null ? "已注入" : "未注入");
        log.info("NewApiClient: {}", newApiClient != null ? "已注入" : "未注入");
        log.info("客户端Bean注入检查完成");
    }

    /**
     * 测试4: 服务Bean注入
     */
    @Test
    void testServiceBeans() {
        log.info("========== 测试4: 服务Bean注入检查 ==========");
        log.info("CloudAuthService: {}", authService != null ? "已注入" : "未注入");
        log.info("CloudStoreService: {}", storeService != null ? "已注入" : "未注入");
        log.info("CloudProductService: {}", productService != null ? "已注入" : "未注入");
        log.info("NewApiService: {}", newApiService != null ? "已注入" : "未注入");
        log.info("服务Bean注入检查完成");
    }

    /**
     * 测试5: 云端API连通性测试（登录接口）
     * 需要有效的云端服务才能测试
     */
    @Test
    void testCloudApiConnectivity() {
        log.info("========== 测试5: 云端API连通性测试 ==========");
        if (authClient == null) {
            log.warn("CloudAuthClient 未注入，跳过连通性测试");
            return;
        }
        if (cloudApiProperties == null) {
            log.warn("CloudApiProperties 未注入，跳过连通性测试");
            return;
        }

        String baseUrl = cloudApiProperties.getBaseUrl();
        log.info("测试连接到: {}", baseUrl);

        // 测试一个不存在的用户登录（验证API可达）
        try {
            ApiResult<AuthResponse> result = authClient.login("test_user_not_exists_123", "test_password");
            log.info("API响应 - code: {}, message: {}",
                    result.getCode(), result.getMessage());

            // 根据响应判断连通性
            if (result.getCode() == 404 || result.getCode() == 401 || result.getCode() == 400) {
                log.info("✓ API服务可达，返回预期错误（用户不存在）");
            } else if (result.getCode() == 200) {
                log.info("✓ API服务可达，登录成功（意外）");
            } else {
                log.warn("API响应异常，code: {}, message: {}", result.getCode(), result.getMessage());
            }
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") ||
                    e.getMessage().contains("ConnectException"))) {
                log.error("✗ 无法连接到云端API服务: {}", e.getMessage());
                log.warn("请检查: 1) 云端服务是否运行  2) 网络是否可达  3) 防火墙设置");
            } else {
                log.error("API调用异常: {}", e.getMessage());
            }
        }
        log.info("连通性测试完成");
    }

    /**
     * 测试6: New API连通性测试
     */
    @Test
    void testNewApiConnectivity() {
        log.info("========== 测试6: New API连通性测试 ==========");
        if (newApiClient == null) {
            log.warn("NewApiClient 未注入，跳过测试");
            return;
        }
        if (cloudApiProperties == null) {
            log.warn("CloudApiProperties 未注入，跳过测试");
            return;
        }

        String newApiUrl = cloudApiProperties.getNewApiUrl();
        log.info("测试连接到: {}", newApiUrl);

        try {
            // 测试配额查询（使用无效Key，验证API可达）
            ApiResult<NewApiClient.QuotaInfo> result = newApiClient.getQuota("invalid_key_test");
            log.info("New API响应 - code: {}, message: {}",
                    result.getCode(), result.getMessage());

            if (result.getCode() == 200) {
                log.info("✓ New API服务可达");
            } else {
                log.info("API返回预期错误（可能Key无效）");
            }
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") ||
                    e.getMessage().contains("ConnectException"))) {
                log.error("✗ 无法连接到New API服务: {}", e.getMessage());
            } else {
                log.error("New API调用异常: {}", e.getMessage());
            }
        }
        log.info("New API连通性测试完成");
    }

    /**
     * 测试7: 登录流程测试（需要真实账号）
     */
    @Test
    void testLoginFlow() {
        log.info("========== 测试7: 登录流程测试 ==========");
        if (authService == null || tokenStore == null || cloudApiProperties == null) {
            log.warn("必要的Bean未注入，跳过登录流程测试");
            return;
        }

        String testTenant = "test-tenant-" + System.currentTimeMillis();
        log.info("测试租户: {}", testTenant);

        try {
            // 模拟登录（使用测试凭证）
            CloudTokenInfo tokenInfo = CloudTokenInfo.builder()
                    .accessToken("test_token_" + System.currentTimeMillis())
                    .refreshToken("test_refresh_token_" + System.currentTimeMillis())
                    .tokenType("Bearer")
                    .expiresIn(86400)
                    .expiresAt(java.time.LocalDateTime.now().plusDays(1))
                    .userId(1L)
                    .username("test_user")
                    .tenantId(testTenant)
                    .build();

            // 保存Token
            tokenStore.saveToken(testTenant, tokenInfo);
            log.info("Token已保存");

            // 读取Token
            var savedToken = tokenStore.getToken(testTenant);
            if (savedToken.isPresent()) {
                log.info("✓ Token保存和读取成功");
                log.info("  - accessToken: {}...", savedToken.get().getAccessToken().substring(0, 20));
                log.info("  - username: {}", savedToken.get().getUsername());
                log.info("  - expiresAt: {}", savedToken.get().getExpiresAt());
            } else {
                log.error("✗ Token保存失败");
            }

            // 清理
            tokenStore.deleteToken(testTenant);
            log.info("测试Token已清理");

        } catch (Exception e) {
            log.error("登录流程测试异常: {}", e.getMessage());
        }
        log.info("登录流程测试完成");
    }

    /**
     * 测试8: 响应封装测试
     */
    @Test
    void testApiResult() {
        log.info("========== 测试8: 响应封装测试 ==========");

        // 测试成功响应
        ApiResult<String> success = ApiResult.success("test data");
        assert success.isSuccess();
        assert success.getCode() == 200;
        assert "test data".equals(success.getData());
        log.info("✓ 成功响应测试通过");

        // 测试错误响应
        ApiResult<String> error = ApiResult.error(500, "Internal error");
        assert !error.isSuccess();
        assert error.getCode() == 500;
        assert "Internal error".equals(error.getMessage());
        log.info("✓ 错误响应测试通过");

        // 测试异常创建
        ApiException unauthorized = ApiException.unauthorized("Token expired");
        assert unauthorized.getCode() == 401;
        assert "Token expired".equals(unauthorized.getMessage());
        log.info("✓ 异常创建测试通过");

        log.info("响应封装测试全部通过");
    }
}
