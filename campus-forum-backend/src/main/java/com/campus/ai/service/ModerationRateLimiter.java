package com.campus.ai.service;

import com.campus.ai.exception.ContentRejectedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 审核调用限流（按用户 + 全局滑动窗口），用于：
 *  1. 防止刷帖/刷评论攻击打爆 AI 账单（成本封顶的实用近似）；
 *  2. 在限流触发时按 fail-closed 拒绝并转人工复核，保证安全。
 *
 * 说明：单实例内存实现，零外部依赖、可离线测试。多实例部署应改用 Redis 计数。
 */
@Component
public class ModerationRateLimiter {

    @Value("${campus.ai.moderation.rate-limit-per-user:20}")
    private int perUserLimit;

    @Value("${campus.ai.moderation.rate-limit-global:200}")
    private int globalLimit;

    @Value("${campus.ai.moderation.rate-limit-window-seconds:60}")
    private int windowSeconds;

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    public void tryAcquire(Long userId) {
        long bucket = System.currentTimeMillis() / (windowSeconds * 1000L);
        maybeEvict(bucket);

        if (userId != null) {
            long userCount = counters
                    .computeIfAbsent("u:" + userId + ":" + bucket, k -> new AtomicLong(0))
                    .incrementAndGet();
            if (userCount > perUserLimit) {
                throw new ContentRejectedException("审核请求过于频繁，请稍后再试（已转人工复核）");
            }
        }
        long globalCount = counters
                .computeIfAbsent("g:" + bucket, k -> new AtomicLong(0))
                .incrementAndGet();
        if (globalCount > globalLimit) {
            throw new ContentRejectedException("系统审核繁忙，请稍后再试（已转人工复核）");
        }
    }

    /** 计数表只保留当前与上一窗口，避免长期运行内存膨胀。 */
    private void maybeEvict(long currentBucket) {
        if (counters.size() <= 20000) {
            return;
        }
        counters.keySet().removeIf(key -> {
            int idx = key.lastIndexOf(':');
            if (idx < 0) {
                return true;
            }
            try {
                long bucket = Long.parseLong(key.substring(idx + 1));
                return bucket < currentBucket - 1;
            } catch (NumberFormatException e) {
                return true;
            }
        });
    }
}
