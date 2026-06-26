package com.tcm.platform.controller;

import com.tcm.platform.common.Result;
import com.tcm.platform.dto.DashboardSummary;
import com.tcm.platform.dto.ConsultationExportFilter;
import com.tcm.platform.entity.User;
import com.tcm.platform.mapper.UserMapper;
import com.tcm.platform.service.ConsultationExportService;
import com.tcm.platform.service.DashboardService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import java.util.List;
import java.util.Map;

/**
 * 后台 Dashboard 统计与问诊导出接口。
 */
@RestController
@RequestMapping("/api/admin")
public class DashboardController {

    private static final MediaType CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv;charset=UTF-8");

    private final DashboardService dashboardService;
    private final ConsultationExportService consultationExportService;
    private final UserMapper userMapper;

    public DashboardController(
            DashboardService dashboardService,
            ConsultationExportService consultationExportService,
            UserMapper userMapper
    ) {
        this.dashboardService = dashboardService;
        this.consultationExportService = consultationExportService;
        this.userMapper = userMapper;
    }

    @GetMapping("/dashboard")
    public Result<DashboardSummary> getDashboard(Authentication authentication) {
        Long doctorId = currentDoctorId(authentication);
        return Result.success(
                doctorId == null
                        ? dashboardService.getAdminSummary()
                        : dashboardService.getDoctorSummary(doctorId)
        );
    }

    @GetMapping("/dashboard/trend")
    public Result<List<Map<String, Object>>> getDashboardTrend(
            @RequestParam(defaultValue = "month") String period
            , Authentication authentication
    ) {
        return Result.success(dashboardService.getTrend(period, currentDoctorId(authentication)));
    }

    @GetMapping("/export/consultations/count")
    public Result<Long> countExportConsultations(ConsultationExportFilter filter) {
        return Result.success(consultationExportService.count(filter));
    }

    @GetMapping("/export/consultations")
    public ResponseEntity<byte[]> exportConsultations(ConsultationExportFilter filter) {
        String filename = exportFilename(filter);
        return ResponseEntity.ok()
                .contentType(CSV_MEDIA_TYPE)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .body(consultationExportService.exportCsv(filter));
    }

    private Long currentDoctorId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        boolean doctor = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_DOCTOR".equals(authority.getAuthority()));
        if (!doctor) {
            return null;
        }

        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, authentication.getName())
        );
        if (user == null || !"doctor".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("当前医生账号不存在");
        }
        return user.getId();
    }

    private String exportFilename(ConsultationExportFilter filter) {
        String from = filter != null && filter.getDateFrom() != null
                ? filter.getDateFrom().toString()
                : "all";
        String to = filter != null && filter.getDateTo() != null
                ? filter.getDateTo().toString()
                : "all";
        return "consultations-" + from + "-to-" + to + ".csv";
    }
}
