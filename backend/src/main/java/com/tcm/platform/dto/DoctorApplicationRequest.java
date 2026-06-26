package com.tcm.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DoctorApplicationRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度应为3到50位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;

    @NotBlank(message = "医生姓名不能为空")
    @Size(max = 100, message = "医生姓名不能超过100位")
    private String displayName;

    @NotNull(message = "请选择科室")
    private Long departmentId;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "请输入11位手机号")
    @Size(max = 20, message = "联系电话不能超过20位")
    private String phone;

    @NotBlank(message = "请填写资质信息")
    @Size(max = 500, message = "资质信息不能超过500位")
    private String qualification;

    @Size(max = 1000, message = "个人简介不能超过1000位")
    private String profile;
}
