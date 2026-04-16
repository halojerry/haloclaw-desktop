package vip.mate.llm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.common.result.R;
import vip.mate.llm.model.*;
import vip.mate.llm.service.ModelConfigService;
import vip.mate.llm.service.ModelDiscoveryService;
import vip.mate.llm.service.ModelProviderService;

import java.util.List;
import java.util.Map;

@Tag(name = "模型配置管理")
@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigService modelConfigService;
    private final ModelProviderService modelProviderService;
    private final ModelDiscoveryService modelDiscoveryService;

    @Operation(summary = "获取 Provider 列表")
    @GetMapping
    public R<List<ProviderInfoDTO>> list() {
        return R.ok(modelProviderService.listProviders());
    }

    @Operation(summary = "获取启用模型列表")
    @GetMapping("/enabled")
    public R<List<ModelConfigEntity>> listEnabled() {
        return R.ok(modelConfigService.listEnabledModels());
    }

    @Operation(summary = "获取默认模型")
    @GetMapping("/default")
    public R<ModelConfigEntity> getDefaultModel() {
        return R.ok(modelConfigService.getDefaultModel());
    }

    @Operation(summary = "获取当前激活模型")
    @GetMapping("/active")
    public R<ActiveModelsInfo> getActiveModel() {
        ModelConfigEntity model = modelConfigService.getDefaultModel();
        ActiveModelsInfo info = new ActiveModelsInfo();
        info.setActiveLlm(new ModelSlotConfig(model.getProvider(), model.getModelName()));
        return R.ok(info);
    }

    @Operation(summary = "设置当前激活模型")
    @PutMapping("/active")
    public R<ActiveModelsInfo> setActiveModel(@RequestBody ModelSlotRequest request) {
        ModelConfigEntity model = modelConfigService.setDefaultModel(request.getProviderId(), request.getModel());
        ActiveModelsInfo info = new ActiveModelsInfo();
        info.setActiveLlm(new ModelSlotConfig(model.getProvider(), model.getModelName()));
        return R.ok(info);
    }

    @Operation(summary = "更新 Provider 配置")
    @PutMapping("/{providerId}/config")
    public R<ProviderInfoDTO> updateProviderConfig(@PathVariable String providerId,
                                                   @RequestBody ProviderConfigRequest request) {
        return R.ok(modelProviderService.updateProviderConfig(providerId, request));
    }

    @Operation(summary = "创建自定义 Provider")
    @PostMapping("/custom-providers")
    public R<ProviderInfoDTO> createCustomProvider(@RequestBody CreateCustomProviderRequest request) {
        return R.ok(modelProviderService.createCustomProvider(request));
    }

    @Operation(summary = "删除自定义 Provider")
    @DeleteMapping("/custom-providers/{providerId}")
    public R<Void> deleteCustomProvider(@PathVariable String providerId) {
        modelProviderService.deleteCustomProvider(providerId);
        return R.ok();
    }

    @Operation(summary = "向 Provider 添加模型")
    @PostMapping("/{providerId}/models")
    public R<ProviderInfoDTO> addProviderModel(@PathVariable String providerId,
                                               @RequestBody AddProviderModelRequest request) {
        return R.ok(modelProviderService.addModel(providerId, request));
    }

    @Operation(summary = "从 Provider 删除模型")
    @DeleteMapping("/{providerId}/models/{modelId}")
    public R<ProviderInfoDTO> removeProviderModel(@PathVariable String providerId,
                                                  @PathVariable String modelId) {
        return R.ok(modelProviderService.removeModel(providerId, modelId));
    }

    @Operation(summary = "获取模型详情")
    @GetMapping("/{id}")
    public R<ModelConfigEntity> get(@PathVariable Long id) {
        return R.ok(modelConfigService.getModel(id));
    }

    @Operation(summary = "创建模型")
    @PostMapping
    public R<ModelConfigEntity> create(@RequestBody ModelConfigEntity entity) {
        return R.ok(modelConfigService.createModel(entity));
    }

    @Operation(summary = "更新模型")
    @PutMapping("/{id}")
    public R<ModelConfigEntity> update(@PathVariable Long id, @RequestBody ModelConfigEntity entity) {
        entity.setId(id);
        return R.ok(modelConfigService.updateModel(entity));
    }

    @Operation(summary = "删除模型")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        modelConfigService.deleteModel(id);
        return R.ok();
    }

    @Operation(summary = "设置默认模型")
    @PostMapping("/{id}/default")
    public R<ModelConfigEntity> setDefault(@PathVariable Long id) {
        return R.ok(modelConfigService.setDefaultModel(id));
    }

    // ==================== 模型发现与连接测试 ====================

    @Operation(summary = "发现远端模型")
    @PostMapping("/{providerId}/discover")
    public R<DiscoverResult> discoverModels(@PathVariable String providerId) {
        return R.ok(modelDiscoveryService.discoverModels(providerId));
    }

    @Operation(summary = "批量添加发现的模型")
    @PostMapping("/{providerId}/discover/apply")
    public R<Map<String, Integer>> applyDiscoveredModels(@PathVariable String providerId,
                                                          @RequestBody ApplyDiscoveredModelsRequest request) {
        int added = modelDiscoveryService.batchAddModels(providerId, request.getModelIds());
        return R.ok(Map.of("added", added));
    }

    @Operation(summary = "测试供应商连接")
    @PostMapping("/{providerId}/test-connection")
    public R<TestResult> testConnection(@PathVariable String providerId) {
        return R.ok(modelDiscoveryService.testConnection(providerId));
    }

    @Operation(summary = "测试单个模型可用性")
    @PostMapping("/{providerId}/models/{modelId}/test")
    public R<TestResult> testModel(@PathVariable String providerId,
                                    @PathVariable String modelId) {
        return R.ok(modelDiscoveryService.testModel(providerId, modelId));
    }
}
