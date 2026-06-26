package com.tcm.platform.service;

import com.tcm.platform.dto.AIAnswerResponse;
import com.tcm.platform.dto.AIContentRecommendation;
import com.tcm.platform.dto.AIQuestionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

/**
 * AI 养生问答业务，外部服务不可用时提供保守的 fallback 回答。
 */
@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    private static final String DISCLAIMER = "本回答仅供一般养生参考，不能替代医生诊断和治疗。";
    private static final String FALLBACK_ANSWER =
            "暂时无法获取智能回答。建议保持规律作息、均衡饮食和适量运动；"
                    + "如症状严重、持续不缓解或出现明显不适，请及时就医。";

    private final DashScopeClient dashScopeClient;
    private final AIContextService aiContextService;
    private final String apiKey;

    public AIService(
            DashScopeClient dashScopeClient,
            AIContextService aiContextService,
            @Value("${ai.dashscope.api-key:}") String apiKey
    ) {
        this.dashScopeClient = dashScopeClient;
        this.aiContextService = aiContextService;
        this.apiKey = apiKey;
    }

    public AIAnswerResponse answer(String question) {
        return answer(question, List.of());
    }

    public AIAnswerResponse answer(String question, List<AIQuestionRequest.ContextMessage> context) {
        return answer(question, context, null, null);
    }

    public AIAnswerResponse answer(
            String question,
            List<AIQuestionRequest.ContextMessage> context,
            Long patientAccountId,
            Long consultationId
    ) {
        if (!hasText(question)) {
            throw new IllegalArgumentException("问题不能为空");
        }
        if (!hasText(apiKey)) {
            return fallback();
        }

        try {
            List<AIQuestionRequest.ContextMessage> enrichedContext =
                    aiContextService.enrichContext(question.trim(), safeContext(context), patientAccountId, consultationId);
            return new AIAnswerResponse(dashScopeClient.ask(apiKey, question.trim(), enrichedContext), false, DISCLAIMER);
        } catch (RuntimeException ex) {
            log.warn("DashScope AI answer failed: {}", ex.getMessage());
            return fallback();
        }
    }

    public void streamAnswer(
            String question,
            List<AIQuestionRequest.ContextMessage> context,
            Long patientAccountId,
            Long consultationId,
            Consumer<String> chunkConsumer
    ) {
        if (!hasText(question)) {
            throw new IllegalArgumentException("问题不能为空");
        }
        if (!hasText(apiKey)) {
            chunkConsumer.accept(FALLBACK_ANSWER);
            return;
        }

        try {
            List<AIQuestionRequest.ContextMessage> enrichedContext =
                    aiContextService.enrichContext(question.trim(), safeContext(context), patientAccountId, consultationId);
            dashScopeClient.askStream(apiKey, question.trim(), enrichedContext, chunkConsumer);
        } catch (RuntimeException ex) {
            log.warn("DashScope AI answer failed: {}", ex.getMessage());
            chunkConsumer.accept(FALLBACK_ANSWER);
        }
    }

    public List<AIContentRecommendation> findRecommendations(
            String question,
            Long patientAccountId,
            Long consultationId
    ) {
        if (!hasText(question)) {
            throw new IllegalArgumentException("问题不能为空");
        }
        return aiContextService.findRecommendations(
                question.trim(),
                patientAccountId,
                consultationId
        );
    }

    private List<AIQuestionRequest.ContextMessage> safeContext(List<AIQuestionRequest.ContextMessage> context) {
        return context == null ? List.of() : context;
    }

    private AIAnswerResponse fallback() {
        return new AIAnswerResponse(FALLBACK_ANSWER, true, DISCLAIMER);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
