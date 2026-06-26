package com.tcm.platform.dto;

import lombok.Data;
import com.tcm.platform.entity.ConsultationProgressRecord;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConsultationWorkspaceRecord {

    private Long id;
    private Long patientAccountId;
    private Long departmentId;
    private String departmentName;
    private String patientName;
    private Integer age;
    private String gender;
    private String phone;
    private String symptoms;
    private String duration;
    private String allergyHistory;
    private String urgency;
    private String patientNote;
    private String reminderLevel;
    private String reminderText;
    private String status;
    private String doctorNote;
    private Long doctorId;
    private String doctorName;
    private String doctorDepartment;
    private LocalDateTime assignedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ConsultationProgressRecord> progressRecords;
    private Long messageCount;
    private String latestMessage;
    private String latestMessageSenderType;
    private LocalDateTime latestMessageAt;
}
