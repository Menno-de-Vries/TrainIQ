package com.trainiq.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CrashContextProviderTest {
    @Test
    fun compactContext_usesStableCompactKeysAndLatestBreadcrumbs() {
        val breadcrumbs = BreadcrumbRingBuffer()
        breadcrumbs.add(message = "opened-home", category = "nav", timestampMillis = 100L)
        breadcrumbs.add(message = "started-workout", category = "workout", timestampMillis = 200L)
        val provider = CrashContextProvider(
            breadcrumbs = breadcrumbs,
            appVersion = "1.2.3",
            buildType = "debug",
            sessionId = "session-42",
        )

        val context = provider.compactContext()

        assertEquals(setOf("av", "bt", "sid", "frame.p99_ms", "jank.rate", "bc"), context.keys)
        assertEquals("1.2.3", context["av"])
        assertEquals("debug", context["bt"])
        assertEquals("session-42", context["sid"])
        assertEquals("nav:opened-home|workout:started-workout", context["bc"])
        assertFalse(context.containsKey("appVersion"))
    }
}
