package com.campus.auth.service;

import com.campus.auth.dto.ResetPasswordRequest;
import com.campus.auth.entity.User;
import com.campus.auth.mapper.UserMapper;
import com.campus.auth.token.OtpStore;
import com.campus.auth.token.RefreshTokenStore;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for session revocation (V-R): logout and resetPassword must invalidate all
 * existing tokens by incrementing the user's {@code token_version}, and logout must also revoke
 * the refresh-token jti in {@link RefreshTokenStore}.
 */
@ExtendWith(MockitoExtension.class)
class TokenVersionRevokeTest {

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
    private ObjectMapper objectMapper;
    @Mock
    private RefreshTokenStore refreshTokenStore;
    @Mock
    private OtpStore otpStore;
    @Mock
    private HttpServletResponse response;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userMapper, redisUtil, emailUtil, jwtUtil, passwordEncoder,
                objectMapper, refreshTokenStore, otpStore);
    }

    @Test
    void logoutIncrementsTokenVersionAndRevokesRefreshToken() {
        authService.logout(1L, "jti-abc", response);

        // token version bump (custom mapper method) + refresh-token revocation
        verify(userMapper).incrTokenVersion(1L);
        verify(refreshTokenStore).revoke(eq("jti-abc"));
    }

    @Test
    void resetPasswordIncrementsTokenVersion() {
        when(otpStore.verify(eq("RESET_PASSWORD"), anyString(), anyString())).thenReturn(true);
        User user = new User();
        user.setId(1L);
        when(userMapper.selectByEmail("a@csu.edu.cn")).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("ENC");

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("a@csu.edu.cn");
        req.setCode("123456");
        req.setNewPassword("NewPass#123");

        authService.resetPassword(req);

        // resetPassword updates the password AND bumps token_version.
        verify(userMapper).updatePasswordAndResetFail(eq(1L), eq("ENC"));
        verify(userMapper).incrTokenVersion(1L);
    }
}
