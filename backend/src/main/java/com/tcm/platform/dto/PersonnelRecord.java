package com.tcm.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员人员列表使用的安全视图，不包含密码哈希。
 */
@Data
public class PersonnelRecord {

    private Long id;

    private Long userId;

    private String username;

    private String role;

    private String displayName;

    private String phone;

    private String department;

    private Long departmentId;

    private String qualification;

    private String profile;

    private String approvalStatus;

    private String approvalNote;

    private Boolean enabled;

    private LocalDateTime createdAt;
}
