<template>
  <el-dialog
    v-model="visible"
    :title="t('store.profitConfig.title')"
    width="550px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      label-position="left"
    >
      <!-- 基础配置 -->
      <div class="config-section">
        <h4 class="section-title">{{ t('store.profitConfig.sections.basic') }}</h4>

        <el-form-item :label="t('store.profitConfig.fields.minProfitRate')" prop="minProfitRate">
          <el-input-number
            v-model="formData.minProfitRate"
            :min="0"
            :max="100"
            :step="1"
            :precision="0"
            style="width: 100%"
          >
            <template #suffix>%</template>
          </el-input-number>
          <div class="field-hint">{{ t('store.profitConfig.hints.minProfitRate') }}</div>
        </el-form-item>

        <el-form-item :label="t('store.profitConfig.fields.targetProfitRate')" prop="targetProfitRate">
          <el-input-number
            v-model="formData.targetProfitRate"
            :min="0"
            :max="100"
            :step="1"
            :precision="0"
            style="width: 100%"
          >
            <template #suffix>%</template>
          </el-input-number>
          <div class="field-hint">{{ t('store.profitConfig.hints.targetProfitRate') }}</div>
        </el-form-item>
      </div>

      <!-- 成本配置 -->
      <div class="config-section">
        <h4 class="section-title">{{ t('store.profitConfig.sections.cost') }}</h4>

        <el-form-item :label="t('store.profitConfig.fields.exchangeRate')" prop="exchangeRate">
          <el-input-number
            v-model="formData.exchangeRate"
            :min="0.1"
            :max="100"
            :step="0.01"
            :precision="2"
            style="width: 100%"
          />
          <div class="field-hint">{{ t('store.profitConfig.hints.exchangeRate') }}</div>
        </el-form-item>

        <el-form-item :label="t('store.profitConfig.fields.logisticsRate')" prop="logisticsRate">
          <el-input-number
            v-model="formData.logisticsRate"
            :min="0"
            :max="100"
            :step="0.5"
            :precision="1"
            style="width: 100%"
          >
            <template #suffix>%</template>
          </el-input-number>
          <div class="field-hint">{{ t('store.profitConfig.hints.logisticsRate') }}</div>
        </el-form-item>

        <el-form-item :label="t('store.profitConfig.fields.platformCommission')" prop="platformCommission">
          <el-input-number
            v-model="formData.platformCommission"
            :min="0"
            :max="100"
            :step="0.5"
            :precision="1"
            style="width: 100%"
          >
            <template #suffix>%</template>
          </el-input-number>
          <div class="field-hint">{{ t('store.profitConfig.hints.platformCommission') }}</div>
        </el-form-item>
      </div>

      <!-- 价格预览 -->
      <div class="config-section preview-section">
        <h4 class="section-title">{{ t('store.profitConfig.preview.title') }}</h4>
        <div class="preview-card">
          <div class="preview-row">
            <span class="preview-label">{{ t('store.profitConfig.preview.purchasePrice') }}</span>
            <span class="preview-value">{{ formData.purchasePrice }} CNY</span>
          </div>
          <div class="preview-formula">
            <span>{{ t('store.profitConfig.preview.sellingPrice') }} = </span>
            <span class="formula-highlight">{{ formData.purchasePrice }} × {{ formData.exchangeRate }} × (1 + {{ formData.logisticsRate }}% + {{ formData.platformCommission }}%) ÷ (1 - {{ formData.targetProfitRate }}%)</span>
          </div>
          <div class="preview-result">
            <span class="result-label">{{ t('store.profitConfig.preview.estimatedPrice') }}</span>
            <span class="result-value">{{ estimatedPrice }} RUB</span>
          </div>
        </div>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">{{ t('common.cancel') }}</el-button>
      <el-button @click="handleReset">{{ t('store.profitConfig.reset') }}</el-button>
      <el-button type="primary" @click="handleSave" :loading="saving">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

export interface ProfitConfig {
  minProfitRate: number
  targetProfitRate: number
  exchangeRate: number
  logisticsRate: number
  platformCommission: number
  purchasePrice?: number
}

const props = defineProps<{
  modelValue: boolean
  storeId: string
  config?: ProfitConfig | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', config: ProfitConfig): void
}>()

const { t } = useI18n()
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const formRef = ref<FormInstance>()
const saving = ref(false)

// 默认配置
const defaultConfig: ProfitConfig = {
  minProfitRate: 15,
  targetProfitRate: 25,
  exchangeRate: 12.5,
  logisticsRate: 15,
  platformCommission: 12
}

const formData = reactive<ProfitConfig>({
  minProfitRate: 15,
  targetProfitRate: 25,
  exchangeRate: 12.5,
  logisticsRate: 15,
  platformCommission: 12,
  purchasePrice: 100
})

// 表单验证规则
const formRules: FormRules = {
  minProfitRate: [
    { required: true, message: t('store.profitConfig.errors.minProfitRateRequired'), trigger: 'blur' }
  ],
  targetProfitRate: [
    { required: true, message: t('store.profitConfig.errors.targetProfitRateRequired'), trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value <= formData.minProfitRate) {
          callback(new Error(t('store.profitConfig.errors.targetMustGreaterThanMin')))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  exchangeRate: [
    { required: true, message: t('store.profitConfig.errors.exchangeRateRequired'), trigger: 'blur' }
  ],
  logisticsRate: [
    { required: true, message: t('store.profitConfig.errors.logisticsRateRequired'), trigger: 'blur' }
  ],
  platformCommission: [
    { required: true, message: t('store.profitConfig.errors.platformCommissionRequired'), trigger: 'blur' }
  ]
}

// 计算预估售价
const estimatedPrice = computed(() => {
  const purchasePrice = formData.purchasePrice || 100
  const costMultiplier = 1 + formData.logisticsRate / 100 + formData.platformCommission / 100
  const profitMultiplier = 1 - formData.targetProfitRate / 100
  const price = (purchasePrice * formData.exchangeRate * costMultiplier) / profitMultiplier
  return price.toFixed(2)
})

// 监听配置变化
watch(() => props.config, (newConfig) => {
  if (newConfig) {
    Object.assign(formData, newConfig)
  } else {
    Object.assign(formData, defaultConfig)
  }
}, { immediate: true })

// 关闭对话框
function handleClose() {
  visible.value = false
}

// 重置表单
function handleReset() {
  Object.assign(formData, defaultConfig)
  formRef.value?.clearValidate()
}

// 保存配置
async function handleSave() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    saving.value = true

    // 模拟保存
    await new Promise(resolve => setTimeout(resolve, 500))

    emit('save', { ...formData })
    ElMessage.success(t('store.profitConfig.saveSuccess'))
    handleClose()
  } catch (error) {
    // 验证失败
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.config-section {
  margin-bottom: 20px;
  padding: 16px;
  background: var(--ozon-bg);
  border-radius: 8px;
}

.section-title {
  margin: 0 0 16px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--ozon-text-primary);
}

.field-hint {
  font-size: 12px;
  color: var(--ozon-text-tertiary);
  margin-top: 4px;
  line-height: 1.4;
}

.preview-section {
  background: var(--ozon-bg-inset);
}

.preview-card {
  background: var(--ozon-bg-card);
  border-radius: 8px;
  padding: 16px;
}

.preview-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.preview-label {
  font-size: 13px;
  color: var(--ozon-text-secondary);
}

.preview-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--ozon-text-primary);
}

.preview-formula {
  font-size: 12px;
  color: var(--ozon-text-tertiary);
  padding: 8px;
  background: var(--ozon-bg);
  border-radius: 4px;
  margin-bottom: 12px;
  word-break: break-all;
}

.formula-highlight {
  color: var(--ozon-primary);
}

.preview-result {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--ozon-border-light);
}

.result-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--ozon-text-primary);
}

.result-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--ozon-primary);
}
</style>
