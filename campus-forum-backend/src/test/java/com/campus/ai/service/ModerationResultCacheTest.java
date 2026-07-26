package com.campus.ai.service;

import com.campus.ai.dto.AiModerationAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationResultCacheTest {

    private ModerationResultCache cache(boolean enabled, long ttl) {
        ModerationResultCache c = new ModerationResultCache();
        ReflectionTestUtils.setField(c, "enabled", enabled);
        ReflectionTestUtils.setField(c, "ttlSeconds", ttl);
        return c;
    }

    @Test
    void returnsSameAdviceForIdenticalContent() {
        ModerationResultCache c = cache(true, 600);
        AiModerationAdvice advice = new AiModerationAdvice("LOW", List.of(), 0.9, List.of(), "ALLOW", "m");
        c.put("POST", "标题", "正文", advice);

        AiModerationAdvice hit = c.get("POST", "标题", "正文");
        assertThat(hit).isNotNull();
        assertThat(hit.getRiskLevel()).isEqualTo("LOW");
    }

    @Test
    void differentContentMisses() {
        ModerationResultCache c = cache(true, 600);
        c.put("POST", "标题A", "正文A", new AiModerationAdvice("LOW", List.of(), 0.9, List.of(), "ALLOW", "m"));

        assertThat(c.get("POST", "标题B", "正文B")).isNull();
    }

    @Test
    void returnsDefensiveCopyNotOriginal() {
        ModerationResultCache c = cache(true, 600);
        AiModerationAdvice original = new AiModerationAdvice("LOW", List.of(), 0.9, List.of(), "ALLOW", "m");
        c.put("POST", "t", "c", original);

        AiModerationAdvice hit = c.get("POST", "t", "c");
        hit.setRiskLevel("HIGH"); // 篡改副本不应影响缓存中的原始对象

        assertThat(c.get("POST", "t", "c").getRiskLevel()).isEqualTo("LOW");
    }

    @Test
    void expiresAfterTtl() {
        ModerationResultCache c = cache(true, 0); // TTL=0 → 立即过期
        c.put("POST", "t", "c", new AiModerationAdvice("LOW", List.of(), 0.9, List.of(), "ALLOW", "m"));
        assertThat(c.get("POST", "t", "c")).isNull();
    }

    @Test
    void disabledReturnsNull() {
        ModerationResultCache c = cache(false, 600);
        c.put("POST", "t", "c", new AiModerationAdvice("LOW", List.of(), 0.9, List.of(), "ALLOW", "m"));
        assertThat(c.get("POST", "t", "c")).isNull();
    }
}
