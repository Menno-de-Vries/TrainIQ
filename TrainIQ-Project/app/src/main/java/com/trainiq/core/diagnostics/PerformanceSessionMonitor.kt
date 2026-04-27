package com.trainiq.core.diagnostics

import android.app.Activity

interface PerformanceSessionMonitor {
    val sessionId: String

    fun start(activity: Activity)
    fun setEnabled(enabled: Boolean)
    fun updateScreen(screenName: String)
    fun stop()
    fun latestSummary(): PerformanceSessionSummary
}
