package com.campus.ai.service;

import com.campus.ai.client.AiProviderClient;
import com.campus.ai.dto.AiChatMessage;
import com.campus.ai.dto.AiModerationAdvice;
import com.campus.ai.entity.AiModerationResult;
import com.campus.ai.exception.ContentRejectedException;
import com.campus.ai.mapper.AiModerationResultMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiModerationServiceTest {

    @Mock
    private AiModerationResultMapper moderationResultMapper;

    /** 构造一个审核服务：限流阈值拉高、缓存关闭，避免影响单元断言。 */
    private AiModerationService service(AiProviderClient provider) {
        ModerationRateLimiter limiter = new ModerationRateLimiter();
        ReflectionTestUtils.setField(limiter, "perUserLimit", 1_000_000);
        ReflectionTestUtils.setField(limiter, "globalLimit", 1_000_000);
        ReflectionTestUtils.setField(limiter, "windowSeconds", 60);

        ModerationResultCache cache = new ModerationResultCache();
        ReflectionTestUtils.setField(cache, "enabled", false);

        return new AiModerationService(provider, null, cache, limiter, new ModerationMetrics());
    }

    @Test
    void reviewFailsClosedWhenProviderFails() {
        AiModerationService service = service(new FailingProvider());
        ReflectionTestUtils.setField(service, "moderationEnabled", true);

        assertThatThrownBy(() -> service.review("POST", "资料", "加我微信 abc123 领资料", 1L, null))
                .isInstanceOf(ContentRejectedException.class);
    }

    @Test
    void bindTargetAndSavePersistsPendingStatusForMediumRisk() {
        AiModerationService service = new AiModerationService(
                new FailingProvider(), moderationResultMapper,
                new ModerationResultCache(), new ModerationRateLimiter(), new ModerationMetrics());
        AiModerationAdvice advice = new AiModerationAdvice(
                "MEDIUM", List.of("SCAM"), 0.81, List.of("疑似兼职诈骗"), "REVIEW", "test-model");

        service.bindTargetAndSave(advice, "COMMENT", 30L, 40L);

        ArgumentCaptor<AiModerationResult> captor = ArgumentCaptor.forClass(AiModerationResult.class);
        verify(moderationResultMapper).insertResult(captor.capture());
        AiModerationResult result = captor.getValue();
        assertThat(result.getTargetType()).isEqualTo("COMMENT");
        assertThat(result.getTargetId()).isEqualTo(30L);
        assertThat(result.getAuthorId()).isEqualTo(40L);
        assertThat(result.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(result.getRiskTypes()).contains("SCAM");
        assertThat(result.getStatus()).isEqualTo("PENDING_ADMIN");
    }

    static class FailingProvider implements AiProviderClient {
        public List<Double> createEmbedding(String input) {
            throw new RuntimeException("down");
        }

        public String createChatCompletion(List<AiChatMessage> messages) {
            throw new RuntimeException("down");
        }

        public String createChatCompletion(List<AiChatMessage> messages, Map<String, Object> responseFormat) {
            throw new RuntimeException("down");
        }

        public AiModerationAdvice moderate(String targetType, String title, String content) {
            throw new RuntimeException("down");
        }
    }

    static class StubProvider implements AiProviderClient {
        private final AiModerationAdvice advice;

        StubProvider(AiModerationAdvice advice) {
            this.advice = advice;
        }

        public List<Double> createEmbedding(String input) {
            throw new UnsupportedOperationException();
        }

        public String createChatCompletion(List<AiChatMessage> messages) {
            throw new UnsupportedOperationException();
        }

        public String createChatCompletion(List<AiChatMessage> messages, Map<String, Object> responseFormat) {
            throw new UnsupportedOperationException();
        }

        public AiModerationAdvice moderate(String targetType, String title, String content) {
            return advice;
        }
    }

    @Test
    void reviewPromotesLowConfidenceLowToMedium() {
        AiModerationService service = service(
                new StubProvider(new AiModerationAdvice("LOW", List.of(), 0.1, List.of(), "ALLOW", "m")));
        ReflectionTestUtils.setField(service, "moderationEnabled", true);
        ReflectionTestUtils.setField(service, "mediumThreshold", 0.55);
        ReflectionTestUtils.setField(service, "highThreshold", 0.82);

        AiModerationAdvice result = service.review("POST", "t", "c", 1L, null);
        assertThat(result.getRiskLevel()).isEqualTo("MEDIUM");
    }

    @Test
    void reviewKeepsHighWhenConfident() {
        AiModerationService service = service(
                new StubProvider(new AiModerationAdvice("HIGH", List.of(), 0.95, List.of(), "REJECT", "m")));
        ReflectionTestUtils.setField(service, "moderationEnabled", true);
        ReflectionTestUtils.setField(service, "mediumThreshold", 0.55);
        ReflectionTestUtils.setField(service, "highThreshold", 0.82);

        AiModerationAdvice result = service.review("POST", "t", "c", 1L, null);
        assertThat(result.getRiskLevel()).isEqualTo("HIGH");
    }

    @Test
    void reviewDowngradesLowConfidenceHighToMedium() {
        AiModerationService service = service(
                new StubProvider(new AiModerationAdvice("HIGH", List.of(), 0.5, List.of(), "REJECT", "m")));
        ReflectionTestUtils.setField(service, "moderationEnabled", true);
        ReflectionTestUtils.setField(service, "mediumThreshold", 0.55);
        ReflectionTestUtils.setField(service, "highThreshold", 0.82);

        AiModerationAdvice result = service.review("POST", "t", "c", 1L, null);
        assertThat(result.getRiskLevel()).isEqualTo("MEDIUM");
    }

    @Test
    void reviewRejectOnLowIsEscalatedToMedium() {
        AiModerationService service = service(
                new StubProvider(new AiModerationAdvice("LOW", List.of(), 0.9, List.of(), "REJECT", "m")));
        ReflectionTestUtils.setField(service, "moderationEnabled", true);
        ReflectionTestUtils.setField(service, "mediumThreshold", 0.55);
        ReflectionTestUtils.setField(service, "highThreshold", 0.82);

        AiModerationAdvice result = service.review("POST", "t", "c", 1L, null);
        // REJECT 真正生效：即使模型给 LOW，也升级为 MEDIUM 转人工，避免自动放行。
        assertThat(result.getRiskLevel()).isEqualTo("MEDIUM");
    }

    @Test
    void reviewReusesCachedResultForIdenticalContent() {
        CountingProvider provider = new CountingProvider(
                new AiModerationAdvice("LOW", List.of(), 0.9, List.of(), "ALLOW", "m"));
        ModerationResultCache cache = new ModerationResultCache();
        ReflectionTestUtils.setField(cache, "enabled", true);
        ReflectionTestUtils.setField(cache, "ttlSeconds", 600);
        ModerationRateLimiter limiter = new ModerationRateLimiter();
        ReflectionTestUtils.setField(limiter, "perUserLimit", 1_000_000);
        ReflectionTestUtils.setField(limiter, "globalLimit", 1_000_000);
        ReflectionTestUtils.setField(limiter, "windowSeconds", 60);

        AiModerationService service = new AiModerationService(
                provider, null, cache, limiter, new ModerationMetrics());
        ReflectionTestUtils.setField(service, "moderationEnabled", true);

        service.review("POST", "二手教材", "出一本线代教材", 1L, null);
        service.review("POST", "二手教材", "出一本线代教材", 1L, null);

        // 第二次相同内容应命中缓存，不再调用模型。
        assertThat(provider.callCount).isEqualTo(1);
    }

    @Test
    void reviewRateLimitRejectsWhenGlobalLimitExceeded() {
        CountingProvider provider = new CountingProvider(
                new AiModerationAdvice("LOW", List.of(), 0.9, List.of(), "ALLOW", "m"));
        ModerationRateLimiter limiter = new ModerationRateLimiter();
        ReflectionTestUtils.setField(limiter, "perUserLimit", 1_000_000);
        ReflectionTestUtils.setField(limiter, "globalLimit", 1); // 第 2 次即超限
        ReflectionTestUtils.setField(limiter, "windowSeconds", 60);
        ModerationResultCache cache = new ModerationResultCache();
        ReflectionTestUtils.setField(cache, "enabled", false);

        AiModerationService service = new AiModerationService(
                provider, null, cache, limiter, new ModerationMetrics());
        ReflectionTestUtils.setField(service, "moderationEnabled", true);

        service.review("POST", "t1", "c1", 1L, null); // 第 1 次：通过
        assertThatThrownBy(() -> service.review("POST", "t2", "c2", 2L, null))
                .isInstanceOf(ContentRejectedException.class); // 第 2 次：超限 fail-closed
    }

    static class CountingProvider implements AiProviderClient {
        private final AiModerationAdvice advice;
        int callCount = 0;

        CountingProvider(AiModerationAdvice advice) {
            this.advice = advice;
        }

        public List<Double> createEmbedding(String input) {
            throw new UnsupportedOperationException();
        }

        public String createChatCompletion(List<AiChatMessage> messages) {
            throw new UnsupportedOperationException();
        }

        public String createChatCompletion(List<AiChatMessage> messages, Map<String, Object> responseFormat) {
            throw new UnsupportedOperationException();
        }

        public AiModerationAdvice moderate(String targetType, String title, String content) {
            callCount++;
            return advice;
        }
    }
}
