package com.campus.ai.dto;

import lombok.Data;

@Data
public class AiCitationVO {
    private Long chunkId;
    private String sourceType;
    private Long sourceId;
    private String title;
    private String snippet;
    private double score;
}
