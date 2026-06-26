package com.tcm.platform.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tcm.platform.entity.KnowledgeArticle;
import com.tcm.platform.mapper.KnowledgeArticleMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeArticleServiceTest {

    @BeforeAll
    static void initializeTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
                KnowledgeArticle.class
        );
    }

    @Mock
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @Test
    void listPublishedArticlesOnlyQueriesPublishedContent() {
        KnowledgeArticleService service = new KnowledgeArticleService(knowledgeArticleMapper);
        when(knowledgeArticleMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>());

        service.listPublishedArticles(1, 6, null, null);

        ArgumentCaptor<LambdaQueryWrapper<KnowledgeArticle>> queryCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeArticleMapper).selectPage(any(IPage.class), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getCustomSqlSegment()).contains("published");
    }

    @Test
    void listPublishedArticlesCombinesCategoryAndKeywordSearch() {
        KnowledgeArticleService service = new KnowledgeArticleService(knowledgeArticleMapper);
        when(knowledgeArticleMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>());

        service.listPublishedArticles(1, 6, "睡眠起居", "节律");

        ArgumentCaptor<LambdaQueryWrapper<KnowledgeArticle>> queryCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeArticleMapper).selectPage(any(IPage.class), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getCustomSqlSegment())
                .contains("published", "category", "title", "summary", "content", "OR");
    }

    @Test
    void openingPublishedArticleIncrementsAndReturnsLatestViewCount() {
        KnowledgeArticleService service = new KnowledgeArticleService(knowledgeArticleMapper);
        KnowledgeArticle before = article("睡眠节律", "正文");
        before.setId(8L);
        before.setPublished(true);
        before.setViewCount(3);
        KnowledgeArticle after = article("睡眠节律", "正文");
        after.setId(8L);
        after.setPublished(true);
        after.setViewCount(4);
        when(knowledgeArticleMapper.selectById(8L)).thenReturn(before, after);
        when(knowledgeArticleMapper.incrementPublishedViewCount(8L)).thenReturn(1);

        KnowledgeArticle result = service.getPublishedArticle(8L);

        assertThat(result.getViewCount()).isEqualTo(4);
        verify(knowledgeArticleMapper).incrementPublishedViewCount(8L);
    }

    @Test
    void createArticleDefaultsToDraftWithZeroViews() {
        KnowledgeArticleService service = new KnowledgeArticleService(knowledgeArticleMapper);
        when(knowledgeArticleMapper.insert(any(KnowledgeArticle.class))).thenReturn(1);
        KnowledgeArticle article = article("春季养生", "春季养生正文");

        KnowledgeArticle created = service.createArticle(article);

        assertThat(created.getPublished()).isFalse();
        assertThat(created.getViewCount()).isZero();
        assertThat(created.getCategory()).isEqualTo("四季养护");
    }

    @Test
    void createArticleAutomaticallyClassifiesMissingCategory() {
        KnowledgeArticleService service = new KnowledgeArticleService(knowledgeArticleMapper);
        when(knowledgeArticleMapper.insert(any(KnowledgeArticle.class))).thenReturn(1);
        KnowledgeArticle article = article("稳定睡眠节律", "每天尽量按时入睡和起床");

        KnowledgeArticle created = service.createArticle(article);

        assertThat(created.getCategory()).isEqualTo("睡眠起居");
    }

    @Test
    void listArticlesFiltersByCategoryPublishedAndKeyword() {
        KnowledgeArticleService service = new KnowledgeArticleService(knowledgeArticleMapper);
        when(knowledgeArticleMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
                .thenReturn(new Page<>());

        service.listArticles(1, 10, "四季养生", true, "春季");

        ArgumentCaptor<LambdaQueryWrapper<KnowledgeArticle>> queryCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(knowledgeArticleMapper).selectPage(any(IPage.class), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getCustomSqlSegment())
                .contains("category", "published", "title", "summary", "OR");
    }

    @Test
    void deleteArticleDeletesExistingArticle() {
        KnowledgeArticleService service = new KnowledgeArticleService(knowledgeArticleMapper);
        when(knowledgeArticleMapper.selectById(9L)).thenReturn(article("待删除文章", "待删除正文"));
        when(knowledgeArticleMapper.deleteById(9L)).thenReturn(1);

        service.deleteArticle(9L);

        verify(knowledgeArticleMapper).deleteById(9L);
    }

    @Test
    void updatePublicationOnlyChangesPublishedState() {
        KnowledgeArticleService service = new KnowledgeArticleService(knowledgeArticleMapper);
        KnowledgeArticle stored = article("睡眠节律", "正文");
        stored.setId(12L);
        stored.setPublished(false);
        when(knowledgeArticleMapper.selectById(12L)).thenReturn(stored);
        when(knowledgeArticleMapper.updateById(stored)).thenReturn(1);

        KnowledgeArticle updated = service.updatePublication(12L, true);

        assertThat(updated.getPublished()).isTrue();
        assertThat(updated.getTitle()).isEqualTo("睡眠节律");
        verify(knowledgeArticleMapper).updateById(stored);
    }

    @Test
    void createArticleTrimsEditableTextFields() {
        KnowledgeArticleService service = new KnowledgeArticleService(knowledgeArticleMapper);
        when(knowledgeArticleMapper.insert(any(KnowledgeArticle.class))).thenReturn(1);
        KnowledgeArticle article = article("  春季养生  ", "  春季养生正文  ");
        article.setCategory("  四季养护  ");
        article.setSummary("  适合春日阅读  ");

        KnowledgeArticle created = service.createArticle(article);

        assertThat(created.getTitle()).isEqualTo("春季养生");
        assertThat(created.getContent()).isEqualTo("春季养生正文");
        assertThat(created.getCategory()).isEqualTo("四季养护");
        assertThat(created.getSummary()).isEqualTo("适合春日阅读");
    }

    private KnowledgeArticle article(String title, String content) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(title);
        article.setContent(content);
        return article;
    }
}
