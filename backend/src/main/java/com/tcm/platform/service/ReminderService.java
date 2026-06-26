package com.tcm.platform.service;

import com.tcm.platform.entity.Consultation;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 根据问诊内容生成提醒等级和提醒文本。
 */
@Service
public class ReminderService {

    private static final String NORMAL = "normal";
    private static final String ATTENTION = "attention";
    private static final String URGENT = "urgent";

    public void applyReminder(Consultation consultation) {
        String level = NORMAL;
        Set<String> reminders = new LinkedHashSet<>();

        String symptoms = text(consultation.getSymptoms());
        String duration = text(consultation.getDuration());

        if ("非常紧急".equals(consultation.getUrgency())) {
            level = URGENT;
            reminders.add("当前情况非常紧急，请立即前往医院就诊。");
        } else if ("紧急".equals(consultation.getUrgency())) {
            level = ATTENTION;
            reminders.add("当前问诊标记为紧急，请优先关注并尽快安排诊疗。");
        }

        if (containsAny(symptoms, "发热", "胸痛", "呼吸困难")) {
            level = URGENT;
            reminders.add("症状可能存在较高风险，请及时前往医院就诊。");
        }

        if (containsAny(duration, "周", "月")) {
            level = higherLevel(level, ATTENTION);
            reminders.add("症状持续时间较长，请重点关注并尽快安排专业诊疗。");
        }

        if (symptoms.contains("失眠")) {
            level = higherLevel(level, ATTENTION);
            reminders.add("建议规律作息、减少睡前刺激，并记录近期睡眠情况。");
        }

        if (containsAny(symptoms, "胃痛", "脾胃")) {
            level = higherLevel(level, ATTENTION);
            reminders.add("建议饮食清淡规律，避免生冷辛辣等刺激性食物。");
        }

        if (reminders.isEmpty()) {
            reminders.add("建议保持观察，如症状加重请及时就医。");
        }

        consultation.setReminderLevel(level);
        consultation.setReminderText(String.join(" ", reminders));
    }

    private String higherLevel(String current, String candidate) {
        return priority(candidate) > priority(current) ? candidate : current;
    }

    private int priority(String level) {
        return switch (level) {
            case URGENT -> 3;
            case ATTENTION -> 2;
            default -> 1;
        };
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
