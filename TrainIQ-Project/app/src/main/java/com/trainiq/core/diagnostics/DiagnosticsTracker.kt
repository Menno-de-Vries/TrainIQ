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

    fun aiFailure(attributes: Map<String, String>) {
        val safeAttributes = attributes.entries
            .filter { it.key in AiFailureAttributeKeys }
            .associate { (key, value) -> key to value.toSafeDiagnosticValue() }
        if (safeAttributes.isEmpty()) return
        breadcrumbs.add(
            message = listOfNotNull(
                "AI failure",
                safeAttributes["request_id"],
                safeAttributes["provider"],
                safeAttributes["feature"],
                safeAttributes["category"],
                safeAttributes["http_status"],
                safeAttributes["error_code"],
            ).joinToString(":"),
            category = "ai",
            timestampMillis = System.currentTimeMillis(),
        )
        telemetry.event("ai.remote.failure", safeAttributes)
    }
}

private val AiFailureAttributeKeys = setOf(
    "provider",
    "feature",
    "category",
    "http_status",
    "error_code",
    "request_id",
    "retry_after_ms",
)

private fun String.toSafeDiagnosticValue(): String =
    trim()
        .take(128)
        .map { char -> if (char.isLetterOrDigit() || char in "-_.:") char else '_' }
        .joinToString("")
