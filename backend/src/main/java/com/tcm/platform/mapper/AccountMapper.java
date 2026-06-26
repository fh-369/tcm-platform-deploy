package com.tcm.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tcm.platform.dto.PersonnelRecord;
import com.tcm.platform.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    @Select("SELECT COUNT(*) FROM accounts WHERE role = 'patient'")
    long countPatientAccounts();

    @Select("""
            SELECT COUNT(*)
            FROM accounts a
            JOIN users u ON u.account_id = a.id
            WHERE a.role = 'doctor'
              AND a.enabled = 1
              AND u.approval_status = 'APPROVED'
            """)
    long countEnabledApprovedDoctors();

    @Select("""
            <script>
            SELECT a.id,
                   a.username,
                   a.role,
                   p.display_name AS displayName,
                   p.phone,
                   NULL AS department,
                   a.enabled,
                   a.created_at AS createdAt
            FROM accounts a
            JOIN patient_accounts p ON p.account_id = a.id
            WHERE a.role = 'patient'
            <if test="keyword != null and keyword != ''">
              AND (
                a.username LIKE CONCAT('%', #{keyword}, '%')
                OR p.display_name LIKE CONCAT('%', #{keyword}, '%')
                OR p.phone LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            ORDER BY a.created_at DESC, a.id DESC
            </script>
            """)
    IPage<PersonnelRecord> selectPatientPersonnel(
            Page<PersonnelRecord> page,
            @Param("keyword") String keyword
    );

    @Select("""
            <script>
            SELECT a.id,
                   u.id AS userId,
                   a.username,
                   a.role,
                   u.display_name AS displayName,
                   u.phone,
                   COALESCE(d.name, u.department) AS department,
                   u.department_id AS departmentId,
                   u.qualification,
                   u.profile,
                   u.approval_status AS approvalStatus,
                   u.approval_note AS approvalNote,
                   a.enabled,
                   a.created_at AS createdAt
            FROM accounts a
            JOIN users u ON u.account_id = a.id
            LEFT JOIN departments d ON d.id = u.department_id
            WHERE a.role = 'doctor'
            <if test="approvalStatus != null and approvalStatus != ''">
              AND u.approval_status = #{approvalStatus}
            </if>
            <if test="keyword != null and keyword != ''">
              AND (
                a.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                OR COALESCE(d.name, u.department) LIKE CONCAT('%', #{keyword}, '%')
                OR u.phone LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            ORDER BY a.created_at DESC, a.id DESC
            </script>
            """)
    IPage<PersonnelRecord> selectDoctorPersonnel(
            Page<PersonnelRecord> page,
            @Param("keyword") String keyword,
            @Param("approvalStatus") String approvalStatus
    );
}
