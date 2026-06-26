package com.tcm.platform.service;

import com.tcm.platform.dto.ConsultationMessageRequest;
import com.tcm.platform.entity.Consultation;
import com.tcm.platform.entity.ConsultationMessage;
import com.tcm.platform.mapper.ConsultationMapper;
import com.tcm.platform.mapper.ConsultationMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ConsultationMessageService {

    private static final String ACTIVE_STATUS = "接诊中";
    private static final String COMPLETED_STATUS = "已完成";
    private static final int MAX_CONTENT_LENGTH = 2000;

    private final ConsultationMessageMapper messageMapper;
    private final ConsultationMapper consultationMapper;

    public ConsultationMessageService(
            ConsultationMessageMapper messageMapper,
            ConsultationMapper consultationMapper
    ) {
        this.messageMapper = messageMapper;
        this.consultationMapper = consultationMapper;
    }

    public List<ConsultationMessage> listForPatient(
            Long consultationId,
            Long patientAccountId
    ) {
        requirePatientConsultation(consultationId, patientAccountId);
        return messageMapper.selectByConsultationId(consultationId);
    }

    public List<ConsultationMessage> listForDoctor(Long consultationId, Long doctorId) {
        requireDoctorConsultation(consultationId, doctorId);
        return messageMapper.selectByConsultationId(consultationId);
    }

    @Transactional
    public ConsultationMessage sendAsPatient(
            Long consultationId,
            Long patientAccountId,
            String patientName,
            ConsultationMessageRequest request
    ) {
        Consultation consultation =
                requirePatientConsultation(consultationId, patientAccountId);
        validateActiveStatus(consultation);
        return insertMessage(
                consultationId,
                "patient",
                patientAccountId,
                patientName,
                request
        );
    }

    @Transactional
    public ConsultationMessage sendAsDoctor(
            Long consultationId,
            Long doctorId,
            String doctorName,
            ConsultationMessageRequest request
    ) {
        Consultation consultation = requireDoctorConsultation(consultationId, doctorId);
        validateActiveStatus(consultation);
        return insertMessage(
                consultationId,
                "doctor",
                doctorId,
                doctorName,
                request
        );
    }

    @Transactional
    public ConsultationMessage appendDoctorMessage(
            Consultation consultation,
            Long doctorId,
            String doctorName,
            String content
    ) {
        ConsultationMessageRequest request = new ConsultationMessageRequest();
        request.setContent(content);
        return insertMessage(
                consultation.getId(),
                "doctor",
                doctorId,
                doctorName,
                request
        );
    }

    private ConsultationMessage insertMessage(
            Long consultationId,
            String senderType,
            Long senderId,
            String senderName,
            ConsultationMessageRequest request
    ) {
        String content = normalizeContent(request == null ? null : request.getContent());
        ConsultationMessage message = new ConsultationMessage();
        message.setConsultationId(consultationId);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setSenderName(normalizeName(senderName, senderType));
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        if (messageMapper.insert(message) != 1) {
            throw new IllegalStateException("回复保存失败");
        }
        return message;
    }

    private Consultation requirePatientConsultation(
            Long consultationId,
            Long patientAccountId
    ) {
        Consultation consultation = requireConsultation(consultationId);
        if (!Objects.equals(patientAccountId, consultation.getPatientAccountId())) {
            throw new IllegalArgumentException("问诊单不存在或无权访问");
        }
        return consultation;
    }

    private Consultation requireDoctorConsultation(Long consultationId, Long doctorId) {
        Consultation consultation = requireConsultation(consultationId);
        if (!Objects.equals(doctorId, consultation.getDoctorId())) {
            throw new IllegalArgumentException("该问诊单未分配给当前医生");
        }
        return consultation;
    }

    private Consultation requireConsultation(Long consultationId) {
        if (consultationId == null) {
            throw new IllegalArgumentException("问诊单 ID 不能为空");
        }
        Consultation consultation = consultationMapper.selectById(consultationId);
        if (consultation == null) {
            throw new IllegalArgumentException("问诊单不存在");
        }
        return consultation;
    }

    private void validateActiveStatus(Consultation consultation) {
        if (COMPLETED_STATUS.equals(consultation.getStatus())) {
            throw new IllegalArgumentException("问诊已完成，不能继续回复");
        }
        if (!ACTIVE_STATUS.equals(consultation.getStatus())) {
            throw new IllegalArgumentException("医生接诊后才能回复");
        }
    }

    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("请输入回复内容");
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("回复内容不能超过 2000 字");
        }
        return normalized;
    }

    private String normalizeName(String name, String senderType) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return "doctor".equals(senderType) ? "接诊医生" : "患者";
    }
}

