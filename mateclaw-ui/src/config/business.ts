/**
 * Ozon-Claw 客户端业务配置
 * 
 * 本文件包含写死的业务配置，用于 Ozon-Claw 桌面客户端
 * 配置在编译时确定，不依赖运行时环境变量
 */

/**
 * API 服务地址
 * 写死为 Ozon-Claw 专属 API 端点
 */
export const API_BASE_URL = 'https://api.ozon-claw.com'

/**
 * 租户标识
 * 用于多租户环境隔离
 */
export const TENANT_ID = 'ozon-claw-v1'

/**
 * 设备ID管理
 * 首次运行时生成并持久化到本地存储
 */
export const DeviceIdManager = {
  STORAGE_KEY: 'ozon-claw-device-id',

  /**
   * 获取当前设备ID
   * 若不存在则生成新的UUID并存储
   */
  getDeviceId(): string {
    let deviceId = localStorage.getItem(this.STORAGE_KEY)
    if (!deviceId) {
      deviceId = this.generateDeviceId()
      localStorage.setItem(this.STORAGE_KEY, deviceId)
    }
    return deviceId
  },

  /**
   * 生成设备ID
   * 格式：ozon-claw-{timestamp}-{random}
   */
  generateDeviceId(): string {
    const timestamp = Date.now().toString(36)
    const random = Math.random().toString(36).substring(2, 10)
    return `ozon-claw-${timestamp}-${random}`
  },

  /**
   * 重置设备ID（用于设备绑定变更等场景）
   */
  resetDeviceId(): string {
    const newId = this.generateDeviceId()
    localStorage.setItem(this.STORAGE_KEY, newId)
    return newId
  },
}

/**
 * 可用模型列表
 * 固定列表，与后端协商确定
 */
export const AVAILABLE_MODELS = [
  // 通义千问系列
  { id: 'qwen-max', name: 'Qwen Max', provider: 'dashscope', type: 'chat' },
  { id: 'qwen-plus', name: 'Qwen Plus', provider: 'dashscope', type: 'chat' },
  { id: 'qwen-turbo', name: 'Qwen Turbo', provider: 'dashscope', type: 'chat' },
  { id: 'qwen-max-longcontext', name: 'Qwen Max 长文本', provider: 'dashscope', type: 'chat' },
  
  // OpenAI 系列
  { id: 'gpt-4o', name: 'GPT-4o', provider: 'openai', type: 'chat' },
  { id: 'gpt-4o-mini', name: 'GPT-4o Mini', provider: 'openai', type: 'chat' },
  { id: 'gpt-4-turbo', name: 'GPT-4 Turbo', provider: 'openai', type: 'chat' },
  
  // Claude 系列
  { id: 'claude-3-5-sonnet', name: 'Claude 3.5 Sonnet', provider: 'anthropic', type: 'chat' },
  { id: 'claude-3-opus', name: 'Claude 3 Opus', provider: 'anthropic', type: 'chat' },
  
  // 图像模型
  { id: 'wanx-plus', name: '通义万相 Plus', provider: 'dashscope', type: 'image' },
  { id: 'dall-e-3', name: 'DALL-E 3', provider: 'openai', type: 'image' },
] as const

/**
 * 模型ID列表（方便快速判断）
 */
export const MODEL_IDS = AVAILABLE_MODELS.map(m => m.id)

/**
 * 默认模型
 */
export const DEFAULT_MODEL = 'qwen-max'

/**
 * New API 服务地址
 * 用于配额管理和计费
 */
export const NEW_API_BASE_URL = 'https://new-api.ozon-claw.com'

/**
 * New API Key（从本地存储获取，由服务端下发）
 */
export const getNewApiKey = (): string | null => {
  return localStorage.getItem('new-api-key')
}

/**
 * 客户端版本信息
 */
export const CLIENT_VERSION = {
  version: '1.0.0',
  build: 'ozon-claw-desktop',
  channel: 'production',
}

/**
 * Ozon 平台配置
 */
export const OZON_CONFIG = {
  // Ozon Seller API 配置
  sellerApiBase: 'https://api-seller.ozon.ru',
  sellerApiVersion: 'v3',
  
  // Ozon Web 配置
  webBase: 'https://www.ozon.ru',
  
  // 常用端点
  endpoints: {
    productImport: '/v3/product/import',
    productInfo: '/v3/product/info/list',
    categoryTree: '/v1/description-category/tree',
    categoryAttribute: '/v3/category/attribute',
    productPictures: '/v1/product/pictures/import',
  },
  
  // API 限制配置
  rateLimits: {
    maxRequestsPerSecond: 10,
    maxRequestsPerMinute: 100,
    retryDelayMs: 5000,
    maxRetries: 3,
  },
}

/**
 * 导出完整配置对象（便于统一引用）
 */
export const BUSINESS_CONFIG = {
  api: {
    baseUrl: API_BASE_URL,
    newApiBaseUrl: NEW_API_BASE_URL,
    tenantId: TENANT_ID,
  },
  device: {
    getId: () => DeviceIdManager.getDeviceId(),
    reset: () => DeviceIdManager.resetDeviceId(),
  },
  models: {
    list: AVAILABLE_MODELS,
    ids: MODEL_IDS,
    default: DEFAULT_MODEL,
  },
  ozon: OZON_CONFIG,
  version: CLIENT_VERSION,
} as const
