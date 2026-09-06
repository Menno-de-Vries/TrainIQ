package com.trainiq.features.nutrition

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** A dismissed scan cannot publish into a newer preview, even if its provider ignores cancellation. */
internal class LatestScanRequest(
    private val scope: CoroutineScope,
    private val releaseImage: (String) -> Unit,
) {
    private var job: Job? = null
    private var revision = 0L

    fun <T> start(path: String, analyze: suspend () -> T, publish: (T) -> Unit, fail: (Throwable) -> Unit) {
        cancel()
        val token = revision
        // Enter try/finally immediately so cancellation before dispatch still releases the image.
        job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val result = analyze()
                currentCoroutineContext().ensureActive()
                if (token == revision) publish(result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (token == revision) fail(error)
            } finally {
                releaseImage(path)
            }
        }
    }

    fun cancel() {
        revision++
        job?.cancel()
        job = null
    }
}
