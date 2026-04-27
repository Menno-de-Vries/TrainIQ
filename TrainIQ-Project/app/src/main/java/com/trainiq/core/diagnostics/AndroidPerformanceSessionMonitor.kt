package com.trainiq.core.diagnostics

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.metrics.performance.JankStats
import com.trainiq.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPerformanceSessionMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val store: PerformanceSessionStore,
    private val crashContextProvider: CrashContextProvider,
    private val anomalyPolicy: FrameAnomalyPolicy,
    private val perfettoTraceController: PerfettoTraceController,
    private val telemetry: Telemetry,
    private val telemetryConfig: TelemetryConfig,
    private val telemetryExporter: TelemetryExporter,
) : PerformanceSessionMonitor {
    override val sessionId: String = UUID.randomUUID().toString()

    private val frameStats = FrameStatsAccumulator()
    private var jankStats: JankStats? = null
    private var currentScreen: String = "Unknown"
    private var latestSummary: PerformanceSessionSummary = PerformanceSessionSummary.Empty

    override fun start(activity: Activity) {
        if (jankStats != null) return
        crashContextProvider.updateSessionId(sessionId)
        jankStats = JankStats.createAndTrack(activity.window) { frameData ->
            frameStats.recordFrameDurationMs(frameData.frameDurationUiNanos / 1_000_000.0)
        }.also { it.isTrackingEnabled = true }
        telemetry.event("app.startup", mapOf("build_type" to BuildConfig.BUILD_TYPE))
    }

    override fun setEnabled(enabled: Boolean) {
        jankStats?.isTrackingEnabled = enabled
        if (!enabled) persistCurrentSummary()
    }

    override fun updateScreen(screenName: String) {
        currentScreen = screenName
        crashContextProvider.updateScreen(screenName)
    }

    override fun stop() {
        persistCurrentSummary()
        jankStats?.isTrackingEnabled = false
        jankStats = null
    }

    override fun latestSummary(): PerformanceSessionSummary = latestSummary

    private fun persistCurrentSummary() {
        val summary = frameStats.summary().copy(
            sessionId = sessionId,
            appVersion = BuildConfig.VERSION_NAME,
            buildType = BuildConfig.BUILD_TYPE,
            screenName = currentScreen,
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
            apiLevel = Build.VERSION.SDK_INT,
            locale = Locale.getDefault().toLanguageTag(),
            batteryPercent = batteryPercent(),
            freeMemoryMb = freeMemoryMb(),
        )
        latestSummary = summary
        crashContextProvider.updatePerformance(summary)
        store.save(summary)
        telemetryExporter.enqueue(TelemetryPayload.Summary(summary))
        if (telemetryConfig.perfettoEnabled && anomalyPolicy.shouldTrace(summary, System.currentTimeMillis())) {
            perfettoTraceController.startTrace("jank:${summary.screenName}:${summary.p99FrameMs}")
        }
    }

    private fun batteryPercent(): Int? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) (level * 100) / scale else null
    }

    private fun freeMemoryMb(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0L
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        return info.availMem / (1024L * 1024L)
    }
}
