<template>
  <el-config-provider :locale="elementLocale">
    <router-view />
    <!-- 自动更新通知组件 -->
    <UpdateNotification v-if="isPackaged" />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, watchEffect, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import en from 'element-plus/es/locale/lang/en'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { currentLocale } from '@/i18n'
import { useThemeStore } from '@/stores/useThemeStore'
import UpdateNotification from '@/components/UpdateNotification.vue'
import { updateService } from '@/services/UpdateService'

// Initialize theme — applies .dark class to <html> immediately
useThemeStore()

const { t } = useI18n()

// 检查是否为打包环境
const isPackaged = computed(() => {
  return typeof window !== 'undefined' && window.electronAPI?.isPackaged === true
})

// 生命周期管理
let unsubscribe: (() => void) | null = null

onMounted(() => {
  // 初始化更新服务
  if (isPackaged.value) {
    updateService.init()
    
    // 监听更新状态
    unsubscribe = updateService.addListener((status) => {
      console.log('[App] Update status:', status)
    })
  }
})

onUnmounted(() => {
  // 清理
  if (unsubscribe) {
    unsubscribe()
  }
  updateService.destroy()
})

watchEffect(() => {
  document.title = t('app.title')
})

const elementLocale = computed(() => (currentLocale.value === 'en-US' ? en : zhCn))
</script>
