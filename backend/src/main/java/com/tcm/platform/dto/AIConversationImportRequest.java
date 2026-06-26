package com.tcm.platform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AIConversationImportRequest {

    @Valid
    @Size(max = 500, message = "单个对话最多导入500条消息")
    private List<MessageItem> messages = new ArrayList<>();

    @Size(max = 4, message = "单个对话最多导入4条推荐")
    private List<AIContentRecommendation> recommendations = new ArrayList<>();

    public record MessageItem(
            String role,
            String content,
            boolean fallback,
            String disclaimer
    ) {
    }
}
