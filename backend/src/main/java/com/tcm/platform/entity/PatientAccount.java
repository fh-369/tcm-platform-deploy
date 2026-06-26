package com.tcm.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 患者账号实体
 * 任务：完善字段定义，对应数据库 patient_accounts 表
 */
@Data
@TableName("patient_accounts")
public class PatientAccount {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long accountId;

    private String username;

    private String passwordHash;

    private String displayName;

    private String phone;

    private String avatarUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
