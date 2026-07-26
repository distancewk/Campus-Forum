package com.campus.ai.service;

import com.campus.ai.dto.AiModerationAdvice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内容去重缓存：相同（targetType + 标题 + 正文）的审核结果在 TTL 内复用，
 * 避免同一段广告/引流文案被不同用户反复打模型，显著降低 AI 成本与延迟。
 *
 * 说明：单实例内存实现，零外部依赖、可离线测试。多实例部署应替换为 Redis 等共享缓存。
 */
@Component
public class ModerationResultCache {

    @Value("${campus.ai.moderation.cache-enabled:true}")
    private boolean enabled;

    @Value("${campus.ai.moderation.cache-ttl-seconds:600}")
    private long ttlSeconds;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public AiModerationAdvice get(String targetType, String title, String content) {
        if (!enabled) {
            return null;
        }
        String key = key(targetType, title, content);
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt <= System.currentTimeMillis()) {
            store.remove(key);
            return null;
        }
        // 返回拷贝，防止调用方（置信度归一化等）污染缓存中的原始对象。
        return entry.advice.copy();
    }

    public void put(String targetType, String title, String content, AiModerationAdvice advice) {
        if (!enabled || advice == null) {
            return;
        }
        store.put(key(targetType, title, content),
                new Entry(advice.copy(), System.currentTimeMillis() + ttlSeconds * 1000));
    }

    private String key(String targetType, String title, String content) {
        String norm = (targetType == null ? "" : targetType)
                + "\n" + (title == null ? "" : title.trim())
                + "\n" + (content == null ? "" : content.trim());
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(norm.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 必然可用；兜底仅作去重（不影响正确性）。
            return String.valueOf(norm.hashCode());
        }
    }

    private static final class Entry {
        final AiModerationAdvice advice;
        final long expireAt;

        Entry(AiModerationAdvice advice, long expireAt) {
            this.advice = advice;
            this.expireAt = expireAt;
        }
    }
}
