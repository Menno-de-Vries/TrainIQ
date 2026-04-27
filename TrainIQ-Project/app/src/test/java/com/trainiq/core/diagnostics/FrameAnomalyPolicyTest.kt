package com.trainiq.core.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameAnomalyPolicyTest {
    @Test
    fun shouldTrace_onExactThresholdBoundary_doesNotTriggerAnomaly() {
        val policy = FrameAnomalyPolicy(
            p99ThresholdMs = 32.0,
            jankyFrameRatioThreshold = 0.25,
            minFrames = 4,
            cooldownMillis = 1_000L,
        )

        assertFalse(policy.shouldTrace(anomalousSummary(), nowMillis = 10_000L))
    }

    @Test
    fun shouldTrace_aboveThreshold_triggersAnomaly() {
        val policy = FrameAnomalyPolicy(
            p99ThresholdMs = 31.0,
            jankyFrameRatioThreshold = 0.25,
            minFrames = 4,
            cooldownMillis = 1_000L,
        )

        assertTrue(policy.shouldTrace(anomalousSummary(), nowMillis = 10_000L))
    }

    @Test
    fun shouldTrace_withCooldown_suppressesUntilCooldownExpires() {
        val policy = FrameAnomalyPolicy(
            p99ThresholdMs = 31.0,
            jankyFrameRatioThreshold = 0.25,
            minFrames = 4,
            cooldownMillis = 1_000L,
        )
        val summary = anomalousSummary()

        assertTrue(policy.shouldTrace(summary, nowMillis = 10_000L))
        assertFalse(policy.shouldTrace(summary, nowMillis = 10_999L))
        assertTrue(policy.shouldTrace(summary, nowMillis = 11_000L))
    }

    private fun anomalousSummary() = PerformanceSessionSummary(
        frameCount = 4,
        jankyFrameCount = 1,
        averageFrameMs = 20.0,
        minFrameMs = 12.0,
        maxFrameMs = 32.0,
        p50FrameMs = 20.0,
        p90FrameMs = 32.0,
        p95FrameMs = 32.0,
        p99FrameMs = 32.0,
    )
}
