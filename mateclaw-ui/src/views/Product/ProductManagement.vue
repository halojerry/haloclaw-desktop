<template>
  <div class="product-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">{{ t('product.title') }}</h1>
        <p class="page-desc">{{ t('product.description') }}</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="handleAddProduct">
          {{ t('product.addProduct') }}
        </el-button>
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section ozon-card">
      <div class="filter-row">
        <el-select
          v-model="filters.storeId"
          :placeholder="t('product.filter.selectStore')"
          clearable
          style="width: 200px"
        >
          <el-option
            v-for="store in stores"
            :key="store.id"
            :label="store.name"
            :value="store.id"
          />
        </el-select>

        <el-select
          v-model="filters.status"
          :placeholder="t('product.filter.selectStatus')"
          clearable
          style="width: 150px"
        >
          <el-option :label="t('product.status.all')" value="" />
          <el-option :label="t('product.status.onSale')" value="on_sale" />
          <el-option :label="t('product.status.pending')" value="pending" />
          <el-option :label="t('product.status.soldOut')" value="sold_out" />
          <el-option :label="t('product.status.draft')" value="draft" />
        </el-select>

        <el-select
          v-model="filters.category"
          :placeholder="t('product.filter.selectCategory')"
          clearable
          filterable
          style="width: 200px"
        >
          <el-option
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>

        <el-input
          v-model="filters.keyword"
          :placeholder="t('product.filter.searchPlaceholder')"
          :prefix-icon="Search"
          clearable
          style="width: 240px"
        />
      </div>

      <div class="filter-row">
        <span class="filter-label">{{ t('product.filter.priceRange') }}:</span>
        <el-input-number
          v-model="filters.priceMin"
          :min="0"
          :precision="2"
          :placeholder="t('product.filter.min')"
          style="width: 120px"
        />
        <span class="filter-separator">-</span>
        <el-input-number
          v-model="filters.priceMax"
          :min="0"
          :precision="2"
          :placeholder="t('product.filter.max')"
          style="width: 120px"
        />

        <el-checkbox v-model="filters.hasImages" style="margin-left: 16px">
          {{ t('product.filter.hasImages') }}
        </el-checkbox>

        <el-button :icon="Refresh" text @click="resetFilters">
          {{ t('product.filter.reset') }}
        </el-button>
      </div>
    </div>

    <!-- 批量操作栏 -->
    <div class="batch-actions" v-if="selectedProducts.length > 0">
      <el-checkbox
        v-model="selectAll"
        :indeterminate="isIndeterminate"
        @change="handleSelectAll"
      >
        {{ t('product.selected', { count: selectedProducts.length }) }}
      </el-checkbox>

      <div class="batch-buttons">
        <el-button size="small" @click="handleBatchPublish">
          {{ t('product.batch.publish') }}
        </el-button>
        <el-button size="small" @click="handleBatchUnpublish">
          {{ t('product.batch.unpublish') }}
        </el-button>
        <el-button size="small" @click="handleBatchDelete" type="danger">
          {{ t('product.batch.delete') }}
        </el-button>
      </div>
    </div>

    <!-- 商品列表 -->
    <div class="product-table" v-loading="loading">
      <el-table
        ref="tableRef"
        :data="filteredProducts"
        @selection-change="handleSelectionChange"
        style="width: 100%"
        :header-cell-style="{ background: 'var(--ozon-bg)', color: 'var(--ozon-text-primary)' }"
      >
        <el-table-column type="selection" width="50" />

        <el-table-column :label="t('product.columns.product')" min-width="280">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image
                :src="row.images?.[0] || '/placeholder.png'"
                fit="cover"
                class="product-image"
              >
                <template #error>
                  <div class="image-placeholder">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="product-info">
                <span class="product-name">{{ row.name }}</span>
                <span class="product-sku">SKU: {{ row.offerId }}</span>
                <span class="product-category">{{ row.categoryName }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="t('product.columns.price')" width="140">
          <template #default="{ row }">
            <div class="price-cell">
              <span class="price-value">{{ row.price }}</span>
              <span class="price-currency">₽</span>
            </div>
            <span class="price-original" v-if="row.oldPrice">
              {{ row.oldPrice }} ₽
            </span>
          </template>
        </el-table-column>

        <el-table-column :label="t('product.columns.stock')" width="100" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.stock === 0 }">
              {{ row.stock }}
            </span>
          </template>
        </el-table-column>

        <el-table-column :label="t('product.columns.status')" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ t('product.status.' + row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="t('product.columns.rating')" width="100" align="center">
          <template #default="{ row }">
            <div class="rating-cell" v-if="row.rating">
              <el-icon class="star-icon"><StarFilled /></el-icon>
              <span>{{ row.rating }}</span>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column :label="t('product.columns.updatedAt')" width="160">
          <template #default="{ row }">
            <span class="text-secondary">{{ formatDate(row.updatedAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column :label="t('product.columns.actions')" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">
              {{ t('common.view') }}
            </el-button>
            <el-button link type="primary" @click="handleEdit(row)">
              {{ t('common.edit') }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              {{ t('common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 商品详情抽屉 -->
    <el-drawer
      v-model="showDetailDrawer"
      :title="t('product.detail.title')"
      size="600px"
      direction="rtl"
    >
      <div class="product-detail" v-if="currentProduct">
        <div class="detail-images">
          <el-image
            v-for="(img, index) in currentProduct.images"
            :key="index"
            :src="img"
            fit="cover"
            class="detail-image"
            :preview-src-list="currentProduct.images"
          />
        </div>

        <div class="detail-info">
          <h2 class="detail-name">{{ currentProduct.name }}</h2>

          <div class="detail-section">
            <h4>{{ t('product.detail.basicInfo') }}</h4>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="t('product.detail.offerId')">
                {{ currentProduct.offerId }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('product.detail.category')">
                {{ currentProduct.categoryName }}
              </el-descriptions-item>
              <el-descriptions-item :label="t('product.detail.price')">
                {{ currentProduct.price }} ₽
              </el-descriptions-item>
              <el-descriptions-item :label="t('product.detail.stock')">
                {{ currentProduct.stock }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="detail-section">
            <h4>{{ t('product.detail.description') }}</h4>
            <div class="detail-description" v-html="currentProduct.description"></div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="showDetailDrawer = false">{{ t('common.close') }}</el-button>
          <el-button type="primary" @click="handleEdit(currentProduct)">
            {{ t('common.edit') }}
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, Picture, StarFilled } from '@element-plus/icons-vue'
import type { TableInstance } from 'element-plus'

const { t } = useI18n()

// 状态
const loading = ref(false)
const tableRef = ref<TableInstance>()
const selectedProducts = ref<any[]>([])
const selectAll = ref(false)
const showDetailDrawer = ref(false)
const currentProduct = ref<any>(null)

// 店铺列表
const stores = ref([
  { id: 1, name: '我的Ozon店铺' },
  { id: 2, name: '俄罗斯精品店' }
])

// 类目列表
const categories = ref([
  { id: 1, name: '电子产品' },
  { id: 2, name: '服装' },
  { id: 3, name: '家居' },
  { id: 4, name: '运动' }
])

// 筛选条件
const filters = reactive({
  storeId: '',
  status: '',
  category: '',
  keyword: '',
  priceMin: null as number | null,
  priceMax: null as number | null,
  hasImages: false
})

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 模拟商品数据
const products = ref([
  {
    id: 1,
    name: '无线蓝牙耳机 高品质音质',
    offerId: 'SKU-001',
    categoryName: '电子产品',
    price: 2990,
    oldPrice: 3990,
    stock: 156,
    status: 'on_sale',
    rating: 4.8,
    images: ['https://via.placeholder.com/200'],
    description: '高品质无线蓝牙耳机，支持降噪功能...',
    updatedAt: '2024-04-15'
  },
  {
    id: 2,
    name: '运动休闲T恤 男款',
    offerId: 'SKU-002',
    categoryName: '服装',
    price: 890,
    stock: 234,
    status: 'on_sale',
    rating: 4.5,
    images: [],
    description: '舒适透气的运动T恤...',
    updatedAt: '2024-04-14'
  },
  {
    id: 3,
    name: '智能手表 运动版',
    offerId: 'SKU-003',
    categoryName: '电子产品',
    price: 5990,
    oldPrice: 7990,
    stock: 0,
    status: 'sold_out',
    rating: 4.6,
    images: ['https://via.placeholder.com/200'],
    description: '多功能智能手表...',
    updatedAt: '2024-04-13'
  },
  {
    id: 4,
    name: '家居收纳盒套装',
    offerId: 'SKU-004',
    categoryName: '家居',
    price: 1290,
    stock: 89,
    status: 'pending',
    images: [],
    description: '简约实用的收纳盒套装...',
    updatedAt: '2024-04-12'
  }
])

// 过滤后的商品列表
const filteredProducts = computed(() => {
  let result = [...products.value]

  if (filters.keyword) {
    const keyword = filters.keyword.toLowerCase()
    result = result.filter(p =>
      p.name.toLowerCase().includes(keyword) ||
      p.offerId.toLowerCase().includes(keyword)
    )
  }

  if (filters.status) {
    result = result.filter(p => p.status === filters.status)
  }

  if (filters.category) {
    const categoryId = Number(filters.category)
    const categoryName = categories.value.find(c => c.id === categoryId)?.name
    if (categoryName) {
      result = result.filter(p => p.categoryName === categoryName)
    }
  }

  if (filters.priceMin !== null) {
    result = result.filter(p => p.price >= (filters.priceMin || 0))
  }

  if (filters.priceMax !== null) {
    result = result.filter(p => p.price <= (filters.priceMax || Infinity))
  }

  if (filters.hasImages) {
    result = result.filter(p => p.images && p.images.length > 0)
  }

  return result
})

const isIndeterminate = computed(() => {
  return selectedProducts.value.length > 0 && selectedProducts.value.length < filteredProducts.value.length
})

onMounted(() => {
  loadProducts()
})

async function loadProducts() {
  loading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
    pagination.total = products.value.length
  } catch (error) {
    ElMessage.error(t('product.errors.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(selection: any[]) {
  selectedProducts.value = selection
  selectAll.value = selection.length === filteredProducts.value.length && filteredProducts.value.length > 0
}

function handleSelectAll(val: boolean) {
  tableRef.value?.toggleAllSelection()
}

function resetFilters() {
  Object.keys(filters).forEach(key => {
    (filters as any)[key] = key === 'hasImages' ? false : null
    if (key === 'hasImages') (filters as any)[key] = false
  })
  filters.keyword = ''
}

function handleSizeChange() {
  pagination.page = 1
}

function handlePageChange() {
  // 实际项目中加载对应页数据
}

function handleAddProduct() {
  ElMessage.info(t('product.addProduct'))
}

function handleView(product: any) {
  currentProduct.value = product
  showDetailDrawer.value = true
}

function handleEdit(product: any) {
  ElMessage.info(t('common.edit') + ': ' + product.name)
}

async function handleDelete(product: any) {
  try {
    await ElMessageBox.confirm(
      t('product.deleteConfirm.message', { name: product.name }),
      t('product.deleteConfirm.title'),
      { type: 'warning' }
    )
    products.value = products.value.filter(p => p.id !== product.id)
    ElMessage.success(t('product.deleteSuccess'))
  } catch {
    // 用户取消
  }
}

async function handleBatchPublish() {
  ElMessage.success(t('product.batch.publishSuccess', { count: selectedProducts.value.length }))
}

async function handleBatchUnpublish() {
  ElMessage.success(t('product.batch.unpublishSuccess', { count: selectedProducts.value.length }))
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      t('product.batch.deleteConfirm.message', { count: selectedProducts.value.length }),
      t('product.batch.deleteConfirm.title'),
      { type: 'warning' }
    )
    ElMessage.success(t('product.batch.deleteSuccess', { count: selectedProducts.value.length }))
    selectedProducts.value = []
  } catch {
    // 用户取消
  }
}

function getStatusType(status: string) {
  const types: Record<string, string> = {
    on_sale: 'success',
    pending: 'warning',
    sold_out: 'info',
    draft: ''
  }
  return types[status] || ''
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}
</script>

<style scoped>
.product-management {
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

/* 筛选区域 */
.filter-section {
  margin-bottom: var(--ozon-spacing-lg);
  padding: var(--ozon-spacing-lg);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: var(--ozon-spacing-md);
  flex-wrap: wrap;
}

.filter-row + .filter-row {
  margin-top: var(--ozon-spacing-md);
  padding-top: var(--ozon-spacing-md);
  border-top: 1px solid var(--ozon-border-light);
}

.filter-label {
  font-size: 14px;
  color: var(--ozon-text-secondary);
  white-space: nowrap;
}

.filter-separator {
  color: var(--ozon-text-tertiary);
}

/* 批量操作栏 */
.batch-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--ozon-spacing-md) var(--ozon-spacing-lg);
  background: var(--ozon-bg-light);
  border-radius: var(--ozon-radius-md);
  margin-bottom: var(--ozon-spacing-md);
}

.batch-buttons {
  display: flex;
  gap: var(--ozon-spacing-sm);
}

/* 商品列表 */
.product-table {
  background: var(--ozon-bg-card);
  border-radius: var(--ozon-radius-lg);
  overflow: hidden;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: var(--ozon-spacing-md);
}

.product-image {
  width: 60px;
  height: 60px;
  border-radius: var(--ozon-radius-sm);
  flex-shrink: 0;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ozon-bg);
  color: var(--ozon-text-tertiary);
}

.product-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.product-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--ozon-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-sku {
  font-size: 12px;
  color: var(--ozon-text-tertiary);
  font-family: monospace;
}

.product-category {
  font-size: 12px;
  color: var(--ozon-primary);
}

.price-cell {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.price-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--ozon-text-primary);
}

.price-currency {
  font-size: 12px;
  color: var(--ozon-text-secondary);
}

.price-original {
  font-size: 12px;
  color: var(--ozon-text-tertiary);
  text-decoration: line-through;
}

.rating-cell {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--ozon-warning);
}

.star-icon {
  font-size: 14px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--ozon-spacing-lg);
}

/* 商品详情抽屉 */
.product-detail {
  padding: var(--ozon-spacing-md);
}

.detail-images {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--ozon-spacing-sm);
  margin-bottom: var(--ozon-spacing-lg);
}

.detail-image {
  width: 100%;
  aspect-ratio: 1;
  border-radius: var(--ozon-radius-md);
  cursor: pointer;
}

.detail-info {
  color: var(--ozon-text-primary);
}

.detail-name {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 var(--ozon-spacing-lg) 0;
}

.detail-section {
  margin-bottom: var(--ozon-spacing-lg);
}

.detail-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--ozon-text-secondary);
  margin: 0 0 var(--ozon-spacing-md) 0;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.detail-description {
  font-size: 14px;
  line-height: 1.6;
  color: var(--ozon-text-secondary);
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--ozon-spacing-sm);
}

.text-secondary {
  color: var(--ozon-text-secondary);
}

.text-muted {
  color: var(--ozon-text-tertiary);
}

.text-danger {
  color: var(--ozon-danger);
}
</style>
