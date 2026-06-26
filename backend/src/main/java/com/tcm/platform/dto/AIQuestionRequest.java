package com.tcm.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 提问请求 DTO
 */
@Data
public class AIQuestionRequest {
    
    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题不能超过500字")
    private String question;

    @Size(max = 12, message = "上下文消息不能超过12条")
    private List<ContextMessage> context = new ArrayList<>();

    private Long consultationId;

    private Long conversationId;

    public List<ContextMessage> getContext() {
        return context == null ? List.of() : context;
    }

    public record ContextMessage(
            String role,
            String content
    ) {
    }
}
