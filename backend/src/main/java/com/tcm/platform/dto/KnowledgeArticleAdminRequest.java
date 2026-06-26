package com.tcm.platform.dto;

import com.tcm.platform.entity.KnowledgeArticle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KnowledgeArticleAdminRequest {

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题不能超过 200 个字符")
    private String title;

    @Size(max = 50, message = "文章分类不能超过 50 个字符")
    private String category;

    @Size(max = 500, message = "文章摘要不能超过 500 个字符")
    private String summary;

    private String content;

    private String tips;

    @Size(max = 500, message = "封面地址不能超过 500 个字符")
    private String coverImageUrl;

    private Boolean published;

    public KnowledgeArticle toEntity() {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(title);
        article.setCategory(category);
        article.setSummary(summary);
        article.setContent(content);
        article.setTips(tips);
        article.setCoverImageUrl(coverImageUrl);
        article.setPublished(published);
        return article;
    }
}
