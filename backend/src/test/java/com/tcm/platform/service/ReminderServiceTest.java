package com.tcm.platform.service;

import com.tcm.platform.entity.Consultation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderServiceTest {

    private final ReminderService service = new ReminderService();

    @Test
    void ordinaryConsultationReceivesNormalObservationReminder() {
        Consultation consultation = consultation("轻微疲倦", "两天", "普通");

        service.applyReminder(consultation);

        assertThat(consultation.getReminderLevel()).isEqualTo("normal");
        assertThat(consultation.getReminderText()).contains("保持观察");
    }

    @Test
    void longTermInsomniaRaisesAttentionAndCombinesDistinctAdvice() {
        Consultation consultation = consultation("近期失眠", "两周", "普通");

        service.applyReminder(consultation);

        assertThat(consultation.getReminderLevel()).isEqualTo("attention");
        assertThat(consultation.getReminderText()).contains("持续时间较长", "规律作息");
    }

    @Test
    void dangerSymptomsOverrideLowerUrgencyAndRaiseUrgentReminder() {
        Consultation consultation = consultation("胸痛并伴有呼吸困难", "一天", "紧急");

        service.applyReminder(consultation);

        assertThat(consultation.getReminderLevel()).isEqualTo("urgent");
        assertThat(consultation.getReminderText()).contains("较高风险", "及时前往医院");
    }

    @Test
    void nullSymptomsAndDurationAreHandledSafely() {
        Consultation consultation = consultation(null, null, null);

        service.applyReminder(consultation);

        assertThat(consultation.getReminderLevel()).isEqualTo("normal");
        assertThat(consultation.getReminderText()).isNotBlank();
    }

    private Consultation consultation(String symptoms, String duration, String urgency) {
        Consultation consultation = new Consultation();
        consultation.setSymptoms(symptoms);
        consultation.setDuration(duration);
        consultation.setUrgency(urgency);
        return consultation;
    }
}
