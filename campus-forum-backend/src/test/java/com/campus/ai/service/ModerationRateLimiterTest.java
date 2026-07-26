package com.campus.ai.service;

import com.campus.ai.exception.ContentRejectedException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModerationRateLimiterTest {

    private ModerationRateLimiter limiter(int perUser, int global) {
        ModerationRateLimiter l = new ModerationRateLimiter();
        ReflectionTestUtils.setField(l, "perUserLimit", perUser);
        ReflectionTestUtils.setField(l, "globalLimit", global);
        ReflectionTestUtils.setField(l, "windowSeconds", 60);
        return l;
    }

    @Test
    void allowsWithinLimits() {
        ModerationRateLimiter l = limiter(5, 100);
        assertThatCode(() -> {
            for (int i = 0; i < 5; i++) {
                l.tryAcquire(1L);
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void rejectsWhenPerUserLimitExceeded() {
        ModerationRateLimiter l = limiter(2, 100);
        l.tryAcquire(7L);
        l.tryAcquire(7L);
        assertThatThrownBy(() -> l.tryAcquire(7L))
                .isInstanceOf(ContentRejectedException.class);
    }

    @Test
    void rejectsWhenGlobalLimitExceeded() {
        ModerationRateLimiter l = limiter(1000, 1);
        l.tryAcquire(1L);
        assertThatThrownBy(() -> l.tryAcquire(2L))
                .isInstanceOf(ContentRejectedException.class);
    }
}
