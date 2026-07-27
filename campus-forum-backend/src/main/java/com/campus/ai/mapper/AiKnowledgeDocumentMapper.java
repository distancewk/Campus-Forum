package com.campus.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.ai.entity.AiKnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiKnowledgeDocumentMapper extends BaseMapper<AiKnowledgeDocument> {

    @Select("SELECT id, title, source_type, source_id, file_url, file_type, status, error_message, " +
            "created_by, created_at, updated_at " +
            "FROM ai_knowledge_document " +
            "WHERE source_type = #{sourceType} AND source_id = #{sourceId}")
    List<AiKnowledgeDocument> selectBySource(@Param("sourceType") String sourceType,
                                            @Param("sourceId") Long sourceId);
}
