/**
 * MateClaw Desktop - IPC预加载脚本
 * 安全桥接主进程和渲染进程
 */
import { contextBridge, ipcRenderer } from 'electron'

/**
 * 更新状态类型
 */
export interface UpdateStatus {
  status: 'idle' | 'checking' | 'available' | 'downloading' | 'downloaded' | 'error'
  progress?: number
  version?: string
  releaseNotes?: string
  error?: string
}

/**
 * 暴露给渲染进程的API
 */
contextBridge.exposeInMainWorld('electronAPI', {
  // 应用信息
  getAppVersion: (): Promise<string> => ipcRenderer.invoke('get-app-version'),

  // Ozon配置
  getOzonConfig: (): Promise<{
    apiUrl: string
    clientId: string
  }> => ipcRenderer.invoke('get-ozon-config'),

  // 打开外部链接
  openExternal: (url: string): Promise<void> => ipcRenderer.invoke('open-external', url),

  // ====== 自动更新相关 ======
  
  // 检查更新
  updateCheck: (): Promise<void> => ipcRenderer.invoke('update-check'),
  
  // 下载更新
  updateDownload: (): Promise<void> => ipcRenderer.invoke('update-download'),
  
  // 安装更新
  updateInstall: (): Promise<void> => ipcRenderer.invoke('update-install'),
  
  // 获取当前版本
  updateGetVersion: (): Promise<string> => ipcRenderer.invoke('update-get-version'),
  
  // 监听更新状态变化
  onUpdateStatus: (callback: (status: UpdateStatus) => void): () => void => {
    const handler = (_event: Electron.IpcRendererEvent, status: UpdateStatus) => {
      callback(status)
    }
    ipcRenderer.on('update-status', handler)
    // 返回取消订阅函数
    return () => {
      ipcRenderer.removeListener('update-status', handler)
    }
  },

  // 平台信息
  platform: process.platform,

  // 环境信息
  isPackaged: process.env.NODE_ENV !== 'development'
})

/**
 * 类型声明（供TypeScript使用）
 */
export interface ElectronAPI {
  getAppVersion: () => Promise<string>
  getOzonConfig: () => Promise<{
    apiUrl: string
    clientId: string
  }>
  openExternal: (url: string) => Promise<void>
  
  // 自动更新
  updateCheck: () => Promise<void>
  updateDownload: () => Promise<void>
  updateInstall: () => Promise<void>
  updateGetVersion: () => Promise<string>
  onUpdateStatus: (callback: (status: UpdateStatus) => void) => () => void
  
  // 环境
  platform: NodeJS.Platform
  isPackaged: boolean
}

declare global {
  interface Window {
    electronAPI: ElectronAPI
  }
}
