package com.tcm.platform.service;

import com.tcm.platform.dto.AIContentRecommendation;
import com.tcm.platform.dto.AIQuestionRequest;
import com.tcm.platform.dto.AIConversationImportRequest;
import com.tcm.platform.entity.AIConversation;
import com.tcm.platform.entity.AIConversationRecommendation;
import com.tcm.platform.entity.AIMessage;
import com.tcm.platform.mapper.AIConversationMapper;
import com.tcm.platform.mapper.AIConversationRecommendationMapper;
import com.tcm.platform.mapper.AIMessageMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIConversationServiceTest {

    @Test
    void initializesRecommendationSnapshotOnlyOnce() {
        AIConversationMapper conversationMapper = mock(AIConversationMapper.class);
        AIMessageMapper messageMapper = mock(AIMessageMapper.class);
        AIConversationRecommendationMapper recommendationMapper =
                mock(AIConversationRecommendationMapper.class);
        AIContextService contextService = mock(AIContextService.class);
        AIConversation conversation = conversation(12L, 7L);
        when(conversationMapper.selectById(12L)).thenReturn(conversation);
        when(contextService.findRecommendations("最近睡不好怎么办？", 7L, 9L))
                .thenReturn(List.of(new AIContentRecommendation(
                        3L,
                        "knowledge",
                        "睡眠不安稳时如何调整作息",
                        "从作息节律开始调整。"
                )));
        when(recommendationMapper.selectByConversationId(12L))
                .thenReturn(List.of());
        when(recommendationMapper.insert(any(AIConversationRecommendation.class)))
                .thenReturn(1);
        AIConversationService service = new AIConversationService(
                conversationMapper,
                messageMapper,
                recommendationMapper,
                contextService
        );

        var first = service.initializeRecommendations(
                12L,
                7L,
                "最近睡不好怎么办？",
                9L
        );
        conversation.setRecommendationInitialized(true);
        AIConversationRecommendation stored = new AIConversationRecommendation();
        stored.setContentId(3L);
        stored.setContentType("knowledge");
        stored.setTitle("睡眠不安稳时如何调整作息");
        stored.setDescription("从作息节律开始调整。");
        when(recommendationMapper.selectByConversationId(12L))
                .thenReturn(List.of(stored));
        var second = service.initializeRecommendations(
                12L,
                7L,
                "后来又问了饮食",
                null
        );

        assertThat(first).hasSize(1);
        assertThat(second).extracting(AIContentRecommendation::title)
                .containsExactly("睡眠不安稳时如何调整作息");
        verify(contextService).findRecommendations("最近睡不好怎么办？", 7L, 9L);
        verify(contextService, never()).findRecommendations("后来又问了饮食", 7L, null);
    }

    @Test
    void rejectsConversationOwnedByAnotherPatient() {
        AIConversationMapper conversationMapper = mock(AIConversationMapper.class);
        AIConversation conversation = conversation(12L, 8L);
        when(conversationMapper.selectById(12L)).thenReturn(conversation);
        AIConversationService service = new AIConversationService(
                conversationMapper,
                mock(AIMessageMapper.class),
                mock(AIConversationRecommendationMapper.class),
                mock(AIContextService.class)
        );

        assertThatThrownBy(() -> service.deleteConversation(12L, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("对话不存在或无权访问");
        verify(conversationMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteReliesOnDatabaseCascadeAfterOwnershipCheck() {
        AIConversationMapper conversationMapper = mock(AIConversationMapper.class);
        when(conversationMapper.selectById(12L)).thenReturn(conversation(12L, 7L));
        when(conversationMapper.deleteById(12L)).thenReturn(1);
        AIConversationService service = new AIConversationService(
                conversationMapper,
                mock(AIMessageMapper.class),
                mock(AIConversationRecommendationMapper.class),
                mock(AIContextService.class)
        );

        service.deleteConversation(12L, 7L);

        verify(conversationMapper).deleteById(12L);
    }

    @Test
    void persistsUserAndAssistantMessagesInOrder() {
        AIConversationMapper conversationMapper = mock(AIConversationMapper.class);
        AIMessageMapper messageMapper = mock(AIMessageMapper.class);
        AIConversation conversation = conversation(12L, 7L);
        when(conversationMapper.selectById(12L)).thenReturn(conversation);
        when(messageMapper.insert(any(AIMessage.class))).thenReturn(1);
        AIConversationService service = new AIConversationService(
                conversationMapper,
                messageMapper,
                mock(AIConversationRecommendationMapper.class),
                mock(AIContextService.class)
        );

        service.appendMessage(12L, 7L, "user", "最近睡不好", false, "");
        service.appendMessage(12L, 7L, "assistant", "可以先固定作息。", false, "仅供参考");

        verify(messageMapper, org.mockito.Mockito.times(2)).insert(any(AIMessage.class));
    }

    @Test
    void rejectsCreatingMoreThanOneHundredConversationsPerPatient() {
        AIConversationMapper conversationMapper = mock(AIConversationMapper.class);
        when(conversationMapper.countByPatientAccountId(7L)).thenReturn(100L);
        AIConversationService service = new AIConversationService(
                conversationMapper,
                mock(AIMessageMapper.class),
                mock(AIConversationRecommendationMapper.class),
                mock(AIContextService.class)
        );

        assertThatThrownBy(() -> service.createConversation(7L, "新的对话", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("每个账号最多保留 100 个 AI 对话，请先删除不需要的对话");
    }

    @Test
    void rejectsMoreThanFiveHundredMessagesInOneConversation() {
        AIConversationMapper conversationMapper = mock(AIConversationMapper.class);
        AIMessageMapper messageMapper = mock(AIMessageMapper.class);
        when(conversationMapper.selectById(12L)).thenReturn(conversation(12L, 7L));
        when(messageMapper.countByConversationId(12L)).thenReturn(500L);
        AIConversationService service = new AIConversationService(
                conversationMapper,
                messageMapper,
                mock(AIConversationRecommendationMapper.class),
                mock(AIContextService.class)
        );

        assertThatThrownBy(() -> service.appendMessage(
                12L,
                7L,
                "user",
                "继续提问",
                false,
                ""
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("单个对话最多保存 500 条消息，请新建对话后继续");
    }

    @Test
    void rejectsMessageLongerThanFiveThousandCharacters() {
        AIConversationMapper conversationMapper = mock(AIConversationMapper.class);
        AIMessageMapper messageMapper = mock(AIMessageMapper.class);
        when(conversationMapper.selectById(12L)).thenReturn(conversation(12L, 7L));
        when(messageMapper.countByConversationId(12L)).thenReturn(0L);
        AIConversationService service = new AIConversationService(
                conversationMapper,
                messageMapper,
                mock(AIConversationRecommendationMapper.class),
                mock(AIContextService.class)
        );

        assertThatThrownBy(() -> service.appendMessage(
                12L,
                7L,
                "assistant",
                "答".repeat(5001),
                false,
                ""
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("单条消息不能超过 5000 字");
    }

    @Test
    void buildsModelContextFromSummaryAndMostRecentTwelveMessages() {
        AIConversationMapper conversationMapper = mock(AIConversationMapper.class);
        AIMessageMapper messageMapper = mock(AIMessageMapper.class);
        AIConversation conversation = conversation(12L, 7L);
        conversation.setHistorySummary("此前主要讨论了睡眠和晚餐时间。");
        when(conversationMapper.selectById(12L)).thenReturn(conversation);
        AIMessage recent = new AIMessage();
        recent.setRole("user");
        recent.setContent("昨晚仍然入睡较晚");
        when(messageMapper.selectRecentByConversationId(12L, 12))
                .thenReturn(List.of(recent));
        AIConversationService service = new AIConversationService(
                conversationMapper,
                messageMapper,
                mock(AIConversationRecommendationMapper.class),
                mock(AIContextService.class)
        );

        var context = service.buildContext(12L, 7L);

        assertThat(context).extracting(AIQuestionRequest.ContextMessage::content)
                .containsExactly(
                        "【较早对话摘要】\n此前主要讨论了睡眠和晚餐时间。",
                        "昨晚仍然入睡较晚"
                );
        verify(messageMapper).selectRecentByConversationId(12L, 12);
    }

    @Test
    void importsLegacyMessagesOnlyIntoAnEmptyOwnedConversation() {
        AIConversationMapper conversationMapper = mock(AIConversationMapper.class);
        AIMessageMapper messageMapper = mock(AIMessageMapper.class);
        AIConversationRecommendationMapper recommendationMapper =
                mock(AIConversationRecommendationMapper.class);
        when(conversationMapper.selectById(12L)).thenReturn(conversation(12L, 7L));
        when(messageMapper.countByConversationId(12L)).thenReturn(0L);
        when(messageMapper.insert(any(AIMessage.class))).thenReturn(1);
        when(recommendationMapper.insert(any(AIConversationRecommendation.class)))
                .thenReturn(1);
        when(recommendationMapper.selectByConversationId(12L)).thenReturn(List.of());
        AIConversationService service = new AIConversationService(
                conversationMapper,
                messageMapper,
                recommendationMapper,
                mock(AIContextService.class)
        );
        AIConversationImportRequest request = new AIConversationImportRequest();
        request.setMessages(List.of(
                new AIConversationImportRequest.MessageItem(
                        "user",
                        "最近睡不好",
                        false,
                        ""
                ),
                new AIConversationImportRequest.MessageItem(
                        "assistant",
                        "可以先固定作息。",
                        false,
                        "仅供参考"
                )
        ));
        request.setRecommendations(List.of(
                new AIContentRecommendation(
                        3L,
                        "knowledge",
                        "睡眠不安稳时如何调整作息",
                        "从作息节律开始调整。"
                )
        ));

        service.importLegacyContent(12L, 7L, request);

        verify(messageMapper, org.mockito.Mockito.times(2)).insert(any(AIMessage.class));
        verify(recommendationMapper).insert(any(AIConversationRecommendation.class));
    }

    @Test
    void reusesConversationWhenLegacyMigrationIsRetried() {
        AIConversationMapper conversationMapper = mock(AIConversationMapper.class);
        AIConversation existing = conversation(12L, 7L);
        existing.setLegacyKey("local-abc");
        when(conversationMapper.selectByPatientAndLegacyKey(7L, "local-abc"))
                .thenReturn(existing);
        AIConversationRecommendationMapper recommendationMapper =
                mock(AIConversationRecommendationMapper.class);
        when(recommendationMapper.selectByConversationId(12L)).thenReturn(List.of());
        AIConversationService service = new AIConversationService(
                conversationMapper,
                mock(AIMessageMapper.class),
                recommendationMapper,
                mock(AIContextService.class)
        );

        var result = service.createConversation(
                7L,
                "旧对话",
                null,
                "local-abc"
        );

        assertThat(result.id()).isEqualTo(12L);
        verify(conversationMapper, never()).insert(any(AIConversation.class));
    }

    private AIConversation conversation(Long id, Long patientAccountId) {
        AIConversation conversation = new AIConversation();
        conversation.setId(id);
        conversation.setPatientAccountId(patientAccountId);
        conversation.setTitle("睡眠调养");
        conversation.setRecommendationInitialized(false);
        return conversation;
    }
}
