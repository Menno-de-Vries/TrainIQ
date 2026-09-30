package com.trainiq.features.progress

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressPhotoImportSessionTest {
    @Test fun unchangedDraftReceivesOneResultAndPendingEnds() = runTest {
        val released = mutableListOf<String>()
        val results = mutableListOf<String>()
        val gate = CompletableDeferred<String>()
        val session = ProgressPhotoImportSession(backgroundScope, released::add)
        val token = session.begin()
        session.analyze(token, "photo", { gate.await() }, results::add, { error("Unexpected failure") })
        assertTrue(session.isPending.value)
        gate.complete("measurement")
        runCurrent()
        assertEquals(listOf("measurement"), results)
        assertFalse(session.isPending.value)
        assertEquals(listOf("photo"), released)
    }

    @Test fun manualEditOrSaveInvalidatesNonCooperativeResult() = runTest {
        val released = mutableListOf<String>()
        val results = mutableListOf<String>()
        val gate = CompletableDeferred<String>()
        val session = ProgressPhotoImportSession(backgroundScope, released::add)
        session.analyze(session.begin(), "photo", { withContext(NonCancellable) { gate.await() } }, results::add, { error("Unexpected failure") })
        session.invalidate()
        assertFalse(session.isPending.value)
        gate.complete("old measurement")
        runCurrent()
        assertTrue(results.isEmpty())
        assertEquals(listOf("photo"), released)
    }

    @Test fun editWhileImageIsCopiedRejectsProviderAdmissionAndReleasesImage() = runTest {
        val released = mutableListOf<String>()
        var calls = 0
        val session = ProgressPhotoImportSession(backgroundScope, released::add)
        val token = session.begin()
        session.invalidate()
        session.analyze(token, "copied-late", { calls++; "measurement" }, { error("Unexpected result") }, { error("Unexpected failure") })
        assertEquals(0, calls)
        assertFalse(session.isPending.value)
        assertEquals(listOf("copied-late"), released)
    }

    @Test fun replacementWinsAndOldCopyCannotCancelTheNewRequest() = runTest {
        val released = mutableListOf<String>()
        val results = mutableListOf<String>()
        val session = ProgressPhotoImportSession(backgroundScope, released::add)
        val oldToken = session.begin()
        val newToken = session.begin()
        val gate = CompletableDeferred<String>()
        session.analyze(newToken, "new", { gate.await() }, results::add, { error("Unexpected failure") })
        session.analyze(oldToken, "old", { error("Stale request admitted") }, results::add, { error("Unexpected failure") })
        assertTrue(session.isPending.value)
        gate.complete("new measurement")
        runCurrent()
        assertEquals(listOf("new measurement"), results)
        assertEquals(setOf("new", "old"), released.toSet())
        assertFalse(session.isPending.value)
    }

    @Test fun analysisOrCopyFailureEndsPendingAndAllowsRetry() = runTest {
        val released = mutableListOf<String>()
        val failures = mutableListOf<Throwable>()
        val session = ProgressPhotoImportSession(backgroundScope, released::add)
        val first = session.begin()
        assertTrue(session.finishCopyFailure(first))
        assertFalse(session.isPending.value)
        session.analyze(session.begin(), "failed", { error("offline") }, { error("Unexpected result") }, failures::add)
        assertEquals(1, failures.size)
        assertFalse(session.isPending.value)
        val results = mutableListOf<String>()
        session.analyze(session.begin(), "retry", { "measurement" }, results::add, failures::add)
        assertEquals(listOf("measurement"), results)
        assertEquals(listOf("failed", "retry"), released)
    }
}
