package com.tcm.platform.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tcm.platform.common.Result;
import com.tcm.platform.dto.ConsultationRequest;
import com.tcm.platform.dto.ConsultationMessageRequest;
import com.tcm.platform.entity.Consultation;
import com.tcm.platform.entity.ConsultationMessage;
import com.tcm.platform.entity.KnowledgeArticle;
import com.tcm.platform.entity.PatientAccount;
import com.tcm.platform.entity.Recipe;
import com.tcm.platform.mapper.PatientAccountMapper;
import com.tcm.platform.service.ConsultationService;
import com.tcm.platform.service.ConsultationMessageService;
import com.tcm.platform.service.RecipeService;
import com.tcm.platform.service.KnowledgeArticleService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 患者提交问诊、查看个人问诊及浏览已发布内容的接口。
 */
@RestController
@RequestMapping("/api/patient")
public class PatientController {

    private final ConsultationService consultationService;
    private final KnowledgeArticleService knowledgeArticleService;
    private final PatientAccountMapper patientAccountMapper;
    private final RecipeService recipeService;
    private final ConsultationMessageService consultationMessageService;

    public PatientController(
            ConsultationService consultationService,
            KnowledgeArticleService knowledgeArticleService,
            PatientAccountMapper patientAccountMapper,
            RecipeService recipeService,
            ConsultationMessageService consultationMessageService
    ) {
        this.consultationService = consultationService;
        this.knowledgeArticleService = knowledgeArticleService;
        this.patientAccountMapper = patientAccountMapper;
        this.recipeService = recipeService;
        this.consultationMessageService = consultationMessageService;
    }

    @PostMapping("/consultation")
    public Result<Consultation> createConsultation(
            Authentication authentication,
            @Valid @RequestBody ConsultationRequest request
    ) {
        PatientAccount patient = currentPatient(authentication);
        request.setPatientAccountId(patient.getId());
        return Result.success("问诊提交成功", consultationService.createConsultation(request));
    }

    @GetMapping("/consultation/my")
    public Result<Page<Consultation>> listMyConsultations(
            Authentication authentication,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String urgency
    ) {
        PatientAccount patient = currentPatient(authentication);
        return Result.success(
                consultationService.listConsultations(current, size, status, urgency, patient.getId(), null)
        );
    }

    @GetMapping("/consultation/{id}/messages")
    public Result<List<ConsultationMessage>> listConsultationMessages(
            @PathVariable Long id,
            Authentication authentication
    ) {
        PatientAccount patient = currentPatient(authentication);
        return Result.success(
                consultationMessageService.listForPatient(id, patient.getId())
        );
    }

    @PostMapping("/consultation/{id}/messages")
    public Result<ConsultationMessage> sendConsultationMessage(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody ConsultationMessageRequest request
    ) {
        PatientAccount patient = currentPatient(authentication);
        return Result.success(
                "回复发送成功",
                consultationMessageService.sendAsPatient(
                        id,
                        patient.getId(),
                        displayName(patient),
                        request
                )
        );
    }

    @GetMapping("/knowledge")
    public Result<Page<KnowledgeArticle>> listPublishedKnowledge(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "6") long size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        return Result.success(knowledgeArticleService.listPublishedArticles(current, size, category, keyword));
    }

    @GetMapping("/knowledge/categories")
    public Result<List<String>> listPublishedKnowledgeCategories() {
        return Result.success(knowledgeArticleService.listPublishedCategories());
    }

    @GetMapping("/knowledge/{id}")
    public Result<KnowledgeArticle> getPublishedKnowledge(@PathVariable Long id) {
        return Result.success(knowledgeArticleService.getPublishedArticle(id));
    }

    @GetMapping("/recipe")
    public Result<List<Recipe>> listPublishedRecipes() {
        return Result.success(recipeService.listPublishedRecipes());
    }

    @GetMapping("/recipe/{id}")
    public Result<Recipe> getPublishedRecipe(@PathVariable Long id) {
        return Result.success(recipeService.getPublishedRecipe(id));
    }

    private PatientAccount currentPatient(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("患者未登录");
        }

        PatientAccount patient = patientAccountMapper.selectOne(
                Wrappers.<PatientAccount>lambdaQuery()
                        .eq(PatientAccount::getUsername, authentication.getName())
        );
        if (patient == null) {
            throw new IllegalArgumentException("当前登录账号不是患者账号");
        }
        return patient;
    }

    private String displayName(PatientAccount patient) {
        return patient.getDisplayName() == null || patient.getDisplayName().isBlank()
                ? patient.getUsername()
                : patient.getDisplayName();
    }
}
