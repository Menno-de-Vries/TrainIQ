package com.trainiq.features.settings

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsDocumentRequestTest {
    @Test fun slowOlderDocumentCannotReplaceLatestPreview() = runTest {
        val old = CompletableDeferred<String>()
        val published = mutableListOf<String>()
        val request = SettingsDocumentRequest(backgroundScope, {
            if (it == "old") withContext(NonCancellable) { old.await() } else "new document"
        }, { published += it }, { error("unexpected failure") })
        request.start("old")
        runCurrent()
        request.start("new")
        runCurrent()
        old.complete("old document")
        runCurrent()
        assertEquals(listOf("new document"), published)
    }

    @Test fun cancellationDoesNotReportReadFailureAndNextDocumentRecovers() = runTest {
        var failures = 0
        val published = mutableListOf<String>()
        val request = SettingsDocumentRequest(backgroundScope, {
            when (it) { "cancel" -> throw CancellationException(); "bad" -> error("storage"); else -> it }
        }, { published += it }, { failures++ })
        request.start("cancel"); runCurrent()
        assertEquals(0, failures)
        request.start("bad"); runCurrent()
        assertEquals(1, failures)
        request.start("good"); runCurrent()
        assertEquals(listOf("good"), published)
    }
}
