package com.trainiq.core.diagnostics

data class PerformanceSessionSummary(
    val sessionId: String = "",
    val appVersion: String = "",
    val buildType: String = "",
    val screenName: String = "",
    val device: String = "",
    val abi: String = "",
    val apiLevel: Int = 0,
    val locale: String = "",
    val batteryPercent: Int? = null,
    val freeMemoryMb: Long = 0L,
    val frameCount: Int,
    val jankyFrameCount: Int,
    val frozenFrameCount: Int = 0,
    val averageFrameMs: Double,
    val minFrameMs: Double,
    val maxFrameMs: Double,
    val p50FrameMs: Double,
    val p90FrameMs: Double,
    val p95FrameMs: Double,
    val p99FrameMs: Double,
) {
    val jankyFrameRatio: Double
        get() = if (frameCount == 0) 0.0 else jankyFrameCount.toDouble() / frameCount.toDouble()

    val frozenFrameRatio: Double
        get() = if (frameCount == 0) 0.0 else frozenFrameCount.toDouble() / frameCount.toDouble()

    companion object {
        val Empty = PerformanceSessionSummary(
            frameCount = 0,
            jankyFrameCount = 0,
            averageFrameMs = 0.0,
            minFrameMs = 0.0,
            maxFrameMs = 0.0,
            p50FrameMs = 0.0,
            p90FrameMs = 0.0,
            p95FrameMs = 0.0,
            p99FrameMs = 0.0,
        )
    }
}
