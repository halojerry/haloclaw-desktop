<template>
  <div class="device-manager">
    <div class="page-header">
      <h2 class="page-title">{{ t('device.title') }}</h2>
      <p class="page-desc">{{ t('device.description') }}</p>
    </div>

    <!-- 当前设备信息 -->
    <el-card class="current-device-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>{{ t('device.currentDevice') }}</span>
          <el-tag :type="currentDeviceStatusType" size="small">
            {{ currentDeviceStatus }}
          </el-tag>
        </div>
      </template>
      
      <div class="device-info-grid">
        <div class="info-item">
          <span class="label">{{ t('device.deviceId') }}</span>
          <span class="value device-id">{{ currentDevice.deviceId }}</span>
        </div>
        <div class="info-item">
          <span class="label">{{ t('device.deviceName') }}</span>
          <span class="value">{{ currentDevice.deviceName || t('device.unknown') }}</span>
        </div>
        <div class="info-item">
          <span class="label">{{ t('device.deviceType') }}</span>
          <span class="value">{{ currentDevice.deviceType || 'desktop' }}</span>
        </div>
        <div class="info-item">
          <span class="label">{{ t('device.lastHeartbeat') }}</span>
          <span class="value">{{ formatTime(currentDevice.lastHeartbeat) }}</span>
        </div>
      </div>

      <div class="device-actions">
        <el-button type="primary" @click="refreshDevice">
          {{ t('device.refresh') }}
        </el-button>
        <el-button type="warning" @click="handleResetDevice">
          {{ t('device.resetDevice') }}
        </el-button>
      </div>
    </el-card>

    <!-- 设备统计 -->
    <el-card class="device-stats-card" shadow="hover">
      <template #header>
        <span>{{ t('device.deviceStats') }}</span>
      </template>
      
      <div class="stats-grid">
        <div class="stat-item">
          <div class="stat-value">{{ deviceStore.currentDeviceCount }}</div>
          <div class="stat-label">{{ t('device.currentCount') }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ deviceStore.deviceLimit }}</div>
          <div class="stat-label">{{ t('device.deviceLimit') }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ remainingSlots }}</div>
          <div class="stat-label">{{ t('device.remainingSlots') }}</div>
        </div>
      </div>

      <el-progress
        :percentage="usagePercentage"
        :status="progressStatus"
        :stroke-width="10"
        class="usage-progress"
      />
    </el-card>

    <!-- 设备列表 -->
    <el-card class="device-list-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>{{ t('device.boundDevices') }}</span>
          <el-button type="primary" size="small" @click="refreshList">
            {{ t('device.refresh') }}
          </el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="deviceStore.boundDevices"
        stripe
        style="width: 100%"
      >
        <el-table-column :label="t('device.columns.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="t('device.columns.deviceName')" min-width="150">
          <template #default="{ row }">
            <div class="device-name-cell">
              <span>{{ row.deviceName || t('device.unknown') }}</span>
              <el-tag v-if="row.isCurrent" type="success" size="small">
                {{ t('device.current') }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="t('device.columns.deviceType')" width="120">
          <template #default="{ row }">
            {{ getDeviceTypeText(row.deviceType) }}
          </template>
        </el-table-column>

        <el-table-column :label="t('device.columns.lastActive')" width="180">
          <template #default="{ row }">
            {{ formatTime(row.lastHeartbeat) }}
          </template>
        </el-table-column>

        <el-table-column :label="t('device.columns.ipAddress')" width="140">
          <template #default="{ row }">
            {{ row.ipAddress || '-' }}
          </template>
        </el-table-column>

        <el-table-column :label="t('device.columns.actions')" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.isCurrent"
              type="danger"
              size="small"
              link
              @click="handleUnbind(row)"
            >
              {{ t('device.unbind') }}
            </el-button>
            <span v-else class="current-device-text">
              {{ t('device.cannotUnbind') }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 解绑确认对话框 -->
    <el-dialog
      v-model="unbindDialogVisible"
      :title="t('device.unbindConfirmTitle')"
      width="400px"
    >
      <p>{{ t('device.unbindConfirmMessage') }}</p>
      <p class="unbind-device-name">{{ unbindTargetDevice?.deviceName }}</p>
      <p class="unbind-device-id">{{ unbindTargetDevice?.deviceId }}</p>
      
      <template #footer>
        <el-button @click="unbindDialogVisible = false">
          {{ t('device.cancel') }}
        </el-button>
        <el-button type="danger" @click="confirmUnbind">
          {{ t('device.confirmUnbind') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDeviceStore } from '@/stores/useDeviceStore'
import { ElMessageBox, ElMessage } from 'element-plus'
import type { DeviceInfo } from '@/api/deviceApi'

const { t } = useI18n()
const deviceStore = useDeviceStore()

const loading = ref(false)
const unbindDialogVisible = ref(false)
const unbindTargetDevice = ref<DeviceInfo | null>(null)

// 当前设备信息
const currentDevice = computed(() => {
  const deviceId = deviceStore.getDeviceId()
  const deviceName = deviceStore.getDeviceName()
  const current = deviceStore.boundDevices.find(d => d.deviceId === deviceId)
  
  return {
    deviceId,
    deviceName,
    deviceType: deviceStore.getDeviceType(),
    lastHeartbeat: current?.lastHeartbeat || null,
    status: current?.status || 'online'
  }
})

// 当前设备状态
const currentDeviceStatus = computed(() => {
  return getStatusText(currentDevice.value.status)
})

const currentDeviceStatusType = computed(() => {
  return getStatusType(currentDevice.value.status)
})

// 剩余槽位
const remainingSlots = computed(() => {
  return Math.max(0, deviceStore.deviceLimit - deviceStore.currentDeviceCount)
})

// 使用百分比
const usagePercentage = computed(() => {
  if (deviceStore.deviceLimit === 0) return 0
  return Math.min(100, (deviceStore.currentDeviceCount / deviceStore.deviceLimit) * 100)
})

// 进度条状态
const progressStatus = computed(() => {
  if (usagePercentage.value >= 100) return 'exception'
  if (usagePercentage.value >= 80) return 'warning'
  return 'success'
})

// 刷新设备信息
async function refreshDevice() {
  await deviceStore.registerDevice()
  await deviceStore.fetchDeviceList()
}

// 刷新列表
async function refreshList() {
  loading.value = true
  try {
    await deviceStore.fetchDeviceList()
  } finally {
    loading.value = false
  }
}

// 重置设备
async function handleResetDevice() {
  try {
    await ElMessageBox.confirm(
      t('device.resetConfirmMessage'),
      t('device.resetConfirmTitle'),
      {
        confirmButtonText: t('device.confirm'),
        cancelButtonText: t('device.cancel'),
        type: 'warning'
      }
    )
    
    deviceStore.resetDevice()
    await deviceStore.fetchDeviceList()
    ElMessage.success(t('device.resetSuccess'))
  } catch {
    // 用户取消
  }
}

// 解绑设备
function handleUnbind(device: DeviceInfo) {
  unbindTargetDevice.value = device
  unbindDialogVisible.value = true
}

// 确认解绑
async function confirmUnbind() {
  if (!unbindTargetDevice.value) return
  
  const success = await deviceStore.unbindDevice(unbindTargetDevice.value.deviceId)
  if (success) {
    unbindDialogVisible.value = false
    unbindTargetDevice.value = null
  }
}

// 获取状态类型
function getStatusType(status: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'online':
      return 'success'
    case 'offline':
      return 'info'
    case 'banned':
      return 'danger'
    default:
      return ''
  }
}

// 获取状态文本
function getStatusText(status: string): string {
  switch (status) {
    case 'online':
      return t('device.status.online')
    case 'offline':
      return t('device.status.offline')
    case 'banned':
      return t('device.status.banned')
    default:
      return status
  }
}

// 获取设备类型文本
function getDeviceTypeText(type: string): string {
  switch (type) {
    case 'desktop':
      return t('device.type.desktop')
    case 'mobile':
      return t('device.type.mobile')
    case 'web':
      return t('device.type.web')
    default:
      return type || t('device.unknown')
  }
}

// 格式化时间
function formatTime(timeStr: string | null | undefined): string {
  if (!timeStr) return '-'
  try {
    const date = new Date(timeStr)
    return date.toLocaleString()
  } catch {
    return timeStr
  }
}

// 生命周期
onMounted(async () => {
  await deviceStore.initialize()
  await deviceStore.fetchDeviceList()
})

onUnmounted(() => {
  // 不要停止心跳，应用可能只是切换页面
})
</script>

<style scoped>
.device-manager {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--mc-text-primary);
  margin: 0 0 8px 0;
}

.page-desc {
  font-size: 14px;
  color: var(--mc-text-secondary);
  margin: 0;
}

.current-device-card,
.device-stats-card,
.device-list-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.device-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item .label {
  font-size: 12px;
  color: var(--mc-text-tertiary);
  text-transform: uppercase;
}

.info-item .value {
  font-size: 14px;
  color: var(--mc-text-primary);
}

.info-item .value.device-id {
  font-family: monospace;
  font-size: 12px;
  word-break: break-all;
}

.device-actions {
  display: flex;
  gap: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--mc-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--mc-text-tertiary);
  margin-top: 4px;
}

.usage-progress {
  margin-top: 10px;
}

.device-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.current-device-text {
  font-size: 12px;
  color: var(--mc-text-tertiary);
}

.unbind-device-name {
  font-weight: 600;
  color: var(--mc-text-primary);
  margin: 12px 0 4px 0;
}

.unbind-device-id {
  font-family: monospace;
  font-size: 12px;
  color: var(--mc-text-secondary);
  margin: 0;
}

@media (max-width: 768px) {
  .device-manager {
    padding: 16px;
  }

  .device-info-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
  }

  .stat-value {
    font-size: 24px;
  }
}
</style>
