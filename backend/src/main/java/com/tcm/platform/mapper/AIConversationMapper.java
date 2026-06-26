package com.tcm.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tcm.platform.entity.AIConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AIConversationMapper extends BaseMapper<AIConversation> {

    @Select("""
            SELECT id, patient_account_id, consultation_id, title,
                   recommendation_initialized, history_summary, legacy_key,
                   created_at, updated_at
            FROM ai_conversations
            WHERE patient_account_id = #{patientAccountId}
            ORDER BY updated_at DESC, id DESC
            """)
    List<AIConversation> selectByPatientAccountId(
            @Param("patientAccountId") Long patientAccountId
    );

    @Select("""
            SELECT COUNT(*)
            FROM ai_conversations
            WHERE patient_account_id = #{patientAccountId}
            """)
    long countByPatientAccountId(@Param("patientAccountId") Long patientAccountId);

    @Select("""
            SELECT id, patient_account_id, consultation_id, title,
                   recommendation_initialized, history_summary, legacy_key,
                   created_at, updated_at
            FROM ai_conversations
            WHERE patient_account_id = #{patientAccountId}
              AND legacy_key = #{legacyKey}
            LIMIT 1
            """)
    AIConversation selectByPatientAndLegacyKey(
            @Param("patientAccountId") Long patientAccountId,
            @Param("legacyKey") String legacyKey
    );
}
