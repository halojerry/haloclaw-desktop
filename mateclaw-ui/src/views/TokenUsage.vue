<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ t('tokenUsage.title') }}</h1>
        <p class="page-desc">{{ t('tokenUsage.desc') }}</p>
      </div>
      <div class="header-actions">
        <div class="date-range">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="–"
            start-placeholder="Start"
            end-placeholder="End"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :clearable="false"
            size="default"
          />
        </div>
        <button class="action-btn" :disabled="loading" @click="fetchData">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10"/>
            <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
          </svg>
          {{ t('tokenUsage.refresh') }}
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading && !data" class="loading-state">
      <div class="spinner"></div>
      <p>{{ t('common.loading') }}</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="empty-state">
      <span class="empty-icon">⚠️</span>
      <p>{{ error }}</p>
      <button class="action-btn" @click="fetchData">{{ t('tokenUsage.refresh') }}</button>
    </div>

    <!-- Content -->
    <template v-else-if="data">
      <!-- Summary cards -->
      <div v-if="data.totalMessages > 0" class="summary-cards">
        <div class="summary-card">
          <div class="card-value">{{ formatNumber(data.totalPromptTokens) }}</div>
          <div class="card-label">{{ t('tokenUsage.promptTokens') }}</div>
        </div>
        <div class="summary-card">
          <div class="card-value">{{ formatNumber(data.totalCompletionTokens) }}</div>
          <div class="card-label">{{ t('tokenUsage.completionTokens') }}</div>
        </div>
        <div class="summary-card">
          <div class="card-value">{{ formatNumber(data.totalMessages) }}</div>
          <div class="card-label">{{ t('tokenUsage.assistantMessages') }}</div>
        </div>
      </div>

      <!-- No data -->
      <div v-if="data.totalMessages === 0" class="empty-state">
        <span class="empty-icon">📊</span>
        <p>{{ t('tokenUsage.noData') }}</p>
      </div>

      <!-- By Model Table -->
      <div v-if="data.byModel && data.byModel.length > 0" class="table-section">
        <h2 class="section-title">{{ t('tokenUsage.byModel') }}</h2>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>{{ t('tokenUsage.provider') }}</th>
                <th>{{ t('tokenUsage.model') }}</th>
                <th class="num-col">{{ t('tokenUsage.promptTokens') }}</th>
                <th class="num-col">{{ t('tokenUsage.completionTokens') }}</th>
                <th class="num-col">{{ t('tokenUsage.messageCount') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, idx) in data.byModel" :key="idx">
                <td><span class="mono-text">{{ item.runtimeProvider || '-' }}</span></td>
                <td><span class="mono-text">{{ item.runtimeModel || '-' }}</span></td>
                <td class="num-col">{{ formatNumber(item.promptTokens) }}</td>
                <td class="num-col">{{ formatNumber(item.completionTokens) }}</td>
                <td class="num-col">{{ formatNumber(item.messageCount) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- By Date Table -->
      <div v-if="data.byDate && data.byDate.length > 0" class="table-section">
        <h2 class="section-title">{{ t('tokenUsage.byDate') }}</h2>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>{{ t('tokenUsage.date') }}</th>
                <th class="num-col">{{ t('tokenUsage.promptTokens') }}</th>
                <th class="num-col">{{ t('tokenUsage.completionTokens') }}</th>
                <th class="num-col">{{ t('tokenUsage.messageCount') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in data.byDate" :key="item.date">
                <td>{{ item.date }}</td>
                <td class="num-col">{{ formatNumber(item.promptTokens) }}</td>
                <td class="num-col">{{ formatNumber(item.completionTokens) }}</td>
                <td class="num-col">{{ formatNumber(item.messageCount) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { tokenUsageApi } from '@/api/index'
import type { TokenUsageSummary } from '@/types/tokenUsage'

const { t } = useI18n()
const loading = ref(false)
const error = ref<string | null>(null)
const data = ref<TokenUsageSummary | null>(null)

// Default: last 30 days
const today = new Date()
const thirtyDaysAgo = new Date(today)
thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)

function toDateStr(d: Date): string {
  return d.toISOString().slice(0, 10)
}

const dateRange = ref<[string, string]>([toDateStr(thirtyDaysAgo), toDateStr(today)])

function formatNumber(n: number): string {
  if (n == null) return '0'
  return n.toLocaleString()
}

async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const res: any = await tokenUsageApi.getSummary({
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
    })
    data.value = res.data || null
  } catch (e: any) {
    const msg = t('tokenUsage.loadFailed')
    ElMessage.error(msg)
    error.value = msg
    data.value = null
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.page-container {
  height: 100%;
  overflow-y: auto;
  padding: 24px;
  background: var(--mc-bg);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--mc-text-primary);
  margin: 0 0 4px;
}

.page-desc {
  font-size: 14px;
  color: var(--mc-text-secondary);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--mc-bg-elevated);
  border: 1px solid var(--mc-border);
  border-radius: 8px;
  color: var(--mc-text-primary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.action-btn:hover {
  background: var(--mc-bg-sunken);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Summary cards */
.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.summary-card {
  background: var(--mc-bg-elevated);
  border: 1px solid var(--mc-border);
  border-radius: 12px;
  padding: 20px;
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--mc-text-primary);
  margin-bottom: 4px;
  font-variant-numeric: tabular-nums;
}

.card-label {
  font-size: 13px;
  color: var(--mc-text-secondary);
}

/* Tables */
.table-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--mc-text-primary);
  margin: 0 0 12px;
}

.table-wrap {
  background: var(--mc-bg-elevated);
  border: 1px solid var(--mc-border);
  border-radius: 12px;
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  padding: 12px 16px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: var(--mc-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  background: var(--mc-bg-sunken);
  border-bottom: 1px solid var(--mc-border);
}

.data-table td {
  padding: 14px 16px;
  font-size: 14px;
  color: var(--mc-text-primary);
  border-bottom: 1px solid var(--mc-border-light);
}

.data-table tr:last-child td {
  border-bottom: none;
}

.data-table tr:hover td {
  background: var(--mc-bg-sunken);
}

.num-col {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.mono-text {
  font-family: monospace;
  font-size: 13px;
}

/* States */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  gap: 12px;
  color: var(--mc-text-tertiary);
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--mc-border);
  border-top-color: var(--mc-primary, #D97757);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 60px 0;
  color: var(--mc-text-tertiary);
}

.empty-icon {
  font-size: 32px;
}

.empty-state p {
  font-size: 14px;
  margin: 0;
}

/* Date picker overrides */
.date-range :deep(.el-date-editor) {
  --el-date-editor-width: 260px;
}

/* Responsive */
@media (max-width: 768px) {
  .summary-cards {
    grid-template-columns: 1fr;
  }
  .page-header {
    flex-direction: column;
  }
}
</style>
