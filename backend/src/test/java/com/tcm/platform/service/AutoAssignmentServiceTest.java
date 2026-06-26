package com.tcm.platform.service;

import com.tcm.platform.entity.Consultation;
import com.tcm.platform.entity.Department;
import com.tcm.platform.mapper.ConsultationMapper;
import com.tcm.platform.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutoAssignmentServiceTest {

    @Test
    void skipsGeneralConsultations() {
        UserMapper userMapper = mock(UserMapper.class);
        ConsultationMapper consultationMapper = mock(ConsultationMapper.class);
        AutoAssignmentService service = new AutoAssignmentService(userMapper, consultationMapper);
        Consultation consultation = consultation(9L, 1L);

        service.tryAssign(consultation, department(1L, "general"));

        assertThat(consultation.getDoctorId()).isNull();
        verify(userMapper, never()).selectAutoAssignmentDoctorId(1L);
        verify(consultationMapper, never()).updateAssignment(9L, null, "待接诊");
    }

    @Test
    void assignsTheCandidateChosenByDeterministicLoadQuery() {
        UserMapper userMapper = mock(UserMapper.class);
        ConsultationMapper consultationMapper = mock(ConsultationMapper.class);
        AutoAssignmentService service = new AutoAssignmentService(userMapper, consultationMapper);
        Consultation consultation = consultation(9L, 2L);
        when(userMapper.selectAutoAssignmentDoctorId(2L)).thenReturn(6L);
        when(consultationMapper.updateAssignment(9L, 6L, "待接诊")).thenReturn(1);

        service.tryAssign(consultation, department(2L, "internal-medicine"));

        assertThat(consultation.getDoctorId()).isEqualTo(6L);
        assertThat(consultation.getAssignedAt()).isNotNull();
        verify(consultationMapper).updateAssignment(9L, 6L, "待接诊");
    }

    @Test
    void keepsConsultationUnassignedWhenNoCandidateExists() {
        UserMapper userMapper = mock(UserMapper.class);
        ConsultationMapper consultationMapper = mock(ConsultationMapper.class);
        AutoAssignmentService service = new AutoAssignmentService(userMapper, consultationMapper);
        Consultation consultation = consultation(9L, 3L);
        when(userMapper.selectAutoAssignmentDoctorId(3L)).thenReturn(null);

        service.tryAssign(consultation, department(3L, "gynecology"));

        assertThat(consultation.getDoctorId()).isNull();
        verify(consultationMapper, never()).updateAssignment(9L, null, "待接诊");
    }

    private Consultation consultation(Long id, Long departmentId) {
        Consultation consultation = new Consultation();
        consultation.setId(id);
        consultation.setDepartmentId(departmentId);
        consultation.setStatus("待接诊");
        return consultation;
    }

    private Department department(Long id, String code) {
        Department department = new Department();
        department.setId(id);
        department.setCode(code);
        department.setEnabled(true);
        return department;
    }
}
