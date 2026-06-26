package com.tcm.platform.dto;

import lombok.Data;

/**
 * 登录响应 DTO
 */
@Data
public class LoginResponse {
    private String token;
    private String role;
    private Long userId;
    private String displayName;
}
