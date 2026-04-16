package vip.mate.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import vip.mate.config.CloudApiProperties;

import java.util.List;
import java.util.Map;

/**
 * New API客户端
 * 调用 https://new-api.ozon-claw.com 的AI能力
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
public class NewApiClient extends BaseApiClient {

    private final CloudApiProperties properties;

    public NewApiClient(
            @Qualifier("newApiRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CloudApiProperties properties) {
        super(restTemplate, objectMapper);
        this.properties = properties;
    }

    @Override
    protected String getBaseUrl() {
        return properties.getNewApiUrl();
    }

    // ========== AI图片生成 ==========

    /**
     * 图片生成（文生图）
     */
    public ApiResult<ImageGenResult> generateImage(String apiKey, ImageGenRequest request) {
        String url = getBaseUrl() + "/v1/images/generations";
        return execute(() -> {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            org.springframework.http.HttpEntity<ImageGenRequest> entity =
                    new org.springframework.http.HttpEntity<>(request, headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                    url, entity, String.class);
            return parseResponse(response, ImageGenResult.class);
        });
    }

    /**
     * 图片生成（图生图）
     */
    public ApiResult<ImageGenResult> generateImageWithReference(String apiKey,
            ImageGenWithRefRequest request) {
        String url = getBaseUrl() + "/v1/images/generations";
        return execute(() -> {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            org.springframework.http.HttpEntity<ImageGenWithRefRequest> entity =
                    new org.springframework.http.HttpEntity<>(request, headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                    url, entity, String.class);
            return parseResponse(response, ImageGenResult.class);
        });
    }

    // ========== AI聊天 ==========

    /**
     * 聊天补全
     */
    public ApiResult<ChatResult> chat(String apiKey, ChatRequest request) {
        String url = getBaseUrl() + "/v1/chat/completions";
        return execute(() -> {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            org.springframework.http.HttpEntity<ChatRequest> entity =
                    new org.springframework.http.HttpEntity<>(request, headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                    url, entity, String.class);
            return parseResponse(response, ChatResult.class);
        });
    }

    // ========== 配额查询 ==========

    /**
     * 获取用户配额
     */
    public ApiResult<QuotaInfo> getQuota(String apiKey) {
        String url = getBaseUrl() + "/v1/user/quota";
        return execute(() -> {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);

            org.springframework.http.HttpEntity<?> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, String.class);
            return parseResponse(response, QuotaInfo.class);
        });
    }

    // ========== 数据模型 ==========

    @lombok.Data
    public static class ImageGenRequest {
        private String model = "stable-diffusion-xl";
        private String prompt;
        private String negativePrompt;
        private Integer n = 1;
        private String size = "1024x1024";
    }

    @lombok.Data
    public static class ImageGenWithRefRequest {
        private String model = "stable-diffusion-xl";
        private String prompt;
        private String negativePrompt;
        private List<String> referenceImages;
        private Integer n = 1;
        private String size = "1024x1024";
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ImageGenResult {
        private List<ImageData> data;

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class ImageData {
            private String url;
            private String base64;
        }
    }

    @lombok.Data
    public static class ChatRequest {
        private String model;
        private List<Message> messages;
        private Double temperature = 0.7;
        private Integer maxTokens = 2000;
        private Boolean stream = false;

        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class Message {
            private String role; // system, user, assistant
            private String content;
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChatResult {
        private String id;
        private String object;
        private long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;

        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class Choice {
            private int index;
            private Message message;
            private String finishReason;
        }

        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class Message {
            private String role;
            private String content;
        }

        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class Usage {
            private int promptTokens;
            private int completionTokens;
            private int totalTokens;
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QuotaInfo {
        private long quota;
        private long usedQuota;
        private long remainQuota;
        private String groupName;
    }
}
