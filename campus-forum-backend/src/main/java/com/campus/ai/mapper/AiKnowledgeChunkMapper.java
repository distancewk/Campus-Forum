package com.campus.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.ai.dto.RetrievedChunk;
import com.campus.ai.entity.AiKnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiKnowledgeChunkMapper extends BaseMapper<AiKnowledgeChunk> {

    void insertChunk(AiKnowledgeChunk chunk);

    void deleteByDocumentId(@Param("documentId") Long documentId);

    List<AiKnowledgeChunk> selectByDocumentId(@Param("documentId") Long documentId);

    List<RetrievedChunk> hybridSearch(@Param("keyword") String keyword,
                                      @Param("embedding") String embedding,
                                      @Param("limit") int limit);
}
