package com.trainiq.features.nutrition

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LatestScanRequestTest {
    @Test fun rejectingAnImportWithoutAProviderReleasesItAndCancelsPreviousAnalysis() = runTest {
        val released = mutableListOf<String>()
        val gate = CompletableDeferred<String>()
        val request = LatestScanRequest(backgroundScope) { released.add(it) }
        request.start("previous", { withContext(NonCancellable) { gate.await() } },
            { error("Rejected scan must not publish") }, { error("Unexpected feedback") })
        request.discard("disabled-provider-import")
        gate.complete("late result")
        runCurrent()
        assertEquals(setOf("previous", "disabled-provider-import"), released.toSet())
        assertEquals(2, released.size)
    }

    @Test fun ownerCancellationDoesNotPublishAProvidersLateFailure() = runTest {
        val owner = Job()
        val released = mutableListOf<String>()
        val gate = CompletableDeferred<Unit>()
        val request = LatestScanRequest(CoroutineScope(coroutineContext + owner)) { released.add(it) }
        request.start<String>("owned", {
            withContext(NonCancellable) { gate.await(); error("late provider failure") }
        }, { error("Unexpected result") }, { error("Unexpected failure feedback") })
        owner.cancel()
        gate.complete(Unit)
        runCurrent()
        owner.join()
        assertEquals(listOf("owned"), released)
    }

    @Test fun dismissedNonCooperativeScanCannotReplaceANewerResult() = runTest {
        val released = mutableListOf<String>()
        val results = mutableListOf<String>()
        val failures = mutableListOf<Throwable>()
        val request = LatestScanRequest(backgroundScope) { released.add(it) }
        val gate = CompletableDeferred<String>()
        request.start("old", { withContext(NonCancellable) { gate.await() } }, { results.add(it) }, { failures.add(it) })
        request.cancel()
        request.start("new", { "new result" }, { results.add(it) }, { failures.add(it) })
        gate.complete("stale result")
        runCurrent()
        assertEquals(listOf("new result"), results)
        assertEquals(setOf("old", "new"), released.toSet())
        assertTrue(failures.isEmpty())
    }

    @Test fun immediateCancellationReleasesImageWithoutShowingAnError() = runTest {
        val released = mutableListOf<String>()
        val request = LatestScanRequest(backgroundScope) { released.add(it) }
        request.start("cancelled", { CompletableDeferred<String>().await() }, { error("Unexpected result") }, { error("Unexpected failure") })
        request.cancel()
        runCurrent()
        assertEquals(listOf("cancelled"), released)
    }

    @Test fun failedScanReleasesImageAndAllowsRetry() = runTest {
        val released = mutableListOf<String>()
        val failures = mutableListOf<Throwable>()
        val results = mutableListOf<String>()
        val request = LatestScanRequest(backgroundScope) { released.add(it) }
        request.start<String>("failed", { error("offline") }, { results.add(it) }, { failures.add(it) })
        request.start("retry", { "meal" }, { results.add(it) }, { failures.add(it) })
        assertEquals(1, failures.size)
        assertEquals(listOf("meal"), results)
        assertEquals(listOf("failed", "retry"), released)
    }
}
