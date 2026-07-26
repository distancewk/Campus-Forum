package com.campus.auth.service;

import com.campus.auth.dto.RegisterRequest;
import com.campus.auth.dto.VerifyCodeRequest;
import com.campus.auth.entity.User;
import com.campus.auth.mapper.UserMapper;
import com.campus.auth.token.OtpStore;
import com.campus.auth.token.RefreshTokenStore;
import com.campus.common.util.EmailUtil;
import com.campus.common.util.JwtUtil;
import com.campus.common.util.RedisUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    private static final String EMAIL = "student@xxx.edu.cn";
    private static final String STUDENT_NO = "20240001";
    private static final String NICKNAME = "测试同学";
    private static final String RAW_PASSWORD = "secret123";
    private static final String ENCODED_PASSWORD = "$2a$10$encoded";

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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        authService = new AuthService(userMapper, redisUtil, emailUtil, jwtUtil, passwordEncoder,
                objectMapper, refreshTokenStore, otpStore);
        ReflectionTestUtils.setField(authService, "refreshExpiration", 604800000L);
        ReflectionTestUtils.setField(authService, "cookieSecure", false);
        ReflectionTestUtils.setField(authService, "cookieSameSite", "Lax");
        ReflectionTestUtils.setField(authService, "schoolEmailDomain", "@xxx.edu.cn");
    }

    @Test
    void registerStoresOnlyEncodedPasswordInPendingRegistration() throws Exception {
        RegisterRequest request = registerRequest();
        when(userMapper.selectByStudentNo(STUDENT_NO)).thenReturn(null);
        when(userMapper.selectByEmail(EMAIL)).thenReturn(null);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(redisUtil.hasKey(anyString())).thenReturn(false);
        when(otpStore.canResend(anyString(), anyString())).thenReturn(true);

        authService.register(request);

        ArgumentCaptor<String> pendingJson = ArgumentCaptor.forClass(String.class);
        verify(redisUtil).set(eq("register:pending:" + EMAIL), pendingJson.capture(), eq(300L));

        JsonNode pending = objectMapper.readTree(pendingJson.getValue());
        assertThat(pending.get("studentNo").asText()).isEqualTo(STUDENT_NO);
        assertThat(pending.get("nickname").asText()).isEqualTo(NICKNAME);
        assertThat(pending.get("email").asText()).isEqualTo(EMAIL);
        assertThat(pending.get("encodedPassword").asText()).isEqualTo(ENCODED_PASSWORD);
        assertThat(pending.has("password")).isFalse();
        assertThat(pendingJson.getValue()).doesNotContain(RAW_PASSWORD);
    }

    @Test
    void registerCleansPendingStateAndFailsWhenEmailCannotBeSent() {
        RegisterRequest request = registerRequest();
        when(userMapper.selectByStudentNo(STUDENT_NO)).thenReturn(null);
        when(userMapper.selectByEmail(EMAIL)).thenReturn(null);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(redisUtil.hasKey(anyString())).thenReturn(false);
        when(otpStore.canResend(anyString(), anyString())).thenReturn(true);
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp unavailable"))
                .when(emailUtil).sendVerifyCode(eq(EMAIL), anyString());

        assertThatThrownBy(() -> authService.register(request))
                .hasMessageContaining("验证码邮件发送失败");

        // 邮件发送失败后不应上架验证码，仅清理暂存注册态
        verify(emailUtil).sendVerifyCode(eq(EMAIL), anyString());
        verify(otpStore, never()).store(anyString(), anyString(), anyString());
        verify(redisUtil).delete("register:pending:" + EMAIL);
    }

    @Test
    void verifyAndCompleteUsesPendingEncodedPasswordWithoutEncodingAgain() {
        VerifyCodeRequest request = new VerifyCodeRequest();
        request.setEmail(EMAIL);
        request.setCode("123456");

        String pendingJson = """
            {"studentNo":"20240001","nickname":"测试同学","email":"student@xxx.edu.cn","encodedPassword":"$2a$10$encoded"}
            """;

        when(redisUtil.get("register:pending:" + EMAIL)).thenReturn(pendingJson);
        when(userMapper.selectByStudentNo(STUDENT_NO)).thenReturn(null);
        when(userMapper.selectByEmail(EMAIL)).thenReturn(null);
        when(otpStore.verify(eq("REGISTER"), eq(EMAIL), anyString())).thenReturn(true);
        when(refreshTokenStore.issue(any(), anyLong())).thenReturn("jti-test");
        when(jwtUtil.generateAccessToken(any(), eq("STUDENT"), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(), any(), any())).thenReturn("refresh-token");

        authService.verifyAndComplete(request, response);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(userCaptor.getValue().getNickname()).isEqualTo(NICKNAME);
        verify(passwordEncoder, never()).encode(anyString());
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setStudentNo(STUDENT_NO);
        request.setNickname(NICKNAME);
        request.setEmail(EMAIL);
        request.setPassword(RAW_PASSWORD);
        return request;
    }
}
