package com.trainiq.features.nutrition

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainiq.domain.model.BarcodeProductLookupResult
import com.trainiq.domain.model.FoodProviderMode
import com.trainiq.domain.usecase.LookupBarcodeProductUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class BarcodeProductScannerViewModel internal constructor(
    private val savedState: androidx.lifecycle.SavedStateHandle? = null,
    lookup: suspend (String, FoodProviderMode) -> BarcodeProductLookupResult?,
) : ViewModel() {
    @Inject constructor(lookup: LookupBarcodeProductUseCase, savedState: androidx.lifecycle.SavedStateHandle) :
        this(savedState, { barcode, mode -> lookup(barcode, mode) })
    private val state = MutableStateFlow<CameraScannerUiState>(CameraScannerUiState.Preview("", true))
    val uiState = state.asStateFlow()
    var lastBarcode: String = savedState?.get<String>("last_barcode").orEmpty()
        private set(value) {
            field = value
            savedState?.set("last_barcode", value)
        }
    private var provider = FoodProviderMode.AUTOMATIC
    private var consumed = false
    private var closed = false
    private var suppressedBarcode: String? = null
    private val request = BarcodeLookupRequest(viewModelScope, { lookup(it, provider) }) { response ->
        if (response.product != null) state.value = CameraScannerUiState.BarcodeReady(response.product)
        else state.value = CameraScannerUiState.Empty("", response.userMessage())
    }
    fun scan(barcode: String, mode: FoodProviderMode) {
        if (closed) return
        if (state.value !is CameraScannerUiState.Preview && state.value !is CameraScannerUiState.Empty) return
        if (barcode == suppressedBarcode) return
        if (barcode.length !in 8..14 || barcode.any { it !in '0'..'9' }) return
        suppressedBarcode = barcode
        consumed = false
        lastBarcode = barcode
        provider = mode
        state.value = CameraScannerUiState.Processing
        request.start(barcode, BarcodeLookupTarget.FOOD_EDITOR)
    }
    fun reset() {
        request.clear()
        lastBarcode = ""
        suppressedBarcode = null
        consumed = false
        state.value = CameraScannerUiState.Preview("", true)
    }
    fun retry() {
        if (closed) return
        request.clear()
        suppressedBarcode = null // Explicit user retry authorizes one more lookup.
        state.value = CameraScannerUiState.Preview("", true)
    }
    fun consumeProduct(): BarcodeProductLookupResult? {
        if (consumed) return null
        val product = (state.value as? CameraScannerUiState.BarcodeReady)?.product ?: return null
        consumed = true
        return product
    }
    fun close() {
        closed = true
        request.clear()
    }
}

@Composable
fun BarcodeProductScannerRoute(
    provider: FoodProviderMode,
    onProduct: (BarcodeProductLookupResult) -> Unit,
    onBack: () -> Unit,
    onManual: (String) -> Unit,
    viewModel: BarcodeProductScannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val product = (state as? CameraScannerUiState.BarcodeReady)?.product
    LaunchedEffect(product) { viewModel.consumeProduct()?.let(onProduct) }
    CameraScannerScreen(uiState = state, scannerMode = ScannerMode.BARCODE,
            onAnalyze = {}, onDismissError = viewModel::retry,
            onScanAgain = viewModel::retry, onReviewItems = {},
            onReviewScaleMeasurement = {}, onBack = { viewModel.close(); onBack() },
            onManual = { val barcode = viewModel.lastBarcode; viewModel.close(); onManual(barcode) },
            onBarcodeScanned = { viewModel.scan(it, provider) })
}
