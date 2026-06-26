package com.tcm.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tcm.platform.dto.AIContentRecommendation;
import com.tcm.platform.dto.AIQuestionRequest;
import com.tcm.platform.entity.Consultation;
import com.tcm.platform.entity.KnowledgeArticle;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 为 AI 问答组装问诊单上下文，并独立检索站内延伸阅读。
 */
@Service
public class AIContextService {

    private final KnowledgeArticleService knowledgeArticleService;
    private final RecipeService recipeService;
    private final ConsultationService consultationService;

    public AIContextService(
            KnowledgeArticleService knowledgeArticleService,
            RecipeService recipeService,
            ConsultationService consultationService
    ) {
        this.knowledgeArticleService = knowledgeArticleService;
        this.recipeService = recipeService;
        this.consultationService = consultationService;
    }

    public List<AIQuestionRequest.ContextMessage> enrichContext(
            String question,
            List<AIQuestionRequest.ContextMessage> existingContext,
            Long patientAccountId,
            Long consultationId
    ) {
        List<AIQuestionRequest.ContextMessage> result = new ArrayList<>();
        if (existingContext != null) {
            result.addAll(existingContext);
        }

        String reference = buildConsultationContext(patientAccountId, consultationId);
        if (hasText(reference)) {
            result.add(new AIQuestionRequest.ContextMessage("user", reference));
        }
        return result;
    }

    public List<AIContentRecommendation> findRecommendations(
            String question,
            Long patientAccountId,
            Long consultationId
    ) {
        if (!hasText(question)) {
            return List.of();
        }

        String consultationContext = buildConsultationContext(patientAccountId, consultationId);
        String recommendationContext = String.join(" ", question, consultationContext);
        List<KnowledgeArticle> articles = records(knowledgeArticleService.listPublishedArticles(1, 100, null, null));
        List<AIContentRecommendation> recommendations = new ArrayList<>();

        for (KnowledgeArticle article : rankArticles(recommendationContext, articles)) {
            recommendations.add(new AIContentRecommendation(
                    article.getId(),
                    "knowledge",
                    text(article.getTitle()),
                    shorten(firstText(article.getSummary(), article.getContent()), 90)
            ));
        }
        return recommendations;
    }

    private List<KnowledgeArticle> rankArticles(String question, List<KnowledgeArticle> articles) {
        return articles.stream()
                .filter(article -> articleScore(question, article) > 0)
                .sorted(Comparator
                        .comparingInt((KnowledgeArticle article) -> articleScore(question, article))
                        .reversed()
                        .thenComparing(KnowledgeArticle::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(4)
                .toList();
    }

    private int articleScore(String question, KnowledgeArticle article) {
        return relevanceScore(
                question,
                String.join(" ",
                        text(article.getTitle()),
                        text(article.getCategory()),
                        text(article.getSummary()),
                        text(article.getContent())
                )
        );
    }

    private int relevanceScore(String question, String content) {
        String normalizedQuestion = normalize(question);
        String normalizedContent = normalize(content);
        int score = 0;

        for (String keyword : recommendationKeywords()) {
            if (normalizedQuestion.contains(keyword) && normalizedContent.contains(keyword)) {
                score += keyword.length() > 1 ? 4 : 2;
            }
        }
        for (Set<String> group : topicGroups()) {
            boolean questionMatches = group.stream().anyMatch(normalizedQuestion::contains);
            boolean contentMatches = group.stream().anyMatch(normalizedContent::contains);
            if (questionMatches && contentMatches) {
                score += 6;
            }
        }
        return score;
    }

    private List<String> recommendationKeywords() {
        return List.of(
                "春", "夏", "秋", "冬", "睡眠", "失眠", "作息", "疲倦", "乏力", "饮食",
                "胃口", "胃", "消化", "运动", "久坐", "情绪", "压力", "焦虑", "补水",
                "上火", "寒", "咳嗽", "感冒", "头痛", "女性", "老人"
        );
    }

    private List<Set<String>> topicGroups() {
        return List.of(
                Set.of("睡眠", "失眠", "入睡", "熬夜", "作息"),
                Set.of("胃口", "胃", "消化", "饮食", "饭", "食欲"),
                Set.of("疲倦", "乏力", "疲劳", "精神"),
                Set.of("运动", "锻炼", "久坐", "步行"),
                Set.of("情绪", "压力", "焦虑", "心情"),
                Set.of("感冒", "咳嗽", "鼻塞", "发热"),
                Set.of("春", "夏", "秋", "冬", "季节", "节气")
        );
    }

    private String normalize(String value) {
        return text(value).toLowerCase().replaceAll("[\\s，。！？、；：,.!?;:]+", "");
    }

    private String buildConsultationContext(Long patientAccountId, Long consultationId) {
        if (consultationId == null) {
            return "";
        }
        return buildConsultationReference(
                consultationService.getPatientConsultation(consultationId, patientAccountId)
        );
    }

    private String buildConsultationReference(Consultation consultation) {
        return """
                【用户选择的问诊单】
                - 患者：%s
                - 主要症状：%s
                - 持续时间：%s
                - 紧急程度：%s
                - 备注：%s
                - 系统提醒：%s
                """.formatted(
                text(consultation.getPatientName()),
                text(consultation.getSymptoms()),
                text(consultation.getDuration()),
                text(consultation.getUrgency()),
                text(consultation.getPatientNote()),
                text(consultation.getReminderText())
        ).trim();
    }

    private <T> List<T> records(Page<T> page) {
        return page == null || page.getRecords() == null ? List.of() : page.getRecords();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String shorten(String value, int maxLength) {
        String text = text(value);
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private String text(Object value) {
        return Objects.toString(value, "").trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
