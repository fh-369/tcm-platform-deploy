package com.tcm.platform.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tcm.platform.common.Result;
import com.tcm.platform.dto.ConsultationUpdateRequest;
import com.tcm.platform.dto.ConsultationMessageRequest;
import com.tcm.platform.dto.ConsultationWorkspaceRecord;
import com.tcm.platform.entity.Consultation;
import com.tcm.platform.entity.ConsultationMessage;
import com.tcm.platform.entity.User;
import com.tcm.platform.mapper.UserMapper;
import com.tcm.platform.service.ConsultationWorkspaceService;
import com.tcm.platform.service.ConsultationMessageService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctor/consultations")
public class DoctorConsultationController {

    private final ConsultationWorkspaceService consultationWorkspaceService;
    private final ConsultationMessageService consultationMessageService;
    private final UserMapper userMapper;

    public DoctorConsultationController(
            ConsultationWorkspaceService consultationWorkspaceService,
            ConsultationMessageService consultationMessageService,
            UserMapper userMapper
    ) {
        this.consultationWorkspaceService = consultationWorkspaceService;
        this.consultationMessageService = consultationMessageService;
        this.userMapper = userMapper;
    }

    @GetMapping("/pool")
    public Result<Page<ConsultationWorkspaceRecord>> listDepartmentPool(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String urgency,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String scope,
            Authentication authentication
    ) {
        User doctor = currentDoctor(authentication);
        return Result.success(
                consultationWorkspaceService.listDepartmentPool(
                        current, size, urgency, keyword, scope, doctor.getId()
                )
        );
    }

    @GetMapping("/mine")
    public Result<Page<ConsultationWorkspaceRecord>> listMine(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String urgency,
            @RequestParam(required = false) String keyword,
            Authentication authentication
    ) {
        User doctor = currentDoctor(authentication);
        return Result.success(
                consultationWorkspaceService.listMine(
                        current, size, status, urgency, keyword, doctor.getId()
                )
        );
    }

    @PutMapping("/{id}/claim")
    public Result<Consultation> claim(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User doctor = currentDoctor(authentication);
        return Result.success(
                "问诊认领成功",
                consultationWorkspaceService.claim(id, doctor.getId())
        );
    }

    @PutMapping("/{id}")
    public Result<Consultation> update(
            @PathVariable Long id,
            @RequestBody ConsultationUpdateRequest request,
            Authentication authentication
    ) {
        User doctor = currentDoctor(authentication);
        request.setDoctorId(null);
        return Result.success(
                "问诊更新成功",
                consultationWorkspaceService.updateAsDoctor(id, request, doctor.getId())
        );
    }

    @GetMapping("/{id}/messages")
    public Result<java.util.List<ConsultationMessage>> listMessages(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User doctor = currentDoctor(authentication);
        return Result.success(
                consultationMessageService.listForDoctor(id, doctor.getId())
        );
    }

    @PostMapping("/{id}/messages")
    public Result<ConsultationMessage> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody ConsultationMessageRequest request,
            Authentication authentication
    ) {
        User doctor = currentDoctor(authentication);
        return Result.success(
                "回复发送成功",
                consultationMessageService.sendAsDoctor(
                        id,
                        doctor.getId(),
                        displayName(doctor),
                        request
                )
        );
    }

    private User currentDoctor(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("医生账号未登录");
        }
        User doctor = userMapper.selectOne(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, authentication.getName())
                        .eq(User::getRole, "doctor")
        );
        if (doctor == null) {
            throw new IllegalArgumentException("当前登录账号不是医生");
        }
        return doctor;
    }

    private String displayName(User doctor) {
        return doctor.getDisplayName() == null || doctor.getDisplayName().isBlank()
                ? doctor.getUsername()
                : doctor.getDisplayName();
    }
}
