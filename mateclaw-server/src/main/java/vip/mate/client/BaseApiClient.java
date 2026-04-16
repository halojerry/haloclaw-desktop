package vip.mate.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 基础HTTP客户端封装
 * 提供统一的请求处理、错误处理和响应转换
 *
 * @author MateClaw Team
 */
@Slf4j
public abstract class BaseApiClient {

    protected final RestTemplate restTemplate;
    protected final ObjectMapper objectMapper;

    protected BaseApiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * GET请求
     */
    protected <T> ApiResult<T> get(String url, Map<String, ?> params, Class<T> responseType) {
        return execute(() -> {
            String fullUrl = buildUrl(url, params);
            ResponseEntity<String> response = restTemplate.getForEntity(fullUrl, String.class);
            return parseResponse(response, responseType);
        });
    }

    /**
     * GET请求（泛型）
     */
    protected <T> ApiResult<T> get(String url, Map<String, ?> params, TypeReference<T> typeRef) {
        return execute(() -> {
            String fullUrl = buildUrl(url, params);
            ResponseEntity<String> response = restTemplate.getForEntity(fullUrl, String.class);
            return parseResponse(response, typeRef);
        });
    }

    /**
     * POST请求
     */
    protected <T> ApiResult<T> post(String url, Object body, Class<T> responseType) {
        return execute(() -> {
            HttpEntity<Object> entity = createHttpEntity(body);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return parseResponse(response, responseType);
        });
    }

    /**
     * POST请求（泛型）
     */
    protected <T> ApiResult<T> post(String url, Object body, TypeReference<T> typeRef) {
        return execute(() -> {
            HttpEntity<Object> entity = createHttpEntity(body);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return parseResponse(response, typeRef);
        });
    }

    /**
     * PUT请求
     */
    protected <T> ApiResult<T> put(String url, Object body, Class<T> responseType) {
        return execute(() -> {
            HttpEntity<Object> entity = createHttpEntity(body);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            return parseResponse(response, responseType);
        });
    }

    /**
     * DELETE请求
     */
    protected <T> ApiResult<T> delete(String url, Map<String, ?> params, Class<T> responseType) {
        return execute(() -> {
            String fullUrl = buildUrl(url, params);
            ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.DELETE, null, String.class);
            return parseResponse(response, responseType);
        });
    }

    /**
     * 执行请求并处理异常
     */
    protected <T> ApiResult<T> execute(Supplier<ApiResult<T>> action) {
        try {
            return action.get();
        } catch (HttpStatusCodeException e) {
            log.error("HTTP错误: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return handleHttpError(e);
        } catch (RestClientException e) {
            log.error("请求异常: {}", e.getMessage());
            return handleRequestError(e);
        } catch (Exception e) {
            log.error("未知异常: {}", e.getMessage(), e);
            return ApiResult.error("请求失败: " + e.getMessage());
        }
    }

    /**
     * 处理HTTP错误
     */
    private <T> ApiResult<T> handleHttpError(HttpStatusCodeException e) {
        int statusCode = e.getStatusCode().value();
        String responseBody = e.getResponseBodyAsString();

        try {
            ApiResult<?> errorResult = objectMapper.readValue(responseBody, ApiResult.class);
            return ApiResult.error(statusCode, (String) errorResult.getMessage());
        } catch (Exception parseError) {
            return ApiResult.error(statusCode, e.getMessage());
        }
    }

    /**
     * 处理请求错误
     */
    private <T> ApiResult<T> handleRequestError(Exception e) {
        String message = e.getMessage();
        if (message.contains("Connection refused") || message.contains("connect timed out")) {
            return ApiResult.error(503, "服务不可用，请检查网络连接");
        }
        if (message.contains("Read timed out")) {
            return ApiResult.error(408, "请求超时，请稍后重试");
        }
        return ApiResult.error("网络请求失败: " + message);
    }

    /**
     * 解析响应
     */
    protected <T> ApiResult<T> parseResponse(ResponseEntity<String> response, Class<T> responseType) {
        int statusCode = response.getStatusCode().value();

        // 处理HTTP错误状态码
        if (statusCode >= 400) {
            String body = response.getBody();
            try {
                ApiResult<?> errorResult = objectMapper.readValue(body, ApiResult.class);
                return ApiResult.error(statusCode, (String) errorResult.getMessage());
            } catch (Exception e) {
                return ApiResult.error(statusCode, "HTTP " + statusCode);
            }
        }

        String body = response.getBody();
        if (body == null || body.isEmpty()) {
            return ApiResult.success(null);
        }

        try {
            // 先解析为ApiResult格式
            ApiResult<Map<String, Object>> rawResult = objectMapper.readValue(body,
                    new TypeReference<ApiResult<Map<String, Object>>>() {});

            if (rawResult.isSuccess() && rawResult.getData() != null) {
                // 将data字段转换为目标类型
                String dataJson = objectMapper.writeValueAsString(rawResult.getData());
                T data = objectMapper.readValue(dataJson, responseType);
                return ApiResult.success(rawResult.getMessage(), data);
            } else {
                return ApiResult.error(rawResult.getCode(), rawResult.getMessage());
            }
        } catch (Exception e) {
            // 如果不是ApiResult格式，直接返回原始数据
            try {
                T data = objectMapper.readValue(body, responseType);
                return ApiResult.success(data);
            } catch (Exception parseError) {
                log.error("响应解析失败: {}", body);
                return ApiResult.error("响应解析失败");
            }
        }
    }

    /**
     * 解析响应（泛型）
     */
    protected <T> ApiResult<T> parseResponse(ResponseEntity<String> response, TypeReference<T> typeRef) {
        int statusCode = response.getStatusCode().value();

        if (statusCode >= 400) {
            return ApiResult.error(statusCode, "HTTP " + statusCode);
        }

        String body = response.getBody();
        if (body == null || body.isEmpty()) {
            return ApiResult.success(null);
        }

        try {
            ApiResult<Map<String, Object>> rawResult = objectMapper.readValue(body,
                    new TypeReference<ApiResult<Map<String, Object>>>() {});

            if (rawResult.isSuccess() && rawResult.getData() != null) {
                String dataJson = objectMapper.writeValueAsString(rawResult.getData());
                T data = objectMapper.readValue(dataJson, typeRef);
                return ApiResult.success(rawResult.getMessage(), data);
            } else {
                return ApiResult.error(rawResult.getCode(), rawResult.getMessage());
            }
        } catch (Exception e) {
            try {
                T data = objectMapper.readValue(body, typeRef);
                return ApiResult.success(data);
            } catch (Exception parseError) {
                return ApiResult.error("响应解析失败");
            }
        }
    }

    /**
     * 构建带参数的URL
     */
    protected String buildUrl(String baseUrl, Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }
        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append(baseUrl.contains("?") ? "&" : "?");
        params.forEach((key, value) -> {
            if (value != null) {
                sb.append(key).append("=");
                sb.append(java.net.URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
                sb.append("&");
            }
        });
        // 移除最后一个 &
        return sb.substring(0, sb.length() - 1);
    }

    /**
     * 创建HTTP实体
     */
    protected HttpEntity<Object> createHttpEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        return new HttpEntity<>(body, headers);
    }

    /**
     * 创建带Token的HTTP实体
     */
    protected HttpEntity<Object> createHttpEntityWithToken(Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    /**
     * 获取API基础URL
     */
    protected abstract String getBaseUrl();
}
