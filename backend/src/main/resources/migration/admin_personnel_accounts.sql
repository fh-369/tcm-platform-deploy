USE tcm_platform;

ALTER TABLE accounts
    ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1
    COMMENT '账号是否启用'
    AFTER role;

CREATE INDEX idx_account_enabled ON accounts (enabled);
