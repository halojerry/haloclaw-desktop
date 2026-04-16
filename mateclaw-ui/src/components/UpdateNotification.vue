<template>
  <Transition name="slide-fade">
    <div v-if="visible" class="update-notification">
      <div class="update-card">
        <!-- 图标 -->
        <div class="update-icon">
          <svg v-if="status === 'downloaded'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
          <svg v-else-if="status === 'available'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="16" x2="12" y2="12"/>
            <line x1="12" y1="8" x2="12.01" y2="8"/>
          </svg>
          <svg v-else-if="status === 'downloading'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="spin">
            <line x1="12" y1="2" x2="12" y2="6"/>
            <line x1="12" y1="18" x2="12" y2="22"/>
            <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"/>
            <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"/>
            <line x1="2" y1="12" x2="6" y2="12"/>
            <line x1="18" y1="12" x2="22" y2="12"/>
            <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"/>
            <line x1="16.24" y1="7.76" x2="19.07" y2="4.93"/>
          </svg>
          <svg v-else-if="status === 'error'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>

        <!-- 内容 -->
        <div class="update-content">
          <h3 class="update-title">{{ title }}</h3>
          <p class="update-message">{{ message }}</p>
          
          <!-- 下载进度条 -->
          <div v-if="status === 'downloading'" class="progress-container">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: `${progress}%` }"></div>
            </div>
            <span class="progress-text">{{ progress }}%</span>
          </div>

          <!-- 发布说明 -->
          <div v-if="releaseNotes" class="release-notes">
            <details>
              <summary>查看更新内容</summary>
              <div class="notes-content" v-html="formattedNotes"></div>
            </details>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="update-actions">
          <button 
            v-if="status === 'available'" 
            class="btn btn-primary"
            @click="handleDownload"
          >
            下载更新
          </button>
          
          <button 
            v-if="status === 'downloaded'" 
            class="btn btn-primary"
            @click="handleInstall"
          >
            立即更新
          </button>
          
          <button 
            v-if="status === 'downloading'" 
            class="btn btn-secondary" 
            disabled
          >
            下载中...
          </button>

          <button 
            v-if="status === 'available' || status === 'downloaded' || status === 'error'" 
            class="btn btn-ghost"
            @click="handleLater"
          >
            稍后提醒
          </button>
          
          <button 
            v-if="status === 'error'" 
            class="btn btn-ghost"
            @click="handleRetry"
          >
            重试
          </button>
        </div>

        <!-- 关闭按钮 -->
        <button class="close-btn" @click="handleClose">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

// 更新状态类型
interface UpdateStatus {
  status: 'idle' | 'checking' | 'available' | 'downloading' | 'downloaded' | 'error'
  progress?: number
  version?: string
  releaseNotes?: string
  error?: string
}

// 状态
const visible = ref(false)
const updateStatus = ref<UpdateStatus>({ status: 'idle' })

// 解析状态
const status = computed(() => updateStatus.value.status)
const progress = computed(() => updateStatus.value.progress || 0)
const releaseNotes = computed(() => updateStatus.value.releaseNotes)

// 格式化标题
const title = computed(() => {
  switch (status.value) {
    case 'checking':
      return '检查更新中...'
    case 'available':
      return `发现新版本 v${updateStatus.value.version}`
    case 'downloading':
      return '正在下载更新'
    case 'downloaded':
      return '更新已准备好'
    case 'error':
      return '更新失败'
    default:
      return '系统更新'
  }
})

// 格式化消息
const message = computed(() => {
  switch (status.value) {
    case 'checking':
      return '正在检查是否有可用更新...'
    case 'available':
      return '新版本包含功能优化和问题修复'
    case 'downloading':
      return `正在下载更新包，请稍候...`
    case 'downloaded':
      return '更新包已下载完成，立即重启以应用更新'
    case 'error':
      return updateStatus.value.error || '检查更新时出现问题'
    default:
      return ''
  }
})

// 格式化发布说明
const formattedNotes = computed(() => {
  if (!releaseNotes.value) return ''
  return releaseNotes.value
    .replace(/\n/g, '<br>')
    .replace(/•/g, '•')
})

// 监听器清理函数
let unsubscribe: (() => void) | null = null

// 初始化
onMounted(() => {
  // 监听更新状态
  if (window.electronAPI?.onUpdateStatus) {
    unsubscribe = window.electronAPI.onUpdateStatus((status: UpdateStatus) => {
      updateStatus.value = status
      
      // 自动显示通知
      if (['available', 'downloading', 'downloaded', 'error'].includes(status.status)) {
        visible.value = true
      }
    })
  }
})

// 清理
onUnmounted(() => {
  if (unsubscribe) {
    unsubscribe()
  }
})

// 处理下载
const handleDownload = async () => {
  await window.electronAPI?.updateDownload()
}

// 处理安装
const handleInstall = async () => {
  await window.electronAPI?.updateInstall()
}

// 稍后提醒
const handleLater = () => {
  visible.value = false
}

// 重试
const handleRetry = async () => {
  await window.electronAPI?.updateCheck()
}

// 关闭
const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.update-notification {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.update-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  max-width: 380px;
  position: relative;
}

.update-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #f0f5ff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.update-icon svg {
  width: 24px;
  height: 24px;
  color: #1677ff;
}

.update-content {
  flex: 1;
  min-width: 0;
}

.update-title {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.update-message {
  margin: 0 0 12px;
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

/* 进度条 */
.progress-container {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #1677ff, #4096ff);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: #999;
  min-width: 36px;
  text-align: right;
}

/* 发布说明 */
.release-notes {
  margin-top: 8px;
}

.release-notes details {
  font-size: 13px;
}

.release-notes summary {
  color: #1677ff;
  cursor: pointer;
  user-select: none;
}

.release-notes summary:hover {
  text-decoration: underline;
}

.notes-content {
  margin-top: 8px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  color: #666;
  max-height: 120px;
  overflow-y: auto;
  line-height: 1.6;
}

/* 操作按钮 */
.update-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.btn {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.btn-primary {
  background: #1677ff;
  color: #fff;
}

.btn-primary:hover {
  background: #4096ff;
}

.btn-secondary {
  background: #f5f5f5;
  color: #666;
}

.btn-ghost {
  background: transparent;
  color: #999;
}

.btn-ghost:hover {
  color: #666;
  background: #f5f5f5;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

/* 关闭按钮 */
.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f5f5f5;
  color: #666;
}

.close-btn svg {
  width: 16px;
  height: 16px;
}

/* 动画 */
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s ease-in;
}

.slide-fade-enter-from {
  transform: translateX(100%);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

/* 旋转动画 */
.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 暗色模式 */
@media (prefers-color-scheme: dark) {
  .update-card {
    background: #1f1f1f;
  }
  
  .update-title {
    color: #fff;
  }
  
  .update-message,
  .notes-content {
    color: #a6a6a6;
  }
  
  .update-icon {
    background: #2a2a2a;
  }
  
  .progress-bar {
    background: #333;
  }
  
  .notes-content {
    background: #2a2a2a;
  }
  
  .btn-secondary {
    background: #333;
    color: #a6a6a6;
  }
}
</style>
