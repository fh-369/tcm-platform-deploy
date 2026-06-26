package com.tcm.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AccountStatusUpdateRequest {

    @NotNull(message = "请选择账号状态")
    private Boolean enabled;
}
