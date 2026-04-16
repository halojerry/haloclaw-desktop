package vip.mate.device;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备实体类
 * 
 * 用于管理客户端设备的注册、认证和状态追踪
 * 
 * @author MateClaw Team
 */
@Data
@TableName("mate_device")
public class Device {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 设备唯一标识（由客户端生成） */
    private String deviceId;

    /** 设备名称（可选，如"我的电脑-Windows"） */
    private String deviceName;

    /** 设备信息（操作系统、浏览器等） */
    private String deviceInfo;

    /** 设备类型（desktop/mobile/web） */
    private String deviceType;

    /** 所属用户ID */
    private Long userId;

    /** 所属用户名 */
    private String username;

    /** 设备状态（online/offline/banned） */
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

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
