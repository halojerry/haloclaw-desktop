package vip.mate.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import vip.mate.client.CloudTokenStore;

/**
 * 云API客户端初始化器
 * 应用启动后初始化Token存储等组件
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudApiInitializer {

    private final CloudTokenStore tokenStore;
    private final CloudApiProperties cloudApiProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=== 云API客户端初始化开始 ===");
        log.info("云端API地址: {}", cloudApiProperties.getBaseUrl());
        log.info("New API地址: {}", cloudApiProperties.getNewApiUrl());
        log.info("默认租户ID: {}", cloudApiProperties.getTenantId());
        log.info("连接超时: {}ms", cloudApiProperties.getConnectTimeout());
        log.info("读取超时: {}ms", cloudApiProperties.getReadTimeout());
        log.info("Token自动刷新: {}", cloudApiProperties.isTokenAutoRefresh() ? "启用" : "禁用");
        log.info("请求重试: {}", cloudApiProperties.isRetryEnabled() ? "启用" : "禁用");

        // 初始化Token存储表
        try {
            tokenStore.initTable();
            log.info("Token存储表初始化成功");
        } catch (Exception e) {
            log.warn("Token存储表初始化失败（可能已存在）: {}", e.getMessage());
        }

        log.info("=== 云API客户端初始化完成 ===");
    }
}
