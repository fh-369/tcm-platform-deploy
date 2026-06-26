package com.tcm.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tcm.platform.entity.AIMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AIMessageMapper extends BaseMapper<AIMessage> {

    @Select("""
            SELECT id, conversation_id, role, content, fallback, disclaimer, created_at
            FROM ai_messages
            WHERE conversation_id = #{conversationId}
            ORDER BY created_at ASC, id ASC
            """)
    List<AIMessage> selectByConversationId(@Param("conversationId") Long conversationId);

    @Select("""
            SELECT COUNT(*)
            FROM ai_messages
            WHERE conversation_id = #{conversationId}
            """)
    long countByConversationId(@Param("conversationId") Long conversationId);

    @Select("""
            SELECT id, conversation_id, role, content, fallback, disclaimer, created_at
            FROM (
                SELECT id, conversation_id, role, content, fallback, disclaimer, created_at
                FROM ai_messages
                WHERE conversation_id = #{conversationId}
                ORDER BY created_at DESC, id DESC
                LIMIT #{limit}
            ) recent
            ORDER BY created_at ASC, id ASC
            """)
    List<AIMessage> selectRecentByConversationId(
            @Param("conversationId") Long conversationId,
            @Param("limit") int limit
    );
}
