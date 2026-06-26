package com.tcm.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 中医常识文章实体
 */
@Data
@TableName("knowledge_articles")
public class KnowledgeArticle {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String category;

    private String summary;

    private String content;

    private String tips;

    private String coverImageUrl;

    private Boolean published;

    private Integer viewCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
