USE tcm_platform;

UPDATE consultations c
LEFT JOIN users u ON u.id = c.doctor_id
SET c.doctor_id = NULL,
    c.status = CASE
        WHEN c.status = '已完成' THEN c.status
        ELSE '待接诊'
    END
WHERE c.doctor_id IS NOT NULL
  AND (u.id IS NULL OR u.role <> 'doctor');
