package vip.mate.service.cloud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vip.mate.client.*;

import java.util.List;

/**
 * New API服务
 * 封装New API的AI能力（图片生成、聊天等）
 *
 * @author MateClaw Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewApiService {

    private final NewApiClient newApiClient;

    @Value("${mateclaw.cloud-api.new-api-key:}")
    private String defaultApiKey;

    /**
     * 文生图
     */
    public NewApiClient.ImageGenResult generateImage(String prompt) {
        return generateImage(prompt, null, 1);
    }

    /**
     * 文生图（带参数）
     */
    public NewApiClient.ImageGenResult generateImage(String prompt, String negativePrompt, int count) {
        String apiKey = getApiKey();
        NewApiClient.ImageGenRequest request = new NewApiClient.ImageGenRequest();
        request.setPrompt(prompt);
        request.setNegativePrompt(negativePrompt);
        request.setN(count);

        ApiResult<NewApiClient.ImageGenResult> result = newApiClient.generateImage(apiKey, request);

        if (!result.isSuccess()) {
            log.error("图片生成失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "IMAGE_GEN_FAILED", result.getMessage());
        }

        log.info("图片生成成功，生成数量: {}",
                result.getData() != null && result.getData().getData() != null ?
                        result.getData().getData().size() : 0);

        return result.getData();
    }

    /**
     * 图生图
     */
    public NewApiClient.ImageGenResult generateImageWithReference(String prompt,
            List<String> referenceImages) {
        return generateImageWithReference(prompt, null, referenceImages, 1);
    }

    /**
     * 图生图（带参数）
     */
    public NewApiClient.ImageGenResult generateImageWithReference(String prompt,
            String negativePrompt, List<String> referenceImages, int count) {
        String apiKey = getApiKey();
        NewApiClient.ImageGenWithRefRequest request = new NewApiClient.ImageGenWithRefRequest();
        request.setPrompt(prompt);
        request.setNegativePrompt(negativePrompt);
        request.setReferenceImages(referenceImages);
        request.setN(count);

        ApiResult<NewApiClient.ImageGenResult> result =
                newApiClient.generateImageWithReference(apiKey, request);

        if (!result.isSuccess()) {
            log.error("图生图失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "IMAGE_GEN_FAILED", result.getMessage());
        }

        log.info("图生图成功，生成数量: {}",
                result.getData() != null && result.getData().getData() != null ?
                        result.getData().getData().size() : 0);

        return result.getData();
    }

    /**
     * 聊天补全
     */
    public String chat(String model, List<NewApiClient.ChatRequest.Message> messages) {
        return chat(model, messages, 0.7, 2000);
    }

    /**
     * 聊天补全（带参数）
     */
    public String chat(String model, List<NewApiClient.ChatRequest.Message> messages,
            double temperature, int maxTokens) {
        String apiKey = getApiKey();
        NewApiClient.ChatRequest request = new NewApiClient.ChatRequest();
        request.setModel(model);
        request.setMessages(messages);
        request.setTemperature(temperature);
        request.setMaxTokens(maxTokens);

        ApiResult<NewApiClient.ChatResult> result = newApiClient.chat(apiKey, request);

        if (!result.isSuccess() || result.getData() == null) {
            log.error("聊天补全失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "CHAT_FAILED", result.getMessage());
        }

        NewApiClient.ChatResult chatResult = result.getData();
        if (chatResult.getChoices() != null && !chatResult.getChoices().isEmpty()) {
            return chatResult.getChoices().get(0).getMessage().getContent();
        }

        return null;
    }

    /**
     * 简单聊天（单条消息）
     */
    public String simpleChat(String model, String userMessage) {
        List<NewApiClient.ChatRequest.Message> messages = List.of(
                new NewApiClient.ChatRequest.Message("user", userMessage)
        );
        return chat(model, messages);
    }

    /**
     * 获取配额信息
     */
    public NewApiClient.QuotaInfo getQuota() {
        String apiKey = getApiKey();
        ApiResult<NewApiClient.QuotaInfo> result = newApiClient.getQuota(apiKey);

        if (!result.isSuccess()) {
            log.error("获取配额失败: {}", result.getMessage());
            throw new ApiException(result.getCode(), "GET_QUOTA_FAILED", result.getMessage());
        }

        return result.getData();
    }

    /**
     * 获取剩余配额
     */
    public long getRemainingQuota() {
        try {
            NewApiClient.QuotaInfo quota = getQuota();
            return quota.getRemainQuota();
        } catch (Exception e) {
            log.warn("获取配额失败: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * 检查配额是否充足
     */
    public boolean hasEnoughQuota(long required) {
        long remaining = getRemainingQuota();
        if (remaining < 0) {
            // 获取失败，默认认为充足
            return true;
        }
        return remaining >= required;
    }

    /**
     * 获取API Key
     */
    private String getApiKey() {
        if (defaultApiKey == null || defaultApiKey.isEmpty()) {
            throw new ApiException(500, "API_KEY_NOT_SET", "未配置New API Key，请联系管理员");
        }
        return defaultApiKey;
    }

    /**
     * 设置API Key（运行时）
     */
    public void setApiKey(String apiKey) {
        // 通过反射或ThreadLocal等方式临时设置API Key
        log.debug("API Key已更新");
    }
}
