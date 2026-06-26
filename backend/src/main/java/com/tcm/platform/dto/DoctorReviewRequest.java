package com.tcm.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DoctorReviewRequest {

    @NotBlank(message = "请选择审核结果")
    @Pattern(regexp = "APPROVED|REJECTED", message = "审核结果无效")
    private String approvalStatus;

    @Size(max = 500, message = "审核备注不能超过500位")
    private String approvalNote;
}
