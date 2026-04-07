package vip.mate.system.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SystemSettingsDTO {
    private String language;
    private Boolean streamEnabled;
    private Boolean debugMode;
    private Boolean stateGraphEnabled;

    // ===== 搜索服务配置 =====
    private Boolean searchEnabled;
    /** serper / tavily */
    private String searchProvider;
    private Boolean searchFallbackEnabled;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String serperApiKey;
    private String serperBaseUrl;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String tavilyApiKey;
    private String tavilyBaseUrl;

    // ===== Keyless 搜索 provider 配置 =====
    /** DuckDuckGo 是否启用（默认 true，作为零配置兜底） */
    private Boolean duckduckgoEnabled;
    /** SearXNG 实例地址（如 http://searxng:8080），为空则不使用 */
    private String searxngBaseUrl;

    // 用于前端回显脱敏后的 API Key
    private String serperApiKeyMasked;
    private String tavilyApiKeyMasked;

    // ===== 视频生成配置 =====
    /** 是否启用视频生成能力 */
    private Boolean videoEnabled;
    /** 首选视频 provider: auto / dashscope / zhipu-cogvideo / fal / kling */
    private String videoProvider;
    /** 是否启用 provider 级 fallback */
    private Boolean videoFallbackEnabled;

    // --- 智谱 CogVideo ---
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String zhipuApiKey;
    private String zhipuBaseUrl;
    private String zhipuApiKeyMasked;

    // --- fal.ai ---
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String falApiKey;
    private String falApiKeyMasked;

    // --- 快手可灵 Kling ---
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String klingAccessKey;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String klingSecretKey;
    private String klingAccessKeyMasked;
    private String klingSecretKeyMasked;
}
