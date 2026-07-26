package com.campus.auth.token;

import com.campus.auth.exception.InvalidTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 刷新令牌存储：以 Redis 作为 jti 的状态后端，支持签发、校验与吊销。
 * 配合 User.tokenVersion 实现会话级撤销（登出/改密/封禁）。
 */
@Component
public class RefreshTokenStore {

    private final StringRedisTemplate redis;
    private final String prefix;

    public RefreshTokenStore(StringRedisTemplate redis,
                             @Value("${campus.redis.rt-prefix:campus:rt:}") String prefix) {
        this.redis = redis;
        this.prefix = prefix;
    }

    /**
     * 签发一个新的刷新令牌 jti，并在 Redis 中记录 jti -> userId 的映射。
     *
     * @return 生成的 jti（应写入 JWT 的 jti claim 以及 HttpOnly Cookie）
     */
    public String issue(Long userId, long ttlSeconds) {
        String jti = UUID.randomUUID().toString();
        redis.opsForValue().set(prefix + "rt:" + jti, String.valueOf(userId), ttlSeconds, TimeUnit.SECONDS);
        return jti;
    }

    /**
     * 校验 jti 是否有效（未被吊销且在 TTL 内）。
     *
     * @throws InvalidTokenException 当 jti 不存在（已吊销或从未签发）时
     */
    public Long validate(String jti) {
        String value = redis.opsForValue().get(prefix + "rt:" + jti);
        if (value == null) {
            throw new InvalidTokenException("refresh token 已失效");
        }
        return Long.valueOf(value);
    }

    /**
     * 吊销指定 jti（登出或令牌轮换时调用）。
     */
    public void revoke(String jti) {
        redis.delete(prefix + "rt:" + jti);
    }
}
