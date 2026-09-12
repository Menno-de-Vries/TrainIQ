package com.trainiq.features.nutrition

import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class BarcodeFrameInstrumentedTest {
    @Test fun emptyFailureCancellationAndSynchronousFailureReleaseEachFrameOnce() {
        var closes = 0
        var errors = 0
        val cancelled = CancellationTokenSource()
        val cancelledTask = TaskCompletionSource<List<Barcode>>(cancelled.token)
        cancelled.cancel()
        val starts = listOf<() -> com.google.android.gms.tasks.Task<List<Barcode>>>(
            { Tasks.forResult(emptyList()) },
            { Tasks.forException(IllegalStateException("Transient frame failure")) },
            { cancelledTask.task },
            { throw IllegalArgumentException("Invalid frame") },
            { Tasks.forResult(emptyList()) }, // Recognition remains usable after failures.
        )
        starts.forEach { start ->
            val released = CountDownLatch(1)
            processBarcodeFrame(
                close = { closes++ }, start = start, onError = { errors++ },
                onClosed = { released.countDown() }, onDetected = { fail("No decoded barcode expected") },
            )
            assertTrue(released.await(5, TimeUnit.SECONDS))
        }
        assertEquals(5, closes)
        assertEquals(2, errors)
    }
}
