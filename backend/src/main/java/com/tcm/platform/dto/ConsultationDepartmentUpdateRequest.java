package com.tcm.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsultationDepartmentUpdateRequest {

    @NotNull(message = "请选择问诊科室")
    private Long departmentId;
}
