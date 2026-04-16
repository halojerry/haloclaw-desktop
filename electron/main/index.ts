/**
 * MateClaw Desktop - Electron主进程
 * Ozon-Claw客户端 - 禁用DevTools版本
 */
import { app, BrowserWindow, Menu, Tray, globalShortcut, ipcMain, dialog, shell, nativeImage } from 'electron'
import { autoUpdater } from 'electron-updater'
import { spawn, ChildProcess } from 'child_process'
import * as path from 'path'
import * as fs from 'fs'
import { updateService } from './updateService'

// 日志记录
const log = (message: string, ...args: any[]) => {
  const timestamp = new Date().toISOString()
  console.log(`[${timestamp}] ${message}`, ...args)
}

// 阻止多实例
const gotTheLock = app.requestSingleInstanceLock()
if (!gotTheLock) {
  app.quit()
}

let mainWindow: BrowserWindow | null = null
let tray: Tray | null = null
let springBootProcess: ChildProcess | null = null
const isDev = process.env.NODE_ENV === 'development' || !app.isPackaged

// Spring Boot后端配置
const BACKEND_CONFIG = {
  jarPath: path.join(process.resourcesPath || '', 'resources', 'app.jar'),
  port: 8080,
  healthCheckUrl: 'http://localhost:8080/actuator/health'
}

/**
 * 禁用DevTools的配置选项
 */
const DISABLED_DEVTOOLS_WEB_PREFERENCES: Electron.WebPreferences = {
  nodeIntegration: false,
  contextIsolation: true,
  sandbox: true,
  webSecurity: true,
  allowRunningInsecureContent: false,
  // 关键：禁用开发者工具
  devTools: false
}

/**
 * 创建主窗口
 */
function createWindow(): void {
  log('Creating main window with DevTools disabled...')

  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 1024,
    minHeight: 700,
    title: 'MateClaw Desktop',
    icon: path.join(__dirname, '../../build/icon.png'),
    webPreferences: {
      ...DISABLED_DEVTOOLS_WEB_PREFERENCES,
      preload: path.join(__dirname, '../preload/index.js')
    },
    show: false
  })

  // 窗口准备就绪后显示
  mainWindow.once('ready-to-show', () => {
    mainWindow?.show()
    log('Main window shown')
  })

  // 加载URL
  if (isDev) {
    mainWindow.loadURL('http://localhost:5173')
  } else {
    mainWindow.loadFile(path.join(__dirname, '../../dist/index.html'))
  }

  // ====== 禁用DevTools的核心防护 ======

  // 1. 拦截快捷键 (F12, Ctrl+Shift+I, Ctrl+Shift+J, Ctrl+U)
  mainWindow.webContents.on('before-input-event', (event, input) => {
    const ctrl = input.control
    const shift = input.shift
    const alt = input.alt
    const key = input.key

    // 禁用所有DevTools相关快捷键
    if (
      key === 'F12' ||
      key === 'F10' ||
      (ctrl && shift && (key === 'I' || key === 'J' || key === 'C')) ||
      (ctrl && alt && key === 'I') ||
      (ctrl && key === 'u' && !alt)
    ) {
      event.preventDefault()
      log('Blocked DevTools shortcut:', { ctrl, shift, alt, key })
    }
  })

  // 2. 禁用右键菜单"检查元素"选项
  mainWindow.webContents.on('context-menu', (event, params) => {
    event.preventDefault()
    log('Context menu blocked')
  })

  // 3. 禁用导航到DevTools协议
  mainWindow.webContents.on('will-navigate', (event, url) => {
    if (url.includes('devtools://') || url.includes('chrome-devtools://')) {
      event.preventDefault()
      log('Blocked DevTools navigation')
    }
  })

  // 4. 监控webContents创建DevTools的尝试
  mainWindow.webContents.on('devtools-opened', () => {
    log('WARNING: DevTools was attempted to be opened!')
    mainWindow?.webContents.closeDevTools()
  })

  // ====== 窗口事件处理 ======

  mainWindow.on('closed', () => {
    mainWindow = null
  })

  // 最小化到托盘
  mainWindow.on('minimize', () => {
    if (process.platform === 'win32') {
      mainWindow?.hide()
    }
  })

  mainWindow.on('close', (event) => {
    if (!app.isQuitting) {
      event.preventDefault()
      mainWindow?.hide()
    }
  })

  // ====== 初始化自动更新服务 ======
  if (!isDev) {
    updateService.init(mainWindow)
    updateService.startScheduledCheck()
    log('Auto-update service started')
  }
}

/**
 * 创建系统托盘
 */
function createTray(): void {
  const iconPath = path.join(__dirname, '../../build/icon.png')
  
  // 如果图标不存在，使用默认图标
  let icon: nativeImage
  if (fs.existsSync(iconPath)) {
    icon = nativeImage.createFromPath(iconPath)
  } else {
    icon = nativeImage.createEmpty()
  }

  tray = new Tray(icon)
  tray.setToolTip('MateClaw Desktop')

  const contextMenu = Menu.buildFromTemplate([
    {
      label: '显示主界面',
      click: () => {
        mainWindow?.show()
      }
    },
    { type: 'separator' },
    {
      label: '检查更新',
      click: () => {
        if (!isDev) {
          updateService.checkForUpdates()
        }
      }
    },
    { type: 'separator' },
    {
      label: '退出',
      click: () => {
        app.isQuitting = true
        app.quit()
      }
    }
  ])

  tray.setContextMenu(contextMenu)

  tray.on('click', () => {
    mainWindow?.show()
  })
}

/**
 * 创建应用菜单
 */
function createMenu(): void {
  const template: Electron.MenuItemConstructorOptions[] = [
    {
      label: '文件',
      submenu: [
        {
          label: '检查更新',
          click: () => {
            if (!isDev) {
              updateService.checkForUpdates()
            }
          }
        },
        { type: 'separator' },
        {
          label: '退出',
          accelerator: process.platform === 'darwin' ? 'Cmd+Q' : 'Ctrl+Q',
          click: () => {
            app.isQuitting = true
            app.quit()
          }
        }
      ]
    },
    {
      label: '编辑',
      submenu: [
        { role: 'undo' },
        { role: 'redo' },
        { type: 'separator' },
        { role: 'cut' },
        { role: 'copy' },
        { role: 'paste' },
        { role: 'selectAll' }
      ]
    },
    {
      label: '视图',
      submenu: [
        { role: 'reload' },
        { role: 'forceReload' },
        { type: 'separator' },
        { role: 'resetZoom' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { type: 'separator' },
        { role: 'togglefullscreen' }
      ]
    },
    {
      label: '窗口',
      submenu: [
        { role: 'minimize' },
        { role: 'close' }
      ]
    },
    {
      label: '帮助',
      submenu: [
        {
          label: '关于',
          click: () => {
            dialog.showMessageBox({
              type: 'info',
              title: '关于 MateClaw Desktop',
              message: 'MateClaw Desktop',
              detail: `版本: ${app.getVersion()}\nOzon-Claw 桌面客户端`
            })
          }
        }
      ]
    }
  ]

  const menu = Menu.buildFromTemplate(template)
  Menu.setApplicationMenu(menu)
}

/**
 * 注册全局快捷键
 */
function registerGlobalShortcuts(): void {
  // 最小化到托盘
  globalShortcut.register('CommandOrControl+Shift+M', () => {
    if (mainWindow?.isVisible()) {
      mainWindow.hide()
    } else {
      mainWindow?.show()
    }
  })
}

/**
 * 启动Spring Boot后端
 */
function startBackend(): void {
  if (!fs.existsSync(BACKEND_CONFIG.jarPath)) {
    log('Backend JAR not found, skipping backend start')
    return
  }

  log('Starting backend service...')
  
  springBootProcess = spawn('java', ['-jar', BACKEND_CONFIG.jarPath], {
    cwd: path.dirname(BACKEND_CONFIG.jarPath),
    detached: true,
    stdio: 'ignore'
  })

  springBootProcess.unref()

  // 健康检查
  setTimeout(() => {
    checkBackendHealth()
  }, 5000)
}

/**
 * 检查后端健康状态
 */
async function checkBackendHealth(): Promise<void> {
  try {
    const response = await fetch(BACKEND_CONFIG.healthCheckUrl)
    if (response.ok) {
      log('Backend service is healthy')
    } else {
      log('Backend service returned non-OK status')
    }
  } catch (error) {
    log('Backend health check failed:', error)
  }
}

/**
 * 设置IPC处理器
 */
function setupIpcHandlers(): void {
  // 获取应用版本
  ipcMain.handle('get-app-version', () => {
    return app.getVersion()
  })

  // 获取Ozon配置
  ipcMain.handle('get-ozon-config', async () => {
    const configPath = path.join(app.getPath('userData'), 'config.json')
    try {
      if (fs.existsSync(configPath)) {
        const config = JSON.parse(fs.readFileSync(configPath, 'utf-8'))
        return {
          apiUrl: config.apiUrl || 'https://api-seller.ozon.ru',
          clientId: config.clientId || ''
        }
      }
    } catch (error) {
      log('Failed to read config:', error)
    }
    return {
      apiUrl: 'https://api-seller.ozon.ru',
      clientId: ''
    }
  })

  // 打开外部链接
  ipcMain.handle('open-external', async (event, url: string) => {
    await shell.openExternal(url)
  })

  // 更新相关IPC
  ipcMain.handle('update-check', async () => {
    if (!isDev) {
      await updateService.checkForUpdates()
    }
  })

  ipcMain.handle('update-download', async () => {
    if (!isDev) {
      await updateService.downloadUpdate()
    }
  })

  ipcMain.handle('update-install', async () => {
    if (!isDev) {
      updateService.installUpdate()
    }
  })

  ipcMain.handle('update-get-version', () => {
    return updateService.getCurrentVersion()
  })
}

// 扩展 app 类型
declare module 'electron' {
  interface App {
    isQuitting: boolean
  }
}
app.isQuitting = false

// 应用就绪
app.whenReady().then(() => {
  log('App is ready, starting initialization...')
  
  createWindow()
  createMenu()
  createTray()
  registerGlobalShortcuts()
  setupIpcHandlers()
  startBackend()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
    } else {
      mainWindow?.show()
    }
  })
})

// 窗口全部关闭
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

// 应用退出前
app.on('before-quit', () => {
  log('App is quitting...')
  app.isQuitting = true
  
  // 停止更新定时器
  if (!isDev) {
    updateService.destroy()
  }
  
  // 终止后端进程
  if (springBootProcess) {
    springBootProcess.kill()
  }
})

// 退出时注销全局快捷键
app.on('will-quit', () => {
  globalShortcut.unregisterAll()
})

log('Main process initialized')
