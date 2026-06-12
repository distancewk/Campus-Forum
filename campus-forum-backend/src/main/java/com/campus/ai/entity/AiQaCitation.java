package com.campus.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_qa_citation")
public class AiQaCitation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long chunkId;
    private String sourceType;
    private Long sourceId;
    private String title;
    private String snippet;
    private Double score;
    private LocalDateTime createdAt;
}
