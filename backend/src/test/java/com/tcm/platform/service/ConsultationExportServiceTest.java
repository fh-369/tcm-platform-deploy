package com.tcm.platform.service;

import com.tcm.platform.dto.ConsultationExportFilter;
import com.tcm.platform.dto.ConsultationExportRecord;
import com.tcm.platform.mapper.ConsultationMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultationExportServiceTest {

    @Test
    void countAndExportUseTheSameCombinedFilter() {
        ConsultationMapper consultationMapper = mock(ConsultationMapper.class);
        ConsultationExportFilter filter = new ConsultationExportFilter();
        filter.setDateFrom(LocalDate.of(2026, 6, 1));
        filter.setDateTo(LocalDate.of(2026, 6, 22));
        filter.setStatus("接诊中");
        filter.setUrgency("紧急");
        filter.setDepartmentId(2L);
        filter.setDoctorId(6L);
        ConsultationExportRecord record = new ConsultationExportRecord();
        record.setId(7L);
        record.setPatientName("李女士");
        record.setDepartmentName("中医内科");
        record.setDoctorName("李医生");
        record.setStatus("接诊中");
        record.setUrgency("紧急");
        record.setAssignedAt(LocalDateTime.of(2026, 6, 10, 8, 0));
        when(consultationMapper.countConsultationExports(filter)).thenReturn(1L);
        when(consultationMapper.selectConsultationExports(filter)).thenReturn(List.of(record));
        ConsultationExportService service = new ConsultationExportService(consultationMapper);

        assertThat(service.count(filter)).isEqualTo(1L);
        String content = new String(service.exportCsv(filter), StandardCharsets.UTF_8);

        assertThat(content).contains(
                "科室",
                "接诊医生",
                "分配时间",
                "中医内科",
                "李医生",
                "2026-06-10 08:00:00"
        );
        verify(consultationMapper).countConsultationExports(filter);
        verify(consultationMapper).selectConsultationExports(filter);
    }

    @Test
    void rejectsAnInvalidDateRange() {
        ConsultationExportService service =
                new ConsultationExportService(mock(ConsultationMapper.class));
        ConsultationExportFilter filter = new ConsultationExportFilter();
        filter.setDateFrom(LocalDate.of(2026, 6, 22));
        filter.setDateTo(LocalDate.of(2026, 6, 1));

        assertThatThrownBy(() -> service.count(filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("开始日期不能晚于结束日期");
    }

    @Test
    void doesNotGenerateAnEmptyCsv() {
        ConsultationMapper consultationMapper = mock(ConsultationMapper.class);
        ConsultationExportFilter filter = new ConsultationExportFilter();
        when(consultationMapper.selectConsultationExports(filter)).thenReturn(List.of());
        ConsultationExportService service = new ConsultationExportService(consultationMapper);

        assertThatThrownBy(() -> service.exportCsv(filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("当前筛选条件下没有可导出的问诊记录");
    }
}
