package com.tcm.platform.service;

import com.tcm.platform.dto.ConsultationMessageRequest;
import com.tcm.platform.entity.Consultation;
import com.tcm.platform.entity.ConsultationMessage;
import com.tcm.platform.mapper.ConsultationMapper;
import com.tcm.platform.mapper.ConsultationMessageMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultationMessageServiceTest {

    @Test
    void patientReadsAndSendsMessagesOnlyForOwnActiveConsultation() {
        ConsultationMessageMapper messageMapper = mock(ConsultationMessageMapper.class);
        ConsultationMapper consultationMapper = mock(ConsultationMapper.class);
        Consultation consultation = consultation(9L, 8L, 6L, "接诊中");
        ConsultationMessage existing = message(1L, "doctor", "张医生", "请先清淡饮食。");
        when(consultationMapper.selectById(9L)).thenReturn(consultation);
        when(messageMapper.selectByConsultationId(9L)).thenReturn(List.of(existing));
        when(messageMapper.insert(any(ConsultationMessage.class))).thenReturn(1);
        ConsultationMessageService service =
                new ConsultationMessageService(messageMapper, consultationMapper);

        assertThat(service.listForPatient(9L, 8L)).containsExactly(existing);

        ConsultationMessage created = service.sendAsPatient(
                9L,
                8L,
                "李女士",
                request("  今天胃口稍有改善。 ")
        );

        assertThat(created.getSenderType()).isEqualTo("patient");
        assertThat(created.getSenderId()).isEqualTo(8L);
        assertThat(created.getSenderName()).isEqualTo("李女士");
        assertThat(created.getContent()).isEqualTo("今天胃口稍有改善。");
        verify(messageMapper).insert(created);
    }

    @Test
    void patientCannotReadAnotherPatientsMessages() {
        ConsultationMessageMapper messageMapper = mock(ConsultationMessageMapper.class);
        ConsultationMapper consultationMapper = mock(ConsultationMapper.class);
        when(consultationMapper.selectById(9L))
                .thenReturn(consultation(9L, 8L, 6L, "接诊中"));
        ConsultationMessageService service =
                new ConsultationMessageService(messageMapper, consultationMapper);

        assertThatThrownBy(() -> service.listForPatient(9L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("问诊单不存在或无权访问");

        verify(messageMapper, never()).selectByConsultationId(any());
    }

    @Test
    void doctorCannotReadOrSendMessagesForAnotherDoctorsConsultation() {
        ConsultationMessageMapper messageMapper = mock(ConsultationMessageMapper.class);
        ConsultationMapper consultationMapper = mock(ConsultationMapper.class);
        when(consultationMapper.selectById(9L))
                .thenReturn(consultation(9L, 8L, 7L, "接诊中"));
        ConsultationMessageService service =
                new ConsultationMessageService(messageMapper, consultationMapper);

        assertThatThrownBy(() -> service.listForDoctor(9L, 6L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("该问诊单未分配给当前医生");
        assertThatThrownBy(() -> service.sendAsDoctor(
                9L, 6L, "张医生", request("继续观察。")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("该问诊单未分配给当前医生");

        verify(messageMapper, never()).insert(any());
    }

    @Test
    void messagesCanOnlyBeSentWhileConsultationIsActive() {
        ConsultationMessageMapper messageMapper = mock(ConsultationMessageMapper.class);
        ConsultationMapper consultationMapper = mock(ConsultationMapper.class);
        when(consultationMapper.selectById(9L))
                .thenReturn(consultation(9L, 8L, 6L, "待接诊"));
        when(consultationMapper.selectById(10L))
                .thenReturn(consultation(10L, 8L, 6L, "已完成"));
        ConsultationMessageService service =
                new ConsultationMessageService(messageMapper, consultationMapper);

        assertThatThrownBy(() -> service.sendAsPatient(
                9L, 8L, "李女士", request("补充情况")
        )).hasMessage("医生接诊后才能回复");
        assertThatThrownBy(() -> service.sendAsPatient(
                10L, 8L, "李女士", request("补充情况")
        )).hasMessage("问诊已完成，不能继续回复");

        verify(messageMapper, never()).insert(any());
    }

    @Test
    void doctorMessageIsTrimmedAndPersistedWithDoctorIdentity() {
        ConsultationMessageMapper messageMapper = mock(ConsultationMessageMapper.class);
        ConsultationMapper consultationMapper = mock(ConsultationMapper.class);
        when(consultationMapper.selectById(9L))
                .thenReturn(consultation(9L, 8L, 6L, "接诊中"));
        when(messageMapper.insert(any(ConsultationMessage.class))).thenReturn(1);
        ConsultationMessageService service =
                new ConsultationMessageService(messageMapper, consultationMapper);

        service.sendAsDoctor(9L, 6L, "张医生", request("  建议继续观察。  "));

        ArgumentCaptor<ConsultationMessage> captor =
                ArgumentCaptor.forClass(ConsultationMessage.class);
        verify(messageMapper).insert(captor.capture());
        assertThat(captor.getValue().getSenderType()).isEqualTo("doctor");
        assertThat(captor.getValue().getSenderId()).isEqualTo(6L);
        assertThat(captor.getValue().getContent()).isEqualTo("建议继续观察。");
    }

    private Consultation consultation(
            Long id,
            Long patientAccountId,
            Long doctorId,
            String status
    ) {
        Consultation consultation = new Consultation();
        consultation.setId(id);
        consultation.setPatientAccountId(patientAccountId);
        consultation.setDoctorId(doctorId);
        consultation.setStatus(status);
        return consultation;
    }

    private ConsultationMessageRequest request(String content) {
        ConsultationMessageRequest request = new ConsultationMessageRequest();
        request.setContent(content);
        return request;
    }

    private ConsultationMessage message(
            Long id,
            String senderType,
            String senderName,
            String content
    ) {
        ConsultationMessage message = new ConsultationMessage();
        message.setId(id);
        message.setConsultationId(9L);
        message.setSenderType(senderType);
        message.setSenderName(senderName);
        message.setContent(content);
        return message;
    }
}

