package com.tcm.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationExportRecord {

    private Long id;
    private String patientName;
    private Integer age;
    private String gender;
    private String phone;
    private String symptoms;
    private String duration;
    private String allergyHistory;
    private String urgency;
    private String status;
    private String departmentName;
    private String doctorName;
    private String doctorNote;
    private LocalDateTime assignedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
