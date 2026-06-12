package com.campus.ai.service;

import com.campus.ai.dto.AdminModerationQuery;
import com.campus.ai.dto.AdminModerationVO;
import com.campus.ai.client.AiProviderClient;
import com.campus.ai.config.AiProperties;
import com.campus.ai.dto.AiModerationAdvice;
import com.campus.ai.entity.AiModerationResult;
import com.campus.ai.mapper.AiModerationResultMapper;
import com.campus.common.response.PageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiModerationService {
    private static final String STATUS_PENDING_ADMIN = "PENDING_ADMIN";
    private static final String STATUS_AUTO_ALLOWED = "AUTO_ALLOWED";

    private final AiProviderClient aiProviderClient;
    private final AiModerationResultMapper moderationResultMapper;
    private final AiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiModerationAdvice review(String targetType, String title, String content, Long authorId, Long targetId) {
        if (!properties.getModeration().isEnabled()) {
            return new AiModerationAdvice("LOW", List.of(), 0.0, List.of("AI 审核已关闭"), "ALLOW", "disabled");
        }
        try {
            AiModerationAdvice advice = aiProviderClient.moderate(targetType, title, content);
            if (advice == null || advice.getRiskLevel() == null || advice.getSuggestedAction() == null) {
                return localFallback(content);
            }
            return advice;
        } catch (RuntimeException e) {
            return localFallback(content);
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

    private AiModerationAdvice localFallback(String content) {
        String text = content == null ? "" : content;
        List<String> types = new ArrayList<>();
        if (text.matches("(?is).*(微信|vx|QQ|q群|手机号|电话|加群).*")) {
            types.add("CONTACT_DIVERSION");
        }
        if (text.matches("(?is).*(刷单|兼职日结|返利|稳赚|贷款|中奖).*")) {
            types.add("SCAM");
        }
        if (text.length() > 20 && text.chars().distinct().count() < 8) {
            types.add("FLOODING");
        }
        if (types.isEmpty()) {
            return new AiModerationAdvice("LOW", List.of(), 0.1, List.of("AI 服务不可用，本地规则未命中"), "ALLOW", "local-fallback");
        }
        return new AiModerationAdvice("MEDIUM", types, 0.6, List.of("AI 服务不可用，本地兜底规则命中"), "REVIEW", "local-fallback");
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
