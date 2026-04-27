package com.trainiq.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameStatsAccumulatorTest {
    @Test
    fun summary_withRecordedFrames_calculatesSortedPercentilesAndCountsJankyFrames() {
        val accumulator = FrameStatsAccumulator(jankThresholdMs = 16.0)

        listOf(8.0, 12.0, 16.0, 20.0, 40.0).forEach(accumulator::recordFrameDurationMs)

        val summary = accumulator.summary()

        assertEquals(5, summary.frameCount)
        assertEquals(2, summary.jankyFrameCount)
        assertEquals(8.0, summary.minFrameMs, 0.0)
        assertEquals(40.0, summary.maxFrameMs, 0.0)
        assertEquals(16.0, summary.p50FrameMs, 0.0)
        assertEquals(40.0, summary.p90FrameMs, 0.0)
        assertEquals(40.0, summary.p95FrameMs, 0.0)
        assertEquals(40.0, summary.p99FrameMs, 0.0)
    }

    @Test
    fun summary_withoutFrames_returnsEmptySummary() {
        val summary = FrameStatsAccumulator().summary()

        assertEquals(0, summary.frameCount)
        assertEquals(0, summary.jankyFrameCount)
        assertTrue(summary.averageFrameMs == 0.0)
    }
}
