package com.trainiq.core.diagnostics

class FrameAnomalyPolicy(
    private val p99ThresholdMs: Double = DefaultP99ThresholdMs,
    private val jankyFrameRatioThreshold: Double = DefaultJankyFrameRatioThreshold,
    private val minFrames: Int = DefaultMinFrames,
    private val cooldownMillis: Long = DefaultCooldownMillis,
) {
    private var lastTraceMillis: Long? = null

    fun shouldTrace(summary: PerformanceSessionSummary, nowMillis: Long): Boolean {
        if (summary.frameCount < minFrames) return false
        if (!summary.isAnomalous()) return false

        val lastTrace = lastTraceMillis
        if (lastTrace != null && nowMillis - lastTrace < cooldownMillis) {
            return false
        }

        lastTraceMillis = nowMillis
        return true
    }

    private fun PerformanceSessionSummary.isAnomalous(): Boolean =
        p99FrameMs > p99ThresholdMs || jankyFrameRatio > jankyFrameRatioThreshold

    private companion object {
        const val DefaultP99ThresholdMs = 48.0
        const val DefaultJankyFrameRatioThreshold = 0.25
        const val DefaultMinFrames = 120
        const val DefaultCooldownMillis = 600_000L
    }
}

interface PerfettoTraceController {
    fun startTrace(reason: String)
    fun stopTrace()
}

class NoopPerfettoTraceController : PerfettoTraceController {
    override fun startTrace(reason: String) = Unit

    override fun stopTrace() = Unit
}
