-- ============================================
-- TCM Platform Database Schema (MySQL 8.0)
-- 任务一：执行此脚本初始化数据库
-- ============================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS tcm_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE tcm_platform;

-- 全局登录账号表
CREATE TABLE accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '全局唯一用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希(BCrypt)',
    role ENUM('patient', 'doctor', 'admin') NOT NULL COMMENT '账号角色',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账号是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_account_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局登录账号表';

-- 科室主数据
CREATE TABLE departments (
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

-- 用户表 (医生/管理员)
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    account_id BIGINT NOT NULL UNIQUE COMMENT '全局账号ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希(BCrypt)',
    role ENUM('admin', 'doctor') NOT NULL DEFAULT 'doctor' COMMENT '角色',
    display_name VARCHAR(100) COMMENT '显示名称',
    department VARCHAR(100) COMMENT '科室',
    department_id BIGINT COMMENT '所属科室ID',
    phone VARCHAR(20) COMMENT '医生联系电话',
    qualification VARCHAR(500) COMMENT '资质或执业信息',
    profile VARCHAR(1000) COMMENT '医生简介',
    approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING'
        COMMENT '医生申请审核状态',
    approval_note VARCHAR(500) COMMENT '审核备注',
    approved_at DATETIME COMMENT '审核通过时间',
    approved_by BIGINT COMMENT '审核管理员用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_user_department (department_id),
    INDEX idx_user_approval_status (approval_status),
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (department_id) REFERENCES departments(id),
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 患者账号表
CREATE TABLE patient_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    account_id BIGINT NOT NULL UNIQUE COMMENT '全局账号ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希(BCrypt)',
    display_name VARCHAR(100) COMMENT '昵称',
    phone VARCHAR(20) COMMENT '手机号',
    avatar_url VARCHAR(500) COMMENT '头像地址',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    FOREIGN KEY (account_id) REFERENCES accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者账号表';

-- 问诊单表（核心业务表）
CREATE TABLE consultations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    patient_account_id BIGINT COMMENT '患者账号ID',
    department_id BIGINT NOT NULL COMMENT '问诊科室ID',
    patient_name VARCHAR(100) NOT NULL COMMENT '患者姓名',
    age INT COMMENT '年龄',
    gender ENUM('男', '女', '其他') COMMENT '性别',
    phone VARCHAR(20) COMMENT '手机号',
    symptoms TEXT NOT NULL COMMENT '症状描述',
    duration VARCHAR(100) COMMENT '持续时间',
    allergy_history TEXT COMMENT '过敏史',
    urgency ENUM('普通', '紧急', '非常紧急') NOT NULL DEFAULT '普通' COMMENT '紧急度',
    patient_note TEXT COMMENT '患者备注',
    reminder_level ENUM('normal', 'attention', 'urgent') DEFAULT 'normal' COMMENT '提醒等级',
    reminder_text TEXT COMMENT '提醒文本',
    status ENUM('待接诊', '接诊中', '已完成') NOT NULL DEFAULT '待接诊' COMMENT '状态',
    doctor_note TEXT COMMENT '医生备注',
    doctor_id BIGINT COMMENT '处理医生ID',
    assigned_at DATETIME COMMENT '最近一次分配或认领时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_patient_account (patient_account_id),
    INDEX idx_consultation_department (department_id),
    INDEX idx_status (status),
    INDEX idx_urgency (urgency),
    INDEX idx_doctor (doctor_id),
    INDEX idx_assigned_at (assigned_at),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (patient_account_id) REFERENCES patient_accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (department_id) REFERENCES departments(id),
    FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问诊单表';

-- 中医常识文章表
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

CREATE TABLE ai_conversations (
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

CREATE TABLE ai_messages (
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

CREATE TABLE ai_conversation_recommendations (
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

CREATE TABLE knowledge_articles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    category VARCHAR(50) COMMENT '分类',
    summary VARCHAR(500) COMMENT '摘要',
    content TEXT NOT NULL COMMENT '正文内容',
    tips TEXT COMMENT '小贴士',
    cover_image_url VARCHAR(500) COMMENT '封面图片URL',
    published TINYINT(1) DEFAULT 0 COMMENT '是否发布(0=草稿,1=发布)',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category),
    INDEX idx_published (published)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中医常识文章表';

-- 药膳表
CREATE TABLE recipes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    season ENUM('春', '夏', '秋', '冬', '四季') COMMENT '适用季节',
    constitution VARCHAR(50) COMMENT '适用体质',
    suitable_for VARCHAR(200) COMMENT '适宜人群',
    summary VARCHAR(500) COMMENT '简介',
    ingredients JSON COMMENT '食材清单',
    steps JSON COMMENT '制作步骤',
    image_url VARCHAR(500) COMMENT '配图URL',
    published TINYINT(1) DEFAULT 0 COMMENT '是否发布(0=草稿,1=发布)',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_season (season),
    INDEX idx_constitution (constitution),
    INDEX idx_published (published)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药膳表';

-- 上传记录表
CREATE TABLE uploads (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_name VARCHAR(255) NOT NULL COMMENT '存储文件名',
    mime_type VARCHAR(100) COMMENT 'MIME类型',
    file_size BIGINT COMMENT '文件大小(字节)',
    access_url VARCHAR(500) NOT NULL COMMENT '访问URL',
    uploaded_by BIGINT COMMENT '上传者ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    INDEX idx_stored_name (stored_name),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传记录表';

-- 初始化默认管理员和医生账号
INSERT INTO accounts (username, password_hash, role) VALUES
('admin', '$2a$10$dOmQRUxZF5udxjV651CP0ez4T0.iQNvC6BVZMZOCpY2WhZCscfWfO', 'admin'),
('doctor1', '$2a$10$oBczDgrrscIsIBdZTBfQlOLNNnUTDf1665Hawnxn.BAtVgZOaizxG', 'doctor');

INSERT INTO users (account_id, username, password_hash, role, display_name, approval_status, approved_at) VALUES
((SELECT id FROM accounts WHERE username = 'admin'), 'admin', '$2a$10$dOmQRUxZF5udxjV651CP0ez4T0.iQNvC6BVZMZOCpY2WhZCscfWfO', 'admin', '系统管理员', 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO users (
    account_id, username, password_hash, role, display_name,
    department, department_id, approval_status, approved_at
) VALUES (
    (SELECT id FROM accounts WHERE username = 'doctor1'),
    'doctor1',
    '$2a$10$oBczDgrrscIsIBdZTBfQlOLNNnUTDf1665Hawnxn.BAtVgZOaizxG',
    'doctor',
    '张医生',
    '中医内科',
    (SELECT id FROM departments WHERE code = 'internal-medicine'),
    'APPROVED',
    CURRENT_TIMESTAMP
);
