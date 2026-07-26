package com.campus.auth.service;

import com.campus.auth.dto.LoginResponse;
import com.campus.auth.dto.RegisterRequest;
import com.campus.auth.dto.VerifyCodeRequest;
import com.campus.auth.entity.User;
import com.campus.auth.mapper.UserMapper;
import com.campus.auth.token.OtpStore;
import com.campus.auth.token.RefreshTokenStore;
import com.campus.common.exception.BusinessException;
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
    void registerCreatesUserDirectlyWithoutSendingCode() {
        RegisterRequest request = registerRequest();
        when(userMapper.selectByStudentNo(STUDENT_NO)).thenReturn(null);
        when(userMapper.selectByEmail(EMAIL)).thenReturn(null);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(refreshTokenStore.issue(any(), anyLong())).thenReturn("jti-test");
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(), any(), any())).thenReturn("refresh-token");

        LoginResponse result = authService.register(request, response);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User inserted = userCaptor.getValue();
        assertThat(inserted.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(inserted.getNickname()).isEqualTo(NICKNAME);
        assertThat(inserted.getEmail()).isEqualTo(EMAIL);
        assertThat(inserted.getRole()).isEqualTo("STUDENT");
        assertThat(inserted.getStatus()).isEqualTo(1);
        assertThat(inserted.getPassword()).doesNotContain(RAW_PASSWORD);
        // 邮箱验证暂未启用：不再暂存 Redis、不再发送验证码
        verify(emailUtil, never()).sendVerifyCode(anyString(), anyString());
        verify(redisUtil, never()).set(anyString(), anyString(), anyLong());
        // 注册后直接签发令牌
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getUser().getStudentNo()).isEqualTo(STUDENT_NO);
    }

    @Test
    void registerWithEmptyEmailCreatesUserWithNullEmail() {
        RegisterRequest request = registerRequest();
        request.setEmail("");
        when(userMapper.selectByStudentNo(STUDENT_NO)).thenReturn(null);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(refreshTokenStore.issue(any(), anyLong())).thenReturn("jti-test");
        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(), any(), any())).thenReturn("refresh-token");

        authService.register(request, response);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isNull();
        // 邮箱为空时不查重
        verify(userMapper, never()).selectByEmail(anyString());
    }

    @Test
    void registerRejectsDuplicateStudentNo() {
        RegisterRequest request = registerRequest();
        when(userMapper.selectByStudentNo(STUDENT_NO)).thenReturn(new User());

        assertThatThrownBy(() -> authService.register(request, response))
                .isInstanceOf(BusinessException.class);

        verify(userMapper, never()).insert(any(User.class));
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
