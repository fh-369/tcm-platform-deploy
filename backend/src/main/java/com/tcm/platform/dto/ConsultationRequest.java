package com.tcm.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 问诊请求 DTO
 */
@Data
public class ConsultationRequest {
    
    private Long patientAccountId;

    @NotNull(message = "请选择问诊科室")
    private Long departmentId;
    
    @NotBlank(message = "请输入患者姓名")
    @Pattern(
            regexp = "^$|^[\\p{IsHan}A-Za-z·\\s]{2,50}$",
            message = "患者姓名应为 2-50 个中文、英文字母、空格或间隔号"
    )
    private String patientName;

    @NotNull(message = "请输入患者年龄")
    @Min(value = 1, message = "患者年龄必须在 1-150 岁之间")
    @Max(value = 150, message = "患者年龄必须在 1-150 岁之间")
    private Integer age;

    @NotBlank(message = "请选择性别")
    @Pattern(regexp = "^$|^(男|女)$", message = "请选择有效性别")
    private String gender;

    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "请输入正确的 11 位手机号")
    private String phone;

    @NotBlank(message = "请描述主要症状")
    @Pattern(regexp = "(?s)^$|^.{2,2000}$", message = "主要症状应为 2-2000 个字符")
    private String symptoms;

    @NotBlank(message = "请输入症状持续时间")
    @Size(max = 100, message = "症状持续时间不能超过 100 个字符")
    private String duration;

    private String allergyHistory;

    @NotBlank(message = "请选择紧急程度")
    @Pattern(regexp = "^$|^(普通|紧急|非常紧急)$", message = "请选择有效紧急程度")
    private String urgency;

    private String patientNote;
}
