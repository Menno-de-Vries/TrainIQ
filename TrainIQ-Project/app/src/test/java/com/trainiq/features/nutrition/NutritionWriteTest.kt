package com.trainiq.features.nutrition

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NutritionWriteTest {
    @Test fun cancellationReleasesPendingWithoutFailureOrSuccessCallback() = runTest {
        var successes = 0
        var failures = 0
        var finished = false
        val cancelled = CancellationException("leaving screen")
        val result = runCatching {
            performNutritionWrite({ throw cancelled }, { successes++ }, { failures++ }, { finished = true })
        }
        assertSame(cancelled, result.exceptionOrNull())
        assertEquals(0, successes)
        assertEquals(0, failures)
        assertTrue(finished)
    }

    @Test fun storageFailureReleasesPendingAndRetryCanPublishSuccess() = runTest {
        val events = mutableListOf<String>()
        suspend fun attempt(fail: Boolean) = performNutritionWrite(
            { if (fail) error("disk"); "saved" }, { events += it }, { events += "failure" }, { events += "finished" })
        attempt(true)
        attempt(false)
        assertEquals(listOf("failure", "finished", "saved", "finished"), events)
    }
}
