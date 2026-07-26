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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiModerationService {
    private static final Logger log = LoggerFactory.getLogger(AiModerationService.class);
    private static final String STATUS_PENDING_ADMIN = "PENDING_ADMIN";
    private static final String STATUS_AUTO_ALLOWED = "AUTO_ALLOWED";

    private final AiProviderClient aiProviderClient;
    private final AiModerationResultMapper moderationResultMapper;
    private final ModerationResultCache moderationResultCache;
    private final ModerationRateLimiter moderationRateLimiter;
    private final ModerationMetrics moderationMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 内容审核默认开启（fail-closed）。异常/模型失败按"拒绝/转人工"处理，绝不自动放行。
     * 仅当用户/环境显式关闭审核（moderationEnabled=false）时，视为信任该环境并直接放行。
     */
    @Value("${campus.ai.moderation-enabled:true}")
    private boolean moderationEnabled;

    // 置信度阈值：与 application.yml 的 campus.ai.moderation-medium-threshold / -high-threshold 对齐。
    @Value("${campus.ai.moderation-medium-threshold:0.55}")
    private double mediumThreshold;
    @Value("${campus.ai.moderation-high-threshold:0.82}")
    private double highThreshold;

    public AiModerationAdvice review(String targetType, String title, String content, Long authorId, Long targetId) {
        if (!moderationEnabled) {
            // 审核被显式关闭：直接返回 LOW/ALLOW，让调用方按"通过"处理（仅用于本地 dev 等可信任环境）。
            return new AiModerationAdvice("LOW", List.of(), 0.0, List.of(), "ALLOW", "moderation-disabled");
        }
        // 1) 去重缓存：相同内容在 TTL 内直接复用，跳过模型调用（省成本/延迟）。
        AiModerationAdvice cached = moderationResultCache.get(targetType, title, content);
        if (cached != null) {
            moderationMetrics.record(cached.getRiskLevel(), cached.getSuggestedAction(), 0L);
            log.debug("AI 审核命中去重缓存 targetType={}", targetType);
            return cached;
        }
        long start = System.nanoTime();
        try {
            // 2) 限流：在真正调用模型前计数，超限按 fail-closed 转人工复核（成本封顶近似）。
            moderationRateLimiter.tryAcquire(authorId);

            AiModerationAdvice advice = aiProviderClient.moderate(targetType, title, content);
            if (advice == null || advice.getRiskLevel() == null || advice.getSuggestedAction() == null) {
                // 模型返回结果无效：fail-closed，拒绝发布。
                throw new ContentRejectedException("内容审核结果无效，已拒绝发布，等待人工复核");
            }
            // 用模型返回的 confidence 对 riskLevel 做二次判定（见 normalizeRiskLevel）。
            normalizeRiskLevel(advice);
            moderationResultCache.put(targetType, title, content, advice);
            moderationMetrics.record(advice.getRiskLevel(), advice.getSuggestedAction(), System.nanoTime() - start);
            return advice;
        } catch (ContentRejectedException e) {
            throw e;
        } catch (RuntimeException e) {
            // 超时、网络错误、解析失败、模型异常、限流：fail-closed，拒绝发布，绝不回退到 ALLOW。
            moderationMetrics.recordFailure(System.nanoTime() - start);
            throw new ContentRejectedException("内容审核失败，已拒绝发布，等待人工复核", e);
        }
    }

    /**
     * 用模型返回的 confidence / suggestedAction 对 riskLevel 做二次判定，减少误判。
     * 原则（保留 fail-closed 安全模型）：绝不把模型标记为 non-LOW 的内容自动放行；
     * confidence 仅用于：
     *  - 把"低置信度的 LOW"升级为 MEDIUM，转人工复核，避免模型不确定时误放行；
     *  - 把"低置信度的 HIGH"降级为 MEDIUM（仍转人工复核），避免高置信误杀。
     * suggestedAction=REJECT 视为强信号：即使模型给出 LOW，也升级为 MEDIUM 转人工复核，
     * 避免"模型判 LOW 但建议拒绝"的内容被自动放行（REJECT 真正生效）。
     * MEDIUM 维持原样（转人工复核）。
     */
    private void normalizeRiskLevel(AiModerationAdvice advice) {
        double confidence = advice.getConfidence();
        String level = advice.getRiskLevel();
        if ("REJECT".equalsIgnoreCase(advice.getSuggestedAction()) && "LOW".equals(level)) {
            advice.setRiskLevel("MEDIUM");
            log.warn("AI 审核：模型建议 REJECT 但 riskLevel=LOW，升级为 MEDIUM 转人工复核");
            return;
        }
        if ("LOW".equals(level)) {
            if (confidence < mediumThreshold) {
                advice.setRiskLevel("MEDIUM");
                log.warn("AI 审核：低置信度 LOW（confidence={} < {}）升级为 MEDIUM 转人工复核",
                        confidence, mediumThreshold);
            }
        } else if ("HIGH".equals(level)) {
            if (confidence < highThreshold) {
                advice.setRiskLevel("MEDIUM");
                log.info("AI 审核：低置信度 HIGH（confidence={} < {}）降级为 MEDIUM（仍转人工复核）",
                        confidence, highThreshold);
            }
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
