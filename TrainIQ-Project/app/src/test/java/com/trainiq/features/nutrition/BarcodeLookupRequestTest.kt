package com.trainiq.features.nutrition

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BarcodeLookupRequestTest {
    @Test fun repeatedCallbacksResolveOnceAndPublishUsableProduct() = runTest {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val product = com.trainiq.domain.model.BarcodeProductLookupResult("12345678", "Kwark", 60.0, 10.0, 4.0, 0.0)
        val results = mutableListOf<BarcodeLookupUiResult>()
        val request = BarcodeLookupRequest(backgroundScope, { calls++; gate.await(); product }, results::add)
        repeat(10) { request.start("12345678", BarcodeLookupTarget.FOOD_EDITOR); runCurrent() }
        assertEquals(1, calls)
        assertTrue(results.isEmpty())
        gate.complete(Unit); runCurrent()
        assertEquals(product, results.single().product)
        assertFalse(results.single().failed)
    }

    @Test fun unknownBarcodeIsExplicitlyNotSuccess() = runTest {
        val results = mutableListOf<BarcodeLookupUiResult>()
        val request = BarcodeLookupRequest(backgroundScope, { null }, results::add)
        request.start("12345678", BarcodeLookupTarget.FOOD_EDITOR); runCurrent()
        assertNull(results.single().product)
        assertTrue(results.single().userMessage().contains("niet gevonden"))
    }

    @Test
    fun oldResponseCannotReplaceNewBarcodeOrEditorTarget() = runTest {
        val first = CompletableDeferred<Unit>()
        val results = mutableListOf<BarcodeLookupUiResult>()
        val request = BarcodeLookupRequest(backgroundScope, { barcode ->
            if (barcode == "11111111") withContext(NonCancellable) { first.await() }
            null
        }, results::add)
        request.start("11111111", BarcodeLookupTarget.entries.first())
        runCurrent()
        request.start("22-222222", BarcodeLookupTarget.entries.last())
        runCurrent()
        first.complete(Unit)
        runCurrent()
        assertEquals(listOf("22222222"), results.map { it.barcode })
        assertEquals(BarcodeLookupTarget.entries.last(), results.single().target)
    }

    @Test
    fun clearDiscardsPendingResponseAndFailureRetainsManualBarcodeFallback() = runTest {
        val gate = CompletableDeferred<Unit>()
        val results = mutableListOf<BarcodeLookupUiResult>()
        val request = BarcodeLookupRequest(backgroundScope, { barcode ->
            if (barcode == "11111111") withContext(NonCancellable) { gate.await() }
            else error("offline")
            null
        }, results::add)
        request.start("11111111", BarcodeLookupTarget.entries.first())
        runCurrent()
        request.clear()
        gate.complete(Unit)
        runCurrent()
        assertTrue(results.isEmpty())
        request.start("22222222", BarcodeLookupTarget.entries.first())
        runCurrent()
        assertEquals("22222222", results.single().barcode)
        assertNull(results.single().product)
        assertTrue(results.single().failed)
        assertTrue(results.single().userMessage().startsWith("Product ophalen mislukt."))
        request.start("letters", BarcodeLookupTarget.entries.first())
        runCurrent()
        assertEquals(2, results.size)
        assertEquals(com.trainiq.domain.model.FoodLookupFailure.INVALID_BARCODE, results.last().failure)
    }
}
