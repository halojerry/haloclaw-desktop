package vip.mate.llm.chatgpt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * ChatGPT 会员模型 — 实现 Spring AI ChatModel 接口，
 * 内部通过 ChatGPTResponsesClient 调用 chatgpt.com/backend-api。
 */
@Slf4j
public class ChatGPTChatModel implements ChatModel {

    private final ChatGPTResponsesClient client;
    private final String modelName;
    private final Double temperature;

    public ChatGPTChatModel(ChatGPTResponsesClient client, String modelName, Double temperature) {
        this.client = client;
        this.modelName = modelName;
        this.temperature = temperature;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        List<Message> messages = prompt.getInstructions();
        String model = resolveModel(prompt);
        Double temp = resolveTemperature(prompt);

        log.debug("ChatGPT call: model={}, messages={}", model, messages.size());
        String content = client.call(model, messages, temp);

        Generation generation = new Generation(new AssistantMessage(content),
                ChatGenerationMetadata.builder().finishReason("stop").build());
        return new ChatResponse(List.of(generation),
                ChatResponseMetadata.builder().model(model).build());
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        List<Message> messages = prompt.getInstructions();
        String model = resolveModel(prompt);
        Double temp = resolveTemperature(prompt);

        log.debug("ChatGPT stream: model={}, messages={}", model, messages.size());
        return client.stream(model, messages, temp)
                .map(delta -> {
                    Generation generation = new Generation(new AssistantMessage(delta),
                            ChatGenerationMetadata.builder().finishReason(null).build());
                    return new ChatResponse(List.of(generation),
                            ChatResponseMetadata.builder().model(model).build());
                });
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return ChatOptions.builder()
                .model(modelName)
                .temperature(temperature)
                .build();
    }

    private String resolveModel(Prompt prompt) {
        if (prompt.getOptions() != null && prompt.getOptions().getModel() != null) {
            return prompt.getOptions().getModel();
        }
        return modelName;
    }

    private Double resolveTemperature(Prompt prompt) {
        if (prompt.getOptions() != null && prompt.getOptions().getTemperature() != null) {
            return prompt.getOptions().getTemperature();
        }
        return temperature;
    }
}
