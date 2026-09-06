package com.trainiq.core.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UserActionTest {
    @Test fun failedWriteDoesNotPublishSuccessAndNextActionCanRetry() = runTest {
        val failures = mutableListOf<Exception>()
        var value = "old"
        var successes = 0
        var failWrite = true
        suspend fun write() {
            if (failWrite) error("synthetic storage failure")
            value = "new"
        }
        launchUserAction({ failures.add(it) }) {
            write()
            successes++
        }.join()
        assertEquals(1, failures.size)
        assertEquals(0, successes)
        assertEquals("old", value)
        failWrite = false
        launchUserAction({ failures.add(it) }) { write(); successes++ }.join()
        assertEquals("new", value)
        assertEquals(1, successes)
    }

    @Test fun pendingSubmissionRejectsDoubleTapAndClearsForRetryAndCancellation() = runTest {
        val pending = MutableStateFlow(false)
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        var failures = 0
        assertTrue(launchSingleSubmission(pending, { failures++ }) { calls++; gate.await(); error("storage") })
        assertFalse(launchSingleSubmission(pending, { failures++ }) { calls++ })
        gate.complete(Unit)
        runCurrent()
        assertEquals(1, calls)
        assertEquals(1, failures)
        assertFalse(pending.value)
        launchSingleSubmission(pending, { failures++ }) { calls++ }
        assertEquals(2, calls)
        assertFalse(pending.value)
        launchSingleSubmission(pending, { failures++ }) { throw CancellationException("cancelled") }
        assertFalse(pending.value)
        assertEquals(1, failures)
    }

    @Test fun cancellationNeverBecomesFailureFeedback() = runTest {
        var failures = 0
        val job = launchUserAction({ failures++ }) { throw CancellationException("closed") }
        job.join()
        assertTrue(job.isCancelled)
        assertEquals(0, failures)
    }

    @Test fun nestedResultHandlingPreservesCancellationAndFailureRecovery() = runTest {
        var feedback = 0
        val job = launchUserAction({ feedback++ }) {
            runUserActionCatching { throw CancellationException("cancelled write") }
                .onFailure { feedback++ }
        }
        job.join()
        assertTrue(job.isCancelled)
        assertEquals(0, feedback)
        assertTrue(runUserActionCatching { error("write failed") }.isFailure)
        assertEquals("saved", runUserActionCatching { "saved" }.getOrThrow())
    }
}
