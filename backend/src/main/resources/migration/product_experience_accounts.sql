USE tcm_platform;

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '全局唯一用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希(BCrypt)',
    role ENUM('patient', 'doctor', 'admin') NOT NULL COMMENT '账号角色',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_account_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局登录账号表';

ALTER TABLE users ADD COLUMN account_id BIGINT NULL AFTER id;
ALTER TABLE patient_accounts ADD COLUMN account_id BIGINT NULL AFTER id;

INSERT IGNORE INTO accounts (username, password_hash, role, created_at, updated_at)
SELECT username, password_hash, role, created_at, updated_at FROM users;

INSERT IGNORE INTO accounts (username, password_hash, role, created_at, updated_at)
SELECT username, password_hash, 'patient', created_at, updated_at FROM patient_accounts;

UPDATE users u
JOIN accounts a ON a.username = u.username
SET u.account_id = a.id
WHERE u.account_id IS NULL;

UPDATE patient_accounts p
JOIN accounts a ON a.username = p.username
SET p.account_id = a.id
WHERE p.account_id IS NULL;

CREATE INDEX idx_user_account ON users (account_id);
CREATE INDEX idx_patient_account_login ON patient_accounts (account_id);
