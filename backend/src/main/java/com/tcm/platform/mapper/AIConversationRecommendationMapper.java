package com.tcm.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tcm.platform.entity.AIConversationRecommendation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AIConversationRecommendationMapper
        extends BaseMapper<AIConversationRecommendation> {

    @Select("""
            SELECT id, conversation_id, content_type, content_id,
                   title, description, sort_order
            FROM ai_conversation_recommendations
            WHERE conversation_id = #{conversationId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<AIConversationRecommendation> selectByConversationId(
            @Param("conversationId") Long conversationId
    );
}
