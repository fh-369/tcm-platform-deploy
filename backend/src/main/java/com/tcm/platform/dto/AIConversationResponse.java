package com.tcm.platform.dto;

import com.tcm.platform.entity.AIMessage;

import java.time.LocalDateTime;
import java.util.List;

public record AIConversationResponse(
        Long id,
        String title,
        Long consultationId,
        boolean recommendationInitialized,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AIMessage> messages,
        List<AIContentRecommendation> recommendations,
        long messageTotal,
        boolean hasMoreMessages
) {
}
