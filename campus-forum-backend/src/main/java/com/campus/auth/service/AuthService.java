package com.campus.auth.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.auth.dto.*;
import com.campus.auth.entity.User;
import com.campus.auth.exception.InvalidTokenException;
import com.campus.auth.mapper.UserMapper;
import com.campus.auth.token.OtpStore;
import com.campus.auth.token.RefreshTokenStore;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REGISTER_PENDING_KEY = "register:pending:%s";

    private static final int CODE_LENGTH = 6;
    private static final long CODE_EXPIRE_SECONDS = 300;
    private static final int MAX_LOGIN_FAIL = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    /**
     * 用于“用户不存在”场景的恒定时间口令比对，避免通过响应时间差异枚举账号（V-P）。
     * 这是一个合法格式的 BCrypt 哈希，任何口令与之比对都会返回 false，但耗时与正常比对一致。
     */
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final UserMapper userMapper;
    private final RedisUtil redisUtil;
    private final EmailUtil emailUtil;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final RefreshTokenStore refreshTokenStore;
    private final OtpStore otpStore;

    @Value("${campus.jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    @Value("${campus.jwt.refresh-ttl:604800}")
    private long refreshTtlSeconds;

    @Value("${campus.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${campus.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${campus.school-email-domain:@your-school.edu.cn}")
    private String schoolEmailDomain;

    // ==================== 发送验证码 ====================

    public void sendVerifyCode(String email, String type) {
        validateSchoolEmail(email);

        // 60 秒重发限流
        if (!otpStore.canResend(type, email)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码发送过于频繁，请稍后重试");
        }

        // 先发送邮件，失败则不上架验证码，避免前端误判“已发送”
        String code = generateVerifyCode();
        try {
            emailUtil.sendVerifyCode(email, code);
        } catch (RuntimeException e) {
            log.warn("验证码邮件发送失败: email={}, type={}, reason={}", email, type, e.getMessage());
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "验证码邮件发送失败，请稍后重试");
        }

        // 邮件发送成功后再写入 Redis 并标记限流
        otpStore.store(type, email, code);
        otpStore.markSent(type, email);
        log.info("验证码已生成并发送: email={}, type={}", email, type);
    }

    // ==================== 注册 ====================

    public void register(RegisterRequest request) {
        validateSchoolEmail(request.getEmail());

        // 检查学号/邮箱是否已注册：统一返回模糊信息，避免账号枚举（V-M）
        User existByStudentNo = userMapper.selectByStudentNo(request.getStudentNo());
        if (existByStudentNo != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "操作失败，请稍后重试");
        }

        User existByEmail = userMapper.selectByEmail(request.getEmail());
        if (existByEmail != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "操作失败，请稍后重试");
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

        // 校验验证码：成功与否统一模糊信息，避免验证码爆破/枚举（V-M/V-B）
        if (!otpStore.verify("REGISTER", email, code)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "操作失败，请稍后重试");
        }

        // 获取暂存的注册信息
        String pendingKey = String.format(REGISTER_PENDING_KEY, email);
        String pendingJson = redisUtil.get(pendingKey);
        if (pendingJson == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "操作失败，请稍后重试");
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
            throw new BusinessException(ResultCode.PARAM_ERROR, "操作失败，请稍后重试");
        }
        if (userMapper.selectByEmail(email) != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "操作失败，请稍后重试");
        }

        // 创建用户（token_version 由数据库 DEFAULT 0 初始化）
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
        otpStore.remove("REGISTER", email);
        redisUtil.delete(pendingKey);

        // 生成 Token（携带 tv 以支持会话撤销）
        int tv = currentTv(user);
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole(), tv);
        String jti = refreshTokenStore.issue(user.getId(), refreshTtlSeconds);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), jti, tv);

        // Refresh Token 写入 HttpOnly Cookie
        addRefreshTokenCookie(response, refreshToken);

        return buildLoginResponse(user, accessToken);
    }

    // ==================== 登录 ====================

    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        // 查询用户
        User user = userMapper.selectByStudentNo(request.getStudentNo());
        if (user == null) {
            // 用户不存在仍执行一次恒定时间比对，抹平响应时间，防止账号枚举（V-P）
            passwordEncoder.matches(request.getPassword(), DUMMY_BCRYPT_HASH);
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
        int tv = currentTv(user);
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole(), tv);
        String jti = refreshTokenStore.issue(user.getId(), refreshTtlSeconds);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), jti, tv);

        // Refresh Token 写入 HttpOnly Cookie
        addRefreshTokenCookie(response, refreshToken);

        return buildLoginResponse(user, accessToken);
    }

    // ==================== 刷新 Token ====================

    public Map<String, String> refreshToken(String refreshToken, HttpServletResponse response) {
        // 1. 校验签名与有效期
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Token 已过期，请重新登录");
        }

        // 2. 解析并校验 jti（Redis 中存在 = 未被吊销/轮换）
        Long userId = jwtUtil.getUserIdFromRefreshToken(refreshToken);
        String jti = jwtUtil.getJtiFromRefreshToken(refreshToken);
        Integer tv = jwtUtil.getTvFromRefreshToken(refreshToken);

        Long storedUserId;
        try {
            storedUserId = refreshTokenStore.validate(jti);
        } catch (InvalidTokenException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Token 已失效，请重新登录");
        }
        if (!userId.equals(storedUserId)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Token 已失效，请重新登录");
        }

        // 3. 查询用户并做状态/锁定/版本校验
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutes = java.time.Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED.getCode(),
                    "账号已锁定，请" + minutes + "分钟后重试");
        }
        int current = currentTv(user);
        if (!tv.equals(current)) {
            // token 版本不匹配（已登出 / 改密 / 被封禁后自增），拒绝续期
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Token 已失效，请重新登录");
        }

        // 4. 轮换：吊销旧 jti，签发新的 jti + 当前 tv
        refreshTokenStore.revoke(jti);
        String newJti = refreshTokenStore.issue(userId, refreshTtlSeconds);
        String newAccessToken = jwtUtil.generateAccessToken(userId, user.getRole(), current);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, newJti, current);
        addRefreshTokenCookie(response, newRefreshToken);

        return Map.of("accessToken", newAccessToken);
    }

    // ==================== 登出 ====================

    public void logout(Long userId, String jti, HttpServletResponse response) {
        // 会话撤销：自增 tokenVersion（使所有旧 token 立即失效）并吊销刷新令牌
        if (userId != null) {
            incrementTokenVersion(userId);
        }
        if (jti != null) {
            refreshTokenStore.revoke(jti);
        }
        addRefreshTokenCookie(response, "", 0);
    }

    // ==================== 忘记密码 ====================

    public void forgotPassword(SendCodeRequest request) {
        validateSchoolEmail(request.getEmail());

        // 检查邮箱是否已注册：为安全不暴露是否注册，直接返回
        User user = userMapper.selectByEmail(request.getEmail());
        if (user == null) {
            return;
        }

        sendVerifyCode(request.getEmail(), "RESET_PASSWORD");
    }

    // ==================== 重置密码 ====================

    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();

        // 校验验证码：成功与否统一模糊信息（V-M）
        if (!otpStore.verify("RESET_PASSWORD", email, request.getCode())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "操作失败，请稍后重试");
        }

        // 查询用户
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            // 理论上验证码通过说明邮箱有效，保险起见仍返回模糊信息
            throw new BusinessException(ResultCode.PARAM_ERROR, "操作失败，请稍后重试");
        }

        // 更新密码（清零登录失败/锁定）
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        userMapper.updatePasswordAndResetFail(user.getId(), encodedPassword);

        // 会话撤销：自增 tokenVersion，使旧的 access/refresh token 立即失效
        incrementTokenVersion(user.getId());

        log.info("用户密码已重置: userId={}", user.getId());
    }

    // ==================== 内部方法 ====================

    private void validateSchoolEmail(String email) {
        String domain = schoolEmailDomain == null ? "" : schoolEmailDomain.trim().toLowerCase(Locale.ROOT);
        // 仅接受真实配置的学校邮箱域名，拒绝占位符 @your-school.edu.cn（V-N/V-O）
        if (domain.isEmpty()
                || !domain.matches("^@[\\w.-]+\\.edu\\.cn$")
                || "@your-school.edu.cn".equals(domain)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请配置正确的学校邮箱域名");
        }
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!normalizedEmail.endsWith(domain)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "必须使用学校邮箱");
        }
    }

    private int currentTv(User user) {
        Integer tv = user.getTokenVersion();
        return tv == null ? 0 : tv;
    }

    private void incrementTokenVersion(Long userId) {
        userMapper.incrTokenVersion(userId);
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateVerifyCode() {
        int code = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private void handleLoginFail(User user) {
        // 1. 原子递增（DB 端完成，无竞态）
        LambdaUpdateWrapper<User> inc = new LambdaUpdateWrapper<>();
        inc.eq(User::getId, user.getId()).setSql("login_fail = login_fail + 1");
        userMapper.update(null, inc);

        // 2. 读取数据库真实失败次数（消除并发竞态）
        User refreshed = userMapper.selectById(user.getId());
        if (refreshed.getLoginFail() >= MAX_LOGIN_FAIL) {
            LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
            LambdaUpdateWrapper<User> lock = new LambdaUpdateWrapper<>();
            lock.eq(User::getId, user.getId()).set(User::getLockedUntil, lockedUntil);
            userMapper.update(null, lock);
            log.warn("用户连续登录失败{}次，已锁定: userId={}", refreshed.getLoginFail(), user.getId());
        }
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
