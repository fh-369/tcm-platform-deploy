package com.tcm.platform.service;

import com.opencsv.CSVWriter;
import com.tcm.platform.dto.ConsultationExportFilter;
import com.tcm.platform.dto.ConsultationExportRecord;
import com.tcm.platform.mapper.ConsultationMapper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
public class ConsultationExportService {

    private static final byte[] UTF_8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> STATUSES = Set.of("待接诊", "接诊中", "已完成");
    private static final Set<String> URGENCIES = Set.of("普通", "紧急", "非常紧急");
    private static final String[] HEADER = {
            "问诊ID", "患者姓名", "年龄", "性别", "联系电话", "科室", "接诊医生",
            "症状描述", "持续时间", "过敏史", "紧急度", "状态", "医生备注",
            "分配时间", "创建时间", "更新时间"
    };

    private final ConsultationMapper consultationMapper;

    public ConsultationExportService(ConsultationMapper consultationMapper) {
        this.consultationMapper = consultationMapper;
    }

    public long count(ConsultationExportFilter filter) {
        ConsultationExportFilter safeFilter = validate(filter);
        return consultationMapper.countConsultationExports(safeFilter);
    }

    public byte[] exportCsv(ConsultationExportFilter filter) {
        ConsultationExportFilter safeFilter = validate(filter);
        List<ConsultationExportRecord> records =
                consultationMapper.selectConsultationExports(safeFilter);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("当前筛选条件下没有可导出的问诊记录");
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            output.write(UTF_8_BOM);
            try (CSVWriter writer = new CSVWriter(
                    new OutputStreamWriter(output, StandardCharsets.UTF_8)
            )) {
                writer.writeNext(HEADER);
                records.forEach(record -> writer.writeNext(toRow(record)));
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("导出问诊 CSV 失败", exception);
        }
    }

    private ConsultationExportFilter validate(ConsultationExportFilter filter) {
        ConsultationExportFilter safeFilter =
                filter == null ? new ConsultationExportFilter() : filter;
        if (safeFilter.getDateFrom() != null
                && safeFilter.getDateTo() != null
                && safeFilter.getDateFrom().isAfter(safeFilter.getDateTo())) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
        if (hasText(safeFilter.getStatus()) && !STATUSES.contains(safeFilter.getStatus())) {
            throw new IllegalArgumentException("问诊状态筛选值无效");
        }
        if (hasText(safeFilter.getUrgency()) && !URGENCIES.contains(safeFilter.getUrgency())) {
            throw new IllegalArgumentException("紧急程度筛选值无效");
        }
        return safeFilter;
    }

    private String[] toRow(ConsultationExportRecord record) {
        return new String[]{
                value(record.getId()),
                value(record.getPatientName()),
                value(record.getAge()),
                value(record.getGender()),
                value(record.getPhone()),
                value(record.getDepartmentName()),
                value(record.getDoctorName()),
                value(record.getSymptoms()),
                value(record.getDuration()),
                value(record.getAllergyHistory()),
                value(record.getUrgency()),
                value(record.getStatus()),
                value(record.getDoctorNote()),
                formatDateTime(record.getAssignedAt()),
                formatDateTime(record.getCreatedAt()),
                formatDateTime(record.getUpdatedAt())
        };
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
