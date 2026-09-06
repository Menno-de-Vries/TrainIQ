package com.trainiq.features.nutrition

import java.util.concurrent.Executors
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ScannerImageImportTest {
    @Test fun fileCopyRunsOnIoDispatcherAndTransfersExactlyOnce() = runTest {
        Executors.newSingleThreadExecutor { Thread(it, "image-import-test") }.asCoroutineDispatcher().use { io ->
            val consumed = mutableListOf<String>()
            importScannerImage(
                copy = { assertEquals("image-import-test", Thread.currentThread().name); "image" },
                consume = { consumed.add(it) }, failed = { fail("Unexpected failure") },
                release = { fail("Transferred file must belong to analysis") }, dispatcher = io,
            )
            assertEquals(listOf("image"), consumed)
        }
    }

    @Test fun cancellationAfterCopyReleasesImageWithoutPublishing() = runTest {
        val released = mutableListOf<String>()
        val owner = Job()
        val job = CoroutineScope(coroutineContext + owner).launch {
            importScannerImage(copy = { owner.cancel(); "image" },
                consume = { fail("Cancelled image published") }, failed = { fail("Cancellation is not an error") },
                release = { released.add(it) })
        }
        job.join()
        assertEquals(listOf("image"), released)
    }

    @Test fun unreadableImageReportsFailureAndRetryTransfers() = runTest {
        var failures = 0
        var consumed: String? = null
        importScannerImage(copy = { null }, consume = { consumed = it }, failed = { failures++ })
        importScannerImage(copy = { "retry" }, consume = { consumed = it }, failed = { failures++ })
        assertEquals(1, failures)
        assertEquals("retry", consumed)
    }
}
