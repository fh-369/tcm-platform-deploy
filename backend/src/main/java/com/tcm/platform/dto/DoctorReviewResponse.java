package com.tcm.platform.dto;

public record DoctorReviewResponse(
        Long accountId,
        String approvalStatus,
        Boolean enabled,
        String approvalNote
) {
}
