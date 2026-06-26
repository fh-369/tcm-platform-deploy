package com.tcm.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tcm.platform.entity.Consultation;
import com.tcm.platform.entity.ConsultationProgressRecord;
import com.tcm.platform.dto.ConsultationExportFilter;
import com.tcm.platform.dto.ConsultationExportRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ConsultationMapper extends BaseMapper<Consultation> {

    @Insert("""
            INSERT INTO consultation_progress_records (
                consultation_id,
                doctor_id,
                doctor_name,
                previous_status,
                status,
                doctor_note
            ) VALUES (
                #{consultationId},
                #{doctorId},
                #{doctorName},
                #{previousStatus},
                #{status},
                #{doctorNote}
            )
            """)
    int insertProgressRecord(ConsultationProgressRecord record);

    @Select({
            "<script>",
            "SELECT id, consultation_id, doctor_id, doctor_name, previous_status, status,",
            "doctor_note, created_at",
            "FROM consultation_progress_records",
            "WHERE consultation_id IN",
            "<foreach collection='consultationIds' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "ORDER BY created_at ASC, id ASC",
            "</script>"
    })
    List<ConsultationProgressRecord> selectProgressRecords(
            @Param("consultationIds") List<Long> consultationIds
    );

    @Update("""
            UPDATE consultations
            SET doctor_id = #{doctorId},
                status = #{status},
                assigned_at = CASE WHEN #{doctorId} IS NULL THEN NULL ELSE CURRENT_TIMESTAMP END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{consultationId}
            """)
    int updateAssignment(
            @Param("consultationId") Long consultationId,
            @Param("doctorId") Long doctorId,
            @Param("status") String status
    );

    @Update("""
            UPDATE consultations
            SET doctor_id = #{doctorId},
                status = '待接诊',
                assigned_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{consultationId}
              AND doctor_id IS NULL
              AND status <> '已完成'
            """)
    int claimIfUnassigned(
            @Param("consultationId") Long consultationId,
            @Param("doctorId") Long doctorId
    );

    @Update("""
            UPDATE consultations
            SET department_id = #{departmentId},
                doctor_id = NULL,
                status = #{status},
                assigned_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{consultationId}
            """)
    int updateDepartmentAndClearAssignment(
            @Param("consultationId") Long consultationId,
            @Param("departmentId") Long departmentId,
            @Param("status") String status
    );

    /** 按状态统计问诊数量 */
    @Select("SELECT status, COUNT(*) AS count FROM consultations GROUP BY status")
    List<Map<String, Object>> countByStatus();
    
    /** 按紧急度统计问诊数量 */
    @Select("SELECT urgency, COUNT(*) AS count FROM consultations GROUP BY urgency")
    List<Map<String, Object>> countByUrgency();
    
    /** 近 6 个月问诊趋势 */
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS count " +
            "FROM consultations " +
            "WHERE created_at >= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 5 MONTH), '%Y-%m-01') " +
            "GROUP BY month ORDER BY month")
    List<Map<String, Object>> trendLast6Months();

    /** 近 7 天问诊趋势 */
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS period, COUNT(*) AS count " +
            "FROM consultations WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY period ORDER BY period")
    List<Map<String, Object>> trendLast7Days();

    /** 近 4 周问诊趋势，以每周周一作为周期标识 */
    @Select("SELECT DATE_FORMAT(DATE_SUB(DATE(created_at), INTERVAL WEEKDAY(created_at) DAY), '%Y-%m-%d') AS period, " +
            "COUNT(*) AS count FROM consultations " +
            "WHERE created_at >= DATE_SUB(" +
            "DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 3 WEEK) " +
            "GROUP BY period ORDER BY period")
    List<Map<String, Object>> trendLast4Weeks();

    /** 近 6 个月问诊趋势，统一使用 period 字段 */
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m') AS period, COUNT(*) AS count " +
            "FROM consultations " +
            "WHERE created_at >= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 5 MONTH), '%Y-%m-01') " +
            "GROUP BY period ORDER BY period")
    List<Map<String, Object>> trendLast6MonthsByPeriod();

    @Select("""
            SELECT status, COUNT(*) AS count
            FROM consultations
            WHERE doctor_id = #{doctorId}
            GROUP BY status
            """)
    List<Map<String, Object>> countByStatusForDoctor(@Param("doctorId") Long doctorId);

    @Select("""
            SELECT urgency, COUNT(*) AS count
            FROM consultations
            WHERE doctor_id = #{doctorId}
            GROUP BY urgency
            """)
    List<Map<String, Object>> countByUrgencyForDoctor(@Param("doctorId") Long doctorId);

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS count
            FROM consultations
            WHERE doctor_id = #{doctorId}
              AND created_at >= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 5 MONTH), '%Y-%m-01')
            GROUP BY month
            ORDER BY month
            """)
    List<Map<String, Object>> trendLast6MonthsForDoctor(@Param("doctorId") Long doctorId);

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS period, COUNT(*) AS count
            FROM consultations
            WHERE doctor_id = #{doctorId}
              AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
            GROUP BY period
            ORDER BY period
            """)
    List<Map<String, Object>> trendLast7DaysForDoctor(@Param("doctorId") Long doctorId);

    @Select("""
            SELECT DATE_FORMAT(
                       DATE_SUB(DATE(created_at), INTERVAL WEEKDAY(created_at) DAY),
                       '%Y-%m-%d'
                   ) AS period,
                   COUNT(*) AS count
            FROM consultations
            WHERE doctor_id = #{doctorId}
              AND created_at >= DATE_SUB(
                    DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY),
                    INTERVAL 3 WEEK
              )
            GROUP BY period
            ORDER BY period
            """)
    List<Map<String, Object>> trendLast4WeeksForDoctor(@Param("doctorId") Long doctorId);

    @Select("""
            SELECT DATE_FORMAT(created_at, '%Y-%m') AS period, COUNT(*) AS count
            FROM consultations
            WHERE doctor_id = #{doctorId}
              AND created_at >= DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 5 MONTH), '%Y-%m-01')
            GROUP BY period
            ORDER BY period
            """)
    List<Map<String, Object>> trendLast6MonthsByPeriodForDoctor(@Param("doctorId") Long doctorId);

    @Select("""
            SELECT COALESCE(d.name, '未分科') AS department, COUNT(*) AS count
            FROM consultations c
            LEFT JOIN departments d ON d.id = c.department_id
            GROUP BY c.department_id, d.name
            ORDER BY count DESC, department ASC
            """)
    List<Map<String, Object>> countByDepartment();

    @Select("""
            SELECT u.id AS doctorId,
                   COALESCE(u.display_name, u.username) AS doctorName,
                   COUNT(c.id) AS activeCount
            FROM users u
            JOIN accounts a ON a.id = u.account_id
            LEFT JOIN consultations c
              ON c.doctor_id = u.id
             AND c.status IN ('待接诊', '接诊中')
            WHERE u.role = 'doctor'
              AND u.approval_status = 'APPROVED'
              AND a.enabled = 1
            GROUP BY u.id, u.display_name, u.username
            ORDER BY activeCount DESC, doctorName ASC
            """)
    List<Map<String, Object>> countDoctorWorkloads();

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM consultations c",
            "WHERE 1 = 1",
            "<if test='filter.dateFrom != null'>",
            "AND c.created_at &gt;= #{filter.dateFrom}",
            "</if>",
            "<if test='filter.dateTo != null'>",
            "AND c.created_at &lt; DATE_ADD(#{filter.dateTo}, INTERVAL 1 DAY)",
            "</if>",
            "<if test='filter.status != null and filter.status != \"\"'>",
            "AND c.status = #{filter.status}",
            "</if>",
            "<if test='filter.urgency != null and filter.urgency != \"\"'>",
            "AND c.urgency = #{filter.urgency}",
            "</if>",
            "<if test='filter.departmentId != null'>",
            "AND c.department_id = #{filter.departmentId}",
            "</if>",
            "<if test='filter.doctorId != null'>",
            "AND c.doctor_id = #{filter.doctorId}",
            "</if>",
            "</script>"
    })
    long countConsultationExports(@Param("filter") ConsultationExportFilter filter);

    @Select({
            "<script>",
            "SELECT c.id,",
            "c.patient_name AS patientName,",
            "c.age,",
            "c.gender,",
            "c.phone,",
            "c.symptoms,",
            "c.duration,",
            "c.allergy_history AS allergyHistory,",
            "c.urgency,",
            "c.status,",
            "COALESCE(d.name, '未分科') AS departmentName,",
            "COALESCE(u.display_name, u.username, '未分配') AS doctorName,",
            "c.doctor_note AS doctorNote,",
            "c.assigned_at AS assignedAt,",
            "c.created_at AS createdAt,",
            "c.updated_at AS updatedAt",
            "FROM consultations c",
            "LEFT JOIN departments d ON d.id = c.department_id",
            "LEFT JOIN users u ON u.id = c.doctor_id",
            "WHERE 1 = 1",
            "<if test='filter.dateFrom != null'>",
            "AND c.created_at &gt;= #{filter.dateFrom}",
            "</if>",
            "<if test='filter.dateTo != null'>",
            "AND c.created_at &lt; DATE_ADD(#{filter.dateTo}, INTERVAL 1 DAY)",
            "</if>",
            "<if test='filter.status != null and filter.status != \"\"'>",
            "AND c.status = #{filter.status}",
            "</if>",
            "<if test='filter.urgency != null and filter.urgency != \"\"'>",
            "AND c.urgency = #{filter.urgency}",
            "</if>",
            "<if test='filter.departmentId != null'>",
            "AND c.department_id = #{filter.departmentId}",
            "</if>",
            "<if test='filter.doctorId != null'>",
            "AND c.doctor_id = #{filter.doctorId}",
            "</if>",
            "ORDER BY c.created_at DESC, c.id DESC",
            "</script>"
    })
    List<ConsultationExportRecord> selectConsultationExports(
            @Param("filter") ConsultationExportFilter filter
    );
}
