package com.tcm.platform.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationProgressRecord {

    private Long id;
    private Long consultationId;
    private Long doctorId;
    private String doctorName;
    private String previousStatus;
    private String status;
    private String doctorNote;
    private LocalDateTime createdAt;
}
