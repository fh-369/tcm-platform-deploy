package com.tcm.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tcm.platform.entity.KnowledgeArticle;
import com.tcm.platform.mapper.KnowledgeArticleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 中医知识文章公开查询与后台管理业务。
 */
@Service
public class KnowledgeArticleService {

    private final KnowledgeArticleMapper knowledgeArticleMapper;

    public KnowledgeArticleService(KnowledgeArticleMapper knowledgeArticleMapper) {
        this.knowledgeArticleMapper = knowledgeArticleMapper;
    }

    public Page<KnowledgeArticle> listPublishedArticles(long current, long size, String category, String keyword) {
        validatePage(current, size);
        return knowledgeArticleMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<KnowledgeArticle>()
                        .eq(KnowledgeArticle::getPublished, true)
                        .eq(hasText(category), KnowledgeArticle::getCategory, category)
                        .and(hasText(keyword), wrapper -> wrapper
                                .like(KnowledgeArticle::getTitle, keyword)
                                .or()
                                .like(KnowledgeArticle::getSummary, keyword)
                                .or()
                                .like(KnowledgeArticle::getContent, keyword))
                        .orderByDesc(KnowledgeArticle::getCreatedAt)
        );
    }

    public List<String> listPublishedCategories() {
        return knowledgeArticleMapper.selectObjs(
                        new LambdaQueryWrapper<KnowledgeArticle>()
                                .select(KnowledgeArticle::getCategory)
                                .eq(KnowledgeArticle::getPublished, true)
                                .isNotNull(KnowledgeArticle::getCategory)
                                .ne(KnowledgeArticle::getCategory, "")
                                .groupBy(KnowledgeArticle::getCategory)
                                .orderByAsc(KnowledgeArticle::getCategory)
                ).stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
    }

    @Transactional
    public KnowledgeArticle getPublishedArticle(Long id) {
        KnowledgeArticle article = getArticle(id);
        if (!Boolean.TRUE.equals(article.getPublished())) {
            throw new IllegalArgumentException("知识文章不存在或尚未发布");
        }
        if (knowledgeArticleMapper.incrementPublishedViewCount(id) != 1) {
            throw new IllegalStateException("文章浏览量更新失败");
        }
        return getArticle(id);
    }

    public Page<KnowledgeArticle> listArticles(
            long current,
            long size,
            String category,
            Boolean published,
            String keyword
    ) {
        validatePage(current, size);

        LambdaQueryWrapper<KnowledgeArticle> query = new LambdaQueryWrapper<>();
        query.eq(hasText(category), KnowledgeArticle::getCategory, category)
                .eq(published != null, KnowledgeArticle::getPublished, published)
                .and(hasText(keyword), wrapper -> wrapper
                        .like(KnowledgeArticle::getTitle, keyword)
                        .or()
                        .like(KnowledgeArticle::getSummary, keyword))
                .orderByDesc(KnowledgeArticle::getCreatedAt);

        return knowledgeArticleMapper.selectPage(new Page<>(current, size), query);
    }

    public KnowledgeArticle getArticle(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("知识文章 ID 不能为空");
        }

        KnowledgeArticle article = knowledgeArticleMapper.selectById(id);
        if (article == null) {
            throw new IllegalArgumentException("知识文章不存在");
        }
        return article;
    }

    @Transactional
    public KnowledgeArticle createArticle(KnowledgeArticle article) {
        validateArticle(article);
        article.setId(null);
        normalizeArticle(article);
        article.setCategory(resolveCategory(article));
        article.setPublished(Boolean.TRUE.equals(article.getPublished()));
        article.setViewCount(article.getViewCount() == null ? 0 : article.getViewCount());

        if (knowledgeArticleMapper.insert(article) != 1) {
            throw new IllegalStateException("创建知识文章失败");
        }
        return article;
    }

    @Transactional
    public KnowledgeArticle updateArticle(Long id, KnowledgeArticle request) {
        KnowledgeArticle article = getArticle(id);
        validateArticle(request);
        normalizeArticle(request);

        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setTips(request.getTips());
        article.setCoverImageUrl(request.getCoverImageUrl());
        article.setPublished(Boolean.TRUE.equals(request.getPublished()));
        article.setCategory(resolveCategory(request));

        if (knowledgeArticleMapper.updateById(article) != 1) {
            throw new IllegalStateException("更新知识文章失败");
        }
        return article;
    }

    @Transactional
    public KnowledgeArticle updatePublication(Long id, Boolean published) {
        KnowledgeArticle article = getArticle(id);
        if (Boolean.TRUE.equals(published)) {
            validatePublishedArticle(article);
        }
        article.setPublished(Boolean.TRUE.equals(published));
        if (knowledgeArticleMapper.updateById(article) != 1) {
            throw new IllegalStateException("更新文章发布状态失败");
        }
        return article;
    }

    @Transactional
    public void deleteArticle(Long id) {
        getArticle(id);
        if (knowledgeArticleMapper.deleteById(id) != 1) {
            throw new IllegalStateException("删除知识文章失败");
        }
    }

    private void validateArticle(KnowledgeArticle article) {
        if (article == null) {
            throw new IllegalArgumentException("知识文章内容不能为空");
        }
        if (!hasText(article.getTitle())) {
            throw new IllegalArgumentException("文章标题不能为空");
        }
        if (Boolean.TRUE.equals(article.getPublished()) && !hasText(article.getContent())) {
            throw new IllegalArgumentException("文章正文不能为空");
        }
    }

    private void validatePublishedArticle(KnowledgeArticle article) {
        if (!hasText(article.getContent())) {
            throw new IllegalArgumentException("文章正文不能为空，无法发布");
        }
    }

    private void normalizeArticle(KnowledgeArticle article) {
        article.setTitle(trim(article.getTitle()));
        article.setCategory(trim(article.getCategory()));
        article.setSummary(trim(article.getSummary()));
        article.setContent(trim(article.getContent()));
        article.setTips(trim(article.getTips()));
        article.setCoverImageUrl(trim(article.getCoverImageUrl()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void validatePage(long current, long size) {
        if (current < 1) {
            throw new IllegalArgumentException("页码必须大于 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("每页数量必须在 1 到 100 之间");
        }
    }

    private String resolveCategory(KnowledgeArticle article) {
        if (hasText(article.getCategory())) {
            return article.getCategory().trim();
        }

        String text = String.join(" ",
                Objects.toString(article.getTitle(), ""),
                Objects.toString(article.getSummary(), ""),
                Objects.toString(article.getContent(), "")
        );
        if (containsAny(text, "睡眠", "入睡", "作息", "熬夜", "午睡")) {
            return "睡眠起居";
        }
        if (containsAny(text, "饮食", "膳食", "盐", "蔬菜", "水果", "喝水", "补水")) {
            return "饮食调养";
        }
        if (containsAny(text, "运动", "活动", "久坐", "步行", "锻炼")) {
            return "运动养护";
        }
        if (containsAny(text, "情绪", "压力", "焦虑", "放松", "心情")) {
            return "情志调适";
        }
        if (containsAny(text, "春", "夏", "秋", "冬", "节气", "高温", "寒冷")) {
            return "四季养护";
        }
        return "养生常识";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
