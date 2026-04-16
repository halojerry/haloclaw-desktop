<template>
  <div class="recharge-history">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">{{ t('recharge.history.title') }}</h1>
        <p class="page-desc">{{ t('recharge.history.description') }}</p>
      </div>
      <div class="header-right">
        <el-button :icon="Refresh" @click="loadHistory" :loading="loading">
          {{ t('recharge.refresh') }}
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon">
          <el-icon><Wallet /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ totalRecharge.toFixed(2) }}</span>
          <span class="stat-label">{{ t('recharge.history.totalRecharge') }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon success">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ successCount }}</span>
          <span class="stat-label">{{ t('recharge.history.successCount') }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon pending">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ pendingCount }}</span>
          <span class="stat-label">{{ t('recharge.history.pendingCount') }}</span>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-radio-group v-model="statusFilter" size="default" @change="handleFilterChange">
        <el-radio-button label="all">{{ t('recharge.history.filter.all') }}</el-radio-button>
        <el-radio-button label="paid">{{ t('recharge.history.filter.paid') }}</el-radio-button>
        <el-radio-button label="pending">{{ t('recharge.history.filter.pending') }}</el-radio-button>
        <el-radio-button label="expired">{{ t('recharge.history.filter.expired') }}</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 充值记录列表 -->
    <div class="history-list" v-loading="loading">
      <!-- 空状态 -->
      <el-empty
        v-if="filteredHistory.length === 0 && !loading"
        :description="t('recharge.history.empty')"
        :image-size="120"
      >
        <el-button type="primary" @click="goToRecharge">
          {{ t('recharge.history.goRecharge') }}
        </el-button>
      </el-empty>

      <!-- 记录卡片 -->
      <div
        v-for="record in paginatedHistory"
        :key="record.id"
        class="record-card"
      >
        <div class="record-header">
          <div class="record-main">
            <div class="record-icon" :class="record.status">
              <el-icon v-if="record.status === 'paid'"><CircleCheck /></el-icon>
              <el-icon v-else-if="record.status === 'pending'"><Clock /></el-icon>
              <el-icon v-else-if="record.status === 'expired'"><CircleClose /></el-icon>
              <el-icon v-else><Document /></el-icon>
            </div>
            <div class="record-info">
              <div class="record-order">{{ record.orderId }}</div>
              <div class="record-time">{{ formatDateTime(record.createdAt) }}</div>
            </div>
          </div>
          <div class="record-status">
            <el-tag :type="getStatusType(record.status)" size="small">
              {{ t('recharge.status.' + record.status) }}
            </el-tag>
          </div>
        </div>

        <div class="record-body">
          <div class="record-amount">
            <span class="amount-value">+{{ record.amount }}</span>
            <span class="amount-bonus" v-if="record.bonus > 0">+{{ record.bonus }} {{ t('recharge.bonus') }}</span>
          </div>
          <div class="record-detail">
            <div class="detail-item">
              <span class="detail-label">{{ t('recharge.history.paymentMethod') }}</span>
              <span class="detail-value">{{ getPaymentName(record.paymentMethod) }}</span>
            </div>
            <div class="detail-item" v-if="record.paidAt">
              <span class="detail-label">{{ t('recharge.history.paidTime') }}</span>
              <span class="detail-value">{{ formatDateTime(record.paidAt) }}</span>
            </div>
          </div>
        </div>

        <div class="record-footer" v-if="record.status === 'pending'">
          <div class="countdown" v-if="getRemainingTime(record.expireTime) > 0">
            <el-icon><Timer /></el-icon>
            <span>{{ t('recharge.history.expireIn') }}: {{ formatCountdown(getRemainingTime(record.expireTime)) }}</span>
          </div>
          <div class="record-actions">
            <el-button size="small" @click="viewDetail(record)">
              {{ t('common.view') }}
            </el-button>
            <el-button size="small" type="primary" @click="continuePayment(record)">
              {{ t('recharge.history.continuePay') }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="filteredHistory.length > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filteredHistory.length"
        layout="prev, pager, next"
        background
      />
    </div>

    <!-- 订单详情弹窗 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="t('recharge.history.orderDetail')"
      width="500px"
    >
      <div class="detail-content" v-if="currentRecord">
        <div class="detail-section">
          <div class="detail-row">
            <span class="detail-label">{{ t('recharge.history.orderNo') }}</span>
            <span class="detail-value">{{ currentRecord.orderId }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">{{ t('recharge.history.paymentMethod') }}</span>
            <span class="detail-value">{{ getPaymentName(currentRecord.paymentMethod) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">{{ t('recharge.rechargeAmount') }}</span>
            <span class="detail-value">¥{{ currentRecord.amount }}</span>
          </div>
          <div class="detail-row" v-if="currentRecord.bonus > 0">
            <span class="detail-label">{{ t('recharge.bonus') }}</span>
            <span class="detail-value">+{{ currentRecord.bonus }} {{ t('recharge.quota') }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">{{ t('recharge.status.label') }}</span>
            <el-tag :type="getStatusType(currentRecord.status)" size="small">
              {{ t('recharge.status.' + currentRecord.status) }}
            </el-tag>
          </div>
          <div class="detail-row">
            <span class="detail-label">{{ t('recharge.history.createTime') }}</span>
            <span class="detail-value">{{ formatDateTime(currentRecord.createdAt) }}</span>
          </div>
          <div class="detail-row" v-if="currentRecord.paidAt">
            <span class="detail-label">{{ t('recharge.history.paidTime') }}</span>
            <span class="detail-value">{{ formatDateTime(currentRecord.paidAt) }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetailDialog = false">{{ t('common.close') }}</el-button>
        <el-button
          v-if="currentRecord?.status === 'pending'"
          type="primary"
          @click="continuePayment(currentRecord); showDetailDialog = false"
        >
          {{ t('recharge.history.continuePay') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Refresh, Wallet, CircleCheck, Clock, CircleClose, Document, Timer } from '@element-plus/icons-vue'

const { t } = useI18n()

// 状态
const loading = ref(false)
const statusFilter = ref('all')
const currentPage = ref(1)
const pageSize = 10
const showDetailDialog = ref(false)
const currentRecord = ref<any>(null)

// 模拟充值记录数据
const historyList = ref([
  {
    id: 1,
    orderId: 'MC202404150001',
    amount: 100,
    bonus: 5,
    paymentMethod: 'alipay',
    status: 'paid',
    createdAt: '2024-04-15 10:30:00',
    paidAt: '2024-04-15 10:32:15',
    expireTime: null
  },
  {
    id: 2,
    orderId: 'MC202404160002',
    amount: 500,
    bonus: 50,
    paymentMethod: 'wechat',
    status: 'paid',
    createdAt: '2024-04-16 14:20:00',
    paidAt: '2024-04-16 14:25:30',
    expireTime: null
  },
  {
    id: 3,
    orderId: 'MC202404170003',
    amount: 200,
    bonus: 10,
    paymentMethod: 'alipay',
    status: 'expired',
    createdAt: '2024-04-17 09:15:00',
    paidAt: null,
    expireTime: '2024-04-17 09:45:00'
  },
  {
    id: 4,
    orderId: 'MC202404180004',
    amount: 1000,
    bonus: 150,
    paymentMethod: 'alipay',
    status: 'pending',
    createdAt: '2024-04-18 16:30:00',
    paidAt: null,
    expireTime: '2024-04-18 17:00:00'
  }
])

// 定时器
let countdownTimer: number | null = null

// 计算属性
const filteredHistory = computed(() => {
  if (statusFilter.value === 'all') {
    return historyList.value
  }
  return historyList.value.filter(item => item.status === statusFilter.value)
})

const paginatedHistory = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredHistory.value.slice(start, start + pageSize)
})

const totalRecharge = computed(() => {
  return historyList.value
    .filter(item => item.status === 'paid')
    .reduce((sum, item) => sum + item.amount + item.bonus, 0)
})

const successCount = computed(() => {
  return historyList.value.filter(item => item.status === 'paid').length
})

const pendingCount = computed(() => {
  return historyList.value.filter(item => item.status === 'pending').length
})

// 获取状态标签类型
function getStatusType(status: string): string {
  const statusMap: Record<string, string> = {
    paid: 'success',
    pending: 'warning',
    expired: 'danger',
    cancelled: 'info'
  }
  return statusMap[status] || 'info'
}

// 获取支付方式名称
function getPaymentName(method: string): string {
  const nameMap: Record<string, string> = {
    alipay: '支付宝',
    wechat: '微信支付'
  }
  return nameMap[method] || method
}

// 格式化日期时间
function formatDateTime(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr
}

// 计算剩余时间
function getRemainingTime(expireTime: string): number {
  if (!expireTime) return 0
  const expire = new Date(expireTime).getTime()
  const now = Date.now()
  return Math.max(0, expire - now)
}

// 格式化倒计时
function formatCountdown(ms: number): string {
  const minutes = Math.floor(ms / 60000)
  const seconds = Math.floor((ms % 60000) / 1000)
  return `${minutes}:${seconds.toString().padStart(2, '0')}`
}

// 加载历史记录
async function loadHistory() {
  loading.value = true
  try {
    // 模拟加载
    await new Promise(resolve => setTimeout(resolve, 500))
    // 实际项目调用API
    // const res = await api.get('/api/v1/payment/history')
    // historyList.value = res.data.records
  } catch (error) {
    ElMessage.error(t('recharge.history.errors.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 筛选变化
function handleFilterChange() {
  currentPage.value = 1
}

// 查看详情
function viewDetail(record: any) {
  currentRecord.value = record
  showDetailDialog.value = true
}

// 继续支付
function continuePayment(record: any) {
  ElMessage.info(t('recharge.history.continuePayHint'))
  // 跳转到充值页面继续支付
  window.location.href = '/recharge'
}

// 跳转到充值页面
function goToRecharge() {
  window.location.href = '/recharge'
}

onMounted(() => {
  loadHistory()
  // 启动倒计时更新
  countdownTimer = window.setInterval(() => {
    // 触发响应式更新
  }, 1000)
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style scoped>
.recharge-history {
  padding: var(--mc-spacing-lg, 24px);
  background: var(--mc-bg, #f6f1ea);
  min-height: 100%;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--mc-spacing-lg, 24px);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--mc-text-primary, #1d1612);
  margin: 0 0 4px 0;
}

.page-desc {
  font-size: 14px;
  color: var(--mc-text-secondary, #665245);
  margin: 0;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--mc-spacing-lg, 24px);
  margin-bottom: var(--mc-spacing-lg, 24px);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--mc-bg-elevated, #ffffff);
  border-radius: var(--mc-radius-lg, 16px);
  padding: 20px;
  border: 1px solid var(--mc-border-light, #ebe3db);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--mc-primary-bg, #f6e2d7);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--mc-primary, #d96d46);
  font-size: 24px;
}

.stat-icon.success {
  background: #e8f5e9;
  color: #2e7d32;
}

.stat-icon.pending {
  background: #fff3e0;
  color: #e65100;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--mc-text-primary, #1d1612);
}

.stat-label {
  font-size: 13px;
  color: var(--mc-text-secondary, #665245);
  margin-top: 4px;
}

/* 筛选栏 */
.filter-bar {
  margin-bottom: var(--mc-spacing-lg, 24px);
}

/* 充值记录列表 */
.history-list {
  min-height: 200px;
}

.record-card {
  background: var(--mc-bg-elevated, #ffffff);
  border-radius: var(--mc-radius-lg, 16px);
  border: 1px solid var(--mc-border-light, #ebe3db);
  padding: 20px;
  margin-bottom: 16px;
  transition: all 0.2s ease;
}

.record-card:hover {
  box-shadow: var(--mc-shadow-soft, 0 10px 30px rgba(58, 32, 19, 0.08));
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.record-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.record-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.record-icon.paid {
  background: #e8f5e9;
  color: #2e7d32;
}

.record-icon.pending {
  background: #fff3e0;
  color: #e65100;
}

.record-icon.expired {
  background: #ffebee;
  color: #c62828;
}

.record-icon.cancelled {
  background: var(--mc-bg-muted, #f1e8df);
  color: var(--mc-text-secondary, #665245);
}

.record-info {
  display: flex;
  flex-direction: column;
}

.record-order {
  font-size: 15px;
  font-weight: 600;
  color: var(--mc-text-primary, #1d1612);
}

.record-time {
  font-size: 12px;
  color: var(--mc-text-secondary, #665245);
  margin-top: 2px;
}

.record-body {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-left: 52px;
}

.record-amount {
  display: flex;
  flex-direction: column;
}

.amount-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--mc-accent, #184a45);
}

.amount-bonus {
  font-size: 13px;
  color: var(--mc-primary, #d96d46);
  margin-top: 4px;
}

.record-detail {
  display: flex;
  gap: 24px;
}

.detail-item {
  display: flex;
  flex-direction: column;
}

.detail-label {
  font-size: 12px;
  color: var(--mc-text-secondary, #665245);
  margin-bottom: 2px;
}

.detail-value {
  font-size: 14px;
  color: var(--mc-text-primary, #1d1612);
  font-weight: 500;
}

.record-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--mc-border-light, #ebe3db);
  padding-left: 52px;
}

.countdown {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #e65100;
}

.record-actions {
  display: flex;
  gap: 8px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 详情弹窗 */
.detail-content {
  padding: 8px 0;
}

.detail-section {
  background: var(--mc-bg-muted, #f1e8df);
  border-radius: var(--mc-radius-md, 12px);
  padding: 16px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--mc-border-light, #ebe3db);
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  font-size: 14px;
  color: var(--mc-text-secondary, #665245);
}

.detail-value {
  font-size: 14px;
  color: var(--mc-text-primary, #1d1612);
  font-weight: 500;
}

/* 响应式 */
@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .record-body {
    flex-direction: column;
    gap: 16px;
    padding-left: 0;
  }

  .record-footer {
    padding-left: 0;
  }
}
</style>
