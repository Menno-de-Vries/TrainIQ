package com.trainiq.core.diagnostics

class CrashContextProvider(
    private val breadcrumbs: BreadcrumbRingBuffer,
    private val appVersion: String,
    private val buildType: String,
    sessionId: String,
) {
    @Volatile
    private var sessionId: String = sessionId

    @Volatile
    private var screenName: String = ""

    @Volatile
    private var performanceSummary: PerformanceSessionSummary = PerformanceSessionSummary.Empty

    fun updateScreen(name: String) {
        screenName = name
    }

    fun updateSessionId(id: String) {
        sessionId = id
    }

    fun updatePerformance(summary: PerformanceSessionSummary) {
        performanceSummary = summary
    }

    fun compactContext(): Map<String, String> = buildMap {
        put("av", appVersion)
        put("bt", buildType)
        put("sid", sessionId)
        if (screenName.isNotBlank()) put("screen", screenName)
        put("frame.p99_ms", performanceSummary.p99FrameMs.toString())
        put("jank.rate", performanceSummary.jankyFrameRatio.toString())
        val breadcrumbSummary = breadcrumbs.snapshot()
            .joinToString(separator = "|") { breadcrumb ->
                "${breadcrumb.category}:${breadcrumb.message}"
            }
        if (breadcrumbSummary.isNotBlank()) {
            put("bc", breadcrumbSummary)
        }
    }
}

interface CrashReporter {
    fun recordNonFatal(throwable: Throwable, context: Map<String, String> = emptyMap())
}

class NoOpCrashReporter : CrashReporter {
    override fun recordNonFatal(throwable: Throwable, context: Map<String, String>) = Unit
}
