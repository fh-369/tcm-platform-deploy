package com.tcm.platform.service;

import com.tcm.platform.entity.Consultation;
import com.tcm.platform.entity.Department;
import com.tcm.platform.mapper.ConsultationMapper;
import com.tcm.platform.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AutoAssignmentService {

    private static final String GENERAL_DEPARTMENT_CODE = "general";
    private static final String PENDING_STATUS = "待接诊";

    private final UserMapper userMapper;
    private final ConsultationMapper consultationMapper;

    public AutoAssignmentService(
            UserMapper userMapper,
            ConsultationMapper consultationMapper
    ) {
        this.userMapper = userMapper;
        this.consultationMapper = consultationMapper;
    }

    public void tryAssign(Consultation consultation, Department department) {
        if (GENERAL_DEPARTMENT_CODE.equals(department.getCode())) {
            return;
        }

        Long doctorId = userMapper.selectAutoAssignmentDoctorId(department.getId());
        if (doctorId == null) {
            return;
        }
        if (consultationMapper.updateAssignment(
                consultation.getId(),
                doctorId,
                PENDING_STATUS
        ) != 1) {
            throw new IllegalStateException("自动分配问诊失败");
        }

        consultation.setDoctorId(doctorId);
        consultation.setStatus(PENDING_STATUS);
        consultation.setAssignedAt(LocalDateTime.now());
    }
}
