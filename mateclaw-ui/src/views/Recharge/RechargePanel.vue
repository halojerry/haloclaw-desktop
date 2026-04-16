<template>
  <div class="recharge-panel">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">{{ t('recharge.title') }}</h1>
        <p class="page-desc">{{ t('recharge.description') }}</p>
      </div>
    </div>

    <!-- 余额卡片 -->
    <div class="balance-card">
      <div class="balance-info">
        <span class="balance-label">{{ t('recharge.currentBalance') }}</span>
        <span class="balance-value">
          <span class="balance-amount">{{ currentBalance }}</span>
          <span class="balance-unit">{{ t('recharge.quota') }}</span>
        </span>
      </div>
      <div class="balance-actions">
        <el-button :icon="Refresh" @click="loadBalance" :loading="balanceLoading">
          {{ t('recharge.refresh') }}
        </el-button>
      </div>
    </div>

    <div class="recharge-content">
      <!-- 左侧：充值金额选择 -->
      <div class="recharge-form">
        <div class="section-title">{{ t('recharge.selectAmount') }}</div>

        <!-- 预设金额选项 -->
        <div class="amount-options">
          <div
            v-for="amount in presetAmounts"
            :key="amount"
            class="amount-option"
            :class="{ active: selectedAmount === amount && !isCustomAmount }"
            @click="selectPresetAmount(amount)"
          >
            <span class="amount-value">{{ amount }}</span>
            <span class="amount-label">{{ t('recharge.quota') }}</span>
            <span class="amount-bonus" v-if="getBonus(amount) > 0">
              +{{ getBonus(amount) }} {{ t('recharge.bonus') }}
            </span>
          </div>
        </div>

        <!-- 自定义金额 -->
        <div class="custom-amount">
          <div class="section-title">{{ t('recharge.customAmount') }}</div>
          <el-input
            v-model="customAmountInput"
            type="number"
            :placeholder="t('recharge.customPlaceholder')"
            @input="handleCustomInput"
            class="custom-input"
          >
            <template #prefix>
              <span class="input-prefix">¥</span>
            </template>
          </el-input>
        </div>

        <!-- 支付方式选择 -->
        <div class="section-title">{{ t('recharge.paymentMethod') }}</div>
        <div class="payment-methods">
          <div
            v-for="method in paymentMethods"
            :key="method.id"
            class="payment-method"
            :class="{ active: selectedPayment === method.id }"
            @click="selectedPayment = method.id"
          >
            <div class="method-icon">
              <img v-if="method.icon" :src="method.icon" :alt="method.name" />
              <span v-else>{{ method.name.charAt(0) }}</span>
            </div>
            <span class="method-name">{{ method.name }}</span>
          </div>
        </div>

        <!-- 充值说明 -->
        <div class="recharge-tips">
          <div class="tip-item" v-for="tip in tips" :key="tip">
            <el-icon><InfoFilled /></el-icon>
            <span>{{ tip }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧：支付二维码 -->
      <div class="payment-section">
        <div v-if="!currentOrder" class="payment-placeholder">
          <div class="placeholder-icon">
            <el-icon :size="64"><Wallet /></el-icon>
          </div>
          <p class="placeholder-text">{{ t('recharge.selectAmountToPay') }}</p>
          <div v-if="selectedAmount > 0" class="payment-summary">
            <div class="summary-row">
              <span>{{ t('recharge.rechargeAmount') }}</span>
              <span class="summary-value">¥{{ selectedAmount }}</span>
            </div>
            <div class="summary-row" v-if="getBonus(selectedAmount) > 0">
              <span>{{ t('recharge.bonus') }}</span>
              <span class="summary-value bonus">+{{ getBonus(selectedAmount) }} {{ t('recharge.quota') }}</span>
            </div>
            <el-divider />
            <div class="summary-row total">
              <span>{{ t('recharge.totalGet') }}</span>
              <span class="summary-value">{{ selectedAmount + getBonus(selectedAmount) }} {{ t('recharge.quota') }}</span>
            </div>
            <el-button
              type="primary"
              size="large"
              :loading="creatingOrder"
              @click="createOrder"
              class="create-order-btn"
            >
              {{ t('recharge.createOrder') }}
            </el-button>
          </div>
        </div>

        <div v-else class="payment-qrcode">
          <div class="qrcode-header">
            <span class="qrcode-title">{{ t('recharge.scanToPay') }}</span>
            <el-tag :type="orderStatusTagType" size="small">
              {{ t('recharge.status.' + currentOrder.status) }}
            </el-tag>
          </div>

          <!-- 二维码区域 -->
          <div class="qrcode-container" v-loading="qrcodeLoading">
            <div v-if="currentOrder.qrcodeUrl" class="qrcode-wrapper">
              <img :src="currentOrder.qrcodeUrl" alt="Payment QR Code" class="qrcode-image" />
            </div>
            <div v-else class="qrcode-loading">
              <el-icon class="is-loading" :size="48"><Loading /></el-icon>
              <p>{{ t('recharge.generatingQrcode') }}</p>
            </div>
          </div>

          <!-- 订单信息 -->
          <div class="order-info">
            <div class="order-amount">
              <span class="amount-label">¥{{ currentOrder.amount }}</span>
              <span class="amount-desc">{{ t('recharge.payAmount') }}</span>
            </div>
            <div class="order-details">
              <div class="detail-row">
                <span>{{ t('recharge.orderNo') }}</span>
                <span class="detail-value">{{ currentOrder.orderId }}</span>
              </div>
              <div class="detail-row" v-if="currentOrder.expireTime">
                <span>{{ t('recharge.expireTime') }}</span>
                <span class="detail-value">{{ currentOrder.expireTime }}</span>
              </div>
            </div>
          </div>

          <!-- 支付状态提示 -->
          <div v-if="currentOrder.status === 'pending'" class="payment-status pending">
            <el-icon><Clock /></el-icon>
            <span>{{ t('recharge.waitingPayment') }}</span>
          </div>
          <div v-else-if="currentOrder.status === 'paid'" class="payment-status success">
            <el-icon><CircleCheck /></el-icon>
            <span>{{ t('recharge.paymentSuccess') }}</span>
          </div>
          <div v-else-if="currentOrder.status === 'expired'" class="payment-status expired">
            <el-icon><CircleClose /></el-icon>
            <span>{{ t('recharge.orderExpired') }}</span>
          </div>

          <!-- 操作按钮 -->
          <div class="qrcode-actions">
            <el-button v-if="currentOrder.status !== 'paid'" @click="cancelOrder">
              {{ t('recharge.cancelOrder') }}
            </el-button>
            <el-button v-if="currentOrder.status === 'paid'" type="primary" @click="goToHistory">
              {{ t('recharge.viewHistory') }}
            </el-button>
            <el-button v-if="currentOrder.status === 'expired'" type="primary" @click="resetOrder">
              {{ t('recharge.newOrder') }}
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Refresh, InfoFilled, Wallet, Loading, Clock, CircleCheck, CircleClose } from '@element-plus/icons-vue'

const { t } = useI18n()

// 预设金额配置
const presetAmounts = [10, 50, 100, 200, 500, 1000]

// 支付方式
const paymentMethods = [
  { id: 'alipay', name: '支付宝', icon: '' },
  { id: 'wechat', name: '微信支付', icon: '' }
]

// 提示信息
const tips = computed(() => [
  t('recharge.tip.1'),
  t('recharge.tip.2'),
  t('recharge.tip.3')
])

// 状态
const currentBalance = ref(0)
const balanceLoading = ref(false)
const selectedAmount = ref(0)
const isCustomAmount = ref(false)
const customAmountInput = ref('')
const selectedPayment = ref('alipay')
const creatingOrder = ref(false)
const qrcodeLoading = ref(false)

// 当前订单
const currentOrder = ref<any>(null)

// 轮询定时器
let statusPollingTimer: number | null = null

// 计算属性
const orderStatusTagType = computed(() => {
  if (!currentOrder.value) return 'info'
  const statusMap: Record<string, string> = {
    pending: 'warning',
    paid: 'success',
    expired: 'danger',
    cancelled: 'info'
  }
  return statusMap[currentOrder.value.status] || 'info'
})

// 计算赠额
function getBonus(amount: number): number {
  if (amount >= 1000) return Math.floor(amount * 0.15)
  if (amount >= 500) return Math.floor(amount * 0.1)
  if (amount >= 200) return Math.floor(amount * 0.05)
  return 0
}

// 选择预设金额
function selectPresetAmount(amount: number) {
  selectedAmount.value = amount
  isCustomAmount.value = false
  customAmountInput.value = ''
}

// 处理自定义输入
function handleCustomInput() {
  isCustomAmount.value = true
  selectedAmount.value = parseInt(customAmountInput.value) || 0
}

// 加载余额
async function loadBalance() {
  balanceLoading.value = true
  try {
    // 模拟加载
    await new Promise(resolve => setTimeout(resolve, 500))
    // 实际项目调用API
    // const res = await api.get('/api/v1/user/balance')
    // currentBalance.value = res.data.balance
    currentBalance.value = 1250
  } catch (error) {
    ElMessage.error(t('recharge.errors.loadBalanceFailed'))
  } finally {
    balanceLoading.value = false
  }
}

// 创建订单
async function createOrder() {
  if (selectedAmount.value <= 0) {
    ElMessage.warning(t('recharge.errors.selectAmount'))
    return
  }

  creatingOrder.value = true
  try {
    // 模拟创建订单
    await new Promise(resolve => setTimeout(resolve, 800))

    // 实际项目调用API
    // const res = await api.post('/api/v1/payment/create', {
    //   amount: selectedAmount.value,
    //   paymentMethod: selectedPayment.value
    // })

    // 模拟返回
    currentOrder.value = {
      orderId: `MC${Date.now()}`,
      amount: selectedAmount.value,
      status: 'pending',
      qrcodeUrl: '',
      expireTime: new Date(Date.now() + 30 * 60 * 1000).toLocaleString()
    }

    // 模拟获取二维码
    qrcodeLoading.value = true
    setTimeout(() => {
      currentOrder.value.qrcodeUrl = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyMDAiIGhlaWdodD0iMjAwIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2Y2ZjZmNiIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIiB0cmFuc2Zvcm09InRyYW5zbGF0ZSgtNTAlLCAtNTAlKSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE0IiBmaWxsPSIjMzMzIj5RUyBDb2RlPC90ZXh0PjwvdHN2Zz4='
      qrcodeLoading.value = false
      startStatusPolling()
    }, 1500)

    ElMessage.success(t('recharge.orderCreated'))
  } catch (error) {
    ElMessage.error(t('recharge.errors.createOrderFailed'))
  } finally {
    creatingOrder.value = false
  }
}

// 取消订单
function cancelOrder() {
  currentOrder.value = null
  stopStatusPolling()
  ElMessage.info(t('recharge.orderCancelled'))
}

// 重置订单
function resetOrder() {
  currentOrder.value = null
  selectedAmount.value = 0
  isCustomAmount.value = false
  customAmountInput.value = ''
}

// 查看历史
function goToHistory() {
  // 跳转到充值历史页面
  window.location.href = '/recharge/history'
}

// 开始轮询订单状态
function startStatusPolling() {
  stopStatusPolling()
  statusPollingTimer = window.setInterval(async () => {
    await checkOrderStatus()
  }, 3000) // 每3秒检查一次
}

// 停止轮询
function stopStatusPolling() {
  if (statusPollingTimer) {
    clearInterval(statusPollingTimer)
    statusPollingTimer = null
  }
}

// 检查订单状态
async function checkOrderStatus() {
  if (!currentOrder.value || currentOrder.value.status !== 'pending') {
    stopStatusPolling()
    return
  }

  try {
    // 实际项目调用API
    // const res = await api.get(`/api/v1/payment/status/${currentOrder.value.orderId}`)

    // 模拟状态更新（5%概率支付成功）
    if (Math.random() < 0.05) {
      currentOrder.value.status = 'paid'
      stopStatusPolling()
      await loadBalance() // 刷新余额
      ElMessage.success(t('recharge.paymentSuccessNotify'))
    }
  } catch (error) {
    console.error('Check order status failed:', error)
  }
}

onMounted(() => {
  loadBalance()
})

onUnmounted(() => {
  stopStatusPolling()
})
</script>

<style scoped>
.recharge-panel {
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

/* 余额卡片 */
.balance-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, var(--mc-primary, #d96d46) 0%, #e88a6a 100%);
  border-radius: var(--mc-radius-lg, 16px);
  padding: 24px 32px;
  margin-bottom: var(--mc-spacing-lg, 24px);
  color: white;
}

.balance-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.balance-label {
  font-size: 14px;
  opacity: 0.9;
}

.balance-value {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.balance-amount {
  font-size: 36px;
  font-weight: 700;
}

.balance-unit {
  font-size: 16px;
  opacity: 0.8;
}

/* 充值内容区 */
.recharge-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--mc-spacing-lg, 24px);
}

/* 充值表单 */
.recharge-form {
  background: var(--mc-bg-elevated, #ffffff);
  border-radius: var(--mc-radius-lg, 16px);
  padding: 24px;
  border: 1px solid var(--mc-border-light, #ebe3db);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--mc-text-primary, #1d1612);
  margin-bottom: 16px;
}

/* 金额选项 */
.amount-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.amount-option {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px 12px;
  background: var(--mc-bg-muted, #f1e8df);
  border: 2px solid transparent;
  border-radius: var(--mc-radius-md, 12px);
  cursor: pointer;
  transition: all 0.2s ease;
}

.amount-option:hover {
  background: var(--mc-primary-bg, #f6e2d7);
  border-color: var(--mc-primary-light, #ebb08f);
}

.amount-option.active {
  background: var(--mc-primary-bg, #f6e2d7);
  border-color: var(--mc-primary, #d96d46);
}

.amount-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--mc-text-primary, #1d1612);
}

.amount-label {
  font-size: 12px;
  color: var(--mc-text-secondary, #665245);
  margin-top: 2px;
}

.amount-bonus {
  position: absolute;
  top: -8px;
  right: -8px;
  background: var(--mc-accent, #184a45);
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
}

/* 自定义金额 */
.custom-amount {
  margin-bottom: 24px;
}

.custom-input {
  width: 100%;
}

.input-prefix {
  color: var(--mc-text-secondary, #665245);
  font-weight: 600;
}

/* 支付方式 */
.payment-methods {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.payment-method {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--mc-bg-muted, #f1e8df);
  border: 2px solid transparent;
  border-radius: var(--mc-radius-md, 12px);
  cursor: pointer;
  transition: all 0.2s ease;
}

.payment-method:hover {
  background: var(--mc-primary-bg, #f6e2d7);
  border-color: var(--mc-primary-light, #ebb08f);
}

.payment-method.active {
  background: var(--mc-primary-bg, #f6e2d7);
  border-color: var(--mc-primary, #d96d46);
}

.method-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: var(--mc-primary, #d96d46);
}

.method-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.method-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--mc-text-primary, #1d1612);
}

/* 充值提示 */
.recharge-tips {
  background: var(--mc-bg-sunken, #ebe3db);
  border-radius: var(--mc-radius-md, 12px);
  padding: 16px;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: var(--mc-text-secondary, #665245);
  margin-bottom: 8px;
}

.tip-item:last-child {
  margin-bottom: 0;
}

.tip-item .el-icon {
  color: var(--mc-primary, #d96d46);
  flex-shrink: 0;
  margin-top: 2px;
}

/* 支付区域 */
.payment-section {
  background: var(--mc-bg-elevated, #ffffff);
  border-radius: var(--mc-radius-lg, 16px);
  padding: 24px;
  border: 1px solid var(--mc-border-light, #ebe3db);
}

.payment-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
}

.placeholder-icon {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: var(--mc-bg-muted, #f1e8df);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  color: var(--mc-primary, #d96d46);
}

.placeholder-text {
  font-size: 14px;
  color: var(--mc-text-secondary, #665245);
  margin-bottom: 24px;
}

.payment-summary {
  width: 100%;
  max-width: 280px;
  text-align: left;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  color: var(--mc-text-secondary, #665245);
}

.summary-row.total {
  font-size: 16px;
  font-weight: 600;
  color: var(--mc-text-primary, #1d1612);
}

.summary-value {
  font-weight: 600;
  color: var(--mc-text-primary, #1d1612);
}

.summary-value.bonus {
  color: var(--mc-accent, #184a45);
}

.summary-row.total .summary-value {
  font-size: 18px;
  color: var(--mc-primary, #d96d46);
}

.create-order-btn {
  width: 100%;
  margin-top: 16px;
}

/* 二维码区域 */
.payment-qrcode {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qrcode-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: 24px;
}

.qrcode-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--mc-text-primary, #1d1612);
}

.qrcode-container {
  width: 220px;
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 1px solid var(--mc-border-light, #ebe3db);
  border-radius: var(--mc-radius-md, 12px);
  margin-bottom: 20px;
}

.qrcode-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qrcode-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.qrcode-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--mc-text-secondary, #665245);
}

/* 订单信息 */
.order-info {
  width: 100%;
  margin-bottom: 20px;
}

.order-amount {
  text-align: center;
  margin-bottom: 16px;
}

.order-amount .amount-label {
  font-size: 28px;
  font-weight: 700;
  color: var(--mc-primary, #d96d46);
  margin-right: 8px;
}

.order-amount .amount-desc {
  font-size: 14px;
  color: var(--mc-text-secondary, #665245);
}

.order-details {
  background: var(--mc-bg-muted, #f1e8df);
  border-radius: var(--mc-radius-md, 12px);
  padding: 12px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 8px;
}

.detail-row:last-child {
  margin-bottom: 0;
}

.detail-row span:first-child {
  color: var(--mc-text-secondary, #665245);
}

.detail-value {
  color: var(--mc-text-primary, #1d1612);
  font-weight: 500;
}

/* 支付状态 */
.payment-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px;
  border-radius: var(--mc-radius-md, 12px);
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 16px;
}

.payment-status.pending {
  background: #fff3e0;
  color: #e65100;
}

.payment-status.success {
  background: #e8f5e9;
  color: #2e7d32;
}

.payment-status.expired {
  background: #ffebee;
  color: #c62828;
}

/* 操作按钮 */
.qrcode-actions {
  display: flex;
  gap: 12px;
  width: 100%;
}

.qrcode-actions .el-button {
  flex: 1;
}

/* 响应式 */
@media (max-width: 768px) {
  .recharge-content {
    grid-template-columns: 1fr;
  }

  .amount-options {
    grid-template-columns: repeat(2, 1fr);
  }

  .payment-methods {
    flex-direction: column;
  }
}
</style>
