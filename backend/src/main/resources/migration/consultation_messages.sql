USE tcm_platform;

CREATE TABLE consultation_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    consultation_id BIGINT NOT NULL COMMENT '问诊ID',
    sender_type VARCHAR(20) NOT NULL COMMENT '发送者类型：patient/doctor',
    sender_id BIGINT NOT NULL COMMENT '患者档案ID或医生用户ID',
    sender_name VARCHAR(100) NOT NULL COMMENT '发送者名称快照',
    content TEXT NOT NULL COMMENT '沟通内容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_message_consultation (consultation_id),
    INDEX idx_message_created_at (created_at),
    FOREIGN KEY (consultation_id) REFERENCES consultations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问诊医患沟通记录表';

INSERT INTO consultation_messages (
    consultation_id,
    sender_type,
    sender_id,
    sender_name,
    content,
    created_at
)
SELECT consultation_id,
       'doctor',
       doctor_id,
       doctor_name,
       doctor_note,
       created_at
FROM consultation_progress_records
WHERE doctor_note IS NOT NULL
  AND TRIM(doctor_note) <> '';

