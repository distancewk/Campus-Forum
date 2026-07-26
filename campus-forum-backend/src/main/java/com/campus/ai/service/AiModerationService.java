package com.campus.ai.service;

import com.campus.ai.dto.AdminModerationQuery;
import com.campus.ai.dto.AdminModerationVO;
import com.campus.ai.client.AiProviderClient;
import com.campus.ai.dto.AiModerationAdvice;
import com.campus.ai.entity.AiModerationResult;
import com.campus.ai.exception.ContentRejectedException;
import com.campus.ai.mapper.AiModerationResultMapper;
import com.campus.common.response.PageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiModerationService {
    private static final String STATUS_PENDING_ADMIN = "PENDING_ADMIN";
    private static final String STATUS_AUTO_ALLOWED = "AUTO_ALLOWED";

    private final AiProviderClient aiProviderClient;
    private final AiModerationResultMapper moderationResultMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 内容审核默认开启（fail-closed）。任何关闭/异常/模型失败都按"拒绝/转人工"处理，绝不自动放行。
     */
    @Value("${campus.ai.moderation-enabled:true}")
    private boolean moderationEnabled;

    public AiModerationAdvice review(String targetType, String title, String content, Long authorId, Long targetId) {
        if (!moderationEnabled) {
            // 审核未开启也按 fail-closed 处理：拒绝发布并转人工复核，不允许自动放行。
            throw new ContentRejectedException("内容审核未启用，已拒绝发布，等待人工复核");
        }
        try {
            AiModerationAdvice advice = aiProviderClient.moderate(targetType, title, content);
            if (advice == null || advice.getRiskLevel() == null || advice.getSuggestedAction() == null) {
                // 模型返回结果无效：fail-closed，拒绝发布。
                throw new ContentRejectedException("内容审核结果无效，已拒绝发布，等待人工复核");
            }
            return advice;
        } catch (ContentRejectedException e) {
            throw e;
        } catch (RuntimeException e) {
            // 超时、网络错误、解析失败、模型异常：fail-closed，拒绝发布，绝不回退到 ALLOW。
            throw new ContentRejectedException("内容审核失败，已拒绝发布，等待人工复核", e);
        }
    }

    public void bindTargetAndSave(AiModerationAdvice advice, String targetType, Long targetId, Long authorId) {
        if (moderationResultMapper == null || advice == null || targetId == null || authorId == null) {
            return;
        }
        AiModerationResult result = new AiModerationResult();
        result.setTargetType(targetType);
        result.setTargetId(targetId);
        result.setAuthorId(authorId);
        result.setRiskLevel(advice.getRiskLevel());
        result.setRiskTypes(toJson(advice.getRiskTypes()));
        result.setConfidence(advice.getConfidence());
        result.setReasons(toJson(advice.getReasons()));
        result.setSuggestedAction(advice.getSuggestedAction());
        result.setModelName(advice.getModelName());
        result.setStatus("LOW".equals(advice.getRiskLevel()) ? STATUS_AUTO_ALLOWED : STATUS_PENDING_ADMIN);
        result.setCreatedAt(LocalDateTime.now());
        moderationResultMapper.insertResult(result);
    }

    public void markAdminReviewed(String targetType, Long targetId, String status, Long adminId) {
        if (moderationResultMapper == null || targetId == null) {
            return;
        }
        moderationResultMapper.markReviewed(targetType, targetId, status, adminId);
    }

    public PageResult<AdminModerationVO> listForAdmin(AdminModerationQuery query) {
        int offset = query.getOffset();
        int size = query.getSize();
        List<AdminModerationVO> records = moderationResultMapper.selectAdminModerationList(
                query.getTargetType(), query.getRiskLevel(), query.getRiskType(), offset, size);
        long total = moderationResultMapper.countAdminModerationList(
                query.getTargetType(), query.getRiskLevel(), query.getRiskType());
        return new PageResult<>(records, total, query.getPage(), query.getSize());
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
