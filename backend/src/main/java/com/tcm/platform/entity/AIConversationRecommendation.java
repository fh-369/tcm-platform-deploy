package com.tcm.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_conversation_recommendations")
public class AIConversationRecommendation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private String contentType;

    private Long contentId;

    private String title;

    private String description;

    private Integer sortOrder;
}
