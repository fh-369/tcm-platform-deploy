package com.tcm.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tcm.platform.dto.ConsultationMessageSummary;
import com.tcm.platform.entity.ConsultationMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ConsultationMessageMapper extends BaseMapper<ConsultationMessage> {

    @Select("""
            SELECT id,
                   consultation_id,
                   sender_type,
                   sender_id,
                   sender_name,
                   content,
                   created_at
            FROM consultation_messages
            WHERE consultation_id = #{consultationId}
            ORDER BY created_at ASC, id ASC
            """)
    List<ConsultationMessage> selectByConsultationId(
            @Param("consultationId") Long consultationId
    );

    @Select({
            "<script>",
            "SELECT grouped.consultation_id AS consultationId,",
            "       grouped.message_count AS messageCount,",
            "       latest.content AS latestMessage,",
            "       latest.sender_type AS latestMessageSenderType,",
            "       latest.created_at AS latestMessageAt",
            "FROM (",
            "    SELECT consultation_id, COUNT(*) AS message_count, MAX(id) AS latest_id",
            "    FROM consultation_messages",
            "    WHERE consultation_id IN",
            "    <foreach collection='consultationIds' item='id' open='(' separator=',' close=')'>",
            "        #{id}",
            "    </foreach>",
            "    GROUP BY consultation_id",
            ") grouped",
            "JOIN consultation_messages latest ON latest.id = grouped.latest_id",
            "</script>"
    })
    List<ConsultationMessageSummary> selectSummaries(
            @Param("consultationIds") List<Long> consultationIds
    );
}
