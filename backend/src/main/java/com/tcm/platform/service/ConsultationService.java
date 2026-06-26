package com.tcm.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tcm.platform.dto.ConsultationRequest;
import com.tcm.platform.dto.ConsultationUpdateRequest;
import com.tcm.platform.entity.Consultation;
import com.tcm.platform.entity.ConsultationProgressRecord;
import com.tcm.platform.entity.Department;
import com.tcm.platform.mapper.ConsultationMapper;
import com.tcm.platform.mapper.ConsultationMessageMapper;
import com.tcm.platform.mapper.DepartmentMapper;
import com.tcm.platform.dto.ConsultationMessageSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 问诊单创建、查询、更新和统计业务。
 */
@Service
public class ConsultationService {

    private static final Logger log = LoggerFactory.getLogger(ConsultationService.class);
    private static final String DEFAULT_URGENCY = "普通";
    private static final String INITIAL_STATUS = "待接诊";
    private static final Set<String> VALID_URGENCIES = Set.of("普通", "紧急", "非常紧急");
    private static final Set<String> VALID_STATUSES = Set.of("待接诊", "接诊中", "已完成");

    private final ConsultationMapper consultationMapper;
    private final DepartmentMapper departmentMapper;
    private final ReminderService reminderService;
    private final AutoAssignmentService autoAssignmentService;
    private final ConsultationMessageMapper consultationMessageMapper;

    public ConsultationService(
            ConsultationMapper consultationMapper,
            DepartmentMapper departmentMapper,
            ReminderService reminderService,
            AutoAssignmentService autoAssignmentService,
            ConsultationMessageMapper consultationMessageMapper
    ) {
        this.consultationMapper = consultationMapper;
        this.departmentMapper = departmentMapper;
        this.reminderService = reminderService;
        this.autoAssignmentService = autoAssignmentService;
        this.consultationMessageMapper = consultationMessageMapper;
    }

    @Transactional
    public Consultation createConsultation(ConsultationRequest request) {
        String urgency = defaultUrgency(request.getUrgency());
        validateUrgency(urgency);
        Department department = requireEnabledDepartment(request.getDepartmentId());

        Consultation consultation = new Consultation();
        consultation.setPatientAccountId(request.getPatientAccountId());
        consultation.setDepartmentId(department.getId());
        consultation.setDepartmentName(department.getName());
        consultation.setPatientName(request.getPatientName().trim());
        consultation.setAge(request.getAge());
        consultation.setGender(request.getGender());
        consultation.setPhone(request.getPhone().trim());
        consultation.setSymptoms(request.getSymptoms().trim());
        consultation.setDuration(request.getDuration().trim());
        consultation.setAllergyHistory(nullIfBlank(request.getAllergyHistory()));
        consultation.setUrgency(urgency);
        consultation.setPatientNote(nullIfBlank(request.getPatientNote()));
        consultation.setStatus(INITIAL_STATUS);
        reminderService.applyReminder(consultation);

        if (consultationMapper.insert(consultation) != 1) {
            throw new IllegalStateException("创建问诊单失败");
        }
        try {
            autoAssignmentService.tryAssign(consultation, department);
        } catch (RuntimeException exception) {
            log.warn(
                    "Automatic assignment failed for consultation {}: {}",
                    consultation.getId(),
                    exception.getMessage()
            );
        }
        return consultation;
    }

    public Page<Consultation> listConsultations(
            long current,
            long size,
            String status,
            String urgency,
            Long patientAccountId,
            String keyword
    ) {
        validatePage(current, size);
        validateOptionalStatus(status);
        validateOptionalUrgency(urgency);

        LambdaQueryWrapper<Consultation> query = new LambdaQueryWrapper<>();
        query.eq(hasText(status), Consultation::getStatus, status)
                .eq(hasText(urgency), Consultation::getUrgency, urgency)
                .eq(patientAccountId != null, Consultation::getPatientAccountId, patientAccountId)
                .and(hasText(keyword), wrapper -> wrapper
                        .like(Consultation::getPatientName, keyword)
                        .or()
                        .like(Consultation::getSymptoms, keyword))
                .orderByDesc(Consultation::getCreatedAt);

        Page<Consultation> page = consultationMapper.selectPage(new Page<>(current, size), query);
        attachDepartmentNames(page.getRecords());
        attachProgressRecords(page.getRecords());
        attachMessageSummaries(page.getRecords());
        return page;
    }

    public Consultation getConsultationById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("问诊单 ID 不能为空");
        }

        Consultation consultation = consultationMapper.selectById(id);
        if (consultation == null) {
            throw new IllegalArgumentException("问诊单不存在");
        }
        return consultation;
    }

    public Consultation getPatientConsultation(Long id, Long patientAccountId) {
        if (patientAccountId == null) {
            throw new IllegalArgumentException("患者账号 ID 不能为空");
        }
        Consultation consultation = getConsultationById(id);
        if (!patientAccountId.equals(consultation.getPatientAccountId())) {
            throw new IllegalArgumentException("问诊单不存在或无权访问");
        }
        return consultation;
    }

    @Transactional
    public Consultation updateConsultation(Long id, ConsultationUpdateRequest request) {
        Consultation consultation = getConsultationById(id);
        validateOptionalStatus(request.getStatus());

        if (hasText(request.getStatus())) {
            consultation.setStatus(request.getStatus());
        }
        if (request.getDoctorNote() != null) {
            consultation.setDoctorNote(request.getDoctorNote());
        }
        if (request.getDoctorId() != null) {
            consultation.setDoctorId(request.getDoctorId());
        }
        if (consultationMapper.updateById(consultation) != 1) {
            throw new IllegalStateException("更新问诊单失败");
        }
        return consultation;
    }

    public List<Map<String, Object>> getStatusDistribution() {
        return consultationMapper.countByStatus();
    }

    public List<Map<String, Object>> getUrgencyDistribution() {
        return consultationMapper.countByUrgency();
    }

    public List<Map<String, Object>> getTrendLast6Months() {
        return consultationMapper.trendLast6Months();
    }

    private void validatePage(long current, long size) {
        if (current < 1) {
            throw new IllegalArgumentException("页码必须大于 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("每页数量必须在 1 到 100 之间");
        }
    }

    private void validateOptionalStatus(String status) {
        if (hasText(status) && !VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("无效的问诊状态");
        }
    }

    private void validateOptionalUrgency(String urgency) {
        if (hasText(urgency)) {
            validateUrgency(urgency);
        }
    }

    private void validateUrgency(String urgency) {
        if (!VALID_URGENCIES.contains(urgency)) {
            throw new IllegalArgumentException("无效的紧急度");
        }
    }

    private String defaultUrgency(String urgency) {
        return hasText(urgency) ? urgency : DEFAULT_URGENCY;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullIfBlank(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private Department requireEnabledDepartment(Long departmentId) {
        if (departmentId == null) {
            throw new IllegalArgumentException("请选择问诊科室");
        }
        Department department = departmentMapper.selectById(departmentId);
        if (department == null || Boolean.FALSE.equals(department.getEnabled())) {
            throw new IllegalArgumentException("请选择有效科室");
        }
        return department;
    }

    private void attachDepartmentNames(List<Consultation> consultations) {
        Set<Long> departmentIds = consultations.stream()
                .map(Consultation::getDepartmentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Department> departments = departmentIds.isEmpty()
                ? Collections.emptyMap()
                : departmentMapper.selectBatchIds(departmentIds).stream()
                        .collect(Collectors.toMap(Department::getId, Function.identity()));
        consultations.forEach(consultation -> {
            Department department = departments.get(consultation.getDepartmentId());
            consultation.setDepartmentName(department == null ? null : department.getName());
        });
    }

    private void attachProgressRecords(List<Consultation> consultations) {
        List<Long> consultationIds = consultations.stream()
                .map(Consultation::getId)
                .filter(Objects::nonNull)
                .toList();
        if (consultationIds.isEmpty()) {
            return;
        }
        Map<Long, List<ConsultationProgressRecord>> recordsByConsultation =
                consultationMapper.selectProgressRecords(consultationIds).stream()
                        .collect(Collectors.groupingBy(
                                ConsultationProgressRecord::getConsultationId,
                                Collectors.toList()
                        ));
        consultations.forEach(consultation ->
                consultation.setProgressRecords(
                        recordsByConsultation.getOrDefault(
                                consultation.getId(),
                                Collections.emptyList()
                        )
                )
        );
    }

    private void attachMessageSummaries(List<Consultation> consultations) {
        List<Long> consultationIds = consultations.stream()
                .map(Consultation::getId)
                .filter(Objects::nonNull)
                .toList();
        if (consultationIds.isEmpty()) {
            return;
        }
        Map<Long, ConsultationMessageSummary> summaries =
                consultationMessageMapper.selectSummaries(consultationIds).stream()
                        .collect(Collectors.toMap(
                                ConsultationMessageSummary::getConsultationId,
                                Function.identity()
                        ));
        consultations.forEach(consultation -> {
            ConsultationMessageSummary summary = summaries.get(consultation.getId());
            if (summary == null) {
                consultation.setMessageCount(0L);
                return;
            }
            consultation.setMessageCount(summary.getMessageCount());
            consultation.setLatestMessage(summary.getLatestMessage());
            consultation.setLatestMessageSenderType(summary.getLatestMessageSenderType());
            consultation.setLatestMessageAt(summary.getLatestMessageAt());
        });
    }
}
