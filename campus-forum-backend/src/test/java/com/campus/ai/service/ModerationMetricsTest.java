package com.campus.ai.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationMetricsTest {

    @Test
    void recordsDistributionAndLatency() {
        ModerationMetrics metrics = new ModerationMetrics();
        metrics.record("LOW", "ALLOW", 1_000_000);       // 1ms
        metrics.record("MEDIUM", "REVIEW", 3_000_000);   // 3ms
        metrics.record("LOW", "REJECT", 2_000_000);      // 2ms
        metrics.recordFailure(4_000_000);                // 4ms 失败

        Map<String, Object> snap = metrics.snapshot();
        assertThat(snap.get("total")).isEqualTo(4L);
        assertThat(snap.get("low")).isEqualTo(2L);
        assertThat(snap.get("medium")).isEqualTo(1L);
        assertThat(snap.get("high")).isEqualTo(0L);
        assertThat(snap.get("rejected")).isEqualTo(1L);
        assertThat(snap.get("failures")).isEqualTo(1L);
        // 平均耗时 = (1+3+2+4)/4 = 2.5ms
        assertThat(snap.get("avgLatencyMs")).isEqualTo(2.5);
    }
}
