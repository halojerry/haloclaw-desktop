/**
 * MateClaw Desktop - 自动更新服务
 * 基于 electron-updater 实现
 */
import { autoUpdater } from 'electron-updater'
import { BrowserWindow } from 'electron'

// 日志
const log = (message: string, ...args: any[]) => {
  const timestamp = new Date().toISOString()
  console.log(`[AutoUpdater] [${timestamp}] ${message}`, ...args)
}

// 更新状态
export interface UpdateStatus {
  status: 'idle' | 'checking' | 'available' | 'downloading' | 'downloaded' | 'error'
  progress?: number
  version?: string
  releaseNotes?: string
  error?: string
}

// 更新事件回调
type UpdateCallback = (status: UpdateStatus) => void

class UpdateService {
  private mainWindow: BrowserWindow | null = null
  private checkInterval: NodeJS.Timeout | null = null
  private listeners: UpdateCallback[] = []
  private lastCheckTime: Date | null = null

  // 检查间隔（4小时）
  private readonly CHECK_INTERVAL_MS = 4 * 60 * 60 * 1000

  /**
   * 初始化更新服务
   */
  init(mainWindow: BrowserWindow): void {
    this.mainWindow = mainWindow
    this.setupAutoUpdater()
    this.setupEventHandlers()
    
    log('UpdateService initialized')
  }

  /**
   * 配置 autoUpdater
   */
  private setupAutoUpdater(): void {
    // 开发模式禁用自动下载
    autoUpdater.autoDownload = false
    autoUpdater.autoInstallOnAppQuit = true
    
    // 设置 logger
    autoUpdater.logger = {
      info: (message: any) => log('INFO:', message),
      warn: (message: any) => log('WARN:', message),
      error: (message: any) => log('ERROR:', message),
      debug: (message: any) => log('DEBUG:', message)
    }
  }

  /**
   * 设置事件处理器
   */
  private setupEventHandlers(): void {
    // 检查更新中
    autoUpdater.on('checking-for-update', () => {
      log('Checking for update...')
      this.notifyRenderers({
        status: 'checking'
      })
    })

    // 发现新版本
    autoUpdater.on('update-available', (info) => {
      log('Update available:', info.version)
      this.notifyRenderers({
        status: 'available',
        version: info.version,
        releaseNotes: typeof info.releaseNotes === 'string' 
          ? info.releaseNotes 
          : info.releaseNotes?.map(n => n.note).join('\n')
      })
    })

    // 没有可用更新
    autoUpdater.on('update-not-available', (info) => {
      log('No update available, current version:', info.version)
      this.lastCheckTime = new Date()
    })

    // 下载进度
    autoUpdater.on('download-progress', (progress) => {
      log(`Download progress: ${progress.percent.toFixed(1)}%`)
      this.notifyRenderers({
        status: 'downloading',
        progress: Math.round(progress.percent)
      })
    })

    // 下载完成
    autoUpdater.on('update-downloaded', (info) => {
      log('Update downloaded:', info.version)
      this.notifyRenderers({
        status: 'downloaded',
        version: info.version
      })
    })

    // 下载失败
    autoUpdater.on('error', (error) => {
      log('Update error:', error.message)
      this.notifyRenderers({
        status: 'error',
        error: error.message
      })
    })
  }

  /**
   * 通知渲染进程更新状态
   */
  private notifyRenderers(status: UpdateStatus): void {
    if (this.mainWindow && !this.mainWindow.isDestroyed()) {
      this.mainWindow.webContents.send('update-status', status)
    }
    
    // 通知所有监听器
    this.listeners.forEach(callback => callback(status))
  }

  /**
   * 启动定时检查
   */
  startScheduledCheck(): void {
    // 立即检查一次
    this.checkForUpdates()
    
    // 设置定时检查
    if (this.checkInterval) {
      clearInterval(this.checkInterval)
    }
    
    this.checkInterval = setInterval(() => {
      this.checkForUpdates()
    }, this.CHECK_INTERVAL_MS)
    
    log(`Scheduled check started, interval: ${this.CHECK_INTERVAL_MS / 1000 / 60 / 60}h`)
  }

  /**
   * 停止定时检查
   */
  stopScheduledCheck(): void {
    if (this.checkInterval) {
      clearInterval(this.checkInterval)
      this.checkInterval = null
      log('Scheduled check stopped')
    }
  }

  /**
   * 检查更新
   */
  async checkForUpdates(): Promise<void> {
    try {
      log('Starting update check...')
      await autoUpdater.checkForUpdates()
    } catch (error: any) {
      log('Check failed:', error.message)
      this.notifyRenderers({
        status: 'error',
        error: error.message
      })
    }
  }

  /**
   * 下载更新
   */
  async downloadUpdate(): Promise<void> {
    try {
      log('Starting download...')
      await autoUpdater.downloadUpdate()
    } catch (error: any) {
      log('Download failed:', error.message)
      this.notifyRenderers({
        status: 'error',
        error: error.message
      })
    }
  }

  /**
   * 安装更新并重启
   */
  installUpdate(): void {
    log('Installing update and restarting...')
    autoUpdater.quitAndInstall(false, true)
  }

  /**
   * 获取当前版本
   */
  getCurrentVersion(): string {
    return autoUpdater.currentVersion.version
  }

  /**
   * 添加状态监听器
   */
  addListener(callback: UpdateCallback): void {
    this.listeners.push(callback)
  }

  /**
   * 移除状态监听器
   */
  removeListener(callback: UpdateCallback): void {
    this.listeners = this.listeners.filter(cb => cb !== callback)
  }

  /**
   * 销毁服务
   */
  destroy(): void {
    this.stopScheduledCheck()
    this.listeners = []
    this.mainWindow = null
    log('UpdateService destroyed')
  }
}

// 导出单例
export const updateService = new UpdateService()
