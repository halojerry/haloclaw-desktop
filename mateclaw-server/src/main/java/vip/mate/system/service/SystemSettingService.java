package vip.mate.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.system.model.SystemSettingEntity;
import vip.mate.system.model.SystemSettingsDTO;
import vip.mate.system.repository.SystemSettingMapper;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private static final String LANGUAGE_KEY = "language";
    private static final String STREAM_ENABLED_KEY = "streamEnabled";
    private static final String DEBUG_MODE_KEY = "debugMode";
    private static final String STATEGRAPH_ENABLED_KEY = "stateGraphEnabled";

    // 搜索服务配置 keys
    private static final String SEARCH_ENABLED_KEY = "searchEnabled";
    private static final String SEARCH_PROVIDER_KEY = "searchProvider";
    private static final String SEARCH_FALLBACK_ENABLED_KEY = "searchFallbackEnabled";
    private static final String SERPER_API_KEY_KEY = "serperApiKey";
    private static final String SERPER_BASE_URL_KEY = "serperBaseUrl";
    private static final String TAVILY_API_KEY_KEY = "tavilyApiKey";
    private static final String TAVILY_BASE_URL_KEY = "tavilyBaseUrl";
    private static final String DUCKDUCKGO_ENABLED_KEY = "duckduckgoEnabled";
    private static final String SEARXNG_BASE_URL_KEY = "searxngBaseUrl";

    // 视频生成配置 keys
    private static final String VIDEO_ENABLED_KEY = "videoEnabled";
    private static final String VIDEO_PROVIDER_KEY = "videoProvider";
    private static final String VIDEO_FALLBACK_ENABLED_KEY = "videoFallbackEnabled";
    private static final String ZHIPU_API_KEY_KEY = "zhipuApiKey";
    private static final String ZHIPU_BASE_URL_KEY = "zhipuBaseUrl";
    private static final String FAL_API_KEY_KEY = "falApiKey";
    private static final String KLING_ACCESS_KEY_KEY = "klingAccessKey";
    private static final String KLING_SECRET_KEY_KEY = "klingSecretKey";

    private final SystemSettingMapper systemSettingMapper;

    public SystemSettingsDTO getSettings() {
        SystemSettingsDTO dto = new SystemSettingsDTO();
        dto.setLanguage(getValue(LANGUAGE_KEY, "zh-CN"));
        dto.setStreamEnabled(Boolean.parseBoolean(getValue(STREAM_ENABLED_KEY, "true")));
        dto.setDebugMode(Boolean.parseBoolean(getValue(DEBUG_MODE_KEY, "false")));
        dto.setStateGraphEnabled(Boolean.parseBoolean(getValue(STATEGRAPH_ENABLED_KEY, "false")));

        // 搜索服务配置
        dto.setSearchEnabled(Boolean.parseBoolean(getValue(SEARCH_ENABLED_KEY, "true")));
        dto.setSearchProvider(getValue(SEARCH_PROVIDER_KEY, "serper"));
        dto.setSearchFallbackEnabled(Boolean.parseBoolean(getValue(SEARCH_FALLBACK_ENABLED_KEY, "false")));
        dto.setSerperBaseUrl(getValue(SERPER_BASE_URL_KEY, "https://google.serper.dev/search"));
        dto.setTavilyBaseUrl(getValue(TAVILY_BASE_URL_KEY, "https://api.tavily.com/search"));
        // Keyless provider 配置
        dto.setDuckduckgoEnabled(Boolean.parseBoolean(getValue(DUCKDUCKGO_ENABLED_KEY, "true")));
        dto.setSearxngBaseUrl(getValue(SEARXNG_BASE_URL_KEY, ""));
        // API Key 脱敏回显
        dto.setSerperApiKeyMasked(maskApiKey(getValue(SERPER_API_KEY_KEY, "")));
        dto.setTavilyApiKeyMasked(maskApiKey(getValue(TAVILY_API_KEY_KEY, "")));

        // 视频生成配置
        dto.setVideoEnabled(Boolean.parseBoolean(getValue(VIDEO_ENABLED_KEY, "false")));
        dto.setVideoProvider(getValue(VIDEO_PROVIDER_KEY, "auto"));
        dto.setVideoFallbackEnabled(Boolean.parseBoolean(getValue(VIDEO_FALLBACK_ENABLED_KEY, "true")));
        dto.setZhipuBaseUrl(getValue(ZHIPU_BASE_URL_KEY, ""));
        dto.setZhipuApiKeyMasked(maskApiKey(getValue(ZHIPU_API_KEY_KEY, "")));
        dto.setFalApiKeyMasked(maskApiKey(getValue(FAL_API_KEY_KEY, "")));
        dto.setKlingAccessKeyMasked(maskApiKey(getValue(KLING_ACCESS_KEY_KEY, "")));
        dto.setKlingSecretKeyMasked(maskApiKey(getValue(KLING_SECRET_KEY_KEY, "")));
        return dto;
    }

    /**
     * 获取全部配置（内部使用，包含明文 API Key）— 供 VideoGenerationService 等后端服务使用
     */
    public SystemSettingsDTO getAllSettings() {
        SystemSettingsDTO dto = getSettings();
        // 补充搜索明文 Key
        dto.setSerperApiKey(getValue(SERPER_API_KEY_KEY, ""));
        dto.setTavilyApiKey(getValue(TAVILY_API_KEY_KEY, ""));
        // 补充视频明文 Key
        dto.setZhipuApiKey(getValue(ZHIPU_API_KEY_KEY, ""));
        dto.setFalApiKey(getValue(FAL_API_KEY_KEY, ""));
        dto.setKlingAccessKey(getValue(KLING_ACCESS_KEY_KEY, ""));
        dto.setKlingSecretKey(getValue(KLING_SECRET_KEY_KEY, ""));
        return dto;
    }

    /**
     * 获取搜索配置（内部使用，包含明文 API Key）
     */
    public SystemSettingsDTO getSearchSettings() {
        SystemSettingsDTO dto = new SystemSettingsDTO();
        dto.setSearchEnabled(Boolean.parseBoolean(getValue(SEARCH_ENABLED_KEY, "true")));
        dto.setSearchProvider(getValue(SEARCH_PROVIDER_KEY, "serper"));
        dto.setSearchFallbackEnabled(Boolean.parseBoolean(getValue(SEARCH_FALLBACK_ENABLED_KEY, "false")));
        dto.setSerperApiKey(getValue(SERPER_API_KEY_KEY, ""));
        dto.setSerperBaseUrl(getValue(SERPER_BASE_URL_KEY, "https://google.serper.dev/search"));
        dto.setTavilyApiKey(getValue(TAVILY_API_KEY_KEY, ""));
        dto.setTavilyBaseUrl(getValue(TAVILY_BASE_URL_KEY, "https://api.tavily.com/search"));
        dto.setDuckduckgoEnabled(Boolean.parseBoolean(getValue(DUCKDUCKGO_ENABLED_KEY, "true")));
        dto.setSearxngBaseUrl(getValue(SEARXNG_BASE_URL_KEY, ""));
        return dto;
    }

    public SystemSettingsDTO saveSettings(SystemSettingsDTO dto) {
        saveValue(LANGUAGE_KEY, dto.getLanguage(), "当前界面语言");
        saveValue(STREAM_ENABLED_KEY, String.valueOf(Boolean.TRUE.equals(dto.getStreamEnabled())), "是否开启流式响应");
        saveValue(DEBUG_MODE_KEY, String.valueOf(Boolean.TRUE.equals(dto.getDebugMode())), "是否开启调试模式");
        saveValue(STATEGRAPH_ENABLED_KEY, String.valueOf(Boolean.TRUE.equals(dto.getStateGraphEnabled())), "启用 StateGraph 架构的 ReAct Agent");

        // 搜索服务配置
        if (dto.getSearchEnabled() != null) {
            saveValue(SEARCH_ENABLED_KEY, String.valueOf(dto.getSearchEnabled()), "是否启用搜索功能");
        }
        if (dto.getSearchProvider() != null) {
            saveValue(SEARCH_PROVIDER_KEY, dto.getSearchProvider(), "搜索服务提供商");
        }
        if (dto.getSearchFallbackEnabled() != null) {
            saveValue(SEARCH_FALLBACK_ENABLED_KEY, String.valueOf(dto.getSearchFallbackEnabled()), "搜索失败时是否回退到备用提供商");
        }
        // API Key 仅在非空时保存（前端不回传明文，避免覆盖为空）
        if (dto.getSerperApiKey() != null && !dto.getSerperApiKey().isBlank()) {
            saveValue(SERPER_API_KEY_KEY, dto.getSerperApiKey(), "Serper API Key");
        }
        if (dto.getSerperBaseUrl() != null) {
            saveValue(SERPER_BASE_URL_KEY, dto.getSerperBaseUrl(), "Serper 接口地址");
        }
        if (dto.getTavilyApiKey() != null && !dto.getTavilyApiKey().isBlank()) {
            saveValue(TAVILY_API_KEY_KEY, dto.getTavilyApiKey(), "Tavily API Key");
        }
        if (dto.getTavilyBaseUrl() != null) {
            saveValue(TAVILY_BASE_URL_KEY, dto.getTavilyBaseUrl(), "Tavily 接口地址");
        }
        // Keyless provider 配置
        if (dto.getDuckduckgoEnabled() != null) {
            saveValue(DUCKDUCKGO_ENABLED_KEY, String.valueOf(dto.getDuckduckgoEnabled()), "DuckDuckGo 免 Key 搜索（零配置兜底）");
        }
        if (dto.getSearxngBaseUrl() != null) {
            saveValue(SEARXNG_BASE_URL_KEY, dto.getSearxngBaseUrl(), "SearXNG 实例地址");
        }

        // 视频生成配置
        if (dto.getVideoEnabled() != null) {
            saveValue(VIDEO_ENABLED_KEY, String.valueOf(dto.getVideoEnabled()), "是否启用视频生成");
        }
        if (dto.getVideoProvider() != null) {
            saveValue(VIDEO_PROVIDER_KEY, dto.getVideoProvider(), "视频生成首选 Provider");
        }
        if (dto.getVideoFallbackEnabled() != null) {
            saveValue(VIDEO_FALLBACK_ENABLED_KEY, String.valueOf(dto.getVideoFallbackEnabled()), "视频 Provider 级 Fallback");
        }
        if (dto.getZhipuApiKey() != null && !dto.getZhipuApiKey().isBlank()) {
            saveValue(ZHIPU_API_KEY_KEY, dto.getZhipuApiKey(), "智谱 CogVideo API Key");
        }
        if (dto.getZhipuBaseUrl() != null) {
            saveValue(ZHIPU_BASE_URL_KEY, dto.getZhipuBaseUrl(), "智谱 API Base URL");
        }
        if (dto.getFalApiKey() != null && !dto.getFalApiKey().isBlank()) {
            saveValue(FAL_API_KEY_KEY, dto.getFalApiKey(), "fal.ai API Key");
        }
        if (dto.getKlingAccessKey() != null && !dto.getKlingAccessKey().isBlank()) {
            saveValue(KLING_ACCESS_KEY_KEY, dto.getKlingAccessKey(), "快手可灵 Access Key");
        }
        if (dto.getKlingSecretKey() != null && !dto.getKlingSecretKey().isBlank()) {
            saveValue(KLING_SECRET_KEY_KEY, dto.getKlingSecretKey(), "快手可灵 Secret Key");
        }
        return getSettings();
    }

    public String getLanguage() {
        return getValue(LANGUAGE_KEY, "zh-CN");
    }

    public String saveLanguage(String language) {
        saveValue(LANGUAGE_KEY, language, "当前界面语言");
        return getLanguage();
    }

    public boolean isStateGraphEnabled() {
        return Boolean.parseBoolean(getValue(STATEGRAPH_ENABLED_KEY, "false"));
    }

    private String getValue(String key, String defaultValue) {
        SystemSettingEntity entity = systemSettingMapper.selectOne(new LambdaQueryWrapper<SystemSettingEntity>()
                .eq(SystemSettingEntity::getSettingKey, key)
                .last("LIMIT 1"));
        return entity != null && entity.getSettingValue() != null ? entity.getSettingValue() : defaultValue;
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        if (apiKey.length() <= 4) {
            return "****";
        }
        return "****" + apiKey.substring(apiKey.length() - 4);
    }

    private void saveValue(String key, String value, String description) {
        SystemSettingEntity entity = systemSettingMapper.selectOne(new LambdaQueryWrapper<SystemSettingEntity>()
                .eq(SystemSettingEntity::getSettingKey, key)
                .last("LIMIT 1"));
        if (entity == null) {
            entity = new SystemSettingEntity();
            entity.setSettingKey(key);
            entity.setDescription(description);
            entity.setSettingValue(value);
            systemSettingMapper.insert(entity);
            return;
        }
        entity.setSettingValue(value);
        entity.setDescription(description);
        systemSettingMapper.updateById(entity);
    }
}
