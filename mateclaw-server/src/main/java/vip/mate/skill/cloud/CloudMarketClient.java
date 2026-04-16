package vip.mate.skill.cloud;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import vip.mate.client.ApiResult;
import vip.mate.config.CloudApiProperties;

import java.util.List;
import java.util.Map;

/**
 * 云端市场API客户端
 * 调用云端市场趋势、竞品数据等接口
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudMarketClient {

    private final CloudApiProperties properties;
    private final ObjectMapper objectMapper;
    @Qualifier("cloudApiRestTemplate")
    private final RestTemplate restTemplate;

    /**
     * 获取市场趋势数据
     */
    public ApiResult<MarketTrends> getMarketTrends(String token, MarketTrendsQuery query) {
        try {
            String url = properties.getBaseUrl() + "/api/market/trends";
            Map<String, Object> params = query.toMap();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    buildUrl(url, params),
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    String.class
            );

            return parseResponse(response, MarketTrends.class);
        } catch (Exception e) {
            log.error("获取市场趋势失败", e);
            return ApiResult.error("获取市场趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取竞品价格数据
     */
    public ApiResult<List<CompetitorProduct>> getCompetitorPrices(String token, Long categoryId, String keyword) {
        try {
            String url = properties.getBaseUrl() + "/api/market/competitors";
            Map<String, Object> params = Map.of(
                    "categoryId", categoryId != null ? categoryId : "",
                    "keyword", keyword != null ? keyword : ""
            );

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    buildUrl(url, params),
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    String.class
            );

            return parseResponse(response, new TypeReference<List<CompetitorProduct>>() {});
        } catch (Exception e) {
            log.error("获取竞品价格失败", e);
            return ApiResult.error("获取竞品价格失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门关键词
     */
    public ApiResult<List<HotKeyword>> getHotKeywords(String token, Long categoryId, int limit) {
        try {
            String url = properties.getBaseUrl() + "/api/market/keywords/hot";
            Map<String, Object> params = Map.of(
                    "categoryId", categoryId != null ? categoryId : "",
                    "limit", limit
            );

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    buildUrl(url, params),
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    String.class
            );

            return parseResponse(response, new TypeReference<List<HotKeyword>>() {});
        } catch (Exception e) {
            log.error("获取热门关键词失败", e);
            return ApiResult.error("获取热门关键词失败: " + e.getMessage());
        }
    }

    /**
     * 获取蓝海产品推荐
     */
    public ApiResult<List<BlueOceanProduct>> getBlueOceanProducts(String token, BlueOceanQuery query) {
        try {
            String url = properties.getBaseUrl() + "/api/market/blue-ocean";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);

            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(query, headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                    url, entity, String.class
            );

            return parseResponse(response, new TypeReference<List<BlueOceanProduct>>() {});
        } catch (Exception e) {
            log.error("获取蓝海产品失败", e);
            return ApiResult.error("获取蓝海产品失败: " + e.getMessage());
        }
    }

    /**
     * 构建URL
     */
    private String buildUrl(String baseUrl, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }
        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append("?");
        params.forEach((key, value) -> {
            if (value != null && !value.toString().isEmpty()) {
                sb.append(key).append("=").append(value).append("&");
            }
        });
        String url = sb.toString();
        return url.endsWith("&") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * 解析响应
     */
    private <T> ApiResult<T> parseResponse(org.springframework.http.ResponseEntity<String> response, TypeReference<T> typeRef) {
        try {
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    T data = objectMapper.convertValue(result.get("data"), typeRef);
                    return ApiResult.success(data);
                } else {
                    return ApiResult.error((String) result.get("message"));
                }
            }
            return ApiResult.error("请求失败: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("解析响应失败", e);
            return ApiResult.error("解析响应失败: " + e.getMessage());
        }
    }

    private <T> ApiResult<T> parseResponse(org.springframework.http.ResponseEntity<String> response, Class<T> clazz) {
        return parseResponse(response, new TypeReference<T>() {});
    }

    // ==================== 数据模型 ====================

    @lombok.Data
    public static class MarketTrendsQuery {
        private String category;
        private String keyword;
        private String platform;
        private Integer limit = 20;
        private Long storeId;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new java.util.HashMap<>();
            if (category != null) map.put("category", category);
            if (keyword != null) map.put("keyword", keyword);
            if (platform != null) map.put("platform", platform);
            if (storeId != null) map.put("storeId", storeId);
            map.put("limit", limit);
            return map;
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class MarketTrends {
        private List<String> hotKeywords;
        private List<CategoryTrend> categories;
        private List<CompetitorProduct> topProducts;
        private MarketSummary summary;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CategoryTrend {
        private Long id;
        private String name;
        private String icon;
        private Double growthRate;
        private Long searchVolume;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class MarketSummary {
        private Long totalProducts;
        private Double avgPrice;
        private Long totalSales;
        private String topKeyword;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CompetitorProduct {
        private Long id;
        private String name;
        private Double price;
        private Double rating;
        private Long salesCount;
        private String source;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class HotKeyword {
        private String keyword;
        private Long searchVolume;
        private Double growthRate;
        private String trend;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class BlueOceanProduct {
        private Long id;
        private String name;
        private String category;
        private Double avgPrice;
        private Long competition;
        private Double demandScore;
        private String reason;
        private List<String> sources;
    }

    @lombok.Data
    public static class BlueOceanQuery {
        private Long storeId;
        private Double minMargin;
        private Double maxPrice;
        private String category;
        private Integer limit = 20;
    }
}
