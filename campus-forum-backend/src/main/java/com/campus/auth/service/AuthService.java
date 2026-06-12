package com.campus.auth.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.auth.dto.*;
import com.campus.auth.entity.User;
import com.campus.auth.mapper.UserMapper;
import com.campus.common.enums.ResultCode;
import com.campus.common.exception.BusinessException;
import com.campus.common.util.EmailUtil;
import com.campus.common.util.JwtUtil;
import com.campus.common.util.RedisUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String VERIFY_LIMIT_KEY = "verify:limit:%s";
    private static final String VERIFY_CODE_KEY = "verify:code:%s:%s";
    private static final String REGISTER_PENDING_KEY = "register:pending:%s";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private static final int CODE_LENGTH = 6;
    private static final long CODE_EXPIRE_SECONDS = 300;
    private static final long LIMIT_EXPIRE_SECONDS = 60;
    private static final int MAX_LOGIN_FAIL = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final UserMapper userMapper;
    private final RedisUtil redisUtil;
    private final EmailUtil emailUtil;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${campus.jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    @Value("${campus.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${campus.cookie.same-site:Lax}")
    private String cookieSameSite;

    // ==================== 发送验证码 ====================

    public void sendVerifyCode(String email, String type) {
        validateSchoolEmail(email);

        // 检查 60 秒限流
        String limitKey = String.format(VERIFY_LIMIT_KEY, email);
        if (Boolean.TRUE.equals(redisUtil.hasKey(limitKey))) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码发送过于频繁，请60秒后重试");
        }

        // 生成 6 位随机验证码
        String code = generateVerifyCode();

        // 存入 Redis
        String codeKey = String.format(VERIFY_CODE_KEY, email, type);
        redisUtil.set(codeKey, code, CODE_EXPIRE_SECONDS);

        // 设置限流标记
        redisUtil.set(limitKey, "1", LIMIT_EXPIRE_SECONDS);

        // 发送邮件，失败时回滚验证码缓存，避免前端误提示“已发送”
        try {
            emailUtil.sendVerifyCode(email, code);
        } catch (RuntimeException e) {
            redisUtil.delete(codeKey);
            redisUtil.delete(limitKey);
            log.warn("验证码邮件发送失败，已清理验证码缓存: email={}, type={}, reason={}", email, type, e.getMessage());
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "验证码邮件发送失败，请稍后重试");
        }
        log.info("验证码已生成并发送: email={}, type={}", email, type);
    }

    // ==================== 注册 ====================

    public void register(RegisterRequest request) {
        validateSchoolEmail(request.getEmail());

        // 检查学号是否已注册
        User existByStudentNo = userMapper.selectByStudentNo(request.getStudentNo());
        if (existByStudentNo != null) {
            throw new BusinessException(ResultCode.STUDENT_NO_EXISTS);
        }

        // 检查邮箱是否已注册
        User existByEmail = userMapper.selectByEmail(request.getEmail());
        if (existByEmail != null) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS);
        }

        // 暂存注册信息到 Redis。只保存密码哈希，避免验证码窗口期内出现明文密码驻留。
        String pendingKey = String.format(REGISTER_PENDING_KEY, request.getEmail());
        try {
            PendingRegistration pending = new PendingRegistration(
                    request.getStudentNo(),
                    request.getNickname(),
                    request.getEmail(),
                    passwordEncoder.encode(request.getPassword()));
            String json = objectMapper.writeValueAsString(pending);
            redisUtil.set(pendingKey, json, CODE_EXPIRE_SECONDS);
        } catch (JsonProcessingException e) {
            log.error("序列化注册信息失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        }

        // 发送验证码
        try {
            sendVerifyCode(request.getEmail(), "REGISTER");
        } catch (BusinessException e) {
            redisUtil.delete(pendingKey);
            throw e;
        }
    }

    public LoginResponse verifyAndComplete(VerifyCodeRequest request, HttpServletResponse response) {
        String email = request.getEmail();
        String code = request.getCode();

        // 校验验证码
        String codeKey = String.format(VERIFY_CODE_KEY, email, "REGISTER");
        String cachedCode = redisUtil.get(codeKey);
        if (cachedCode == null) {
            throw new BusinessException(ResultCode.VERIFY_CODE_EXPIRED);
        }
        if (!constantTimeEquals(cachedCode, code)) {
            throw new BusinessException(ResultCode.VERIFY_CODE_WRONG);
        }

        // 获取暂存的注册信息
        String pendingKey = String.format(REGISTER_PENDING_KEY, email);
        String pendingJson = redisUtil.get(pendingKey);
        if (pendingJson == null) {
            throw new BusinessException(ResultCode.VERIFY_CODE_EXPIRED, "注册信息已过期，请重新注册");
        }

        PendingRegistration pendingRegistration;
        try {
            pendingRegistration = objectMapper.readValue(pendingJson, PendingRegistration.class);
        } catch (JsonProcessingException e) {
            log.error("反序列化注册信息失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        }

        // 二次校验：学号/邮箱可能在此期间被注册
        if (userMapper.selectByStudentNo(pendingRegistration.studentNo()) != null) {
            throw new BusinessException(ResultCode.STUDENT_NO_EXISTS);
        }
        if (userMapper.selectByEmail(email) != null) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS);
        }

        // 创建用户
        User user = new User();
        user.setStudentNo(pendingRegistration.studentNo());
        user.setNickname(resolveNickname(pendingRegistration));
        user.setPassword(pendingRegistration.encodedPassword());
        user.setEmail(email);
        user.setRole("STUDENT");
        user.setStatus(1);
        user.setLoginFail(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        userMapper.insert(user);

        // 清理 Redis
        redisUtil.delete(codeKey);
        redisUtil.delete(pendingKey);

        // 生成 Token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Refresh Token 写入 HttpOnly Cookie
        addRefreshTokenCookie(response, refreshToken);

        return buildLoginResponse(user, accessToken);
    }

    // ==================== 登录 ====================

    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        // 查询用户
        User user = userMapper.selectByStudentNo(request.getStudentNo());
        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_FAIL);
        }

        // 检查账号状态
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 检查锁定状态
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutes = java.time.Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED.getCode(),
                    "账号已锁定，请" + minutes + "分钟后重试");
        }

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleLoginFail(user);
            throw new BusinessException(ResultCode.LOGIN_FAIL);
        }

        // 密码正确：重置登录失败次数
        if (user.getLoginFail() > 0) {
            resetLoginFail(user.getId());
        }

        // 清除锁定（如果之前被锁定但现在已过期）
        if (user.getLockedUntil() != null) {
            clearLock(user.getId());
        }

        // 生成 Token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Refresh Token 写入 HttpOnly Cookie
        addRefreshTokenCookie(response, refreshToken);

        return buildLoginResponse(user, accessToken);
    }

    // ==================== 刷新 Token ====================

    public Map<String, String> refreshToken(String refreshToken, HttpServletResponse response) {
        // 校验 Refresh Token
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Token 已过期，请重新登录");
        }

        Long userId = jwtUtil.getUserIdFromRefreshToken(refreshToken);

        // 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 生成新 Access Token
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());

        // Rotation: 生成新 Refresh Token 替换旧的
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());
        addRefreshTokenCookie(response, newRefreshToken);

        return Map.of("accessToken", newAccessToken);
    }

    // ==================== 登出 ====================

    public void logout(HttpServletResponse response) {
        addRefreshTokenCookie(response, "", 0);
    }

    // ==================== 忘记密码 ====================

    public void forgotPassword(SendCodeRequest request) {
        validateSchoolEmail(request.getEmail());

        // 检查邮箱是否已注册
        User user = userMapper.selectByEmail(request.getEmail());
        if (user == null) {
            // 为了安全，不暴露邮箱是否已注册，仍然返回成功
            return;
        }

        sendVerifyCode(request.getEmail(), "RESET_PASSWORD");
    }

    // ==================== 重置密码 ====================

    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();

        // 校验验证码
        String codeKey = String.format(VERIFY_CODE_KEY, email, "RESET_PASSWORD");
        String cachedCode = redisUtil.get(codeKey);
        if (cachedCode == null) {
            throw new BusinessException(ResultCode.VERIFY_CODE_EXPIRED);
        }
        if (!constantTimeEquals(cachedCode, request.getCode())) {
            throw new BusinessException(ResultCode.VERIFY_CODE_WRONG);
        }

        // 查询用户
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 更新密码
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, user.getId())
                .set(User::getPassword, encodedPassword)
                .set(User::getLoginFail, 0)
                .set(User::getLockedUntil, null)
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, updateWrapper);

        // 清除验证码
        redisUtil.delete(codeKey);

        log.info("用户密码已重置: userId={}", user.getId());
    }

    // ==================== 内部方法 ====================

    private void validateSchoolEmail(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!normalizedEmail.endsWith("@xxx.edu.cn")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "必须使用学校邮箱");
        }
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateVerifyCode() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private void handleLoginFail(User user) {
        // 原子递增登录失败次数，避免并发竞态条件
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, user.getId())
                .setSql("login_fail = login_fail + 1");

        // 检查是否需要锁定（在递增后查询）
        int newFailCount = user.getLoginFail() + 1;
        if (newFailCount >= MAX_LOGIN_FAIL) {
            LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
            updateWrapper.set(User::getLockedUntil, lockedUntil);
            log.warn("用户连续登录失败{}次，已锁定: userId={}", newFailCount, user.getId());
        }

        userMapper.update(null, updateWrapper);
    }

    private void resetLoginFail(Long userId) {
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                .set(User::getLoginFail, 0)
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, updateWrapper);
    }

    private void clearLock(Long userId) {
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                .set(User::getLockedUntil, null)
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, updateWrapper);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        addRefreshTokenCookie(response, refreshToken, refreshExpiration / 1000);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeSeconds) {
        String sameSite = cookieSameSite == null || cookieSameSite.isBlank() ? "Lax" : cookieSameSite;
        String secure = cookieSecure ? "; Secure" : "";
        response.addHeader("Set-Cookie",
                String.format("%s=%s; Path=/api/auth/refresh; HttpOnly%s; SameSite=%s; Max-Age=%d",
                        REFRESH_TOKEN_COOKIE, refreshToken == null ? "" : refreshToken, secure, sameSite, maxAgeSeconds));
    }

    /**
     * 恒定时间比较字符串，防止时序侧信道攻击
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        return MessageDigest.isEqual(a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private LoginResponse buildLoginResponse(User user, String accessToken) {
        UserInfoVO userInfo = UserInfoVO.builder()
                .id(user.getId())
                .studentNo(user.getStudentNo())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();
        return new LoginResponse(accessToken, userInfo);
    }

    private String resolveNickname(PendingRegistration pendingRegistration) {
        String nickname = pendingRegistration.nickname();
        if (nickname == null || nickname.isBlank()) {
            return "用户" + pendingRegistration.studentNo();
        }
        return nickname.trim();
    }

    private record PendingRegistration(String studentNo, String nickname, String email, String encodedPassword) {
    }
}
