USE tcm_platform;

ALTER TABLE consultations
    ADD COLUMN assigned_at DATETIME NULL COMMENT '最近一次分配或认领时间' AFTER doctor_id,
    ADD INDEX idx_assigned_at (assigned_at);
