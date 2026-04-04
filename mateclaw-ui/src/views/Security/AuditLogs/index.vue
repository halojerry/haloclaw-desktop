<template>
  <div class="settings-section">
    <div class="section-header">
      <div>
        <h2 class="section-title">{{ t('security.audit.title') }}</h2>
        <p class="section-desc">{{ t('security.audit.desc') }}</p>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-value">{{ auditStats.total || 0 }}</div>
        <div class="stat-label">{{ t('security.audit.stats.total') }}</div>
      </div>
      <div class="stat-card stat-blocked">
        <div class="stat-value">{{ auditStats.blocked || 0 }}</div>
        <div class="stat-label">{{ t('security.audit.stats.blocked') }}</div>
      </div>
      <div class="stat-card stat-approval">
        <div class="stat-value">{{ auditStats.needsApproval || 0 }}</div>
        <div class="stat-label">{{ t('security.audit.stats.needsApproval') }}</div>
      </div>
      <div class="stat-card stat-allowed">
        <div class="stat-value">{{ auditStats.allowed || 0 }}</div>
        <div class="stat-label">{{ t('security.audit.stats.allowed') }}</div>
      </div>
    </div>

    <!-- Filters -->
    <div class="filter-row">
      <input
        v-model="auditFilters.toolName"
        :placeholder="t('security.audit.filters.toolName')"
        class="filter-input"
      />
      <select v-model="auditFilters.decision" class="filter-select">
        <option value="">{{ t('security.audit.filters.decision') }}</option>
        <option value="ALLOW">{{ t('security.decision.ALLOW') }}</option>
        <option value="NEEDS_APPROVAL">{{ t('security.decision.NEEDS_APPROVAL') }}</option>
        <option value="BLOCK">{{ t('security.decision.BLOCK') }}</option>
      </select>
      <button class="btn-secondary" @click="loadAuditLogs">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="1 4 1 10 7 10"/>
          <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
        </svg>
      </button>
    </div>

    <!-- Audit Table -->
    <div class="rules-table-wrapper">
      <table class="rules-table">
        <thead>
          <tr>
            <th>{{ t('security.audit.columns.time') }}</th>
            <th>{{ t('security.audit.columns.tool') }}</th>
            <th>{{ t('security.audit.columns.decision') }}</th>
            <th>{{ t('security.audit.columns.severity') }}</th>
            <th>{{ t('security.audit.columns.conversationId') }}</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <template v-for="log in auditLogs" :key="log.id">
            <tr>
              <td class="cell-time">{{ formatTime(log.createTime) }}</td>
              <td><code class="tool-name-code">{{ log.toolName }}</code></td>
              <td>
                <span class="decision-badge" :class="'decision-' + log.decision?.toLowerCase()">
                  {{ t('security.decision.' + log.decision) || log.decision }}
                </span>
              </td>
              <td>
                <span v-if="log.maxSeverity" class="severity-badge" :class="'severity-' + log.maxSeverity?.toLowerCase()">
                  {{ t('security.severity.' + log.maxSeverity) || log.maxSeverity }}
                </span>
              </td>
              <td class="cell-conv">{{ truncateConvId(log.conversationId) }}</td>
              <td>
                <button
                  v-if="log.findingsJson"
                  class="action-btn"
                  @click="toggleExpand(log.id)"
                  :title="t('security.audit.expandFindings')"
                >
                  <svg
                    width="14" height="14" viewBox="0 0 24 24" fill="none"
                    stroke="currentColor" stroke-width="2"
                    :style="{ transform: expandedRows.has(log.id) ? 'rotate(180deg)' : '' }"
                  >
                    <polyline points="6 9 12 15 18 9"/>
                  </svg>
                </button>
              </td>
            </tr>
            <tr v-if="expandedRows.has(log.id)" class="expanded-row">
              <td colspan="6">
                <div class="findings-detail">
                  <div v-for="(finding, idx) in parseFindings(log.findingsJson)" :key="idx" class="finding-item">
                    <span class="severity-badge severity-sm" :class="'severity-' + finding.severity?.toLowerCase()">
                      {{ finding.severity }}
                    </span>
                    <span class="finding-category">{{ finding.category }}</span>
                    <span class="finding-title">{{ finding.title }}</span>
                    <span v-if="finding.remediation" class="finding-remediation">{{ finding.remediation }}</span>
                  </div>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
      <div v-if="!auditLogs.length" class="empty-state">{{ t('security.audit.noLogs') }}</div>
    </div>

    <!-- Pagination -->
    <div v-if="auditTotal > auditPageSize" class="pagination">
      <button
        class="btn-secondary btn-sm"
        :disabled="auditPage <= 1"
        @click="auditPage--; loadAuditLogs()"
      >&laquo;</button>
      <span class="page-info">{{ auditPage }} / {{ Math.ceil(auditTotal / auditPageSize) }}</span>
      <button
        class="btn-secondary btn-sm"
        :disabled="auditPage >= Math.ceil(auditTotal / auditPageSize)"
        @click="auditPage++; loadAuditLogs()"
      >&raquo;</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { securityApi } from '@/api'
import { parseFindings, formatTime, truncateConvId } from '../composables/helpers'
import type { AuditStats } from '@/types'

const { t } = useI18n()

const auditLogs = ref<any[]>([])
const auditStats = reactive<AuditStats>({ total: 0, blocked: 0, needsApproval: 0, allowed: 0 })
const auditPage = ref(1)
const auditPageSize = 20
const auditTotal = ref(0)
const auditFilters = reactive({ toolName: '', decision: '' })
const expandedRows = ref(new Set<number>())

async function loadAuditLogs() {
  try {
    const params: any = {
      page: auditPage.value,
      size: auditPageSize,
    }
    if (auditFilters.toolName) params.toolName = auditFilters.toolName
    if (auditFilters.decision) params.decision = auditFilters.decision
    const res: any = await securityApi.listAuditLogs(params)
    auditLogs.value = res.data?.records || []
    auditTotal.value = res.data?.total || 0
  } catch {
    // ignore
  }
}

async function loadAuditStats() {
  try {
    const res: any = await securityApi.getAuditStats()
    Object.assign(auditStats, res.data || {})
  } catch {
    // ignore
  }
}

function toggleExpand(id: number) {
  if (expandedRows.value.has(id)) {
    expandedRows.value.delete(id)
  } else {
    expandedRows.value.add(id)
  }
  expandedRows.value = new Set(expandedRows.value)
}

onMounted(async () => {
  await Promise.all([loadAuditLogs(), loadAuditStats()])
})
</script>

<style>
@import '../shared.css';
</style>

<style scoped>
/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--mc-bg-elevated);
  border: 1px solid var(--mc-border-light);
  border-radius: 10px;
  padding: 16px 20px;
  text-align: center;
}

.stat-value { font-size: 28px; font-weight: 700; color: var(--mc-text-primary); }
.stat-label { font-size: 12px; color: var(--mc-text-tertiary); margin-top: 4px; }
.stat-blocked .stat-value { color: #ef4444; }
.stat-approval .stat-value { color: #f59e0b; }
.stat-allowed .stat-value { color: #10b981; }

/* Filters */
.filter-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.filter-input {
  padding: 6px 12px;
  border: 1px solid var(--mc-border);
  border-radius: 6px;
  background: var(--mc-bg);
  color: var(--mc-text-primary);
  font-size: 13px;
  flex: 1;
  max-width: 200px;
}

.filter-select {
  padding: 6px 12px;
  border: 1px solid var(--mc-border);
  border-radius: 6px;
  background: var(--mc-bg);
  color: var(--mc-text-primary);
  font-size: 13px;
}

/* Audit table specific */
.cell-time { font-size: 12px; color: var(--mc-text-tertiary); white-space: nowrap; }
.cell-conv { font-size: 12px; font-family: 'SF Mono', monospace; color: var(--mc-text-tertiary); }
.tool-name-code {
  padding: 2px 6px;
  background: var(--mc-bg-sunken);
  border-radius: 4px;
  font-size: 12px;
}

.expanded-row td {
  padding: 0 14px 14px;
  background: var(--mc-bg-sunken);
}

.findings-detail {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.finding-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.finding-category {
  font-family: 'SF Mono', monospace;
  color: var(--mc-text-tertiary);
  font-size: 11px;
}

.finding-title { color: var(--mc-text-primary); }
.finding-remediation { color: var(--mc-text-tertiary); font-style: italic; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.page-info { font-size: 13px; color: var(--mc-text-tertiary); }
</style>
