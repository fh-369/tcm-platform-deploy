package com.tcm.platform.service;

import com.tcm.platform.dto.DashboardSummary;
import com.tcm.platform.mapper.AccountMapper;
import com.tcm.platform.mapper.ConsultationMapper;
import com.tcm.platform.mapper.KnowledgeArticleMapper;
import com.tcm.platform.mapper.RecipeMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final ConsultationMapper consultationMapper;
    private final AccountMapper accountMapper;
    private final KnowledgeArticleMapper knowledgeArticleMapper;
    private final RecipeMapper recipeMapper;

    public DashboardService(
            ConsultationMapper consultationMapper,
            AccountMapper accountMapper,
            KnowledgeArticleMapper knowledgeArticleMapper,
            RecipeMapper recipeMapper
    ) {
        this.consultationMapper = consultationMapper;
        this.accountMapper = accountMapper;
        this.knowledgeArticleMapper = knowledgeArticleMapper;
        this.recipeMapper = recipeMapper;
    }

    public DashboardSummary getAdminSummary() {
        Map<String, Long> metrics = new LinkedHashMap<>();
        metrics.put("registeredPatients", accountMapper.countPatientAccounts());
        metrics.put("enabledDoctors", accountMapper.countEnabledApprovedDoctors());
        metrics.put("publishedKnowledge", knowledgeArticleMapper.countPublished());
        metrics.put("publishedRecipes", recipeMapper.countPublished());

        return new DashboardSummary(
                "platform",
                consultationMapper.countByStatus(),
                consultationMapper.countByUrgency(),
                consultationMapper.trendLast6Months(),
                metrics,
                consultationMapper.countByDepartment(),
                consultationMapper.countDoctorWorkloads()
        );
    }

    public DashboardSummary getDoctorSummary(Long doctorId) {
        if (doctorId == null) {
            throw new IllegalArgumentException("医生 ID 不能为空");
        }
        List<Map<String, Object>> statuses = consultationMapper.countByStatusForDoctor(doctorId);
        Map<String, Long> metrics = Map.of("assignedTotal", totalCount(statuses));

        return new DashboardSummary(
                "doctor",
                statuses,
                consultationMapper.countByUrgencyForDoctor(doctorId),
                consultationMapper.trendLast6MonthsForDoctor(doctorId),
                metrics,
                List.of(),
                List.of()
        );
    }

    public List<Map<String, Object>> getTrend(String period, Long doctorId) {
        if (doctorId == null) {
            return switch (period) {
                case "day" -> consultationMapper.trendLast7Days();
                case "week" -> consultationMapper.trendLast4Weeks();
                case "month" -> consultationMapper.trendLast6MonthsByPeriod();
                default -> throw new IllegalArgumentException("不支持的趋势周期");
            };
        }

        return switch (period) {
            case "day" -> consultationMapper.trendLast7DaysForDoctor(doctorId);
            case "week" -> consultationMapper.trendLast4WeeksForDoctor(doctorId);
            case "month" -> consultationMapper.trendLast6MonthsByPeriodForDoctor(doctorId);
            default -> throw new IllegalArgumentException("不支持的趋势周期");
        };
    }

    private long totalCount(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> row.get("count"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToLong(Number::longValue)
                .sum();
    }
}
