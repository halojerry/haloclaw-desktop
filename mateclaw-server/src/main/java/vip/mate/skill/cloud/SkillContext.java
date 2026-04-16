package vip.mate.skill.cloud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 技能执行上下文
 * 包含执行技能所需的用户信息、店铺信息和配置参数
 *
 * @author MateClaw Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillContext {

    /** 用户Token */
    private String token;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 店铺ID */
    private Long storeId;

    /** 店铺名称 */
    private String storeName;

    /** 店铺平台 (ozon/wb/other) */
    private String storePlatform;

    /** 技能类型 */
    private String skillType;

    /** 技能名称 */
    private String skillName;

    /** 执行参数 */
    private Map<String, Object> params;

    /** 执行开始时间 */
    @Builder.Default
    private LocalDateTime startTime = LocalDateTime.now();

    /** 请求ID，用于日志追踪 */
    private String requestId;

    /** New API Key (可选，用于AI调用) */
    private String newApiKey;

    /** 语言设置 */
    @Builder.Default
    private String language = "zh";

    /**
     * 获取店铺平台前缀
     */
    public String getPlatformPrefix() {
        if (storePlatform == null) {
            return "unknown";
        }
        return storePlatform.toLowerCase();
    }

    /**
     * 检查是否指定了店铺
     */
    public boolean hasStore() {
        return storeId != null && storeId > 0;
    }

    /**
     * 检查是否指定了用户
     */
    public boolean hasUser() {
        return userId != null && userId > 0;
    }

    /**
     * 获取参数值
     */
    @SuppressWarnings("unchecked")
    public <T> T getParam(String key, T defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        Object value = params.get(key);
        if (defaultValue != null && value != null) {
            if (value instanceof Number && defaultValue instanceof Number) {
                return (T) value;
            }
        }
        return (T) value;
    }

    /**
     * 获取字符串参数
     */
    public String getStringParam(String key) {
        Object value = params != null ? params.get(key) : null;
        return value != null ? value.toString() : null;
    }

    /**
     * 获取数值参数
     */
    public Double getNumberParam(String key) {
        Object value = params != null ? params.get(key) : null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取Long参数
     */
    public Long getLongParam(String key) {
        Object value = params != null ? params.get(key) : null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取通用参数
     */
    public Object getParam(String key) {
        return params != null ? params.get(key) : null;
    }
}
