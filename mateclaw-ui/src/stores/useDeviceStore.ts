/**
 * 设备管理 Store
 * 
 * 管理设备ID生成、注册、认证和心跳
 */

import { defineStore } from 'pinia'
import { deviceApi } from '@/api/deviceApi'
import type { DeviceInfo, DeviceAuthResponse } from '@/api/deviceApi'
import { ElMessage } from 'element-plus'

// 设备ID存储键
const DEVICE_ID_KEY = 'mateclaw_device_id'
const DEVICE_NAME_KEY = 'mateclaw_device_name'
const SESSION_TOKEN_KEY = 'mateclaw_session_token'

/**
 * 简单的哈希函数（独立于 store）
 */
function simpleHash(str: string): string {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash // Convert to 32bit integer
  }
  return Math.abs(hash).toString(16).padStart(8, '0').toUpperCase()
}

export const useDeviceStore = defineStore('device', {
  state: () => ({
    /** 设备唯一标识 */
    deviceId: '',
    /** 设备名称 */
    deviceName: '',
    /** 会话Token */
    sessionToken: '',
    /** 设备是否已注册 */
    isRegistered: false,
    /** 心跳定时器 */
    heartbeatTimer: null as ReturnType<typeof setInterval> | null,
    /** 心跳间隔（毫秒） */
    heartbeatInterval: 5 * 60 * 1000, // 5分钟
    /** 用户绑定的设备列表 */
    boundDevices: [] as DeviceInfo[],
    /** 设备数量限制 */
    deviceLimit: 5,
    /** 当前设备数量 */
    currentDeviceCount: 0,
    /** 是否正在注册 */
    isRegistering: false
  }),

  getters: {
    /** 是否已登录（设备已注册且用户已登录） */
    isAuthenticated(): boolean {
      return this.isRegistered && !!localStorage.getItem('token')
    }
  },

  actions: {
    /** 获取设备ID（如果没有则生成新的） */
    getDeviceId(): string {
      if (!this.deviceId) {
        // 尝试从本地存储加载
        let deviceId = localStorage.getItem(DEVICE_ID_KEY)
        
        if (!deviceId) {
          // 生成新的设备ID
          const ua = navigator.userAgent
          const screenInfo = `${screen.width}x${screen.height}x${screen.colorDepth}`
          const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
          const language = navigator.language
          const platform = navigator.platform || 'unknown'
          const deviceInfo = `${ua}|${screenInfo}|${timezone}|${language}|${platform}`
          const hash = simpleHash(deviceInfo)
          const timestamp = Date.now().toString(36)
          deviceId = `DC-${hash}-${timestamp}`.toUpperCase()
          localStorage.setItem(DEVICE_ID_KEY, deviceId)
        }
        
        this.deviceId = deviceId
      }
      return this.deviceId
    },

    /**
     * 加载或生成设备ID
     */
    loadOrGenerateDeviceId(): string {
      // 尝试从本地存储加载
      let deviceId = localStorage.getItem(DEVICE_ID_KEY)
      
      if (deviceId) {
        this.deviceId = deviceId
        return deviceId
      }
      
      // 生成新的设备ID
      deviceId = this.generateDeviceId()
      localStorage.setItem(DEVICE_ID_KEY, deviceId)
      this.deviceId = deviceId
      return deviceId
    },

    /**
     * 生成设备ID
     * 
     * 使用机器特征生成唯一ID：
     * - 浏览器：使用userAgent + screen信息 + timezone
     * - Electron：使用机器码（CPU ID + 主板序列号 + MAC地址）
     */
    generateDeviceId(): string {
      const ua = navigator.userAgent
      const screenInfo = `${screen.width}x${screen.height}x${screen.colorDepth}`
      const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
      const language = navigator.language
      const platform = navigator.platform || 'unknown'
      
      // 组合所有特征
      const deviceInfo = `${ua}|${screenInfo}|${timezone}|${language}|${platform}`
      
      // 使用简单的哈希函数生成ID
      const hash = simpleHash(deviceInfo)
      const timestamp = Date.now().toString(36)
      
      return `DC-${hash}-${timestamp}`.toUpperCase()
    },

    /**
     * 获取设备名称
     */
    getDeviceName(): string {
      if (!this.deviceName) {
        this.deviceName = localStorage.getItem(DEVICE_NAME_KEY) || this.generateDeviceName()
      }
      return this.deviceName
    },

    /**
     * 生成设备名称
     */
    generateDeviceName(): string {
      const isElectron = !!(window as any).require?.(/* electron */)
      const isMobile = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent)
      
      let name = ''
      if (isElectron) {
        name = '桌面端'
      } else if (isMobile) {
        name = '移动端'
      } else {
        name = '浏览器'
      }
      
      const now = new Date()
      name += `-${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
      
      localStorage.setItem(DEVICE_NAME_KEY, name)
      this.deviceName = name
      return name
    },

    /**
     * 获取设备信息
     */
    getDeviceInfo(): {
      deviceId: string
      deviceName: string
      deviceInfo: string
      deviceType: string
    } {
      return {
        deviceId: this.getDeviceId(),
        deviceName: this.getDeviceName(),
        deviceInfo: navigator.userAgent,
        deviceType: this.getDeviceType()
      }
    },

    /**
     * 获取设备类型
     */
    getDeviceType(): string {
      // @ts-ignore
      const isElectron = !!(window as any).process?.type
      const isMobile = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent)
      
      if (isElectron) return 'desktop'
      if (isMobile) return 'mobile'
      return 'web'
    },

    /**
     * 注册设备
     */
    async registerDevice(): Promise<boolean> {
      if (this.isRegistering) return false
      
      this.isRegistering = true
      
      try {
        const deviceInfo = this.getDeviceInfo()
        const response = await deviceApi.register(deviceInfo)
        const data = response.data
        
        if (data.success) {
          this.isRegistered = true
          this.sessionToken = data.sessionToken || ''
          this.boundDevices = data.boundDevices || []
          this.deviceLimit = data.deviceLimit || 5
          this.currentDeviceCount = data.currentDeviceCount || 1
          
          if (data.sessionToken) {
            localStorage.setItem(SESSION_TOKEN_KEY, data.sessionToken)
          }
          
          ElMessage.success(data.message || '设备注册成功')
          return true
        } else {
          ElMessage.warning(data.message || '设备注册失败')
          return false
        }
      } catch (error: any) {
        console.error('设备注册失败:', error)
        ElMessage.error('设备注册失败: ' + (error.message || '未知错误'))
        return false
      } finally {
        this.isRegistering = false
      }
    },

    /**
     * 认证设备
     */
    async authenticateDevice(): Promise<boolean> {
      try {
        const deviceId = this.getDeviceId()
        const response = await deviceApi.authenticate(deviceId)
        const data = response.data
        
        if (data.success) {
          this.isRegistered = true
          this.sessionToken = data.sessionToken || ''
          this.boundDevices = data.boundDevices || []
          this.currentDeviceCount = data.currentDeviceCount || 1
          
          return true
        } else {
          // 设备未注册，需要重新注册
          return await this.registerDevice()
        }
      } catch (error: any) {
        console.error('设备认证失败:', error)
        return false
      }
    },

    /**
     * 发送心跳
     */
    async sendHeartbeat(): Promise<void> {
      try {
        const deviceId = this.getDeviceId()
        const response = await deviceApi.heartbeat(deviceId)
        const data = response.data
        
        if (data.success) {
          console.debug('心跳成功:', new Date().toISOString())
        }
      } catch (error: any) {
        console.error('心跳失败:', error)
      }
    },

    /**
     * 启动心跳定时器
     */
    startHeartbeat(): void {
      if (this.heartbeatTimer) {
        clearInterval(this.heartbeatTimer)
      }
      
      // 立即发送一次心跳
      this.sendHeartbeat()
      
      // 启动定时器
      this.heartbeatTimer = setInterval(() => {
        this.sendHeartbeat()
      }, this.heartbeatInterval)
      
      console.log('心跳定时器已启动，间隔:', this.heartbeatInterval / 1000, '秒')
    },

    /**
     * 停止心跳定时器
     */
    stopHeartbeat(): void {
      if (this.heartbeatTimer) {
        clearInterval(this.heartbeatTimer)
        this.heartbeatTimer = null
        console.log('心跳定时器已停止')
      }
    },

    /**
     * 获取设备列表
     */
    async fetchDeviceList(): Promise<void> {
      try {
        const response = await deviceApi.list()
        const devices = response.data
        this.boundDevices = devices
        this.currentDeviceCount = devices.length
      } catch (error: any) {
        console.error('获取设备列表失败:', error)
      }
    },

    /**
     * 解绑设备
     */
    async unbindDevice(deviceId: string): Promise<boolean> {
      try {
        await deviceApi.unbind(deviceId)
        await this.fetchDeviceList()
        ElMessage.success('设备已解绑')
        return true
      } catch (error: any) {
        console.error('解绑设备失败:', error)
        ElMessage.error('解绑失败: ' + (error.message || '未知错误'))
        return false
      }
    },

    /**
     * 初始化设备
     * 
     * 应用启动时调用，确保设备已注册
     */
    async initialize(): Promise<void> {
      // 生成或加载设备ID
      this.loadOrGenerateDeviceId()
      
      // 检查是否已注册
      const savedToken = localStorage.getItem(SESSION_TOKEN_KEY)
      if (savedToken) {
        this.sessionToken = savedToken
        await this.authenticateDevice()
      } else {
        await this.registerDevice()
      }
      
      // 启动心跳
      this.startHeartbeat()
    },

    /**
     * 清理设备状态
     * 
     * 登出时调用
     */
    cleanup(): void {
      this.stopHeartbeat()
      this.isRegistered = false
      this.sessionToken = ''
      this.boundDevices = []
    },

    /**
     * 重置设备
     * 
     * 清除本地存储的设备ID，重新生成
     */
    resetDevice(): void {
      this.stopHeartbeat()
      localStorage.removeItem(DEVICE_ID_KEY)
      localStorage.removeItem(DEVICE_NAME_KEY)
      localStorage.removeItem(SESSION_TOKEN_KEY)
      
      this.deviceId = ''
      this.deviceName = ''
      this.sessionToken = ''
      this.isRegistered = false
      this.boundDevices = []
      
      // 重新初始化
      this.initialize()
    }
  }
})
