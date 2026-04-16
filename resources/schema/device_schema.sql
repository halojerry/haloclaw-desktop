-- 设备管理表
-- 用于存储客户端设备注册和认证信息

CREATE TABLE IF NOT EXISTS mate_device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    device_id VARCHAR(64) NOT NULL COMMENT '设备唯一标识',
    device_name VARCHAR(128) COMMENT '设备名称',
    device_info VARCHAR(512) COMMENT '设备信息',
    device_type VARCHAR(32) DEFAULT 'desktop' COMMENT '设备类型: desktop/mobile/web',
    user_id BIGINT COMMENT '所属用户ID',
    username VARCHAR(64) COMMENT '所属用户名',
    status VARCHAR(16) DEFAULT 'online' COMMENT '设备状态: online/offline/banned/unbound',
    last_heartbeat DATETIME COMMENT '最后活跃时间',
    registered_at DATETIME COMMENT '注册时间',
    is_current TINYINT(1) DEFAULT 0 COMMENT '是否为当前设备',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    os_info VARCHAR(128) COMMENT '操作系统信息',
    app_version VARCHAR(32) COMMENT '应用程序版本',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除标记',
    
    UNIQUE KEY uk_device_id (device_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_last_heartbeat (last_heartbeat)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备管理表';
