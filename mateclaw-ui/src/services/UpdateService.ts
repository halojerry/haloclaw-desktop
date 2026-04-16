/**
 * MateClaw Desktop - 前端更新服务
 * 封装与主进程更新服务的通信
 */

// 更新状态类型
export interface UpdateStatus {
  status: 'idle' | 'checking' | 'available' | 'downloading' | 'downloaded' | 'error'
  progress?: number
  version?: string
  releaseNotes?: string
  error?: string
}

// 事件类型
type UpdateListener = (status: UpdateStatus) => void

class UpdateService {
  private listeners: UpdateListener[] = []
  private currentStatus: UpdateStatus = { status: 'idle' }
  private unsubscribe: (() => void) | null = null

  /**
   * 初始化更新服务
   */
  init(): void {
    if (typeof window === 'undefined' || !window.electronAPI) {
      console.warn('[UpdateService] Electron API not available')
      return
    }

    // 订阅主进程更新状态
    this.unsubscribe = window.electronAPI.onUpdateStatus((status: UpdateStatus) => {
      this.currentStatus = status
      this.notifyListeners(status)
    })

    console.log('[UpdateService] Initialized')
  }

  /**
   * 销毁更新服务
   */
  destroy(): void {
    if (this.unsubscribe) {
      this.unsubscribe()
      this.unsubscribe = null
    }
    this.listeners = []
  }

  /**
   * 添加状态监听器
   */
  addListener(callback: UpdateListener): () => void {
    this.listeners.push(callback)
    // 返回取消订阅函数
    return () => {
      this.listeners = this.listeners.filter(cb => cb !== callback)
    }
  }

  /**
   * 通知所有监听器
   */
  private notifyListeners(status: UpdateStatus): void {
    this.listeners.forEach(callback => {
      try {
        callback(status)
      } catch (error) {
        console.error('[UpdateService] Listener error:', error)
      }
    })
  }

  /**
   * 获取当前状态
   */
  getStatus(): UpdateStatus {
    return { ...this.currentStatus }
  }

  /**
   * 检查更新
   */
  async check(): Promise<void> {
    if (typeof window !== 'undefined' && window.electronAPI?.updateCheck) {
      await window.electronAPI.updateCheck()
    }
  }

  /**
   * 下载更新
   */
  async download(): Promise<void> {
    if (typeof window !== 'undefined' && window.electronAPI?.updateDownload) {
      await window.electronAPI.updateDownload()
    }
  }

  /**
   * 安装更新并重启
   */
  async install(): Promise<void> {
    if (typeof window !== 'undefined' && window.electronAPI?.updateInstall) {
      await window.electronAPI.updateInstall()
    }
  }

  /**
   * 获取当前版本
   */
  async getVersion(): Promise<string> {
    if (typeof window !== 'undefined' && window.electronAPI?.updateGetVersion) {
      return await window.electronAPI.updateGetVersion()
    }
    return '1.0.0'
  }

  /**
   * 检查是否有待安装的更新
   */
  hasPendingUpdate(): boolean {
    return this.currentStatus.status === 'downloaded'
  }

  /**
   * 检查是否正在下载
   */
  isDownloading(): boolean {
    return this.currentStatus.status === 'downloading'
  }
}

// 导出单例
export const updateService = new UpdateService()

// 类型声明
declare global {
  interface Window {
    electronAPI: {
      getAppVersion: () => Promise<string>
      getOzonConfig: () => Promise<{ apiUrl: string; clientId: string }>
      openExternal: (url: string) => Promise<void>
      updateCheck: () => Promise<void>
      updateDownload: () => Promise<void>
      updateInstall: () => Promise<void>
      updateGetVersion: () => Promise<string>
      onUpdateStatus: (callback: (status: UpdateStatus) => void) => () => void
      platform: string
      isPackaged: boolean
    }
  }
}
