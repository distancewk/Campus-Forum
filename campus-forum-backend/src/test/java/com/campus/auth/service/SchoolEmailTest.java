package com.campus.auth.service;

import com.campus.auth.mapper.UserMapper;
import com.campus.auth.token.OtpStore;
import com.campus.auth.token.RefreshTokenStore;
import com.campus.common.exception.BusinessException;
import com.campus.common.util.EmailUtil;
import com.campus.common.util.JwtUtil;
import com.campus.common.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for V-N: school-email validation must accept a real school
 * domain and reject non-school / spoofed domains.
 *
 * NOTE: the production code currently validates by {@code email.endsWith(schoolEmailDomain)}
 * (configurable, NOT the anchored regex {@code ^@[\w.-]+\.edu\.cn$} described in V-N).
 * With {@code schoolEmailDomain=@csu.edu.cn} the example inputs below behave as specified,
 * but the anchored-regex guarantee (e.g. rejection of subdomain tricks) is NOT enforced.
 * This test encodes the example-level property; see report for details.
 */
@ExtendWith(MockitoExtension.class)
class SchoolEmailTest {

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
    private HttpServletResponse response;
    @Mock
    private RefreshTokenStore refreshTokenStore;
    @Mock
    private OtpStore otpStore;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userMapper, redisUtil, emailUtil, jwtUtil, passwordEncoder,
                new ObjectMapper(), refreshTokenStore, otpStore);
        ReflectionTestUtils.setField(authService, "schoolEmailDomain", "@csu.edu.cn");
        // Only the "accepts" path reaches canResend; the reject paths bail out earlier.
        lenient().when(otpStore.canResend(anyString(), anyString())).thenReturn(true);
    }

    @Test
    void acceptsValidSchoolEmail() {
        assertThatCode(() -> authService.sendVerifyCode("alice@csu.edu.cn", "REGISTER"))
                .doesNotThrowAnyException();
        verify(emailUtil).sendVerifyCode(eq("alice@csu.edu.cn"), anyString());
    }

    @Test
    void rejectsNonEducationEmail() {
        assertThatThrownBy(() -> authService.sendVerifyCode("alice@evil.com", "REGISTER"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("学校邮箱");
    }

    @Test
    void rejectsSubdomainSpoof() {
        assertThatThrownBy(() -> authService.sendVerifyCode("alice@x.edu.cn.evil.com", "REGISTER"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("学校邮箱");
    }
}
