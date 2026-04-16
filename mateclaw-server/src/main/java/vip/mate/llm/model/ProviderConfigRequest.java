package vip.mate.llm.model;

import lombok.Data;

import java.util.Map;

@Data
public class ProviderConfigRequest {
    private String apiKey;
    private String baseUrl;
    private String protocol;
    private String chatModel;
    private Map<String, Object> generateKwargs;
}
