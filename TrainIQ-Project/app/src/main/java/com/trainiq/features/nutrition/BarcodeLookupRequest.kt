package com.trainiq.features.nutrition

import com.trainiq.domain.model.BarcodeProductLookupResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Only the latest editor request may publish product data, even if an older lookup ignores cancellation. */
internal class BarcodeLookupRequest(
    private val scope: CoroutineScope,
    private val lookup: suspend (String) -> BarcodeProductLookupResult?,
    private val publish: (BarcodeLookupUiResult) -> Unit,
) {
    private var job: Job? = null
    private var revision = 0L

    fun start(barcode: String, target: BarcodeLookupTarget) {
        clear()
        val cleanBarcode = barcode.filter(Char::isDigit)
        if (cleanBarcode.isBlank()) return
        val requestRevision = revision
        job = scope.launch {
            val product = try {
                lookup(cleanBarcode)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (requestRevision == revision) {
                publish(BarcodeLookupUiResult(target, product, cleanBarcode))
            }
        }
    }

    fun clear() {
        revision++
        job?.cancel()
        job = null
    }
}
