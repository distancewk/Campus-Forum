package com.campus.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for rate limiting: after {@code limit} requests within a window from the same
 * key, the interceptor blocks (preHandle returns false) and sets HTTP 429.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() throws Exception {
        ValueOperations<String, String> vops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(vops);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        // Same key -> incrementing counter 1,2,3,4...
        AtomicInteger counter = new AtomicInteger(0);
        when(vops.increment(anyString())).thenAnswer(inv -> (long) counter.incrementAndGet());

        // Authenticate as user 7 so the "@RateLimit(key="#userId")" scope resolves to "7".
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(7L, null, List.of()));

        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        interceptor = new RateLimitInterceptor(redisTemplate, "campus:rl:");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksAfterLimit() throws Exception {
        HandlerMethod hm = new HandlerMethod(new DummyController(), DummyController.class.getMethod("limited"));

        // limit = 3 -> first 3 pass, 4th is blocked.
        assertThat(interceptor.preHandle(request, response, hm)).isTrue();
        assertThat(interceptor.preHandle(request, response, hm)).isTrue();
        assertThat(interceptor.preHandle(request, response, hm)).isTrue();
        assertThat(interceptor.preHandle(request, response, hm)).isFalse();

        verify(response).setStatus(429);
    }

    @org.springframework.stereotype.Controller
    static class DummyController {
        @RateLimit(scope = "user", key = "#userId", limit = 3, window = 60)
        public void limited() {
        }
    }
}
