<template>
  <div class="wiki-config">
    <div class="config-header">
      <h3 class="config-title">{{ t('wiki.configTitle') }}</h3>
      <p class="config-desc">{{ t('wiki.configDesc') }}</p>
    </div>

    <textarea
      v-model="configContent"
      class="config-editor"
      rows="20"
      :placeholder="t('wiki.configPlaceholder')"
    ></textarea>

    <div class="config-actions">
      <button class="btn-secondary" @click="loadConfig">{{ t('common.reset') }}</button>
      <button class="btn-primary" @click="saveConfig" :disabled="saving">
        {{ saving ? 'Saving...' : t('common.save') }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useWikiStore } from '@/stores/useWikiStore'
import { wikiApi } from '@/api/index'

const { t } = useI18n()
const store = useWikiStore()

const configContent = ref('')
const saving = ref(false)

async function loadConfig() {
  if (!store.currentKB) return
  try {
    const res: any = await wikiApi.getConfig(store.currentKB.id)
    configContent.value = res.data?.content || ''
  } catch (e) {
    console.error('Failed to load config', e)
  }
}

async function saveConfig() {
  if (!store.currentKB) return
  saving.value = true
  try {
    await wikiApi.updateConfig(store.currentKB.id, configContent.value)
  } catch (e) {
    console.error('Failed to save config', e)
  } finally {
    saving.value = false
  }
}

watch(() => store.currentKB, () => {
  loadConfig()
}, { immediate: true })
</script>

<style scoped>
/* Header */
.config-header { margin-bottom: 16px; }
.config-title { font-size: 15px; font-weight: 600; color: var(--mc-text-primary); margin: 0 0 4px; }
.config-desc { font-size: 13px; color: var(--mc-text-tertiary); margin: 0; }

/* Editor */
.config-editor { width: 100%; padding: 16px; border: 1px solid var(--mc-border); border-radius: 12px; font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace; font-size: 13px; line-height: 1.7; resize: vertical; background: var(--mc-bg-elevated); color: var(--mc-text-primary); outline: none; }
.config-editor:focus { border-color: var(--mc-primary); box-shadow: 0 0 0 2px rgba(217,119,87,0.1); }

/* Actions */
.config-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }

/* Buttons */
.btn-primary { display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px; background: var(--mc-primary); color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-primary:hover { background: var(--mc-primary-hover); }
.btn-primary:disabled { background: var(--mc-border); cursor: not-allowed; }
.btn-secondary { display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px; background: var(--mc-bg-elevated); color: var(--mc-text-primary); border: 1px solid var(--mc-border); border-radius: 8px; font-size: 14px; cursor: pointer; }
.btn-secondary:hover { background: var(--mc-bg-sunken); }
</style>
