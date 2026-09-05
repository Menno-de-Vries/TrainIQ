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
        request.start("letters", BarcodeLookupTarget.entries.first())
        runCurrent()
        assertEquals(1, results.size)
    }
}
