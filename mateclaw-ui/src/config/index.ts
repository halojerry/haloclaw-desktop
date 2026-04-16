/**
 * 配置模块导出
 * 
 * 提供统一的配置访问入口
 */

export {
  // API 配置
  API_BASE_URL,
  NEW_API_BASE_URL,
  TENANT_ID,
  
  // 设备ID管理
  DeviceIdManager,
  
  // 模型配置
  AVAILABLE_MODELS,
  MODEL_IDS,
  DEFAULT_MODEL,
  
  // Ozon 配置
  OZON_CONFIG,
  
  // New API
  getNewApiKey,
  
  // 版本信息
  CLIENT_VERSION,
  
  // 完整配置对象
  BUSINESS_CONFIG,
} from './business'
