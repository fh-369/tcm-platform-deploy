package com.tcm.platform.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tcm.platform.common.Result;
import com.tcm.platform.dto.AIAnswerResponse;
import com.tcm.platform.dto.AIContentRecommendation;
import com.tcm.platform.dto.AIConversationCreateRequest;
import com.tcm.platform.dto.AIConversationImportRequest;
import com.tcm.platform.dto.AIConversationResponse;
import com.tcm.platform.dto.AIQuestionRequest;
import com.tcm.platform.entity.PatientAccount;
import com.tcm.platform.mapper.PatientAccountMapper;
import com.tcm.platform.service.AIService;
import com.tcm.platform.service.AIConversationService;
import jakarta.validation.Valid;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 患者 AI 养生问答接口。
 */
@RestController
@RequestMapping("/api/patient/ai")
public class AIController {

    private static final String DISCLAIMER =
            "本回答仅供一般养生参考，不能替代医生诊断和治疗。";

    private final AIService aiService;
    private final AIConversationService conversationService;
    private final PatientAccountMapper patientAccountMapper;

    public AIController(
            AIService aiService,
            AIConversationService conversationService,
            PatientAccountMapper patientAccountMapper
    ) {
        this.aiService = aiService;
        this.conversationService = conversationService;
        this.patientAccountMapper = patientAccountMapper;
    }

    @PostMapping("/question")
    public Result<AIAnswerResponse> answer(Authentication authentication, @Valid @RequestBody AIQuestionRequest request) {
        PatientAccount patient = currentPatient(authentication);
        List<AIQuestionRequest.ContextMessage> context =
                request.getConversationId() == null
                        ? request.getContext()
                        : conversationService.buildContext(
                                request.getConversationId(),
                                patient.getId()
                        );
        if (request.getConversationId() != null) {
            conversationService.appendMessage(
                    request.getConversationId(),
                    patient.getId(),
                    "user",
                    request.getQuestion(),
                    false,
                    ""
            );
            conversationService.initializeRecommendations(
                    request.getConversationId(),
                    patient.getId(),
                    request.getQuestion(),
                    request.getConsultationId()
            );
        }
        AIAnswerResponse response = aiService.answer(
                request.getQuestion(),
                context,
                patient.getId(),
                request.getConsultationId()
        );
        if (request.getConversationId() != null) {
            conversationService.appendMessage(
                    request.getConversationId(),
                    patient.getId(),
                    "assistant",
                    response.answer(),
                    response.fallback(),
                    response.disclaimer()
            );
        }
        return Result.success(response);
    }

    @PostMapping(value = "/question/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<StreamingResponseBody> streamAnswer(
            Authentication authentication,
            @Valid @RequestBody AIQuestionRequest request
    ) {
        PatientAccount patient = currentPatient(authentication);
        if (request.getConversationId() == null) {
            throw new IllegalArgumentException("请先创建 AI 对话");
        }
        List<AIQuestionRequest.ContextMessage> context =
                conversationService.buildContext(
                        request.getConversationId(),
                        patient.getId()
                );
        conversationService.appendMessage(
                request.getConversationId(),
                patient.getId(),
                "user",
                request.getQuestion(),
                false,
                ""
        );
        conversationService.initializeRecommendations(
                request.getConversationId(),
                patient.getId(),
                request.getQuestion(),
                request.getConsultationId()
        );
        StreamingResponseBody body = outputStream -> {
            StringBuilder answer = new StringBuilder();
            try {
                aiService.streamAnswer(
                        request.getQuestion(),
                        context,
                        patient.getId(),
                        request.getConsultationId(),
                        chunk -> {
                            try {
                                answer.append(chunk);
                                outputStream.write(chunk.getBytes(StandardCharsets.UTF_8));
                                outputStream.flush();
                            } catch (java.io.IOException ex) {
                                throw new IllegalStateException("AI 流式响应写入失败", ex);
                            }
                        }
                );
            } finally {
                if (!answer.isEmpty()) {
                    conversationService.appendMessage(
                            request.getConversationId(),
                            patient.getId(),
                            "assistant",
                            answer.toString(),
                            false,
                            DISCLAIMER
                    );
                }
            }
        };
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(body);
    }

    @PostMapping("/recommendations")
    public Result<List<AIContentRecommendation>> recommendations(
            Authentication authentication,
            @Valid @RequestBody AIQuestionRequest request
    ) {
        PatientAccount patient = currentPatient(authentication);
        if (request.getConversationId() != null) {
            return Result.success(conversationService.initializeRecommendations(
                    request.getConversationId(),
                    patient.getId(),
                    request.getQuestion(),
                    request.getConsultationId()
            ));
        }
        return Result.success(aiService.findRecommendations(
                request.getQuestion(),
                patient.getId(),
                request.getConsultationId()
        ));
    }

    @GetMapping("/conversations")
    public Result<Page<AIConversationResponse>> conversations(
            Authentication authentication,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size
    ) {
        PatientAccount patient = currentPatient(authentication);
        return Result.success(conversationService.listConversations(
                patient.getId(),
                current,
                size
        ));
    }

    @PostMapping("/conversations")
    public Result<AIConversationResponse> createConversation(
            Authentication authentication,
            @Valid @RequestBody AIConversationCreateRequest request
    ) {
        PatientAccount patient = currentPatient(authentication);
        return Result.success(conversationService.createConversation(
                patient.getId(),
                request.getTitle(),
                request.getConsultationId(),
                request.getLegacyKey()
        ));
    }

    @GetMapping("/conversations/{id}")
    public Result<AIConversationResponse> conversation(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long messageCurrent,
            @RequestParam(defaultValue = "30") long messageSize
    ) {
        PatientAccount patient = currentPatient(authentication);
        return Result.success(conversationService.getConversation(
                id,
                patient.getId(),
                messageCurrent,
                messageSize
        ));
    }

    @DeleteMapping("/conversations/{id}")
    public Result<Void> deleteConversation(
            Authentication authentication,
            @PathVariable Long id
    ) {
        PatientAccount patient = currentPatient(authentication);
        conversationService.deleteConversation(id, patient.getId());
        return Result.success(null);
    }

    @PostMapping("/conversations/{id}/legacy-content")
    public Result<AIConversationResponse> importLegacyContent(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AIConversationImportRequest request
    ) {
        PatientAccount patient = currentPatient(authentication);
        return Result.success(conversationService.importLegacyContent(
                id,
                patient.getId(),
                request
        ));
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
}
