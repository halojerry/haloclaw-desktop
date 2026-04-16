/**
 * 设备管理 API
 */

import { http } from './index'

/**
 * 设备注册请求
 */
export interface DeviceRegisterRequest {
  deviceId: string
  deviceName?: string
  deviceInfo?: string
  deviceType?: string
}

/**
 * 设备认证响应
 */
export interface DeviceAuthResponse {
  success: boolean
  message: string
  sessionToken?: string
  userId?: number
  username?: string
  token?: string
  deviceId?: string
  isNewDevice?: boolean
  boundDevices?: DeviceInfo[]
  deviceLimit?: number
  currentDeviceCount?: number
  serverTime?: number
  nextHeartbeat?: number
}

/**
 * 设备信息
 */
export interface DeviceInfo {
  deviceId: string
  deviceName?: string
  deviceInfo?: string
  deviceType?: string
  status?: string
  lastHeartbeat?: string
  registeredAt?: string
  isCurrent?: boolean
  ipAddress?: string
  osInfo?: string
  appVersion?: string
}

/**
 * 版本信息
 */
export interface VersionInfo {
  clientVersion: string
  serverVersion: string
  serverTime: number
  features: string[]
}

// ==================== 设备 API ====================

export const deviceApi = {
  /**
   * 注册设备
   */
  register: (data: DeviceRegisterRequest) =>
    http.post<DeviceAuthResponse>('/device/register', data),

  /**
   * 设备认证
   */
  authenticate: (deviceId: string) =>
    http.post<DeviceAuthResponse>('/device/authenticate', null, {
      params: { deviceId }
    }),

  /**
   * 设备心跳
   */
  heartbeat: (deviceId: string) =>
    http.post<DeviceAuthResponse>('/device/heartbeat', null, {
      params: { deviceId }
    }),

  /**
   * 获取设备列表
   */
  list: () => http.get<DeviceInfo[]>('/device/list'),

  /**
   * 解绑设备
   */
  unbind: (deviceId: string) =>
    http.delete('/device/unbind', { params: { deviceId } }),

  /**
   * 获取设备信息
   */
  info: (deviceId: string) =>
    http.get<DeviceInfo>('/device/info', { params: { deviceId } }),

  /**
   * 获取版本信息
   */
  version: () => http.get<VersionInfo>('/device/version')
}
