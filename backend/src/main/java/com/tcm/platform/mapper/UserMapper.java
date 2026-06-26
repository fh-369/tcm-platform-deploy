package com.tcm.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tcm.platform.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
            SELECT u.id
            FROM users u
            JOIN accounts a ON a.id = u.account_id
            LEFT JOIN consultations active_consultation
              ON active_consultation.doctor_id = u.id
             AND active_consultation.status IN ('待接诊', '接诊中')
            LEFT JOIN consultations assignment_history
              ON assignment_history.doctor_id = u.id
             AND assignment_history.assigned_at IS NOT NULL
            WHERE u.role = 'doctor'
              AND u.approval_status = 'APPROVED'
              AND u.department_id = #{departmentId}
              AND a.enabled = 1
            GROUP BY u.id
            ORDER BY COUNT(DISTINCT active_consultation.id) ASC,
                     CASE WHEN MAX(assignment_history.assigned_at) IS NULL THEN 0 ELSE 1 END ASC,
                     MAX(assignment_history.assigned_at) ASC,
                     u.id ASC
            LIMIT 1
            """)
    Long selectAutoAssignmentDoctorId(@Param("departmentId") Long departmentId);
}
