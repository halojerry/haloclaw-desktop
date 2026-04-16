/**
 * MateClaw Desktop - 更新功能集成示例
 * 
 * 如何在应用中使用自动更新功能
 */

import { onMounted, onUnmounted } from 'vue'
import UpdateNotification from '@/components/UpdateNotification.vue'
import { updateService, type UpdateStatus } from '@/services/UpdateService'

// ====== 示例1：在 Vue 组件中使用 ======

/**
 * 在 App.vue 中集成更新通知
 */
export function useUpdateNotification() {
  let unsubscribe: (() => void) | null = null

  onMounted(() => {
    // 初始化更新服务
    updateService.init()
    
    // 监听更新状态变化
    unsubscribe = updateService.addListener((status: UpdateStatus) => {
      console.log('[Update] Status changed:', status)
      
      // 自定义处理逻辑
      if (status.status === 'error') {
        console.error('[Update] Error:', status.error)
      }
    })
    
    // 可选：启动时自动检查更新
    // updateService.check()
  })

  onUnmounted(() => {
    // 清理
    if (unsubscribe) {
      unsubscribe()
    }
    updateService.destroy()
  })

  return {
    component: UpdateNotification
  }
}

// ====== 示例2：手动控制更新 ======

/**
 * 手动检查和下载更新
 */
export async function manualUpdate() {
  // 获取当前版本
  const currentVersion = await updateService.getVersion()
  console.log('Current version:', currentVersion)

  // 检查更新
  console.log('Checking for updates...')
  await updateService.check()
}

// ====== 示例3：检查待安装更新 ======

/**
 * 检查是否有待安装的更新
 */
export function checkPendingUpdate() {
  if (updateService.hasPendingUpdate()) {
    console.log('A new version is ready to install!')
    // 可以显示一个更醒目的提示
    return true
  }
  return false
}

// ====== 示例4：在设置页面中集成 ======

/**
 * 设置页面的更新管理
 */
export const updateSettings = {
  // 检查更新
  async checkForUpdates() {
    await updateService.check()
  },

  // 下载更新
  async downloadUpdate() {
    if (updateService.getStatus().status === 'available') {
      await updateService.download()
    }
  },

  // 安装更新
  async installUpdate() {
    if (updateService.hasPendingUpdate()) {
      await updateService.install()
    }
  },

  // 获取当前状态
  getStatus(): UpdateStatus {
    return updateService.getStatus()
  }
}

// ====== 示例5：模板中使用 ======

/**
 * 完整的 App.vue 集成示例
 */
/*
<template>
  <div id="app">
    <!-- 主应用内容 -->
    <router-view />
    
    <!-- 更新通知组件 -->
    <UpdateNotification />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import UpdateNotification from '@/components/UpdateNotification.vue'
import { updateService } from '@/services/UpdateService'

let unsubscribe: (() => void) | null = null

onMounted(() => {
  // 初始化更新服务
  updateService.init()
  
  // 监听更新状态
  unsubscribe = updateService.addListener((status) => {
    // 这里可以添加额外的处理逻辑
    if (status.status === 'downloaded') {
      // 更新下载完成，可以播放提示音等
    }
  })
})

onUnmounted(() => {
  if (unsubscribe) {
    unsubscribe()
  }
  updateService.destroy()
})
</script>
*/

// ====== 导出组件 ======
export { UpdateNotification }
