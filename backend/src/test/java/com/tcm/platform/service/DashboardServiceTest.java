package com.tcm.platform.service;

import com.tcm.platform.dto.DashboardSummary;
import com.tcm.platform.mapper.AccountMapper;
import com.tcm.platform.mapper.ConsultationMapper;
import com.tcm.platform.mapper.KnowledgeArticleMapper;
import com.tcm.platform.mapper.RecipeMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    @Test
    void administratorSummaryIncludesPlatformOperationsAndWorkloadData() {
        ConsultationMapper consultationMapper = mock(ConsultationMapper.class);
        AccountMapper accountMapper = mock(AccountMapper.class);
        KnowledgeArticleMapper knowledgeArticleMapper = mock(KnowledgeArticleMapper.class);
        RecipeMapper recipeMapper = mock(RecipeMapper.class);
        List<Map<String, Object>> status = List.of(Map.of("status", "待接诊", "count", 2L));
        List<Map<String, Object>> urgency = List.of(Map.of("urgency", "紧急", "count", 1L));
        List<Map<String, Object>> trend = List.of(Map.of("month", "2026-06", "count", 3L));
        List<Map<String, Object>> departments =
                List.of(Map.of("department", "中医内科", "count", 3L));
        List<Map<String, Object>> workloads =
                List.of(Map.of("doctorId", 6L, "doctorName", "李医生", "activeCount", 2L));
        when(consultationMapper.countByStatus()).thenReturn(status);
        when(consultationMapper.countByUrgency()).thenReturn(urgency);
        when(consultationMapper.trendLast6Months()).thenReturn(trend);
        when(consultationMapper.countByDepartment()).thenReturn(departments);
        when(consultationMapper.countDoctorWorkloads()).thenReturn(workloads);
        when(accountMapper.countPatientAccounts()).thenReturn(11L);
        when(accountMapper.countEnabledApprovedDoctors()).thenReturn(3L);
        when(knowledgeArticleMapper.countPublished()).thenReturn(7L);
        when(recipeMapper.countPublished()).thenReturn(6L);

        DashboardSummary summary = service(
                consultationMapper, accountMapper, knowledgeArticleMapper, recipeMapper
        ).getAdminSummary();

        assertThat(summary.scope()).isEqualTo("platform");
        assertThat(summary.statusDistribution()).isEqualTo(status);
        assertThat(summary.metrics()).containsEntry("registeredPatients", 11L)
                .containsEntry("enabledDoctors", 3L)
                .containsEntry("publishedKnowledge", 7L)
                .containsEntry("publishedRecipes", 6L);
        assertThat(summary.departmentDistribution()).isEqualTo(departments);
        assertThat(summary.doctorWorkloads()).isEqualTo(workloads);
    }

    @Test
    void doctorSummaryAndTrendOnlyUseAssignedConsultations() {
        ConsultationMapper consultationMapper = mock(ConsultationMapper.class);
        AccountMapper accountMapper = mock(AccountMapper.class);
        KnowledgeArticleMapper knowledgeArticleMapper = mock(KnowledgeArticleMapper.class);
        RecipeMapper recipeMapper = mock(RecipeMapper.class);
        List<Map<String, Object>> status = List.of(Map.of("status", "接诊中", "count", 2L));
        List<Map<String, Object>> urgency = List.of(Map.of("urgency", "普通", "count", 2L));
        List<Map<String, Object>> trend = List.of(Map.of("month", "2026-06", "count", 2L));
        List<Map<String, Object>> weekly =
                List.of(Map.of("period", "2026-06-16", "count", 2L));
        when(consultationMapper.countByStatusForDoctor(6L)).thenReturn(status);
        when(consultationMapper.countByUrgencyForDoctor(6L)).thenReturn(urgency);
        when(consultationMapper.trendLast6MonthsForDoctor(6L)).thenReturn(trend);
        when(consultationMapper.trendLast4WeeksForDoctor(6L)).thenReturn(weekly);
        DashboardService service = service(
                consultationMapper, accountMapper, knowledgeArticleMapper, recipeMapper
        );

        DashboardSummary summary = service.getDoctorSummary(6L);

        assertThat(summary.scope()).isEqualTo("doctor");
        assertThat(summary.statusDistribution()).isEqualTo(status);
        assertThat(summary.metrics()).containsEntry("assignedTotal", 2L);
        assertThat(summary.departmentDistribution()).isEmpty();
        assertThat(summary.doctorWorkloads()).isEmpty();
        assertThat(service.getTrend("week", 6L)).isEqualTo(weekly);
        verify(consultationMapper).countByStatusForDoctor(6L);
        verify(consultationMapper).trendLast4WeeksForDoctor(6L);
    }

    @Test
    void administratorTrendUsesPlatformAggregation() {
        ConsultationMapper consultationMapper = mock(ConsultationMapper.class);
        List<Map<String, Object>> monthly =
                List.of(Map.of("period", "2026-06", "count", 11L));
        when(consultationMapper.trendLast6MonthsByPeriod()).thenReturn(monthly);
        DashboardService service = service(
                consultationMapper,
                mock(AccountMapper.class),
                mock(KnowledgeArticleMapper.class),
                mock(RecipeMapper.class)
        );

        assertThat(service.getTrend("month", null)).isEqualTo(monthly);
        verify(consultationMapper).trendLast6MonthsByPeriod();
    }

    private DashboardService service(
            ConsultationMapper consultationMapper,
            AccountMapper accountMapper,
            KnowledgeArticleMapper knowledgeArticleMapper,
            RecipeMapper recipeMapper
    ) {
        return new DashboardService(
                consultationMapper,
                accountMapper,
                knowledgeArticleMapper,
                recipeMapper
        );
    }
}
