package com.trainiq.core.diagnostics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceSessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences("trainiq_performance_sessions", Context.MODE_PRIVATE)

    fun save(summary: PerformanceSessionSummary) {
        val compact = listOf(
            "sid=${summary.sessionId}",
            "av=${summary.appVersion}",
            "bt=${summary.buildType}",
            "screen=${summary.screenName}",
            "abi=${summary.abi}",
            "frames=${summary.frameCount}",
            "jank=${summary.jankyFrameCount}",
            "frozen=${summary.frozenFrameCount}",
            "p50=${summary.p50FrameMs}",
            "p90=${summary.p90FrameMs}",
            "p99=${summary.p99FrameMs}",
            "jr=${summary.jankyFrameRatio}",
        ).joinToString(separator = ";")
        preferences.edit().putString("latest_summary", compact).apply()
    }

    fun latestCompactSummary(): String = preferences.getString("latest_summary", "").orEmpty()
}
