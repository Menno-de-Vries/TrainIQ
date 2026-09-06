package com.trainiq.features.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class SettingsDocumentRequest(
    private val scope: CoroutineScope,
    private val read: suspend (String) -> String,
    private val publish: (String) -> Unit,
    private val failure: () -> Unit,
) {
    private var job: Job? = null
    private var revision = 0L

    fun start(document: String) {
        val request = ++revision
        job?.cancel()
        job = scope.launch {
            try {
                val json = read(document)
                if (request == revision) publish(json)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (request == revision) failure()
            }
        }
    }
}
