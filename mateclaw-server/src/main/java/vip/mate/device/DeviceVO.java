package vip.mate.device;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备视图对象
 * 
 * @author MateClaw Team
 */
@Data
public class DeviceVO {
    
    /** 设备ID */
    private String deviceId;
    
    /** 设备名称 */
    private String deviceName;
    
    /** 设备信息 */
    private String deviceInfo;
    
    /** 设备类型 */
    private String deviceType;
    
    /** 设备状态 */
    private String status;
    
    /** 最后活跃时间 */
    private LocalDateTime lastHeartbeat;
    
    /** 注册时间 */
    private LocalDateTime registeredAt;
    
    /** 是否为当前设备 */
    private Boolean isCurrent;
    
    /** IP地址 */
    private String ipAddress;
    
    /** 操作系统信息 */
    private String osInfo;
    
    /** 应用程序版本 */
    private String appVersion;
    
    /**
     * 从实体转换为VO
     */
    public static DeviceVO fromEntity(Device device) {
        DeviceVO vo = new DeviceVO();
        vo.setDeviceId(device.getDeviceId());
        vo.setDeviceName(device.getDeviceName());
        vo.setDeviceInfo(device.getDeviceInfo());
        vo.setDeviceType(device.getDeviceType());
        vo.setStatus(device.getStatus());
        vo.setLastHeartbeat(device.getLastHeartbeat());
        vo.setRegisteredAt(device.getRegisteredAt());
        vo.setIsCurrent(device.getIsCurrent());
        vo.setIpAddress(device.getIpAddress());
        vo.setOsInfo(device.getOsInfo());
        vo.setAppVersion(device.getAppVersion());
        return vo;
    }
}
