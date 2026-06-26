USE tcm_platform;

ALTER TABLE consultations
    ADD COLUMN department_id BIGINT NULL COMMENT '问诊科室ID' AFTER patient_account_id;

UPDATE consultations
SET department_id = (SELECT id FROM departments WHERE code = 'general')
WHERE department_id IS NULL;

ALTER TABLE consultations
    MODIFY COLUMN department_id BIGINT NOT NULL COMMENT '问诊科室ID',
    ADD INDEX idx_consultation_department (department_id),
    ADD CONSTRAINT fk_consultation_department
        FOREIGN KEY (department_id) REFERENCES departments(id);
