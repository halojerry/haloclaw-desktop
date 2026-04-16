<template>
  <div class="store-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">{{ t('store.title') }}</h1>
        <p class="page-desc">{{ t('store.description') }}</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="showAddDialog = true">
          {{ t('store.addStore') }}
        </el-button>
      </div>
    </div>

    <!-- 状态筛选 -->
    <div class="filter-bar">
      <el-radio-group v-model="statusFilter" size="default">
        <el-radio-button label="all">{{ t('store.status.all') }}</el-radio-button>
        <el-radio-button label="active">{{ t('store.status.active') }}</el-radio-button>
        <el-radio-button label="expired">{{ t('store.status.expired') }}</el-radio-button>
        <el-radio-button label="unauthorized">{{ t('store.status.unauthorized') }}</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 店铺列表 -->
    <div class="store-grid" v-loading="loading">
      <!-- 空状态 -->
      <el-empty
        v-if="filteredStores.length === 0 && !loading"
        :description="t('store.empty')"
        :image-size="120"
      >
        <el-button type="primary" @click="showAddDialog = true">
          {{ t('store.addFirstStore') }}
        </el-button>
      </el-empty>

      <!-- 店铺卡片 -->
      <div
        v-for="store in filteredStores"
        :key="store.id"
        class="store-card"
        :class="{ 'store-card--inactive': store.status !== 'active' }"
      >
        <div class="store-card-header">
          <div class="store-avatar">
            <img v-if="store.logo" :src="store.logo" :alt="store.name" />
            <span v-else class="avatar-placeholder">{{ store.name.charAt(0) }}</span>
          </div>
          <div class="store-info">
            <h3 class="store-name">{{ store.name }}</h3>
            <span class="store-id">ID: {{ store.storeId }}</span>
          </div>
          <el-dropdown trigger="click" @command="handleCommand($event, store)">
            <el-button text circle :icon="More" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit" :icon="Edit">
                  {{ t('store.actions.edit') }}
                </el-dropdown-item>
                <el-dropdown-item command="sync" :icon="Refresh">
                  {{ t('store.actions.sync') }}
                </el-dropdown-item>
                <el-dropdown-item command="profit" :icon="Money">
                  {{ t('store.actions.profitConfig') }}
                </el-dropdown-item>
                <el-dropdown-item command="settings" :icon="Setting">
                  {{ t('store.actions.settings') }}
                </el-dropdown-item>
                <el-dropdown-item
                  command="delete"
                  :icon="Delete"
                  divided
                  style="color: var(--ozon-danger)"
                >
                  {{ t('store.actions.delete') }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div class="store-card-body">
          <div class="store-stats">
            <div class="stat-item">
              <span class="stat-value">{{ store.products || 0 }}</span>
              <span class="stat-label">{{ t('store.stats.products') }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ store.orders || 0 }}</span>
              <span class="stat-label">{{ t('store.stats.orders') }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ store.revenue || 0 }} ₽</span>
              <span class="stat-label">{{ t('store.stats.revenue') }}</span>
            </div>
          </div>

          <!-- 利润配置概览 -->
          <div v-if="store.profitConfig" class="profit-preview">
            <div class="profit-info">
              <span class="profit-label">{{ t('store.profitConfig.quickView') }}</span>
              <span class="profit-rate">{{ t('store.profitConfig.targetRate') }}: {{ store.profitConfig.targetProfitRate }}%</span>
            </div>
            <el-button 
              size="small" 
              text 
              :icon="Edit" 
              @click.stop="openProfitConfig(store)"
            >
              {{ t('common.edit') }}
            </el-button>
          </div>
        </div>

        <div class="store-card-footer">
          <div class="store-status">
            <span class="status-dot" :class="'status-' + store.status"></span>
            <span class="status-text">{{ t('store.status.' + store.status) }}</span>
          </div>
          <el-tag v-if="store.bindTime" size="small" type="info">
            {{ t('store.bindedAt') }}: {{ formatDate(store.bindTime) }}
          </el-tag>
        </div>
      </div>

      <!-- 添加店铺卡片 -->
      <div class="store-card store-card--add" @click="showAddDialog = true">
        <div class="add-content">
          <el-icon class="add-icon"><Plus /></el-icon>
          <span class="add-text">{{ t('store.addStore') }}</span>
        </div>
      </div>
    </div>

    <!-- 添加店铺弹窗 -->
    <el-dialog
      v-model="showAddDialog"
      :title="t('store.addStore')"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="storeFormRef"
        :model="storeForm"
        :rules="storeFormRules"
        label-width="100px"
      >
        <el-form-item :label="t('store.form.storeName')" prop="name">
          <el-input
            v-model="storeForm.name"
            :placeholder="t('store.form.storeNamePlaceholder')"
          />
        </el-form-item>

        <el-form-item :label="t('store.form.storeId')" prop="storeId">
          <el-input
            v-model="storeForm.storeId"
            :placeholder="t('store.form.storeIdPlaceholder')"
          />
        </el-form-item>

        <el-form-item :label="t('store.form.apiKey')" prop="apiKey">
          <el-input
            v-model="storeForm.apiKey"
            type="password"
            :placeholder="t('store.form.apiKeyPlaceholder')"
            show-password
          />
        </el-form-item>

        <el-form-item :label="t('store.form.clientId')" prop="clientId">
          <el-input
            v-model="storeForm.clientId"
            :placeholder="t('store.form.clientIdPlaceholder')"
          />
        </el-form-item>

        <el-divider content-position="center">
          <span class="ozon-link">{{ t('store.form.bindOzonAccount') }}</span>
        </el-divider>

        <div class="ozon-bind-section">
          <p class="bind-hint">{{ t('store.form.bindHint') }}</p>
          <ol class="bind-steps">
            <li>{{ t('store.form.step1') }}</li>
            <li>{{ t('store.form.step2') }}</li>
            <li>{{ t('store.form.step3') }}</li>
          </ol>
          <el-button type="primary" text @click="openOzonSeller">
            {{ t('store.form.openOzonSeller') }}
            <el-icon class="el-icon--right"><Link /></el-icon>
          </el-button>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="showAddDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleAddStore" :loading="submitLoading">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 删除确认弹窗 -->
    <el-dialog
      v-model="showDeleteDialog"
      :title="t('store.deleteConfirm.title')"
      width="400px"
    >
      <p>{{ t('store.deleteConfirm.message', { name: currentStore?.name }) }}</p>
      <template #footer>
        <el-button @click="showDeleteDialog = false">{{ t('common.cancel') }}</el-button>
        <el-button type="danger" @click="confirmDelete">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- 利润配置弹窗 -->
    <ProfitConfigForm
      v-model="showProfitDialog"
      :store-id="currentStore?.storeId || ''"
      :config="currentStore?.profitConfig"
      @save="handleSaveProfit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  Plus, More, Edit, Refresh, Setting, Delete, Link, Money
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import ProfitConfigForm from './ProfitConfigForm.vue'
import type { ProfitConfig } from './ProfitConfigForm.vue'

const { t } = useI18n()

// 状态
const loading = ref(false)
const submitLoading = ref(false)
const statusFilter = ref('all')
const showAddDialog = ref(false)
const showDeleteDialog = ref(false)
const showProfitDialog = ref(false)
const currentStore = ref<any>(null)

// 模拟店铺数据
const stores = ref([
  {
    id: 1,
    name: '我的Ozon店铺',
    storeId: 'OZON-2024-001',
    logo: '',
    status: 'active',
    products: 156,
    orders: 2340,
    revenue: 456780,
    bindTime: '2024-03-15',
    profitConfig: {
      minProfitRate: 15,
      targetProfitRate: 25,
      exchangeRate: 12.5,
      logisticsRate: 15,
      platformCommission: 12
    }
  },
  {
    id: 2,
    name: '俄罗斯精品店',
    storeId: 'OZON-2024-002',
    logo: '',
    status: 'expired',
    products: 89,
    orders: 1200,
    revenue: 234500,
    bindTime: '2024-01-20',
    profitConfig: null
  }
])

// 表单数据
const storeFormRef = ref<FormInstance>()
const storeForm = reactive({
  name: '',
  storeId: '',
  apiKey: '',
  clientId: ''
})

const storeFormRules: FormRules = {
  name: [
    { required: true, message: t('store.errors.storeNameRequired'), trigger: 'blur' }
  ],
  storeId: [
    { required: true, message: t('store.errors.storeIdRequired'), trigger: 'blur' }
  ],
  apiKey: [
    { required: true, message: t('store.errors.apiKeyRequired'), trigger: 'blur' }
  ],
  clientId: [
    { required: true, message: t('store.errors.clientIdRequired'), trigger: 'blur' }
  ]
}

// 过滤后的店铺列表
const filteredStores = computed(() => {
  if (statusFilter.value === 'all') {
    return stores.value
  }
  return stores.value.filter(store => store.status === statusFilter.value)
})

onMounted(() => {
  loadStores()
})

async function loadStores() {
  loading.value = true
  try {
    // 模拟加载数据
    await new Promise(resolve => setTimeout(resolve, 500))
    // 实际项目中调用 API
  } catch (error) {
    ElMessage.error(t('store.errors.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function handleAddStore() {
  if (!storeFormRef.value) return

  try {
    await storeFormRef.value.validate()
    submitLoading.value = true

    // 模拟添加店铺
    await new Promise(resolve => setTimeout(resolve, 1000))

    const newStore = {
      id: Date.now(),
      name: storeForm.name,
      storeId: storeForm.storeId,
      logo: '',
      status: 'active' as const,
      products: 0,
      orders: 0,
      revenue: 0,
      bindTime: new Date().toISOString().split('T')[0],
      profitConfig: null
    }

    stores.value.unshift(newStore)
    showAddDialog.value = false
    ElMessage.success(t('store.addSuccess'))

    // 重置表单
    storeFormRef.value.resetFields()
  } catch (error) {
    // 验证失败
  } finally {
    submitLoading.value = false
  }
}

function handleCommand(command: string, store: any) {
  currentStore.value = store

  switch (command) {
    case 'edit':
      ElMessage.info(t('store.actions.edit') + ': ' + store.name)
      break
    case 'sync':
      handleSyncStore(store)
      break
    case 'profit':
      openProfitConfig(store)
      break
    case 'settings':
      ElMessage.info(t('store.actions.settings') + ': ' + store.name)
      break
    case 'delete':
      showDeleteDialog.value = true
      break
  }
}

function openProfitConfig(store: any) {
  currentStore.value = store
  showProfitDialog.value = true
}

function handleSaveProfit(config: ProfitConfig) {
  if (!currentStore.value) return

  const storeIndex = stores.value.findIndex(s => s.id === currentStore.value.id)
  if (storeIndex !== -1) {
    stores.value[storeIndex].profitConfig = config
    ElMessage.success(t('store.profitConfig.saveSuccess'))
  }
}

async function handleSyncStore(store: any) {
  ElMessage.info(t('store.syncing'))
  await new Promise(resolve => setTimeout(resolve, 1500))
  ElMessage.success(t('store.syncSuccess'))
}

async function confirmDelete() {
  if (!currentStore.value) return

  stores.value = stores.value.filter(s => s.id !== currentStore.value.id)
  showDeleteDialog.value = false
  ElMessage.success(t('store.deleteSuccess'))
  currentStore.value = null
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString()
}

function openOzonSeller() {
  window.open('https://seller.ozon.ru', '_blank')
}
</script>

<style scoped>
.store-management {
  padding: var(--ozon-spacing-lg);
  background: var(--ozon-bg);
  min-height: 100%;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--ozon-spacing-lg);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--ozon-text-primary);
  margin: 0 0 4px 0;
}

.page-desc {
  font-size: 14px;
  color: var(--ozon-text-secondary);
  margin: 0;
}

/* 筛选栏 */
.filter-bar {
  margin-bottom: var(--ozon-spacing-lg);
}

/* 店铺网格 */
.store-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--ozon-spacing-lg);
}

/* 店铺卡片 */
.store-card {
  background: var(--ozon-bg-card);
  border-radius: var(--ozon-radius-lg);
  border: 1px solid var(--ozon-border-light);
  padding: var(--ozon-spacing-lg);
  transition: var(--ozon-transition);
}

.store-card:hover {
  box-shadow: var(--ozon-shadow-md);
  transform: translateY(-2px);
}

.store-card--inactive {
  opacity: 0.7;
}

.store-card--add {
  border: 2px dashed var(--ozon-border);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  min-height: 200px;
}

.store-card--add:hover {
  border-color: var(--ozon-primary);
  background: rgba(0, 91, 255, 0.02);
}

.add-content {
  text-align: center;
}

.add-icon {
  font-size: 32px;
  color: var(--ozon-text-tertiary);
  margin-bottom: 8px;
}

.store-card--add:hover .add-icon {
  color: var(--ozon-primary);
}

.add-text {
  font-size: 14px;
  color: var(--ozon-text-secondary);
}

/* 卡片头部 */
.store-card-header {
  display: flex;
  align-items: center;
  gap: var(--ozon-spacing-md);
  margin-bottom: var(--ozon-spacing-md);
}

.store-avatar {
  width: 48px;
  height: 48px;
  border-radius: var(--ozon-radius-md);
  overflow: hidden;
  background: var(--ozon-bg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.store-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  font-size: 20px;
  font-weight: 600;
  color: var(--ozon-primary);
}

.store-info {
  flex: 1;
  min-width: 0;
}

.store-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--ozon-text-primary);
  margin: 0 0 2px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.store-id {
  font-size: 12px;
  color: var(--ozon-text-tertiary);
  font-family: monospace;
}

/* 卡片内容 */
.store-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--ozon-spacing-sm);
  padding: var(--ozon-spacing-md) 0;
  border-top: 1px solid var(--ozon-border-light);
  border-bottom: 1px solid var(--ozon-border-light);
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: var(--ozon-text-primary);
}

.stat-label {
  font-size: 11px;
  color: var(--ozon-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

/* 利润配置预览 */
.profit-preview {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--ozon-spacing-md);
  padding: var(--ozon-spacing-sm) var(--ozon-spacing-md);
  background: rgba(0, 91, 255, 0.05);
  border-radius: var(--ozon-radius-md);
  border: 1px solid rgba(0, 91, 255, 0.1);
}

.profit-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.profit-label {
  font-size: 11px;
  color: var(--ozon-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.profit-rate {
  font-size: 14px;
  font-weight: 500;
  color: var(--ozon-primary);
}

/* 卡片底部 */
.store-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--ozon-spacing-md);
}

.store-status {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.status-active {
  background: var(--ozon-success);
  box-shadow: 0 0 6px var(--ozon-success);
}

.status-dot.status-expired {
  background: var(--ozon-warning);
}

.status-dot.status-unauthorized {
  background: var(--ozon-danger);
}

.status-text {
  font-size: 13px;
  color: var(--ozon-text-secondary);
}

/* Ozon 绑定区域 */
.ozon-link {
  color: var(--ozon-primary);
  font-weight: 500;
  cursor: pointer;
}

.ozon-bind-section {
  background: var(--ozon-bg);
  border-radius: var(--ozon-radius-md);
  padding: var(--ozon-spacing-md);
}

.bind-hint {
  font-size: 13px;
  color: var(--ozon-text-secondary);
  margin: 0 0 8px 0;
}

.bind-steps {
  font-size: 13px;
  color: var(--ozon-text-secondary);
  padding-left: 20px;
  margin: 0 0 12px 0;
}

.bind-steps li {
  margin-bottom: 4px;
}
</style>
