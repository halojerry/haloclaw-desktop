<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ t('cronJobs.title') }}</h1>
        <p class="page-desc">{{ t('cronJobs.desc') }}</p>
      </div>
      <button class="btn-primary" @click="openCreateModal">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        {{ t('cronJobs.createJob') }}
      </button>
    </div>

    <!-- 任务列表 -->
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>{{ t('cronJobs.columns.name') }}</th>
            <th>{{ t('cronJobs.columns.agent') }}</th>
            <th>{{ t('cronJobs.columns.taskType') }}</th>
            <th>{{ t('cronJobs.columns.cron') }}</th>
            <th>{{ t('cronJobs.columns.nextRun') }}</th>
            <th>{{ t('cronJobs.columns.lastRun') }}</th>
            <th>{{ t('cronJobs.columns.enabled') }}</th>
            <th>{{ t('cronJobs.columns.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="job in store.jobs" :key="job.id" class="data-row">
            <td>
              <div class="job-name">{{ job.name }}</div>
            </td>
            <td>
              <span class="agent-badge">{{ job.agentName || 'Unknown' }}</span>
            </td>
            <td>
              <span class="type-badge" :class="'type-' + job.taskType">
                {{ t('cronJobs.taskTypes.' + job.taskType) }}
              </span>
            </td>
            <td>
              <code class="cron-code" :title="cronToHumanReadable(job.cronExpression, job.timezone)">
                {{ job.cronExpression }}
              </code>
              <div class="cron-readable">{{ cronToHumanReadable(job.cronExpression, job.timezone) }}</div>
            </td>
            <td>
              <span v-if="job.nextRunTime" class="time-text">{{ formatTime(job.nextRunTime) }}</span>
              <span v-else class="time-empty">-</span>
            </td>
            <td>
              <span v-if="job.lastRunTime" class="time-text">{{ formatTime(job.lastRunTime) }}</span>
              <span v-else class="time-empty">-</span>
            </td>
            <td>
              <label class="toggle-switch">
                <input type="checkbox" :checked="job.enabled" @change="handleToggle(job)" />
                <span class="toggle-slider"></span>
              </label>
            </td>
            <td>
              <div class="row-actions">
                <button class="row-btn" :title="t('cronJobs.actions.runNow')" @click="handleRunNow(job)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polygon points="5 3 19 12 5 21 5 3"/>
                  </svg>
                </button>
                <button class="row-btn" :title="t('cronJobs.actions.edit')" @click="openEditModal(job)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                </button>
                <button class="row-btn danger" :title="t('cronJobs.actions.delete')" @click="handleDelete(job)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
                  </svg>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="store.jobs.length === 0">
            <td colspan="8" class="empty-row">
              <div class="empty-state">
                <span class="empty-icon">&#9201;</span>
                <p>{{ t('cronJobs.noJobs') }}</p>
                <button class="btn-primary btn-sm" @click="openCreateModal">{{ t('cronJobs.createFirst') }}</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新建/编辑弹窗 -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal modal-lg">
        <div class="modal-header">
          <h2>{{ editing ? t('cronJobs.editJob') : t('cronJobs.createJob') }}</h2>
          <button class="modal-close" @click="closeModal">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <!-- 名称 -->
          <div class="form-group">
            <label class="form-label">{{ t('cronJobs.fields.name') }} *</label>
            <input v-model="form.name" class="form-input" :placeholder="t('cronJobs.fields.namePlaceholder')" />
          </div>

          <!-- 关联 Agent -->
          <div class="form-group">
            <label class="form-label">{{ t('cronJobs.fields.agent') }} *</label>
            <select v-model="form.agentId" class="form-input">
              <option :value="undefined" disabled>{{ t('cronJobs.fields.agentPlaceholder') }}</option>
              <option v-for="a in agents" :key="a.id" :value="a.id">{{ a.name }}</option>
            </select>
          </div>

          <!-- 任务类型 -->
          <div class="form-group">
            <label class="form-label">{{ t('cronJobs.fields.taskType') }}</label>
            <div class="radio-group">
              <label class="radio-option" :class="{ active: form.taskType === 'text' }">
                <input type="radio" v-model="form.taskType" value="text" />
                {{ t('cronJobs.taskTypes.text') }}
              </label>
              <label class="radio-option" :class="{ active: form.taskType === 'agent' }">
                <input type="radio" v-model="form.taskType" value="agent" />
                {{ t('cronJobs.taskTypes.agent') }}
              </label>
            </div>
          </div>

          <!-- 触发消息 / 执行目标 -->
          <div v-if="form.taskType === 'text'" class="form-group">
            <label class="form-label">{{ t('cronJobs.fields.triggerMessage') }} *</label>
            <textarea v-model="form.triggerMessage" class="form-textarea" rows="3"
              :placeholder="t('cronJobs.fields.triggerMessagePlaceholder')"></textarea>
          </div>
          <div v-else class="form-group">
            <label class="form-label">{{ t('cronJobs.fields.requestBody') }} *</label>
            <textarea v-model="form.requestBody" class="form-textarea" rows="3"
              :placeholder="t('cronJobs.fields.requestBodyPlaceholder')"></textarea>
          </div>

          <!-- Cron 频率 -->
          <div class="form-group">
            <label class="form-label">{{ t('cronJobs.fields.cronFrequency') }}</label>
            <div class="radio-group">
              <label v-for="ct in cronTypeOptions" :key="ct" class="radio-option" :class="{ active: cronType === ct }">
                <input type="radio" v-model="cronType" :value="ct" />
                {{ t('cronJobs.cronTypes.' + ct) }}
              </label>
            </div>
          </div>

          <!-- 时间选择器（daily/weekly） -->
          <div v-if="cronType === 'daily' || cronType === 'weekly'" class="form-row">
            <div v-if="cronType === 'weekly'" class="form-group">
              <label class="form-label">{{ t('cronJobs.fields.cronDays') }}</label>
              <div class="day-picker">
                <label v-for="(dayKey, idx) in dayKeys" :key="dayKey" class="day-chip"
                  :class="{ active: selectedDays.includes(idx + 1) }">
                  <input type="checkbox" :value="idx + 1"
                    :checked="selectedDays.includes(idx + 1)"
                    @change="toggleDay(idx + 1)" />
                  {{ t('cronJobs.days.' + dayKey) }}
                </label>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">{{ t('cronJobs.fields.cronTime') }}</label>
              <input type="time" v-model="cronTime" class="form-input" />
            </div>
          </div>

          <!-- 自定义表达式 -->
          <div v-if="cronType === 'custom'" class="form-group">
            <label class="form-label">{{ t('cronJobs.fields.cronExpression') }}</label>
            <input v-model="form.cronExpression" class="form-input mono"
              :placeholder="t('cronJobs.fields.cronExpressionPlaceholder')" />
          </div>

          <!-- 时区 -->
          <div class="form-group">
            <label class="form-label">{{ t('cronJobs.fields.timezone') }}</label>
            <select v-model="form.timezone" class="form-input">
              <option v-for="tz in timezones" :key="tz" :value="tz">{{ tz }}</option>
            </select>
          </div>

          <!-- 启用 -->
          <div class="form-group">
            <label class="toggle-label">
              <label class="toggle-switch">
                <input type="checkbox" v-model="form.enabled" />
                <span class="toggle-slider"></span>
              </label>
              <span>{{ t('cronJobs.fields.enabled') }}</span>
            </label>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeModal">{{ t('common.cancel') }}</button>
          <button class="btn-primary" @click="saveJob" :disabled="!canSave">{{ t('common.save') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCronJobStore } from '@/stores/useCronJobStore'
import { useAgentStore } from '@/stores/useAgentStore'
import type { CronJob } from '@/types/index'

const { t } = useI18n()
const store = useCronJobStore()
const agentStore = useAgentStore()
const agents = computed(() => agentStore.agents)

const showModal = ref(false)
const editing = ref<CronJob | null>(null)

const cronTypeOptions = ['hourly', 'daily', 'weekly', 'custom'] as const
const cronType = ref<string>('daily')
const cronTime = ref('09:00')
const selectedDays = ref<number[]>([1, 2, 3, 4, 5])
const dayKeys = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun']

const timezones = [
  'Asia/Shanghai', 'Asia/Tokyo', 'Asia/Seoul', 'Asia/Singapore',
  'Asia/Kolkata', 'UTC', 'America/New_York', 'America/Chicago',
  'America/Los_Angeles', 'Europe/London', 'Europe/Berlin', 'Europe/Paris',
  'Australia/Sydney',
]

const defaultForm = (): Partial<CronJob> => ({
  name: '',
  cronExpression: '',
  timezone: 'Asia/Shanghai',
  agentId: undefined,
  taskType: 'text',
  triggerMessage: '',
  requestBody: '',
  enabled: true,
})
const form = ref<any>(defaultForm())

const canSave = computed(() => {
  if (!form.value.name || !form.value.agentId) return false
  if (form.value.taskType === 'text' && !form.value.triggerMessage) return false
  if (form.value.taskType === 'agent' && !form.value.requestBody) return false
  if (cronType.value === 'custom' && !form.value.cronExpression?.trim()) return false
  return true
})

onMounted(() => {
  store.fetchJobs()
  agentStore.fetchAgents()
})

// 根据 cronType/cronTime/selectedDays 自动生成 cronExpression
watch([cronType, cronTime, selectedDays], () => {
  if (cronType.value === 'custom') return
  const [h, m] = cronTime.value.split(':').map(Number)
  if (cronType.value === 'hourly') {
    form.value.cronExpression = '0 * * * *'
  } else if (cronType.value === 'daily') {
    form.value.cronExpression = `${m} ${h} * * *`
  } else if (cronType.value === 'weekly') {
    const days = selectedDays.value.length > 0 ? selectedDays.value.sort().join(',') : '*'
    form.value.cronExpression = `${m} ${h} * * ${days}`
  }
}, { deep: true })

function toggleDay(day: number) {
  const idx = selectedDays.value.indexOf(day)
  if (idx >= 0) {
    selectedDays.value.splice(idx, 1)
  } else {
    selectedDays.value.push(day)
  }
}

function openCreateModal() {
  editing.value = null
  form.value = defaultForm()
  cronType.value = 'daily'
  cronTime.value = '09:00'
  selectedDays.value = [1, 2, 3, 4, 5]
  showModal.value = true
}

function openEditModal(job: CronJob) {
  editing.value = job
  form.value = { ...job }
  const parsed = parseCronToForm(job.cronExpression)
  cronType.value = parsed.type
  cronTime.value = parsed.time
  selectedDays.value = [...parsed.days]
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editing.value = null
}

async function saveJob() {
  try {
    if (editing.value) {
      await store.updateJob(editing.value.id, form.value)
      ElMessage.success(t('cronJobs.messages.updateSuccess'))
    } else {
      await store.createJob(form.value)
      ElMessage.success(t('cronJobs.messages.createSuccess'))
    }
    closeModal()
    // store.createJob / updateJob 已在本地数组更新，无需再全量刷新
  } catch (e: any) {
    ElMessage.error(e?.message || e)
  }
}

async function handleDelete(job: CronJob) {
  try {
    await ElMessageBox.confirm(
      t('cronJobs.messages.deleteConfirm', { name: job.name }),
      { type: 'warning' },
    )
  } catch { return }
  try {
    await store.deleteJob(job.id)
    ElMessage.success(t('cronJobs.messages.deleteSuccess'))
  } catch (e: any) {
    ElMessage.error(e?.message || e)
  }
}

async function handleToggle(job: CronJob) {
  try {
    const newEnabled = !job.enabled
    await store.toggleJob(job.id, newEnabled)
    ElMessage.success(newEnabled ? t('cronJobs.messages.enableSuccess') : t('cronJobs.messages.disableSuccess'))
    store.fetchJobs()
  } catch (e: any) {
    ElMessage.error(e?.message || e)
  }
}

async function handleRunNow(job: CronJob) {
  try {
    await store.runNow(job.id)
    ElMessage.success(t('cronJobs.messages.runTriggered', { id: job.id }))
    // Agent 执行完成后刷新列表，让 lastRunTime 显示最新值
    setTimeout(() => store.fetchJobs(), 3000)
  } catch (e: any) {
    ElMessage.error(e?.message || e)
  }
}

// ==================== Cron 工具函数 ====================

interface CronFormParts {
  type: string
  time: string
  days: number[]
}

/** dow 是否为纯整数逗号列表，如 "1"、"1,3,5"，不含范围/步长 */
function isSimpleIntList(s: string): boolean {
  return s.split(',').every((v) => /^\d+$/.test(v.trim()))
}

function parseCronToForm(expr: string): CronFormParts {
  const parts = expr.trim().split(/\s+/)
  if (parts.length !== 5) return { type: 'custom', time: '09:00', days: [] }

  const [min, hour, dom, mon, dow] = parts

  // 每小时
  if (min === '0' && hour === '*' && dom === '*' && mon === '*' && dow === '*') {
    return { type: 'hourly', time: '00:00', days: [] }
  }

  // min / hour 必须是纯整数，否则直接降级 custom（如 */5 * * * *）
  if (!/^\d+$/.test(min) || !/^\d+$/.test(hour)) {
    return { type: 'custom', time: '09:00', days: [] }
  }

  const timeStr = pad(+hour) + ':' + pad(+min)

  // 每天（dom/mon/dow 均为 *）
  if (dom === '*' && mon === '*' && dow === '*') {
    return { type: 'daily', time: timeStr, days: [] }
  }

  // 每周 —— 严格要求：
  //   1. dom='*' mon='*'
  //   2. dow 是纯数字逗号列表（不含范围 1-5、步长 1-7/2 等）
  // 否则降级为 custom，防止 watch 把表达式改坏
  if (dom === '*' && mon === '*' && dow !== '*' && isSimpleIntList(dow)) {
    const days = dow.split(',').map(Number)
    return { type: 'weekly', time: timeStr, days }
  }

  // 其余所有情况（包含 1-5、*/2、dom/mon 非 * 等）一律 custom
  return { type: 'custom', time: timeStr, days: [] }
}

function pad(n: number): string {
  return n < 10 ? '0' + n : '' + n
}

function cronToHumanReadable(expr: string, timezone: string): string {
  const parts = expr.trim().split(/\s+/)
  if (parts.length !== 5) return expr

  const [min, hour, dom, mon, dow] = parts
  const tzLabel = timezone ? ` (${timezone})` : ''

  if (min === '0' && hour === '*' && dom === '*' && mon === '*' && dow === '*') {
    return t('cronJobs.cronTypes.hourly') + tzLabel
  }

  if (dom === '*' && mon === '*' && dow === '*' && !isNaN(+min) && !isNaN(+hour)) {
    return t('cronJobs.cronTypes.daily') + ' ' + pad(+hour) + ':' + pad(+min) + tzLabel
  }

  if (dom === '*' && mon === '*' && dow !== '*' && !isNaN(+min) && !isNaN(+hour)) {
    const dayNames = dow.split(',').map((d) => {
      const n = +d
      // 0 和 7 均表示 Sunday，映射到 dayKeys[6]；1=Mon→0 … 6=Sat→5
      const idx = n === 0 ? 6 : n - 1
      return idx >= 0 && idx < dayKeys.length ? t('cronJobs.days.' + dayKeys[idx]) : d
    })
    return t('cronJobs.cronTypes.weekly') + ' ' + dayNames.join(',') + ' ' + pad(+hour) + ':' + pad(+min) + tzLabel
  }

  return expr + tzLabel
}

function formatTime(datetime: string | undefined): string {
  if (!datetime) return '-'
  try {
    const d = new Date(datetime)
    return d.toLocaleString()
  } catch {
    return datetime
  }
}
</script>

<style scoped>
.page-container { height: 100%; overflow-y: auto; padding: 24px; background: var(--mc-bg); }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 24px; }
.page-title { font-size: 20px; font-weight: 700; color: var(--mc-text-primary); margin: 0 0 4px; }
.page-desc { font-size: 14px; color: var(--mc-text-secondary); margin: 0; }

.btn-primary { display: flex; align-items: center; gap: 6px; padding: 8px 16px; background: var(--mc-primary); color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-primary:hover { background: var(--mc-primary-hover); }
.btn-primary:disabled { background: var(--mc-border); cursor: not-allowed; }
.btn-primary.btn-sm { padding: 6px 14px; font-size: 13px; }
.btn-secondary { padding: 8px 16px; background: var(--mc-bg-elevated); color: var(--mc-text-primary); border: 1px solid var(--mc-border); border-radius: 8px; font-size: 14px; cursor: pointer; }
.btn-secondary:hover { background: var(--mc-bg-sunken); }

/* 表格 */
.table-wrap { background: var(--mc-bg-elevated); border: 1px solid var(--mc-border); border-radius: 12px; overflow: hidden; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { padding: 12px 16px; text-align: left; font-size: 12px; font-weight: 600; color: var(--mc-text-secondary); text-transform: uppercase; letter-spacing: 0.05em; background: var(--mc-bg-sunken); border-bottom: 1px solid var(--mc-border); white-space: nowrap; }
.data-row { border-bottom: 1px solid var(--mc-border-light); transition: background 0.1s; }
.data-row:hover { background: var(--mc-bg-sunken); }
.data-row:last-child { border-bottom: none; }
.data-table td { padding: 14px 16px; font-size: 14px; color: var(--mc-text-primary); }

.job-name { font-weight: 500; color: var(--mc-text-primary); }
.agent-badge { padding: 2px 8px; border-radius: 6px; font-size: 12px; background: var(--mc-bg-sunken); color: var(--mc-text-secondary); }
.type-badge { padding: 3px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.type-text { background: var(--mc-primary-bg); color: var(--mc-primary); }
.type-agent { background: var(--mc-success-bg, var(--mc-primary-bg)); color: var(--mc-success, var(--mc-primary-hover)); }
.cron-code { background: var(--mc-bg-sunken); padding: 2px 8px; border-radius: 4px; font-size: 12px; color: var(--mc-text-primary); font-family: monospace; }
.cron-readable { font-size: 11px; color: var(--mc-text-tertiary); margin-top: 2px; }
.time-text { font-size: 13px; color: var(--mc-text-secondary); }
.time-empty { color: var(--mc-text-tertiary); }

/* Toggle */
.toggle-switch { position: relative; display: inline-block; width: 36px; height: 20px; cursor: pointer; }
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.toggle-slider { position: absolute; inset: 0; background: var(--mc-border); border-radius: 20px; transition: 0.2s; }
.toggle-slider::before { content: ''; position: absolute; width: 14px; height: 14px; left: 3px; top: 3px; background: var(--mc-bg-elevated); border-radius: 50%; transition: 0.2s; }
.toggle-switch input:checked + .toggle-slider { background: var(--mc-primary); }
.toggle-switch input:checked + .toggle-slider::before { transform: translateX(16px); }

/* 操作按钮 */
.row-actions { display: flex; gap: 4px; }
.row-btn { width: 28px; height: 28px; border: 1px solid var(--mc-border); background: var(--mc-bg-elevated); border-radius: 6px; cursor: pointer; display: flex; align-items: center; justify-content: center; color: var(--mc-text-secondary); transition: all 0.15s; }
.row-btn:hover { background: var(--mc-bg-sunken); color: var(--mc-primary); }
.row-btn.danger:hover { background: var(--mc-danger-bg); border-color: var(--mc-danger); color: var(--mc-danger); }

/* 空状态 */
.empty-row { padding: 40px !important; }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 12px; color: var(--mc-text-tertiary); }
.empty-icon { font-size: 32px; }
.empty-state p { font-size: 14px; margin: 0; }

/* 弹窗 */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 20px; }
.modal { background: var(--mc-bg-elevated); border: 1px solid var(--mc-border); border-radius: 16px; width: 100%; max-width: 520px; max-height: 90vh; display: flex; flex-direction: column; box-shadow: 0 20px 60px rgba(0,0,0,0.15); }
.modal.modal-lg { max-width: 600px; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 20px 24px; border-bottom: 1px solid var(--mc-border-light); }
.modal-header h2 { font-size: 18px; font-weight: 600; color: var(--mc-text-primary); margin: 0; }
.modal-close { width: 32px; height: 32px; border: none; background: none; cursor: pointer; color: var(--mc-text-tertiary); display: flex; align-items: center; justify-content: center; border-radius: 6px; }
.modal-close:hover { background: var(--mc-bg-sunken); }
.modal-body { flex: 1; overflow-y: auto; padding: 20px 24px; display: flex; flex-direction: column; gap: 16px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 24px; border-top: 1px solid var(--mc-border-light); }

/* 表单 */
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 13px; font-weight: 500; color: var(--mc-text-secondary); }
.form-input { padding: 8px 12px; border: 1px solid var(--mc-border); border-radius: 8px; font-size: 14px; color: var(--mc-text-primary); outline: none; background: var(--mc-bg-sunken); width: 100%; }
.form-input:focus { border-color: var(--mc-primary); box-shadow: 0 0 0 2px rgba(217,119,87,0.1); }
.form-input.mono { font-family: monospace; }
.form-textarea { padding: 8px 12px; border: 1px solid var(--mc-border); border-radius: 8px; font-size: 14px; color: var(--mc-text-primary); outline: none; background: var(--mc-bg-sunken); width: 100%; resize: vertical; font-family: inherit; }
.form-textarea:focus { border-color: var(--mc-primary); box-shadow: 0 0 0 2px rgba(217,119,87,0.1); }
.form-row { display: flex; gap: 16px; }
.form-row .form-group { flex: 1; }

/* Radio 组 */
.radio-group { display: flex; gap: 8px; flex-wrap: wrap; }
.radio-option { display: flex; align-items: center; gap: 4px; padding: 6px 14px; border: 1px solid var(--mc-border); border-radius: 8px; font-size: 13px; color: var(--mc-text-secondary); cursor: pointer; transition: all 0.15s; }
.radio-option:hover { border-color: var(--mc-primary); }
.radio-option.active { border-color: var(--mc-primary); background: var(--mc-primary-bg); color: var(--mc-primary); }
.radio-option input { display: none; }

/* 星期选择 */
.day-picker { display: flex; gap: 6px; flex-wrap: wrap; }
.day-chip { display: flex; align-items: center; justify-content: center; padding: 4px 10px; border: 1px solid var(--mc-border); border-radius: 6px; font-size: 12px; color: var(--mc-text-secondary); cursor: pointer; transition: all 0.15s; user-select: none; }
.day-chip:hover { border-color: var(--mc-primary); }
.day-chip.active { border-color: var(--mc-primary); background: var(--mc-primary-bg); color: var(--mc-primary); }
.day-chip input { display: none; }

/* Toggle label */
.toggle-label { display: flex; align-items: center; gap: 10px; font-size: 14px; color: var(--mc-text-primary); }
</style>
