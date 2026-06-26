package com.tcm.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tcm.platform.dto.AIContentRecommendation;
import com.tcm.platform.dto.AIConversationResponse;
import com.tcm.platform.dto.AIConversationImportRequest;
import com.tcm.platform.dto.AIQuestionRequest;
import com.tcm.platform.entity.AIConversation;
import com.tcm.platform.entity.AIConversationRecommendation;
import com.tcm.platform.entity.AIMessage;
import com.tcm.platform.mapper.AIConversationMapper;
import com.tcm.platform.mapper.AIConversationRecommendationMapper;
import com.tcm.platform.mapper.AIMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class AIConversationService {

    private static final int TITLE_LIMIT = 24;
    private static final int MAX_CONVERSATIONS_PER_PATIENT = 100;
    private static final int MAX_MESSAGES_PER_CONVERSATION = 500;
    private static final int MAX_MESSAGE_LENGTH = 5000;
    private static final int MODEL_CONTEXT_MESSAGE_LIMIT = 12;

    private final AIConversationMapper conversationMapper;
    private final AIMessageMapper messageMapper;
    private final AIConversationRecommendationMapper recommendationMapper;
    private final AIContextService contextService;

    public AIConversationService(
            AIConversationMapper conversationMapper,
            AIMessageMapper messageMapper,
            AIConversationRecommendationMapper recommendationMapper,
            AIContextService contextService
    ) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.recommendationMapper = recommendationMapper;
        this.contextService = contextService;
    }

    @Transactional
    public AIConversationResponse createConversation(
            Long patientAccountId,
            String title,
            Long consultationId
    ) {
        return createConversation(patientAccountId, title, consultationId, null);
    }

    @Transactional
    public AIConversationResponse createConversation(
            Long patientAccountId,
            String title,
            Long consultationId,
            String legacyKey
    ) {
        String normalizedLegacyKey = normalizeOptional(legacyKey);
        if (hasText(normalizedLegacyKey)) {
            AIConversation existing = conversationMapper.selectByPatientAndLegacyKey(
                    patientAccountId,
                    normalizedLegacyKey
            );
            if (existing != null) {
                long messageTotal =
                        messageMapper.countByConversationId(existing.getId());
                return toResponse(existing, List.of(), messageTotal, false);
            }
        }
        if (conversationMapper.countByPatientAccountId(patientAccountId)
                >= MAX_CONVERSATIONS_PER_PATIENT) {
            throw new IllegalArgumentException(
                    "每个账号最多保留 100 个 AI 对话，请先删除不需要的对话"
            );
        }
        AIConversation conversation = new AIConversation();
        conversation.setPatientAccountId(patientAccountId);
        conversation.setConsultationId(consultationId);
        conversation.setTitle(normalizeTitle(title));
        conversation.setRecommendationInitialized(false);
        conversation.setLegacyKey(normalizedLegacyKey);
        if (conversationMapper.insert(conversation) != 1) {
            throw new IllegalStateException("对话创建失败");
        }
        return toResponse(conversation, List.of(), 0, false);
    }

    public Page<AIConversationResponse> listConversations(
            Long patientAccountId,
            long current,
            long size
    ) {
        Page<AIConversation> page = new Page<>(
                normalizePage(current),
                normalizeSize(size, 20)
        );
        conversationMapper.selectPage(
                page,
                com.baomidou.mybatisplus.core.toolkit.Wrappers
                        .<AIConversation>lambdaQuery()
                        .eq(AIConversation::getPatientAccountId, patientAccountId)
                        .orderByDesc(AIConversation::getUpdatedAt)
                        .orderByDesc(AIConversation::getId)
        );
        Page<AIConversationResponse> response = new Page<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal()
        );
        response.setRecords(page.getRecords().stream()
                .map(conversation -> toResponse(
                        conversation,
                        List.of(),
                        messageMapper.countByConversationId(conversation.getId()),
                        false
                ))
                .toList());
        return response;
    }

    public AIConversationResponse getConversation(
            Long conversationId,
            Long patientAccountId,
            long messageCurrent,
            long messageSize
    ) {
        AIConversation conversation =
                requireOwnedConversation(conversationId, patientAccountId);
        Page<AIMessage> page = new Page<>(
                normalizePage(messageCurrent),
                normalizeSize(messageSize, 30)
        );
        messageMapper.selectPage(
                page,
                com.baomidou.mybatisplus.core.toolkit.Wrappers
                        .<AIMessage>lambdaQuery()
                        .eq(AIMessage::getConversationId, conversationId)
                        .orderByDesc(AIMessage::getCreatedAt)
                        .orderByDesc(AIMessage::getId)
        );
        List<AIMessage> messages = new ArrayList<>(page.getRecords());
        Collections.reverse(messages);
        return toResponse(
                conversation,
                messages,
                page.getTotal(),
                page.getCurrent() * page.getSize() < page.getTotal()
        );
    }

    public List<AIQuestionRequest.ContextMessage> buildContext(
            Long conversationId,
            Long patientAccountId
    ) {
        AIConversation conversation =
                requireOwnedConversation(conversationId, patientAccountId);
        List<AIQuestionRequest.ContextMessage> context = new ArrayList<>();
        if (hasText(conversation.getHistorySummary())) {
            context.add(new AIQuestionRequest.ContextMessage(
                    "system",
                    "【较早对话摘要】\n" + conversation.getHistorySummary().trim()
            ));
        }
        context.addAll(messageMapper
                .selectRecentByConversationId(
                        conversationId,
                        MODEL_CONTEXT_MESSAGE_LIMIT
                )
                .stream()
                .filter(message -> hasText(message.getContent()))
                .map(message -> new AIQuestionRequest.ContextMessage(
                        message.getRole(),
                        message.getContent()
                ))
                .toList());
        return context;
    }

    @Transactional
    public AIMessage appendMessage(
            Long conversationId,
            Long patientAccountId,
            String role,
            String content,
            boolean fallback,
            String disclaimer
    ) {
        AIConversation conversation = requireOwnedConversation(conversationId, patientAccountId);
        if (messageMapper.countByConversationId(conversationId)
                >= MAX_MESSAGES_PER_CONVERSATION) {
            throw new IllegalArgumentException(
                    "单个对话最多保存 500 条消息，请新建对话后继续"
            );
        }
        AIMessage message = new AIMessage();
        message.setConversationId(conversationId);
        message.setRole(normalizeRole(role));
        message.setContent(requireContent(content));
        message.setFallback(fallback);
        message.setDisclaimer(disclaimer == null ? "" : disclaimer.trim());
        if (messageMapper.insert(message) != 1) {
            throw new IllegalStateException("对话消息保存失败");
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
        return message;
    }

    @Transactional
    public List<AIContentRecommendation> initializeRecommendations(
            Long conversationId,
            Long patientAccountId,
            String firstQuestion,
            Long consultationId
    ) {
        AIConversation conversation = requireOwnedConversation(conversationId, patientAccountId);
        if (Boolean.TRUE.equals(conversation.getRecommendationInitialized())) {
            return storedRecommendations(conversationId);
        }

        Long selectedConsultationId = conversation.getConsultationId() != null
                ? conversation.getConsultationId()
                : consultationId;
        List<AIContentRecommendation> recommendations =
                contextService.findRecommendations(
                        requireContent(firstQuestion),
                        patientAccountId,
                        selectedConsultationId
                );
        persistRecommendationSnapshot(conversationId, recommendations);
        conversation.setRecommendationInitialized(true);
        conversation.setConsultationId(selectedConsultationId);
        conversationMapper.updateById(conversation);
        return recommendations;
    }

    @Transactional
    public void deleteConversation(Long conversationId, Long patientAccountId) {
        requireOwnedConversation(conversationId, patientAccountId);
        if (conversationMapper.deleteById(conversationId) != 1) {
            throw new IllegalStateException("对话删除失败");
        }
    }

    @Transactional
    public AIConversationResponse importLegacyContent(
            Long conversationId,
            Long patientAccountId,
            AIConversationImportRequest request
    ) {
        AIConversation conversation =
                requireOwnedConversation(conversationId, patientAccountId);
        if (messageMapper.countByConversationId(conversationId) > 0) {
            throw new IllegalArgumentException("该对话已有消息，不能重复导入");
        }
        if (request != null && request.getMessages() != null) {
            request.getMessages().stream()
                    .limit(MAX_MESSAGES_PER_CONVERSATION)
                    .forEach(item -> appendMessage(
                            conversationId,
                            patientAccountId,
                            item.role(),
                            item.content(),
                            item.fallback(),
                            item.disclaimer()
                    ));
        }
        List<AIContentRecommendation> recommendations =
                request == null || request.getRecommendations() == null
                        ? List.of()
                        : request.getRecommendations().stream()
                                .filter(item -> "knowledge".equals(item.type()))
                                .limit(4)
                                .toList();
        persistRecommendationSnapshot(conversationId, recommendations);
        if (!recommendations.isEmpty()) {
            conversation.setRecommendationInitialized(true);
            conversationMapper.updateById(conversation);
        }
        return getConversation(conversationId, patientAccountId, 1, 30);
    }

    private AIConversationResponse toResponse(
            AIConversation conversation,
            List<AIMessage> messages,
            long messageTotal,
            boolean hasMoreMessages
    ) {
        return new AIConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getConsultationId(),
                Boolean.TRUE.equals(conversation.getRecommendationInitialized()),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages,
                storedRecommendations(conversation.getId()),
                messageTotal,
                hasMoreMessages
        );
    }

    private List<AIContentRecommendation> storedRecommendations(Long conversationId) {
        return recommendationMapper.selectByConversationId(conversationId).stream()
                .map(item -> new AIContentRecommendation(
                        item.getContentId(),
                        item.getContentType(),
                        item.getTitle(),
                        item.getDescription()
                ))
                .toList();
    }

    private void persistRecommendationSnapshot(
            Long conversationId,
            List<AIContentRecommendation> recommendations
    ) {
        for (int index = 0; index < recommendations.size(); index++) {
            AIContentRecommendation item = recommendations.get(index);
            AIConversationRecommendation stored = new AIConversationRecommendation();
            stored.setConversationId(conversationId);
            stored.setContentType(item.type());
            stored.setContentId(item.id());
            stored.setTitle(item.title());
            stored.setDescription(item.description());
            stored.setSortOrder(index);
            if (recommendationMapper.insert(stored) != 1) {
                throw new IllegalStateException("站内推荐快照保存失败");
            }
        }
    }

    private AIConversation requireOwnedConversation(
            Long conversationId,
            Long patientAccountId
    ) {
        if (conversationId == null) {
            throw new IllegalArgumentException("对话 ID 不能为空");
        }
        AIConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null
                || !Objects.equals(conversation.getPatientAccountId(), patientAccountId)) {
            throw new IllegalArgumentException("对话不存在或无权访问");
        }
        return conversation;
    }

    private String normalizeTitle(String title) {
        String normalized = requireContent(title);
        return normalized.length() <= TITLE_LIMIT
                ? normalized
                : normalized.substring(0, TITLE_LIMIT);
    }

    private String normalizeRole(String role) {
        if (!"user".equals(role) && !"assistant".equals(role)) {
            throw new IllegalArgumentException("不支持的消息角色");
        }
        return role;
    }

    private String requireContent(String content) {
        if (!hasText(content)) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("单条消息不能超过 5000 字");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (!hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= 100
                ? normalized
                : normalized.substring(0, 100);
    }

    private long normalizePage(long current) {
        return Math.max(current, 1);
    }

    private long normalizeSize(long size, long defaultSize) {
        if (size <= 0) {
            return defaultSize;
        }
        return Math.min(size, 50);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
