-- Skills 技能执行记录表
CREATE TABLE IF NOT EXISTS mate_skill_execution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL COMMENT '任务ID',
    skill_type VARCHAR(64) NOT NULL COMMENT '技能类型',
    skill_name VARCHAR(128) NOT NULL COMMENT '技能名称',
    user_id BIGINT COMMENT '用户ID',
    store_id BIGINT COMMENT '店铺ID',
    status VARCHAR(32) NOT NULL COMMENT '执行状态: RUNNING/SUCCESS/FAILED/PARTIAL_SUCCESS',
    request_params TEXT COMMENT '请求参数(JSON)',
    result_data TEXT COMMENT '结果数据(JSON)',
    error_message TEXT COMMENT '错误信息',
    duration_ms BIGINT COMMENT '执行耗时(毫秒)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    finished_at TIMESTAMP COMMENT '完成时间',
    INDEX idx_task_id (task_id),
    INDEX idx_skill_type (skill_type),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能执行记录表';

-- Skills 技能执行步骤表
CREATE TABLE IF NOT EXISTS mate_skill_execution_step (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id BIGINT NOT NULL COMMENT '执行记录ID',
    step_name VARCHAR(64) NOT NULL COMMENT '步骤名称',
    step_description VARCHAR(256) COMMENT '步骤描述',
    success BOOLEAN DEFAULT TRUE COMMENT '是否成功',
    step_data TEXT COMMENT '步骤数据(JSON)',
    started_at TIMESTAMP COMMENT '开始时间',
    finished_at TIMESTAMP COMMENT '结束时间',
    INDEX idx_execution_id (execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能执行步骤表';

-- Skills 用户反馈表
CREATE TABLE IF NOT EXISTS mate_skill_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id BIGINT NOT NULL COMMENT '执行记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT COMMENT '商品ID',
    feedback_type VARCHAR(32) NOT NULL COMMENT '反馈类型: accept/reject/modify',
    feedback_content TEXT COMMENT '反馈内容',
    score INT COMMENT '评分(1-5)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_execution_id (execution_id),
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能用户反馈表';
