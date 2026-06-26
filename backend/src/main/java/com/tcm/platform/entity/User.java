package com.tcm.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体（医生/管理员）
 * 任务：完善字段定义，对应数据库 users 表
 */
@Data
@TableName("users")
public class User {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long accountId;

    private String username;

    private String passwordHash;

    private String role;

    private String displayName;

    private String department;

    private Long departmentId;

    private String phone;

    private String qualification;

    private String profile;

    private String approvalStatus;

    private String approvalNote;

    private LocalDateTime approvedAt;

    private Long approvedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
