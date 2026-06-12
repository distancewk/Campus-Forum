package com.campus.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunk {
    private Long chunkId;
    private Long documentId;
    private String sourceType;
    private Long sourceId;
    private String title;
    private String content;
    private double score;
}
