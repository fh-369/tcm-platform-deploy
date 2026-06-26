package com.tcm.platform.dto;

import jakarta.validation.constraints.NotNull;

public record PublicationRequest(
        @NotNull(message = "发布状态不能为空") Boolean published
) {
}
