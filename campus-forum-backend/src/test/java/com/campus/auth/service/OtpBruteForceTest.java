package com.campus.auth.service;

import com.campus.auth.dto.ResetPasswordRequest;
import com.campus.auth.entity.User;
import com.campus.auth.mapper.UserMapper;
import com.campus.auth.token.OtpStore;
import com.campus.auth.token.RefreshTokenStore;
import com.campus.common.exception.BusinessException;
import com.campus.common.util.EmailUtil;
import com.campus.common.util.JwtUtil;
import com.campus.common.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for V-B: the {@link OtpStore} invalidates the verification code after
 * 5 consecutive wrong attempts, so a subsequent attempt with the *correct* code is rejected.
 *
 * We drive a REAL {@link OtpStore} (the component that owns the attempt-counter logic) with a
 * mocked {@link StringRedisTemplate}, simulating the Redis counters and the lock key. The
 * {@link AuthService#resetPassword} path is exercised end-to-end. GREEN: matches the implemented
 * behavior.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpBruteForceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RedisUtil redisUtil;
    @Mock
    private EmailUtil emailUtil;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenStore refreshTokenStore;
    @Mock
    private StringRedisTemplate redisTemplate;

    private AuthService authService;

    private static final String EMAIL = "alice@csu.edu.cn";
    private static final String CORRECT = "123456";

    @BeforeEach
    void setUp() {
        OtpStore otpStore = new OtpStore(redisTemplate, "campus:otp:", 300, 5, 60);
        authService = new AuthService(userMapper, redisUtil, emailUtil, jwtUtil, passwordEncoder,
                new ObjectMapper(), refreshTokenStore, otpStore);
        ReflectionTestUtils.setField(authService, "schoolEmailDomain", "@csu.edu.cn");
    }

    @Test
    void fiveWrongAttemptsInvalidateCode() {
        String codeKey = "campus:otp:RESET_PASSWORD:" + EMAIL;
        String attKey = codeKey + ":att";
        String lockKey = codeKey + ":lock";

        ValueOperations<String, String> vops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(vops);

        AtomicBoolean codeDeleted = new AtomicBoolean(false);
        AtomicBoolean locked = new AtomicBoolean(false);

        when(vops.get(codeKey)).thenAnswer(inv -> codeDeleted.get() ? null : CORRECT);
        when(redisTemplate.hasKey(lockKey)).thenAnswer(inv -> locked.get());
        doAnswer(inv -> { locked.set(true); return null; })
                .when(vops).set(eq(lockKey), anyString(), anyLong(), any());
        doAnswer(inv -> { codeDeleted.set(true); return null; })
                .when(redisTemplate).delete(eq(codeKey));
        when(redisTemplate.expire(anyString(), anyLong(), any())).thenReturn(true);

        AtomicInteger counter = new AtomicInteger(0);
        when(vops.increment(eq(attKey))).thenAnswer(inv -> (long) counter.incrementAndGet());

        when(userMapper.selectByEmail(EMAIL)).thenReturn(new User());
        when(passwordEncoder.encode(anyString())).thenReturn("ENC");

        ResetPasswordRequest wrong = new ResetPasswordRequest();
        wrong.setEmail(EMAIL);
        wrong.setCode("000000");
        wrong.setNewPassword("NewPass#123");

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.resetPassword(wrong))
                    .isInstanceOf(BusinessException.class);
        }

        // 6th attempt with the *correct* code must now be rejected (code invalidated + locked)
        ResetPasswordRequest correct = new ResetPasswordRequest();
        correct.setEmail(EMAIL);
        correct.setCode(CORRECT);
        correct.setNewPassword("NewPass#123");

        assertThatThrownBy(() -> authService.resetPassword(correct))
                .isInstanceOf(BusinessException.class);

        // After 5 wrong attempts the code was invalidated (deleted) and a lock was set.
        verify(redisTemplate).delete(eq(codeKey));
        verify(vops).set(eq(lockKey), anyString(), anyLong(), any());
    }
}
