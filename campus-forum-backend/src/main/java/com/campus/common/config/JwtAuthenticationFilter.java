package com.campus.common.config;

import com.campus.auth.entity.User;
import com.campus.auth.mapper.UserMapper;
import com.campus.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && jwtUtil.validateAccessToken(token)) {
            try {
                Claims claims = jwtUtil.parseAccessToken(token);
                Long userId = Long.valueOf(claims.getSubject());
                String role = claims.get("role", String.class);
                Integer tv = claims.get("tv", Integer.class);

                User user = userMapper.selectById(userId);
                if (user == null) {
                    // 用户不存在/已删除：拒绝
                    SecurityContextHolder.clearContext();
                } else if (user.getStatus() != null && user.getStatus() == 0) {
                    // 账号被封禁：拒绝（V-A 会话撤销/封禁即时生效）
                    SecurityContextHolder.clearContext();
                } else {
                    Integer currentTv = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
                    if (tv == null || !tv.equals(currentTv)) {
                        // token 版本不匹配（已登出/改密/封禁自增）：拒绝
                        SecurityContextHolder.clearContext();
                    } else if (role == null || !role.equals(user.getRole())) {
                        // 角色与令牌声明不一致：拒绝
                        SecurityContextHolder.clearContext();
                    } else {
                        UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | NumberFormatException e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
