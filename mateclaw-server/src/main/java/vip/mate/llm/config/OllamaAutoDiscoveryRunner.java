package vip.mate.llm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import vip.mate.llm.model.DiscoverResult;
import vip.mate.llm.model.ModelConfigEntity;
import vip.mate.llm.model.ModelInfoDTO;
import vip.mate.llm.model.TestResult;
import vip.mate.llm.service.ModelConfigService;
import vip.mate.llm.service.ModelDiscoveryService;

import vip.mate.llm.service.ModelProviderService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Startup runner: auto-detect running Ollama instance and register discovered models.
 * <p>
 * On application startup, pings the Ollama endpoint (default http://127.0.0.1:11434).
 * If Ollama is online, discovers all pulled models and auto-enables matching pre-configured models.
 * If offline, silently skips (debug log only).
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@Order(200)
@RequiredArgsConstructor
public class OllamaAutoDiscoveryRunner implements ApplicationRunner {

    private static final String OLLAMA_PROVIDER_ID = "ollama";

    private final ModelDiscoveryService modelDiscoveryService;
    private final ModelConfigService modelConfigService;
    private final ModelProviderService modelProviderService;

    @Async
    @Override
    public void run(ApplicationArguments args) {
        try {
            // Test connection first
            TestResult result = modelDiscoveryService.testConnection(OLLAMA_PROVIDER_ID);
            if (!result.isSuccess()) {
                log.debug("Ollama not running, skipping auto-discovery: {}", result.getErrorMessage());
                return;
            }

            log.info("Ollama detected, discovering models...");

            // Discover and register new models
            DiscoverResult discovered = modelDiscoveryService.discoverModels(OLLAMA_PROVIDER_ID);
            if (discovered.getNewCount() > 0) {
                var newModelIds = discovered.getNewModels().stream()
                        .map(ModelInfoDTO::getId)
                        .toList();
                int added = modelDiscoveryService.batchAddModels(OLLAMA_PROVIDER_ID, newModelIds);
                log.info("Ollama auto-discovery: found {} models, registered {} new", discovered.getTotalDiscovered(), added);
            } else {
                log.info("Ollama auto-discovery: {} models found, all already registered", discovered.getTotalDiscovered());
            }

            // Enable pre-configured Ollama models that match discovered models
            Set<String> discoveredIds = discovered.getDiscoveredModels().stream()
                    .map(ModelInfoDTO::getId)
                    .collect(Collectors.toSet());

            for (ModelConfigEntity model : modelConfigService.listModelsByProvider(OLLAMA_PROVIDER_ID)) {
                if (Boolean.TRUE.equals(model.getEnabled())) {
                    continue;
                }
                String modelTag = model.getModelName();
                String modelBase = modelTag.contains(":") ? modelTag.substring(0, modelTag.indexOf(":")) : modelTag;
                boolean matches = discoveredIds.contains(modelTag)
                        || discoveredIds.contains(modelBase)
                        || discoveredIds.stream().anyMatch(id -> id.startsWith(modelBase + ":"));
                if (matches) {
                    model.setEnabled(true);
                    modelConfigService.updateModel(model);
                    log.info("Ollama: auto-enabled model '{}'", modelTag);
                }
            }
            // Auto-activate first Ollama model if no valid default exists
            tryAutoActivateOllamaModel();
        } catch (Exception e) {
            log.debug("Ollama auto-discovery skipped: {}", e.getMessage());
        }
    }

    private void tryAutoActivateOllamaModel() {
        List<ModelConfigEntity> ollamaModels = modelConfigService.listModelsByProvider(OLLAMA_PROVIDER_ID);
        List<ModelConfigEntity> enabledModels = ollamaModels.stream()
                .filter(m -> Boolean.TRUE.equals(m.getEnabled()))
                .toList();
        if (enabledModels.isEmpty()) {
            return;
        }
        boolean hasValidDefault;
        try {
            ModelConfigEntity current = modelConfigService.getDefaultModel();
            hasValidDefault = modelProviderService.isProviderAvailable(current.getProvider());
        } catch (Exception e) {
            hasValidDefault = false;
        }
        if (!hasValidDefault) {
            ModelConfigEntity first = enabledModels.get(0);
            modelConfigService.setDefaultModel(OLLAMA_PROVIDER_ID, first.getModelName());
            log.info("Ollama: auto-activated default model '{}'", first.getModelName());
        }
    }
}
