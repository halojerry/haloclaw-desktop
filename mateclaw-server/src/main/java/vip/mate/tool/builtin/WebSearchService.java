package vip.mate.tool.builtin;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.mate.system.model.SystemSettingsDTO;
import vip.mate.system.service.SystemSettingService;

/**
 * 搜索服务：封装 Serper / Tavily 双 provider + fallback 逻辑
 *
 * @author MateClaw Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchService {

    private final SystemSettingService systemSettingService;

    /**
     * 执行搜索，根据系统设置动态选择 provider
     */
    public String search(String query) {
        SystemSettingsDTO config = systemSettingService.getSearchSettings();

        if (!Boolean.TRUE.equals(config.getSearchEnabled())) {
            return "搜索功能已关闭，请在系统设置中启用。";
        }

        String primaryProvider = config.getSearchProvider();
        if (primaryProvider == null || primaryProvider.isBlank()) {
            primaryProvider = "serper";
        }

        // 尝试主 provider
        String result = doSearch(query, primaryProvider, config);
        if (result != null) {
            return result;
        }

        // 主 provider 失败，尝试 fallback
        if (Boolean.TRUE.equals(config.getSearchFallbackEnabled())) {
            String fallbackProvider = "serper".equals(primaryProvider) ? "tavily" : "serper";
            log.info("主搜索提供商 {} 调用失败，回退到 {}", primaryProvider, fallbackProvider);
            result = doSearch(query, fallbackProvider, config);
            if (result != null) {
                return result;
            }
        }

        return "搜索失败：所有搜索提供商均不可用，请在系统设置中检查 API Key 配置。";
    }

    /**
     * 调用指定 provider 执行搜索，失败返回 null
     */
    private String doSearch(String query, String provider, SystemSettingsDTO config) {
        try {
            return switch (provider) {
                case "serper" -> searchWithSerper(query, config);
                case "tavily" -> searchWithTavily(query, config);
                default -> {
                    log.warn("未知的搜索提供商: {}", provider);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("搜索提供商 {} 调用异常: {}", provider, e.getMessage(), e);
            return null;
        }
    }

    private String searchWithSerper(String query, SystemSettingsDTO config) {
        String apiKey = config.getSerperApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Serper API Key 未配置");
            return null;
        }
        String baseUrl = config.getSerperBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://google.serper.dev/search";
        }

        String body = JSONUtil.toJsonStr(new JSONObject().set("q", query).set("num", 5));
        String result = HttpUtil.createPost(baseUrl)
                .header("X-API-KEY", apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .timeout(15000)
                .execute()
                .body();
        log.debug("Serper search result for '{}': {}", query, result);
        return result;
    }

    private String searchWithTavily(String query, SystemSettingsDTO config) {
        String apiKey = config.getTavilyApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Tavily API Key 未配置");
            return null;
        }
        String baseUrl = config.getTavilyBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.tavily.com/search";
        }

        String body = JSONUtil.toJsonStr(new JSONObject()
                .set("query", query)
                .set("max_results", 5)
                .set("api_key", apiKey));
        String result = HttpUtil.createPost(baseUrl)
                .header("Content-Type", "application/json")
                .body(body)
                .timeout(15000)
                .execute()
                .body();
        log.debug("Tavily search result for '{}': {}", query, result);
        return result;
    }
}
