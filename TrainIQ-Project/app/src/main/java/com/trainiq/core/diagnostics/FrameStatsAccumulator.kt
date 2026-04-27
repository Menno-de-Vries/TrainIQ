package com.trainiq.core.diagnostics

import kotlin.math.ceil

class FrameStatsAccumulator(
    private val jankThresholdMs: Double = DefaultJankThresholdMs,
    private val frozenThresholdMs: Double = DefaultFrozenThresholdMs,
) {
    private val frameDurationsMs = mutableListOf<Double>()

    @Synchronized
    fun recordFrameDurationMs(durationMs: Double) {
        if (durationMs.isFinite() && durationMs >= 0.0) {
            frameDurationsMs += durationMs
        }
    }

    @Synchronized
    fun summary(): PerformanceSessionSummary {
        if (frameDurationsMs.isEmpty()) return PerformanceSessionSummary.Empty

        val sorted = frameDurationsMs.sorted()
        val frameCount = sorted.size
        val jankyFrameCount = sorted.count { it > jankThresholdMs }
        val frozenFrameCount = sorted.count { it > frozenThresholdMs }
        val average = sorted.sum() / frameCount.toDouble()

        return PerformanceSessionSummary(
            frameCount = frameCount,
            jankyFrameCount = jankyFrameCount,
            frozenFrameCount = frozenFrameCount,
            averageFrameMs = average,
            minFrameMs = sorted.first(),
            maxFrameMs = sorted.last(),
            p50FrameMs = sorted.percentile(50),
            p90FrameMs = sorted.percentile(90),
            p95FrameMs = sorted.percentile(95),
            p99FrameMs = sorted.percentile(99),
        )
    }

    @Synchronized
    fun reset() {
        frameDurationsMs.clear()
    }

    private fun List<Double>.percentile(percentile: Int): Double {
        val rank = ceil((percentile / 100.0) * size).toInt().coerceIn(1, size)
        return this[rank - 1]
    }

    private companion object {
        const val DefaultJankThresholdMs = 16.67
        const val DefaultFrozenThresholdMs = 700.0
    }
}
