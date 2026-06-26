package com.tcm.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AIConversationCreateRequest {

    @NotBlank(message = "对话标题不能为空")
    @Size(max = 100, message = "对话标题不能超过100字")
    private String title;

    private Long consultationId;

    @Size(max = 100, message = "迁移标识不能超过100字")
    private String legacyKey;
}
