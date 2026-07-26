package com.campus.auth.controller;

import com.campus.auth.dto.*;
import com.campus.auth.service.AuthService;
import com.campus.common.ratelimit.RateLimit;
import com.campus.common.response.R;
import com.campus.common.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    /**
     * 发送注册验证码
     */
    @PostMapping("/send-code")
    @RateLimit(scope = "ip", limit = 5, window = 60)
    public R<Void> sendVerifyCode(@RequestBody @Valid SendCodeRequest request) {
        authService.sendVerifyCode(request.getEmail(), "REGISTER");
        return R.ok();
    }

    /**
     * 注册（暂存信息 + 发送验证码）
     */
    @PostMapping("/register")
    public R<LoginResponse> register(@RequestBody @Valid RegisterRequest request, HttpServletResponse response) {
        LoginResponse loginResponse = authService.register(request, response);
        return R.ok(loginResponse);
    }

    /**
     * 验证码校验并完成注册
     */
    @PostMapping("/register/verify")
    public R<LoginResponse> verifyAndComplete(@RequestBody @Valid VerifyCodeRequest request,
                                              HttpServletResponse response) {
        LoginResponse loginResponse = authService.verifyAndComplete(request, response);
        return R.ok(loginResponse);
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody @Valid LoginRequest request,
                                   HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(request, response);
        return R.ok(loginResponse);
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    public R<Map<String, String>> refresh(HttpServletRequest request,
                                           HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            return R.fail(401, "未登录或 Token 已过期");
        }
        Map<String, String> result = authService.refreshToken(refreshToken, response);
        return R.ok(result);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        Long userId = null;
        String jti = null;
        if (refreshToken != null) {
            try {
                userId = jwtUtil.getUserIdFromRefreshToken(refreshToken);
                jti = jwtUtil.getJtiFromRefreshToken(refreshToken);
            } catch (RuntimeException ignored) {
                // 令牌无效：仍执行本地 Cookie 清理
            }
        }
        authService.logout(userId, jti, response);
        return R.ok();
    }

    /**
     * 忘记密码 - 发送验证码
     */
    @PostMapping("/forgot-password")
    @RateLimit(scope = "ip", limit = 5, window = 60)
    public R<Void> forgotPassword(@RequestBody @Valid SendCodeRequest request) {
        authService.forgotPassword(request);
        return R.ok();
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public R<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request);
        return R.ok();
    }

    // ==================== 内部方法 ====================

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
