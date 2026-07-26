package com.campus.auth.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

/**
 * 验证码（OTP）存储与防爆破：
 * - 验证码写入 Redis，键 campus:otp:{purpose}:{account}，TTL 可配置。
 * - 每次校验失败递增尝试计数 ...:att，达到上限后删除验证码并锁定 ...:lock（10 分钟）。
 * - 发送频率限制 ...:rl，默认 60 秒内只能发送一次。
 */
@Component
public class OtpStore {

    private final StringRedisTemplate redis;
    private final String prefix;
    private final long ttlSeconds;
    private final int maxAttempts;
    private final long resendSeconds;

    public OtpStore(StringRedisTemplate redis,
                    @Value("${campus.otp.prefix:campus:otp:}") String prefix,
                    @Value("${campus.otp.ttl:300}") long ttlSeconds,
                    @Value("${campus.otp.max-attempts:5}") int maxAttempts,
                    @Value("${campus.otp.resend-seconds:60}") long resendSeconds) {
        this.redis = redis;
        this.prefix = prefix;
        this.ttlSeconds = ttlSeconds;
        this.maxAttempts = maxAttempts;
        this.resendSeconds = resendSeconds;
    }

    private String codeKey(String purpose, String account) {
        return prefix + purpose + ":" + account;
    }

    private String attKey(String purpose, String account) {
        return codeKey(purpose, account) + ":att";
    }

    private String lockKey(String purpose, String account) {
        return codeKey(purpose, account) + ":lock";
    }

    private String resendKey(String purpose, String account) {
        return codeKey(purpose, account) + ":rl";
    }

    /** 是否允许发送（60 秒限流）。false 表示发送过于频繁。 */
    public boolean canResend(String purpose, String account) {
        return !Boolean.TRUE.equals(redis.hasKey(resendKey(purpose, account)));
    }

    /** 标记已发送，用于 60 秒重发限流。 */
    public void markSent(String purpose, String account) {
        redis.opsForValue().set(resendKey(purpose, account), "1", resendSeconds, TimeUnit.SECONDS);
    }

    /** 该账号的验证码是否处于锁定状态（连续错误超限）。 */
    public boolean isLocked(String purpose, String account) {
        return Boolean.TRUE.equals(redis.hasKey(lockKey(purpose, account)));
    }

    /** 写入验证码，并清除历史尝试计数。 */
    public void store(String purpose, String account, String code) {
        redis.opsForValue().set(codeKey(purpose, account), code, ttlSeconds, TimeUnit.SECONDS);
        redis.delete(attKey(purpose, account));
    }

    /** 校验验证码：通过则清除；失败则递增计数，达到上限后锁定并删除验证码。 */
    public boolean verify(String purpose, String account, String code) {
        if (Boolean.TRUE.equals(redis.hasKey(lockKey(purpose, account)))) {
            return false;
        }
        String cached = redis.opsForValue().get(codeKey(purpose, account));
        if (cached == null) {
            return false;
        }
        boolean ok = constantTimeEquals(cached, code);
        if (ok) {
            redis.delete(codeKey(purpose, account));
            redis.delete(attKey(purpose, account));
            return true;
        }
        Long attempts = redis.opsForValue().increment(attKey(purpose, account));
        if (attempts == null) {
            attempts = 1L;
        }
        redis.expire(attKey(purpose, account), ttlSeconds, TimeUnit.SECONDS);
        if (attempts >= maxAttempts) {
            redis.delete(codeKey(purpose, account));
            redis.delete(attKey(purpose, account));
            redis.opsForValue().set(lockKey(purpose, account), "1", 600, TimeUnit.SECONDS);
        }
        return false;
    }

    /** 主动清除验证码与计数（注册完成后）。 */
    public void remove(String purpose, String account) {
        redis.delete(codeKey(purpose, account));
        redis.delete(attKey(purpose, account));
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
