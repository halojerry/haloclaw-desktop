<template>
  <div class="settings-section model-section">
    <div class="section-header">
      <div>
        <h2 class="section-title">{{ t('settings.model.title') }}</h2>
        <p class="section-desc">{{ t('settings.model.desc') }}</p>
      </div>
      <button class="btn-primary" @click="openCreateProviderModal">
        {{ t('settings.model.addProvider') }}
      </button>
    </div>

    <div class="provider-grid">
      <div v-for="provider in providers" :key="provider.id" class="provider-card">
        <div class="provider-header">
          <div>
            <div class="provider-title-row">
              <img
                :src="getProviderIcon(provider.id)"
                :alt="provider.name"
                class="provider-icon"
                @error="onIconError"
              />
              <h3 class="provider-name">{{ provider.name }}</h3>
              <span class="provider-badge" :class="provider.isCustom ? 'custom' : 'builtin'">
                {{ provider.isCustom ? t('settings.model.custom') : t('settings.model.builtin') }}
              </span>
              <span v-if="isProviderActive(provider)" class="provider-badge active">
                {{ t('settings.model.active') }}
              </span>
            </div>
            <p class="provider-id">{{ provider.id }}</p>
          </div>
          <div class="provider-status" :class="providerStatus(provider).type">
            {{ providerStatus(provider).label }}
          </div>
        </div>

        <div class="provider-info">
          <div class="info-row">
            <span class="info-label">{{ t('settings.model.baseUrl') }}</span>
            <span class="info-value mono" :title="provider.baseUrl || ''">
              {{ provider.baseUrl || t('settings.model.notSet') }}
            </span>
          </div>
          <div class="info-row">
            <span class="info-label">{{ t('settings.model.apiKey') }}</span>
            <span class="info-value mono">{{ provider.apiKey || t('settings.model.notSet') }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">{{ t('settings.fields.modelName') }}</span>
            <span class="info-value">
              {{ t('settings.model.modelCount', { count: (provider.models?.length || 0) + (provider.extraModels?.length || 0) }) }}
            </span>
          </div>
        </div>

        <div class="card-actions">
          <button class="card-btn" @click="openManageModelsModal(provider)">
            {{ t('settings.model.actions.manageModels') }}
          </button>
          <button class="card-btn" @click="openProviderConfigModal(provider)">
            {{ t('settings.model.actions.providerSettings') }}
          </button>
          <button
            v-if="provider.supportConnectionCheck && provider.configured"
            class="card-btn"
            :class="{ testing: connectionTestingId === provider.id }"
            :disabled="connectionTestingId === provider.id"
            @click="handleTestConnection(provider)"
          >
            {{ connectionTestingId === provider.id ? t('settings.model.discovery.testing') : t('settings.model.discovery.testConnection') }}
          </button>
          <button
            v-if="provider.isCustom"
            class="card-btn danger"
            @click="onDeleteProvider(provider)"
          >
            {{ t('common.delete') }}
          </button>
        </div>

        <div v-if="connectionResults[provider.id]" class="connection-result" :class="connectionResults[provider.id].success ? 'success' : 'error'">
          <span v-if="connectionResults[provider.id].success">
            {{ t('settings.model.discovery.connectionOk') }} · {{ t('settings.model.discovery.latency', { ms: connectionResults[provider.id].latencyMs }) }}
          </span>
          <span v-else>
            {{ t('settings.model.discovery.connectionFail') }}: {{ connectionResults[provider.id].errorMessage }}
          </span>
        </div>
      </div>
    </div>

    <div v-if="savedTip" class="save-tip">{{ savedTip }}</div>

    <!-- Provider Config Modal -->
    <ProviderConfigModal
      :show="showProviderModal"
      :editing-provider="editingProvider"
      :form="providerForm"
      :advanced-open="advancedOpen"
      :protocol-options="protocolOptions"
      :base-url-placeholder="providerBaseUrlPlaceholder"
      :base-url-hint="providerBaseUrlHint"
      :api-key-placeholder="providerApiKeyPlaceholder"
      @close="closeProviderModal"
      @save="onSaveProvider"
      @toggle-advanced="advancedOpen = !advancedOpen"
    />

    <!-- Manage Models Modal -->
    <ManageModelsModal
      :show="showManageModelsModal"
      :provider="currentProvider"
      :model-form="providerModelForm"
      :discovering="discovering"
      :discover-result="discoverResult"
      :selected-new-model-ids="selectedNewModelIds"
      :applying-models="applyingModels"
      :all-new-selected="allNewSelected"
      :testing-model-id="testingModelId"
      :model-test-results="modelTestResults"
      :is-extra-model="isExtraModel"
      :is-active-model="isActiveModel"
      :get-provider-icon="getProviderIcon"
      :on-icon-error="onIconError"
      @close="closeManageModelsModal"
      @discover="handleDiscoverModels"
      @toggle-select-all="toggleSelectAll"
      @toggle-model="onToggleModel"
      @apply-models="onApplyModels"
      @test-model="handleTestModel"
      @set-active="onSetActiveModel"
      @remove-model="onRemoveProviderModel"
      @add-model="onAddProviderModel"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { ProviderInfo, ProviderModelInfo } from '@/types'
import { useProviders } from './useProviders'
import ProviderConfigModal from './modals/ProviderConfigModal.vue'
import ManageModelsModal from './modals/ManageModelsModal.vue'

const { t } = useI18n()
const savedTip = ref('')

const {
  providers,
  editingProvider,
  currentProvider,
  showProviderModal,
  showManageModelsModal,
  advancedOpen,
  discovering,
  discoverResult,
  selectedNewModelIds,
  applyingModels,
  connectionTestingId,
  connectionResults,
  testingModelId,
  modelTestResults,
  providerForm,
  providerModelForm,
  protocolOptions,
  allNewSelected,
  providerBaseUrlPlaceholder,
  providerBaseUrlHint,
  providerApiKeyPlaceholder,
  loadProviders,
  loadActiveModel,
  openCreateProviderModal,
  openProviderConfigModal,
  closeProviderModal,
  saveProvider,
  deleteProvider,
  openManageModelsModal,
  closeManageModelsModal,
  isExtraModel,
  addProviderModel,
  removeProviderModel,
  isProviderActive,
  isActiveModel,
  setActiveModel,
  toggleSelectAll,
  handleDiscoverModels,
  handleApplyModels,
  handleTestConnection,
  handleTestModel,
  providerStatus,
  getProviderIcon,
  onIconError,
} = useProviders()

onMounted(async () => {
  await Promise.all([loadProviders(), loadActiveModel()])
})

async function onSaveProvider() {
  try {
    await saveProvider()
    showSavedTip(t('settings.model.providerSaved'))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('settings.messages.saveFailed'))
  }
}

async function onDeleteProvider(provider: ProviderInfo) {
  const deleted = await deleteProvider(provider)
  if (deleted) showSavedTip(t('settings.model.providerDeleted'))
}

async function onAddProviderModel() {
  await addProviderModel()
  showSavedTip(t('settings.model.modelAdded'))
}

async function onRemoveProviderModel(model: ProviderModelInfo) {
  await removeProviderModel(model)
  showSavedTip(t('settings.model.modelRemoved'))
}

async function onSetActiveModel(model: ProviderModelInfo) {
  await setActiveModel(model)
  showSavedTip(t('settings.model.activeChanged'))
}

async function onApplyModels() {
  const added = await handleApplyModels()
  if (added) showSavedTip(t('settings.model.discovery.addedCount', { count: added }))
}

function onToggleModel(modelId: string) {
  const idx = selectedNewModelIds.value.indexOf(modelId)
  if (idx >= 0) {
    selectedNewModelIds.value.splice(idx, 1)
  } else {
    selectedNewModelIds.value.push(modelId)
  }
}

function showSavedTip(message: string) {
  savedTip.value = message
  window.setTimeout(() => { savedTip.value = '' }, 2500)
}
</script>

<style scoped>
.settings-section { width: 100%; }
.settings-section.model-section { max-width: none; }
.section-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 20px; }
.section-title { margin: 0 0 6px; font-size: 22px; font-weight: 700; color: var(--mc-text-primary); }
.section-desc { margin: 0; font-size: 14px; color: var(--mc-text-secondary); }

.provider-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: stretch;
}
.provider-card {
  flex: 1 1 calc(33.333% - 16px);
  min-width: 360px;
  background: var(--mc-bg-elevated); border: 1px solid var(--mc-border); border-radius: 16px; padding: 18px; box-shadow: 0 8px 24px rgba(124, 63, 30, 0.04);
}
.provider-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.provider-title-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.provider-icon { width: 28px; height: 28px; border-radius: 6px; object-fit: contain; flex-shrink: 0; }
.provider-name { margin: 0; font-size: 18px; color: var(--mc-text-primary); }
.provider-id { margin: 6px 0 0; font-size: 13px; color: var(--mc-primary); }
.provider-badge { display: inline-flex; align-items: center; border-radius: 999px; padding: 3px 9px; font-size: 12px; font-weight: 600; }
.provider-badge.builtin { background: var(--mc-primary-bg); color: var(--mc-primary); }
.provider-badge.custom { background: var(--mc-primary-bg); color: var(--mc-primary-hover); }
.provider-badge.active { background: rgba(217, 119, 87, 0.12); color: var(--mc-primary-light); }
.provider-status { flex-shrink: 0; padding: 4px 10px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.provider-status.configured { background: var(--mc-primary-bg); color: var(--mc-primary); }
.provider-status.partial { background: var(--mc-primary-bg); color: var(--mc-primary-hover); }
.provider-status.unavailable { background: var(--mc-bg-sunken); color: var(--mc-text-tertiary); }
.provider-info { display: grid; gap: 10px; }
.info-row { display: flex; justify-content: space-between; gap: 12px; }
.info-label { color: var(--mc-text-secondary); font-size: 13px; }
.info-value { color: var(--mc-text-primary); font-size: 13px; text-align: right; word-break: break-all; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; }
.card-actions { display: flex; gap: 8px; margin-top: 16px; flex-wrap: wrap; }

.btn-primary, .card-btn { border: none; border-radius: 10px; padding: 9px 14px; font-size: 14px; cursor: pointer; transition: all 0.15s; }
.btn-primary { background: var(--mc-primary); color: white; }
.btn-primary:hover { background: var(--mc-primary-hover); }
.card-btn { background: var(--mc-primary-bg); color: var(--mc-primary); }
.card-btn:hover { background: rgba(217, 119, 87, 0.18); }
.card-btn.danger { background: var(--mc-danger-bg); color: var(--mc-danger); }
.card-btn.testing { opacity: 0.6; cursor: wait; }

.connection-result { margin-top: 10px; padding: 8px 12px; border-radius: 8px; font-size: 12px; }
.connection-result.success { background: var(--mc-primary-bg); color: var(--mc-primary); }
.connection-result.error { background: var(--mc-danger-bg); color: var(--mc-danger); }

.save-tip { position: fixed; right: 24px; bottom: 24px; background: var(--mc-text-primary); color: var(--mc-text-inverse); padding: 10px 14px; border-radius: 10px; box-shadow: 0 10px 30px rgba(124, 63, 30, 0.22); }

@media (max-width: 900px) {
  .section-header { flex-direction: column; }
  .provider-card { flex-basis: 100%; min-width: 0; }
}
</style>
