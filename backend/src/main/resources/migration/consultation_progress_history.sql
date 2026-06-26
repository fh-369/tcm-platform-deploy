USE tcm_platform;

CREATE TABLE consultation_progress_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    consultation_id BIGINT NOT NULL COMMENT '问诊ID',
    doctor_id BIGINT NOT NULL COMMENT '操作医生ID',
    doctor_name VARCHAR(100) NOT NULL COMMENT '操作医生名称快照',
    previous_status VARCHAR(20) NOT NULL COMMENT '更新前状态',
    status VARCHAR(20) NOT NULL COMMENT '更新后状态',
    doctor_note TEXT COMMENT '本次医生回复',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    INDEX idx_progress_consultation (consultation_id),
    INDEX idx_progress_created_at (created_at),
    FOREIGN KEY (consultation_id) REFERENCES consultations(id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id) REFERENCES users(id)
);
