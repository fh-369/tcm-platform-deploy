USE tcm_platform;

CREATE TABLE IF NOT EXISTS ai_conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'AI 对话ID',
    patient_account_id BIGINT NOT NULL COMMENT '所属患者账号ID',
    consultation_id BIGINT COMMENT '首次对话选择的问诊单ID',
    title VARCHAR(100) NOT NULL COMMENT '对话标题',
    recommendation_initialized TINYINT(1) NOT NULL DEFAULT 0
        COMMENT '是否已生成首次站内推荐快照',
    history_summary TEXT COMMENT '较早对话摘要，当前版本仅预留',
    legacy_key VARCHAR(100) COMMENT '旧浏览器会话迁移幂等标识',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        COMMENT '更新时间',
    INDEX idx_ai_conversation_patient_updated (patient_account_id, updated_at),
    INDEX idx_ai_conversation_consultation (consultation_id),
    UNIQUE KEY uk_ai_conversation_legacy (
        patient_account_id,
        legacy_key
    ),
    FOREIGN KEY (patient_account_id)
        REFERENCES patient_accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (consultation_id)
        REFERENCES consultations(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者 AI 对话';

CREATE TABLE IF NOT EXISTS ai_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'AI 消息ID',
    conversation_id BIGINT NOT NULL COMMENT '所属对话ID',
    role VARCHAR(20) NOT NULL COMMENT '消息角色：user/assistant',
    content TEXT NOT NULL COMMENT '消息内容',
    fallback TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为降级回答',
    disclaimer VARCHAR(500) COMMENT '回答提示',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_ai_message_conversation_created (
        conversation_id,
        created_at,
        id
    ),
    FOREIGN KEY (conversation_id)
        REFERENCES ai_conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话消息';

CREATE TABLE IF NOT EXISTS ai_conversation_recommendations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐快照ID',
    conversation_id BIGINT NOT NULL COMMENT '所属对话ID',
    content_type VARCHAR(20) NOT NULL COMMENT '内容类型',
    content_id BIGINT NOT NULL COMMENT '站内内容ID',
    title VARCHAR(200) NOT NULL COMMENT '推荐标题快照',
    description VARCHAR(500) COMMENT '推荐摘要快照',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
    UNIQUE KEY uk_ai_recommendation_content (
        conversation_id,
        content_type,
        content_id
    ),
    INDEX idx_ai_recommendation_conversation (
        conversation_id,
        sort_order
    ),
    FOREIGN KEY (conversation_id)
        REFERENCES ai_conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话首次站内推荐快照';
