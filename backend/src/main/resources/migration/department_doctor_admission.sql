USE tcm_platform;

CREATE TABLE IF NOT EXISTS departments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '稳定科室编码',
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '科室名称',
    description VARCHAR(500) COMMENT '科室说明',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_department_enabled_sort (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室主数据';

INSERT INTO departments (code, name, description, sort_order) VALUES
('general', '综合咨询', '暂不确定具体科室时，由平台协助分诊。', 10),
('internal-medicine', '中医内科', '面向常见内科不适与日常调养需求。', 20),
('gynecology', '中医妇科', '面向女性生理周期与妇科相关调养需求。', 30),
('pediatrics', '中医儿科', '面向儿童常见不适与成长调养需求。', 40),
('acupuncture-tuina', '针灸推拿科', '面向经络、疼痛、运动劳损与推拿调理需求。', 50);

ALTER TABLE users
    ADD COLUMN department_id BIGINT NULL COMMENT '所属科室ID' AFTER department,
    ADD COLUMN phone VARCHAR(20) NULL COMMENT '医生联系电话' AFTER department_id,
    ADD COLUMN qualification VARCHAR(500) NULL COMMENT '资质或执业信息' AFTER phone,
    ADD COLUMN profile VARCHAR(1000) NULL COMMENT '医生简介' AFTER qualification,
    ADD COLUMN approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING'
        COMMENT '医生申请审核状态' AFTER profile,
    ADD COLUMN approval_note VARCHAR(500) NULL COMMENT '审核备注' AFTER approval_status,
    ADD COLUMN approved_at DATETIME NULL COMMENT '审核通过时间' AFTER approval_note,
    ADD COLUMN approved_by BIGINT NULL COMMENT '审核管理员用户ID' AFTER approved_at,
    ADD INDEX idx_user_department (department_id),
    ADD INDEX idx_user_approval_status (approval_status),
    ADD CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES departments(id),
    ADD CONSTRAINT fk_user_approved_by FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL;

UPDATE users
SET department_id = (SELECT id FROM departments WHERE code = 'internal-medicine'),
    department = '中医内科'
WHERE role = 'doctor'
  AND department IN ('内科', '中医内科');

UPDATE users
SET department_id = (
        SELECT d.id
        FROM departments d
        WHERE d.name = users.department
        LIMIT 1
    )
WHERE role = 'doctor'
  AND department IS NOT NULL;

UPDATE users
SET department_id = (SELECT id FROM departments WHERE code = 'general'),
    department = '综合咨询'
WHERE role = 'doctor'
  AND department_id IS NULL;

UPDATE users
SET approval_status = 'APPROVED',
    approved_at = COALESCE(approved_at, created_at)
WHERE role IN ('doctor', 'admin');
