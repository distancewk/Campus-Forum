package com.campus.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_knowledge_chunk")
public class AiKnowledgeChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private String sourceType;

    private Long sourceId;

    private Integer chunkIndex;

    private String title;

    private String content;

    private String contentHash;

    private String embedding;

    private Integer tokenCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
