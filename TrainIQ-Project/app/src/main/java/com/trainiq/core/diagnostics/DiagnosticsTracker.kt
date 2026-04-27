package com.trainiq.core.diagnostics

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticsTracker @Inject constructor(
    private val breadcrumbs: BreadcrumbRingBuffer,
    private val telemetry: Telemetry,
    private val performanceSessionMonitor: PerformanceSessionMonitor,
) {
    val sessionId: String
        get() = performanceSessionMonitor.sessionId

    fun screen(name: String) {
        breadcrumbs.add(message = "Screen:$name", category = "screen", timestampMillis = System.currentTimeMillis())
        performanceSessionMonitor.updateScreen(name)
        telemetry.event("screen.show", mapOf("screen" to name))
    }

    fun tap(name: String) {
        breadcrumbs.add(message = name, category = "tap", timestampMillis = System.currentTimeMillis())
        telemetry.event("tap", mapOf("target" to name))
    }

    fun state(name: String) {
        breadcrumbs.add(message = name, category = "state", timestampMillis = System.currentTimeMillis())
        telemetry.event("state.change", mapOf("state" to name))
    }
}
