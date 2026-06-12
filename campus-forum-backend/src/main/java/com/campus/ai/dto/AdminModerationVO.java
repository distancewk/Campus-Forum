package com.campus.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminModerationVO {
    private Long id;
    private String targetType;
    private Long targetId;
    private Long authorId;
    private String riskLevel;
    private String riskTypes;
    private Double confidence;
    private String reasons;
    private String suggestedAction;
    private String modelName;
    private String status;
    private LocalDateTime createdAt;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
}
