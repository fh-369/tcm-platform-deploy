package com.tcm.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tcm.platform.entity.Consultation;
import com.tcm.platform.entity.ConsultationProgressRecord;
import com.tcm.platform.entity.Department;
import com.tcm.platform.dto.ConsultationMessageSummary;
import com.tcm.platform.dto.ConsultationRequest;
import com.tcm.platform.dto.ConsultationUpdateRequest;
import com.tcm.platform.mapper.ConsultationMapper;
import com.tcm.platform.mapper.ConsultationMessageMapper;
import com.tcm.platform.mapper.DepartmentMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @BeforeAll
    static void initializeTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
                Consultation.class
        );
    }

    @Mock
    private ConsultationMapper consultationMapper;

    @Mock
    private ReminderService reminderService;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private AutoAssignmentService autoAssignmentService;

    @Mock
    private ConsultationMessageMapper consultationMessageMapper;

    @Test
    void listConsultationsSearchesPatientNameOrSymptomsByKeyword() {
        ConsultationService service = service();
        when(consultationMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>());

        service.listConsultations(1, 10, null, null, null, "胃痛");

        ArgumentCaptor<LambdaQueryWrapper<Consultation>> queryCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(consultationMapper).selectPage(any(IPage.class), queryCaptor.capture());

        String sql = queryCaptor.getValue().getCustomSqlSegment();
        assertThat(sql).contains("patient_name", "symptoms", "OR");
    }

    @Test
    void patientConsultationsIncludeProgressRecords() {
        ConsultationService service = service();
        Consultation consultation = new Consultation();
        consultation.setId(7L);
        consultation.setPatientAccountId(8L);
        Page<Consultation> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(consultation));
        ConsultationProgressRecord record = new ConsultationProgressRecord();
        record.setConsultationId(7L);
        record.setDoctorNote("请三天后反馈恢复情况。");
        when(consultationMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);
        when(consultationMapper.selectProgressRecords(List.of(7L)))
                .thenReturn(List.of(record));

        Page<Consultation> result =
                service.listConsultations(1, 10, null, null, 8L, null);

        assertThat(result.getRecords().get(0).getProgressRecords())
                .extracting(ConsultationProgressRecord::getDoctorNote)
                .containsExactly("请三天后反馈恢复情况。");
    }

    @Test
    void patientConsultationsIncludeLatestMessageSummary() {
        ConsultationService service = service();
        Consultation consultation = new Consultation();
        consultation.setId(7L);
        consultation.setPatientAccountId(8L);
        Page<Consultation> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(consultation));
        ConsultationMessageSummary summary = new ConsultationMessageSummary();
        summary.setConsultationId(7L);
        summary.setMessageCount(3L);
        summary.setLatestMessage("请继续记录饮食和睡眠情况。");
        summary.setLatestMessageSenderType("doctor");
        when(consultationMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);
        when(consultationMessageMapper.selectSummaries(List.of(7L)))
                .thenReturn(List.of(summary));

        Page<Consultation> result =
                service.listConsultations(1, 10, null, null, 8L, null);

        Consultation record = result.getRecords().get(0);
        assertThat(record.getMessageCount()).isEqualTo(3L);
        assertThat(record.getLatestMessage()).isEqualTo("请继续记录饮食和睡眠情况。");
        assertThat(record.getLatestMessageSenderType()).isEqualTo("doctor");
    }

    @Test
    void createConsultationAppliesDefaultsAndReminderBeforeInsert() {
        ConsultationService service = service();
        Department department = department(2L, true);
        when(consultationMapper.insert(any(Consultation.class))).thenReturn(1);
        when(departmentMapper.selectById(2L)).thenReturn(department);
        ConsultationRequest request = consultationRequest(null);

        Consultation created = service.createConsultation(request);

        assertThat(created.getDepartmentId()).isEqualTo(2L);
        assertThat(created.getUrgency()).isEqualTo("普通");
        assertThat(created.getStatus()).isEqualTo("待接诊");
        assertThat(created.getPatientName()).isEqualTo("李女士");
        verify(reminderService).applyReminder(created);
        verify(consultationMapper).insert(created);
        verify(autoAssignmentService).tryAssign(created, department);
    }

    @Test
    void createConsultationSucceedsWhenAutomaticAssignmentFails() {
        ConsultationService service = service();
        Department department = department(2L, true);
        when(consultationMapper.insert(any(Consultation.class))).thenReturn(1);
        when(departmentMapper.selectById(2L)).thenReturn(department);
        doThrow(new IllegalStateException("temporary assignment failure"))
                .when(autoAssignmentService)
                .tryAssign(any(Consultation.class), any(Department.class));

        Consultation created = service.createConsultation(consultationRequest("普通"));

        assertThat(created.getPatientName()).isEqualTo("李女士");
        assertThat(created.getDoctorId()).isNull();
        verify(consultationMapper).insert(created);
        verify(autoAssignmentService).tryAssign(created, department);
    }

    @Test
    void createConsultationRejectsInvalidUrgencyBeforeInsert() {
        ConsultationService service = service();

        assertThatThrownBy(() -> service.createConsultation(consultationRequest("最高优先")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("无效的紧急度");

        verify(consultationMapper, never()).insert(any());
        verify(reminderService, never()).applyReminder(any());
    }

    @Test
    void listConsultationsRejectsInvalidPaginationAndFilters() {
        ConsultationService service = service();

        assertThatThrownBy(() -> service.listConsultations(0, 10, null, null, null, null))
                .hasMessage("页码必须大于 0");
        assertThatThrownBy(() -> service.listConsultations(1, 101, null, null, null, null))
                .hasMessage("每页数量必须在 1 到 100 之间");
        assertThatThrownBy(() -> service.listConsultations(1, 10, "已取消", null, null, null))
                .hasMessage("无效的问诊状态");
        assertThatThrownBy(() -> service.listConsultations(1, 10, null, "未知", null, null))
                .hasMessage("无效的紧急度");

        verify(consultationMapper, never()).selectPage(any(), any());
    }

    @Test
    void updateConsultationChangesProvidedFieldsAndKeepsOtherValues() {
        ConsultationService service = service();
        Consultation existing = new Consultation();
        existing.setId(7L);
        existing.setStatus("待接诊");
        existing.setDoctorNote("旧备注");
        when(consultationMapper.selectById(7L)).thenReturn(existing);
        when(consultationMapper.updateById(existing)).thenReturn(1);
        ConsultationUpdateRequest request = new ConsultationUpdateRequest();
        request.setStatus("接诊中");
        request.setDoctorNote("已查看");
        request.setDoctorId(3L);

        Consultation updated = service.updateConsultation(7L, request);

        assertThat(updated.getStatus()).isEqualTo("接诊中");
        assertThat(updated.getDoctorNote()).isEqualTo("已查看");
        assertThat(updated.getDoctorId()).isEqualTo(3L);
        verify(consultationMapper).updateById(existing);
    }

    @Test
    void updateConsultationRejectsMissingRecordAndInvalidStatus() {
        ConsultationService service = service();
        when(consultationMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateConsultation(99L, new ConsultationUpdateRequest()))
                .hasMessage("问诊单不存在");

        Consultation existing = new Consultation();
        existing.setId(7L);
        when(consultationMapper.selectById(7L)).thenReturn(existing);
        ConsultationUpdateRequest invalid = new ConsultationUpdateRequest();
        invalid.setStatus("已取消");

        assertThatThrownBy(() -> service.updateConsultation(7L, invalid))
                .hasMessage("无效的问诊状态");
        verify(consultationMapper, never()).updateById(any());
    }

    @Test
    void patientCannotReadAnotherPatientsConsultation() {
        ConsultationService service = service();
        Consultation consultation = new Consultation();
        consultation.setId(7L);
        consultation.setPatientAccountId(8L);
        when(consultationMapper.selectById(7L)).thenReturn(consultation);

        assertThatThrownBy(() -> service.getPatientConsultation(7L, 9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("问诊单不存在或无权访问");
    }

    @Test
    void createConsultationRejectsMissingOrDisabledDepartment() {
        ConsultationRequest missingDepartment = consultationRequest("普通");
        missingDepartment.setDepartmentId(null);

        assertThatThrownBy(() -> service().createConsultation(missingDepartment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("请选择问诊科室");

        when(departmentMapper.selectById(2L)).thenReturn(department(2L, false));

        assertThatThrownBy(() -> service().createConsultation(consultationRequest("普通")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("请选择有效科室");

        verify(consultationMapper, never()).insert(any());
        verify(reminderService, never()).applyReminder(any());
    }

    private ConsultationRequest consultationRequest(String urgency) {
        ConsultationRequest request = new ConsultationRequest();
        request.setPatientAccountId(8L);
        request.setDepartmentId(2L);
        request.setPatientName("李女士");
        request.setAge(35);
        request.setGender("女");
        request.setPhone("13800000000");
        request.setSymptoms("容易疲倦");
        request.setDuration("约两周");
        request.setUrgency(urgency);
        return request;
    }

    private ConsultationService service() {
        return new ConsultationService(
                consultationMapper,
                departmentMapper,
                reminderService,
                autoAssignmentService,
                consultationMessageMapper
        );
    }

    private Department department(Long id, boolean enabled) {
        Department department = new Department();
        department.setId(id);
        department.setCode("internal-medicine");
        department.setName("中医内科");
        department.setEnabled(enabled);
        return department;
    }
}
