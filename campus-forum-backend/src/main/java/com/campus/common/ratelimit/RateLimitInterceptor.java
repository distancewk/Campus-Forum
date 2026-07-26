package com.campus.common.ratelimit;

import com.campus.common.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * 基于 Redis 的滑动窗口计数器限流拦截器。
 * key = prefix + scope + ":" + resolvedKey + ":" + window；INCR + EXPIRE=window。
 * count > limit → 响应 429 并拒绝请求。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final StringRedisTemplate redisTemplate;
    private final String prefix;

    private final ExpressionParser parser = new SpelExpressionParser();

    public RateLimitInterceptor(StringRedisTemplate redisTemplate,
                                @Value("${campus.ratelimit.prefix:campus:rl:}") String prefix) {
        this.redisTemplate = redisTemplate;
        this.prefix = prefix;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }
        if (rateLimit == null) {
            return true;
        }

        String scope = rateLimit.scope();
        int limit = rateLimit.limit();
        int window = rateLimit.window();

        String resolvedKey;
        if ("ip".equalsIgnoreCase(scope)) {
            resolvedKey = clientIp(request);
        } else {
            resolvedKey = resolveKey(rateLimit.key(), request);
            if (resolvedKey == null || resolvedKey.isBlank()) {
                // 匿名用户无法定位用户 ID 时，回退到 IP 维度，避免完全不限流。
                resolvedKey = "anon:" + clientIp(request);
            }
        }

        String redisKey = prefix + scope + ":" + resolvedKey + ":" + window;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(window));
        }
        if (count != null && count > limit) {
            log.warn("限流触发 scope={} key={} count={} limit={}", scope, resolvedKey, count, limit);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(window));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }
        return true;
    }

    /**
     * 通过 SpEL 解析限流 key。上下文内置变量：userId（当前用户 ID）、request、principal。
     */
    private String resolveKey(String keyExpr, HttpServletRequest request) {
        try {
            EvaluationContext context = new StandardEvaluationContext();
            context.setVariable("userId", SecurityUtil.getCurrentUserId());
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            context.setVariable("principal", authentication);
            context.setVariable("request", request);
            Expression expression = parser.parseExpression(keyExpr);
            Object value = expression.getValue(context);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            log.warn("限流 key 表达式解析失败 keyExpr={}，回退到 IP：{}", keyExpr, e.getMessage());
            return "anon:" + clientIp(request);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
