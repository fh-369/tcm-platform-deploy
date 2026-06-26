package com.tcm.platform.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tcm.platform.common.Result;
import com.tcm.platform.dto.ConsultationAssignmentRequest;
import com.tcm.platform.dto.ConsultationDepartmentUpdateRequest;
import com.tcm.platform.dto.ConsultationUpdateRequest;
import com.tcm.platform.dto.ConsultationWorkspaceRecord;
import com.tcm.platform.dto.KnowledgeArticleAdminRequest;
import com.tcm.platform.dto.PublicationRequest;
import com.tcm.platform.entity.Consultation;
import com.tcm.platform.entity.KnowledgeArticle;
import com.tcm.platform.entity.User;
import com.tcm.platform.mapper.UserMapper;
import com.tcm.platform.service.ConsultationService;
import com.tcm.platform.service.ConsultationWorkspaceService;
import com.tcm.platform.service.KnowledgeArticleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 医生和管理员查询、处理问诊单的接口。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ConsultationService consultationService;
    private final ConsultationWorkspaceService consultationWorkspaceService;
    private final KnowledgeArticleService knowledgeArticleService;
    private final UserMapper userMapper;

    public AdminController(
            ConsultationService consultationService,
            ConsultationWorkspaceService consultationWorkspaceService,
            KnowledgeArticleService knowledgeArticleService,
            UserMapper userMapper
    ) {
        this.consultationService = consultationService;
        this.consultationWorkspaceService = consultationWorkspaceService;
        this.knowledgeArticleService = knowledgeArticleService;
        this.userMapper = userMapper;
    }

    @GetMapping("/consultation")
    public Result<Page<ConsultationWorkspaceRecord>> listConsultations(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String urgency,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) Long departmentId
    ) {
        return Result.success(
                consultationWorkspaceService.listForAdmin(
                        current, size, status, urgency, keyword, doctorId, unassigned, departmentId
                )
        );
    }

    @PutMapping("/consultation/{id}")
    public Result<Consultation> updateConsultation(
            @PathVariable Long id,
            @RequestBody ConsultationUpdateRequest request
    ) {
        request.setDoctorId(null);
        return Result.success("问诊更新成功", consultationService.updateConsultation(id, request));
    }

    @PutMapping("/consultation/{id}/assignment")
    public Result<Consultation> assignConsultation(
            @PathVariable Long id,
            @RequestBody ConsultationAssignmentRequest request
    ) {
        return Result.success(
                request.getDoctorId() == null ? "已取消问诊分配" : "问诊分配成功",
                consultationWorkspaceService.assign(id, request.getDoctorId())
        );
    }

    @PutMapping("/consultation/{id}/claim")
    public Result<Consultation> claimConsultation(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User doctor = currentUser(authentication);
        return Result.success(
                "问诊认领成功",
                consultationWorkspaceService.claim(id, doctor.getId())
        );
    }

    @PutMapping("/consultation/{id}/department")
    public Result<Consultation> updateConsultationDepartment(
            @PathVariable Long id,
            @Valid @RequestBody ConsultationDepartmentUpdateRequest request
    ) {
        return Result.success(
                "问诊科室已更新",
                consultationWorkspaceService.updateDepartment(id, request.getDepartmentId())
        );
    }

    @GetMapping("/knowledge")
    public Result<Page<KnowledgeArticle>> listKnowledgeArticles(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) String keyword
    ) {
        return Result.success(
                knowledgeArticleService.listArticles(current, size, category, published, keyword)
        );
    }

    @GetMapping("/knowledge/{id}")
    public Result<KnowledgeArticle> getKnowledgeArticle(@PathVariable Long id) {
        return Result.success(knowledgeArticleService.getArticle(id));
    }

    @PostMapping("/knowledge")
    public Result<KnowledgeArticle> createKnowledgeArticle(
            @Valid @RequestBody KnowledgeArticleAdminRequest request
    ) {
        return Result.success("知识文章创建成功", knowledgeArticleService.createArticle(request.toEntity()));
    }

    @PutMapping("/knowledge/{id}")
    public Result<KnowledgeArticle> updateKnowledgeArticle(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeArticleAdminRequest request
    ) {
        return Result.success("知识文章更新成功", knowledgeArticleService.updateArticle(id, request.toEntity()));
    }

    @PutMapping("/knowledge/{id}/publication")
    public Result<KnowledgeArticle> updateKnowledgePublication(
            @PathVariable Long id,
            @Valid @RequestBody PublicationRequest request
    ) {
        return Result.success(
                Boolean.TRUE.equals(request.published()) ? "文章已发布" : "文章已取消发布",
                knowledgeArticleService.updatePublication(id, request.published())
        );
    }

    @DeleteMapping("/knowledge/{id}")
    public Result<Void> deleteKnowledgeArticle(@PathVariable Long id) {
        knowledgeArticleService.deleteArticle(id);
        return Result.success("知识文章删除成功", null);
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("后台用户未登录");
        }

        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, authentication.getName())
        );
        if (user == null) {
            throw new IllegalArgumentException("当前登录账号不是后台账号");
        }
        return user;
    }
}
