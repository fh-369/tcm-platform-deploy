package com.tcm.platform.dto;

public record DoctorProfileResponse(
        Long accountId,
        String displayName,
        Long departmentId,
        String departmentName,
        String phone,
        String qualification,
        String profile
) {
}
