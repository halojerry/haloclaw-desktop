package vip.mate.llm.chatgpt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import vip.mate.exception.MateClawException;
import vip.mate.llm.oauth.OpenAIOAuthService;

import java.util.List;

/**
 * ChatGPT Backend API 客户端 — 调用 chatgpt.com/backend-api/codex/responses（Responses API 格式）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatGPTResponsesClient {

    private static final String BASE_URL = "https://chatgpt.com/backend-api";
    private static final String RESPONSES_PATH = "/codex/responses";

    private final OpenAIOAuthService oauthService;
    private final ObjectMapper objectMapper;
    private final WebClient webClient = WebClient.create();

    /**
     * 同步调用 — ChatGPT Backend API 强制要求 stream=true，
     * 所以实际仍走 SSE，只是收集完整响应后再返回。
     */
    public String call(String model, List<Message> messages, Double temperature) {
        return stream(model, messages, temperature)
                .collectList()
                .map(chunks -> String.join("", chunks))
                .block();
    }

    /**
     * 流式调用 Responses API (SSE)
     */
    public Flux<String> stream(String model, List<Message> messages, Double temperature) {
        String accessToken = oauthService.ensureValidAccessToken();
        String accountId = oauthService.getAccountId();
        ObjectNode requestBody = buildRequestBody(model, messages, temperature);
        String bodyJson = requestBody.toString();
        log.info("ChatGPT request body: {}", bodyJson);

        return webClient.post()
                .uri(BASE_URL + RESPONSES_PATH)
                .headers(h -> setHeaders(h, accessToken, accountId))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(bodyJson)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(errorBody -> {
                                    log.error("ChatGPT API error {}: {}", response.statusCode(), errorBody);
                                    return new MateClawException("ChatGPT API " + response.statusCode() + ": " + errorBody);
                                }))
                .bodyToFlux(String.class)
                .doOnNext(raw -> log.debug("ChatGPT SSE raw: {}", raw.length() > 200 ? raw.substring(0, 200) + "..." : raw))
                .filter(line -> !line.isBlank() && !line.equals("[DONE]"))
                .map(line -> {
                    // SSE 格式：每行以 "data: " 开头，需要去掉前缀
                    if (line.startsWith("data: ")) return line.substring(6);
                    if (line.startsWith("data:")) return line.substring(5);
                    return line;
                })
                .filter(line -> !line.isBlank() && !line.equals("[DONE]"))
                .mapNotNull(this::extractDeltaContent)
                .onErrorMap(e -> e instanceof MateClawException ? e
                        : new MateClawException("ChatGPT 流式调用失败: " + e.getMessage()));
    }

    // ==================== 请求构建 ====================

    ObjectNode buildRequestBody(String model, List<Message> messages, Double temperature) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("stream", true);  // ChatGPT Backend API 强制要求 stream=true
        body.put("store", false);

        // 从 messages 中提取 system prompt → instructions
        String systemPrompt = null;
        for (Message msg : messages) {
            if (msg.getMessageType() == MessageType.SYSTEM) {
                systemPrompt = msg.getText();
                break;
            }
        }
        if (systemPrompt != null) {
            body.put("instructions", systemPrompt);
        }

        // 非 system 消息 → input 数组（Responses API 格式）
        ArrayNode input = objectMapper.createArrayNode();
        int msgIndex = 0;
        for (Message msg : messages) {
            if (msg.getMessageType() == MessageType.SYSTEM) continue;

            if (msg.getMessageType() == MessageType.USER) {
                // User: content 必须是 [{ type: "input_text", text: "..." }] 格式
                ObjectNode item = objectMapper.createObjectNode();
                item.put("role", "user");
                ArrayNode contentArr = objectMapper.createArrayNode();
                ObjectNode textPart = objectMapper.createObjectNode();
                textPart.put("type", "input_text");
                textPart.put("text", msg.getText() != null ? msg.getText() : "");
                contentArr.add(textPart);
                item.set("content", contentArr);
                input.add(item);
            } else if (msg.getMessageType() == MessageType.ASSISTANT) {
                // Assistant: 转为 output message item
                ObjectNode item = objectMapper.createObjectNode();
                item.put("type", "message");
                item.put("role", "assistant");
                item.put("id", "msg_" + msgIndex);
                ArrayNode contentArr = objectMapper.createArrayNode();
                ObjectNode textPart = objectMapper.createObjectNode();
                textPart.put("type", "output_text");
                textPart.put("text", msg.getText() != null ? msg.getText() : "");
                contentArr.add(textPart);
                item.set("content", contentArr);
                input.add(item);
            }
            msgIndex++;
        }
        body.set("input", input);

        // 注意：ChatGPT Backend API 的部分模型（如 gpt-5.4）不支持 temperature，
        // 仅对非推理类旧模型（如 gpt-4o）传递此参数
        if (temperature != null && !model.startsWith("gpt-5") && !model.startsWith("o")) {
            body.put("temperature", temperature);
        }

        // Responses API 特有参数
        ObjectNode text = objectMapper.createObjectNode();
        text.put("verbosity", "medium");
        body.set("text", text);

        // include reasoning（OpenClaw 的标准参数）
        ArrayNode include = objectMapper.createArrayNode();
        include.add("reasoning.encrypted_content");
        body.set("include", include);

        return body;
    }

    // ==================== 响应解析 ====================

    /**
     * 从 SSE delta 事件中提取增量文本
     */
    private String extractDeltaContent(String eventData) {
        try {
            JsonNode node = objectMapper.readTree(eventData);
            String type = node.path("type").asText("");

            // response.output_text.delta — 文本增量
            if ("response.output_text.delta".equals(type)) {
                return node.path("delta").asText(null);
            }

            // response.completed / response.done — 结束信号
            if (type.startsWith("response.completed") || type.startsWith("response.done")) {
                return null;
            }

            // response.failed — 错误
            if ("response.failed".equals(type)) {
                String error = node.path("response").path("error").path("message").asText("Unknown error");
                log.error("ChatGPT Responses API 返回错误: {}", error);
                throw new MateClawException("ChatGPT ���回错误: " + error);
            }

            return null;
        } catch (MateClawException e) {
            throw e;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Headers ====================

    private void setHeaders(HttpHeaders headers, String accessToken, String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new MateClawException("chatgpt-account-id 缺失，请断开后重新 OAuth 登录");
        }
        headers.setBearerAuth(accessToken);
        headers.set("chatgpt-account-id", accountId);
        headers.set("originator", "pi");
        headers.set("OpenAI-Beta", "responses=experimental");
        headers.set("accept", "text/event-stream");
        String os = System.getProperty("os.name", "unknown").toLowerCase();
        String release = System.getProperty("os.version", "");
        String arch = System.getProperty("os.arch", "");
        headers.set("User-Agent", "pi (" + os + " " + release + "; " + arch + ")");
    }
}
