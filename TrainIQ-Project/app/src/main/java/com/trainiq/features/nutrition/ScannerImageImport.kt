package com.trainiq.features.nutrition

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Transfers a copied temporary image to its analysis owner only after returning to the UI. */
internal suspend fun importScannerImage(
    copy: () -> String?,
    consume: (String) -> Unit,
    failed: () -> Unit,
    release: (String) -> Unit = { deleteScannerTemporaryImage(it) },
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    var path: String? = null
    var transferred = false
    try {
        withContext(dispatcher) { path = copy() }
        val imported = path
        if (imported == null) failed() else {
            consume(imported)
            transferred = true
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        failed()
    } finally {
        if (!transferred) path?.let(release)
    }
}
