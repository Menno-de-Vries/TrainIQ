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
    private var activeRequest: Pair<String, BarcodeLookupTarget>? = null

    fun start(barcode: String, target: BarcodeLookupTarget) {
        val cleanBarcode = barcode.trim().replace("-", "").replace(" ", "")
        if (activeRequest == (cleanBarcode to target) && job?.isActive == true) return
        clear()
        activeRequest = cleanBarcode to target
        if (cleanBarcode.isBlank() || cleanBarcode.any { it !in '0'..'9' }) {
            publish(BarcodeLookupUiResult(target, null, cleanBarcode, true, com.trainiq.domain.model.FoodLookupFailure.INVALID_BARCODE))
            return
        }
        val requestRevision = revision
        job = scope.launch {
            var failed = false
            var failure: com.trainiq.domain.model.FoodLookupFailure? = null
            val product = try {
                lookup(cleanBarcode)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: com.trainiq.domain.model.FoodLookupException) {
                failed = true
                failure = error.failure
                null
            } catch (_: Exception) {
                failed = true
                null
            }
            if (requestRevision == revision) {
                publish(BarcodeLookupUiResult(target, product, cleanBarcode, failed, failure))
            }
        }
    }

    fun clear() {
        revision++
        job?.cancel()
        job = null
        activeRequest = null
    }
}
