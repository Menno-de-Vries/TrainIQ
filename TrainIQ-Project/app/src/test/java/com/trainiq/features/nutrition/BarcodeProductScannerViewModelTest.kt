package com.trainiq.features.nutrition

import com.trainiq.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BarcodeProductScannerViewModelTest {
    @Test fun onlyUsableCompletedLookupCanLeaveResolvingAndCallbacksAreSingleFlight() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val gate = CompletableDeferred<BarcodeProductLookupResult?>()
            var calls = 0
            val model = BarcodeProductScannerViewModel { _, _ -> calls++; gate.await() }
            repeat(20) { model.scan("12345678", FoodProviderMode.OPEN_FOOD_FACTS) }
            runCurrent()
            assertEquals(1, calls)
            assertEquals(CameraScannerUiState.Processing, model.uiState.value)
            val product = BarcodeProductLookupResult("12345678", "Kwark", 60.0, 10.0, 4.0, 0.0)
            gate.complete(product); runCurrent()
            assertEquals(CameraScannerUiState.BarcodeReady(product), model.uiState.value)
            model.reset()
        } finally { Dispatchers.resetMain() }
    }

    @Test fun missingAndFailedLookupStayInScannerAndAllowRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var fail = false
            val model = BarcodeProductScannerViewModel { _, _ -> if (fail) error("offline") else null }
            model.scan("12345678", FoodProviderMode.OPEN_FOOD_FACTS); runCurrent()
            assertTrue(model.uiState.value is CameraScannerUiState.Empty)
            assertEquals("12345678", model.lastBarcode)
            model.reset(); fail = true
            model.scan("87654321", FoodProviderMode.OPEN_FOOD_FACTS); runCurrent()
            val state = model.uiState.value as CameraScannerUiState.Empty
            assertTrue(state.message.contains("ophalen mislukt"))
            model.reset()
        } finally { Dispatchers.resetMain() }
    }

    @Test fun leavingScannerInvalidatesEvenUncooperativePendingLookup() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val gate = CompletableDeferred<Unit>()
            val model = BarcodeProductScannerViewModel { _, _ -> withContext(NonCancellable) { gate.await() }; null }
            model.scan("12345678", FoodProviderMode.OPEN_FOOD_FACTS); runCurrent()
            model.reset(); gate.complete(Unit); runCurrent()
            assertTrue(model.uiState.value is CameraScannerUiState.Preview)
            assertEquals("", model.lastBarcode)
        } finally { Dispatchers.resetMain() }
    }
}
