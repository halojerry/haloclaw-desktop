package vip.mate.device;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备注册请求
 * 
 * @author MateClaw Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegisterRequest {
    
    /** 设备唯一标识 */
    private String deviceId;
    
    /** 设备名称 */
    private String deviceName;
    
    /** 设备信息（操作系统、浏览器等） */
    private String deviceInfo;
    
    /** 设备类型 */
    private String deviceType;
}
