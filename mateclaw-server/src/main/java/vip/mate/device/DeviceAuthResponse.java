package vip.mate.device;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 设备认证响应
 * 
 * @author MateClaw Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAuthResponse {
    
    /** 认证是否成功 */
    private Boolean success;
    
    /** 认证结果消息 */
    private String message;
    
    /** 会话Token */
    private String sessionToken;
    
    /** 用户ID */
    private Long userId;
    
    /** 用户名 */
    private String username;
    
    /** JWT Token */
    private String token;
    
    /** 设备ID */
    private String deviceId;
    
    /** 是否新设备（需要绑定确认） */
    private Boolean isNewDevice;
    
    /** 当前用户绑定的设备列表 */
    private List<DeviceVO> boundDevices;
    
    /** 设备数量限制 */
    private Integer deviceLimit;
    
    /** 当前设备数量 */
    private Integer currentDeviceCount;
    
    /** 服务器时间 */
    private Long serverTime;
    
    /** 下一次心跳时间 */
    private Long nextHeartbeat;
}
