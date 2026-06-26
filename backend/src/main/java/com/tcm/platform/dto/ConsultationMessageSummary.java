package com.tcm.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationMessageSummary {

    private Long consultationId;
    private Long messageCount;
    private String latestMessage;
    private String latestMessageSenderType;
    private LocalDateTime latestMessageAt;
}

