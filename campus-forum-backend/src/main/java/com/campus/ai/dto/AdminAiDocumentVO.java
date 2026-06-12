package com.campus.ai.dto;

import com.campus.ai.entity.AiKnowledgeDocument;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAiDocumentVO {
    private Long id;
    private String title;
    private String sourceType;
    private Long sourceId;
    private String fileUrl;
    private String fileType;
    private String status;
    private Long createdBy;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminAiDocumentVO from(AiKnowledgeDocument document) {
        AdminAiDocumentVO vo = new AdminAiDocumentVO();
        vo.setId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setSourceType(document.getSourceType());
        vo.setSourceId(document.getSourceId());
        vo.setFileUrl(document.getFileUrl());
        vo.setFileType(document.getFileType());
        vo.setStatus(document.getStatus());
        vo.setCreatedBy(document.getCreatedBy());
        vo.setErrorMessage(document.getErrorMessage());
        vo.setCreatedAt(document.getCreatedAt());
        vo.setUpdatedAt(document.getUpdatedAt());
        return vo;
    }
}
