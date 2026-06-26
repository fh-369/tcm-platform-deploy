package com.tcm.platform.service;

import com.tcm.platform.dto.AIAnswerResponse;
import com.tcm.platform.dto.AIQuestionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class AIServiceTest {

    @Test
    void answerRejectsBlankQuestion() {
        AIService service = service(mock(DashScopeClient.class), "", mock(AIContextService.class));

        assertThatThrownBy(() -> service.answer("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("问题不能为空");
    }

    @Test
    void answerUsesFallbackWhenApiKeyIsMissing() {
        DashScopeClient dashScopeClient = mock(DashScopeClient.class);
        AIContextService aiContextService = mock(AIContextService.class);
        AIService service = service(dashScopeClient, "", aiContextService);

        AIAnswerResponse response = service.answer("春季容易困倦如何调养？");

        assertThat(response.fallback()).isTrue();
        assertThat(response.answer()).contains("规律作息", "及时就医");
        assertThat(response.disclaimer()).contains("不能替代医生");
        verifyNoInteractions(dashScopeClient);
    }

    @Test
    void answerReturnsDashScopeAnswerWhenCallSucceeds() {
        DashScopeClient dashScopeClient = mock(DashScopeClient.class);
        AIContextService aiContextService = mock(AIContextService.class);
        when(aiContextService.enrichContext("春季容易困倦如何调养？", List.of(), null, null)).thenReturn(List.of());
        when(dashScopeClient.ask("test-key", "春季容易困倦如何调养？", List.of()))
                .thenReturn("建议早睡早起，并适量运动。");
        AIService service = service(dashScopeClient, "test-key", aiContextService);

        AIAnswerResponse response = service.answer("春季容易困倦如何调养？");

        assertThat(response.answer()).isEqualTo("建议早睡早起，并适量运动。");
        assertThat(response.fallback()).isFalse();
        assertThat(response.disclaimer()).contains("不能替代医生");
    }

    @Test
    void answerPassesRecentContextToDashScope() {
        DashScopeClient dashScopeClient = mock(DashScopeClient.class);
        AIContextService aiContextService = mock(AIContextService.class);
        List<AIQuestionRequest.ContextMessage> context = List.of(
                new AIQuestionRequest.ContextMessage("user", "我最近下午容易疲倦"),
                new AIQuestionRequest.ContextMessage("assistant", "可以先观察作息和饮食")
        );
        when(aiContextService.enrichContext("那晚饭要注意什么？", context, null, null)).thenReturn(context);
        when(dashScopeClient.ask("test-key", "那晚饭要注意什么？", context))
                .thenReturn("晚饭建议清淡适量，避免过晚。");
        AIService service = service(dashScopeClient, "test-key", aiContextService);

        AIAnswerResponse response = service.answer("那晚饭要注意什么？", context);

        assertThat(response.answer()).contains("晚饭建议");
        verify(dashScopeClient).ask("test-key", "那晚饭要注意什么？", context);
    }

    @Test
    void answerUsesFallbackWhenDashScopeCallFails() {
        DashScopeClient dashScopeClient = mock(DashScopeClient.class);
        AIContextService aiContextService = mock(AIContextService.class);
        when(aiContextService.enrichContext("最近胃部不适怎么办？", List.of(), null, null)).thenReturn(List.of());
        when(dashScopeClient.ask("test-key", "最近胃部不适怎么办？", List.of()))
                .thenThrow(new IllegalStateException("external service unavailable"));
        AIService service = service(dashScopeClient, "test-key", aiContextService);

        AIAnswerResponse response = service.answer("最近胃部不适怎么办？");

        assertThat(response.fallback()).isTrue();
        assertThat(response.answer()).contains("及时就医");
    }

    @Test
    void streamAnswerUsesEnrichedContextAndEmitsChunks() {
        DashScopeClient dashScopeClient = mock(DashScopeClient.class);
        AIContextService aiContextService = mock(AIContextService.class);
        List<AIQuestionRequest.ContextMessage> context = List.of(
                new AIQuestionRequest.ContextMessage("user", "我想结合问诊单")
        );
        when(aiContextService.enrichContext("胃口不好怎么调养？", context, 7L, 12L)).thenReturn(context);
        org.mockito.Mockito.doAnswer(invocation -> {
            java.util.function.Consumer<String> consumer = invocation.getArgument(3);
            consumer.accept("建议先");
            consumer.accept("清淡饮食。");
            return null;
        }).when(dashScopeClient).askStream(
                eq("test-key"),
                eq("胃口不好怎么调养？"),
                eq(context),
                any()
        );
        AIService service = service(dashScopeClient, "test-key", aiContextService);
        List<String> chunks = new ArrayList<>();

        service.streamAnswer("胃口不好怎么调养？", context, 7L, 12L, chunks::add);

        assertThat(chunks).containsExactly("建议先", "清淡饮食。");
        verify(aiContextService).enrichContext("胃口不好怎么调养？", context, 7L, 12L);
    }

    @Test
    void returnsPlatformRecommendationsSeparatelyFromModelContext() {
        DashScopeClient dashScopeClient = mock(DashScopeClient.class);
        AIContextService aiContextService = mock(AIContextService.class);
        var recommendation = new com.tcm.platform.dto.AIContentRecommendation(
                6L,
                "knowledge",
                "一餐如何吃得更均衡",
                "从食物种类与比例开始调整。"
        );
        when(aiContextService.findRecommendations("晚饭怎么吃？", 7L, 12L)).thenReturn(List.of(recommendation));
        AIService service = service(dashScopeClient, "test-key", aiContextService);

        var result = service.findRecommendations(" 晚饭怎么吃？ ", 7L, 12L);

        assertThat(result).containsExactly(recommendation);
        verify(aiContextService).findRecommendations("晚饭怎么吃？", 7L, 12L);
        verifyNoInteractions(dashScopeClient);
    }

    private AIService service(DashScopeClient dashScopeClient, String apiKey, AIContextService aiContextService) {
        return new AIService(dashScopeClient, aiContextService, apiKey);
    }
}
