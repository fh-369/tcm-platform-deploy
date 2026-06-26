package com.tcm.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 问诊单实体（核心业务表）
 * 任务：完善字段定义，对应数据库 consultations 表
 */
@Data
@TableName("consultations")
public class Consultation {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientAccountId;

    private Long departmentId;

    @TableField(exist = false)
    private String departmentName;

    @TableField(exist = false)
    private List<ConsultationProgressRecord> progressRecords;

    @TableField(exist = false)
    private Long messageCount;

    @TableField(exist = false)
    private String latestMessage;

    @TableField(exist = false)
    private String latestMessageSenderType;

    @TableField(exist = false)
    private LocalDateTime latestMessageAt;

    private String patientName;

    private Integer age;

    private String gender;

    private String phone;

    private String symptoms;

    private String duration;

    private String allergyHistory;

    private String urgency;

    private String patientNote;

    private String reminderLevel;

    private String reminderText;

    private String status;

    private String doctorNote;

    private Long doctorId;

    private LocalDateTime assignedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
