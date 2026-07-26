package com.campus.ai.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 审核可观测性：累计调用数、按风险等级分布、REJECT 数、失败数、平均耗时。
 * 通过 GET /api/admin/ai/moderation-metrics 暴露，用于后续调阈值与评估误杀率。
 */
@Component
public class ModerationMetrics {

    private final AtomicLong total = new AtomicLong();
    private final AtomicLong low = new AtomicLong();
    private final AtomicLong medium = new AtomicLong();
    private final AtomicLong high = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong totalLatencyNanos = new AtomicLong();

    public void record(String riskLevel, String suggestedAction, long latencyNanos) {
        total.incrementAndGet();
        totalLatencyNanos.addAndGet(latencyNanos);
        if ("LOW".equalsIgnoreCase(riskLevel)) {
            low.incrementAndGet();
        } else if ("MEDIUM".equalsIgnoreCase(riskLevel)) {
            medium.incrementAndGet();
        } else if ("HIGH".equalsIgnoreCase(riskLevel)) {
            high.incrementAndGet();
        }
        if ("REJECT".equalsIgnoreCase(suggestedAction)) {
            rejected.incrementAndGet();
        }
    }

    public void recordFailure(long latencyNanos) {
        total.incrementAndGet();
        failures.incrementAndGet();
        totalLatencyNanos.addAndGet(latencyNanos);
    }

    public Map<String, Object> snapshot() {
        long t = total.get();
        double avgMs = t == 0 ? 0.0 : (totalLatencyNanos.get() / 1_000_000.0) / t;
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("total", t);
        snapshot.put("low", low.get());
        snapshot.put("medium", medium.get());
        snapshot.put("high", high.get());
        snapshot.put("rejected", rejected.get());
        snapshot.put("failures", failures.get());
        snapshot.put("avgLatencyMs", Math.round(avgMs * 100.0) / 100.0);
        return snapshot;
    }
}
