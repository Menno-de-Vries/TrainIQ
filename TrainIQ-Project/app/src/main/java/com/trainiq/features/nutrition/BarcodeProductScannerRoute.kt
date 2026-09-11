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
class BarcodeProductScannerViewModel internal constructor(lookup: suspend (String, FoodProviderMode) -> BarcodeProductLookupResult?) : ViewModel() {
    @Inject constructor(lookup: LookupBarcodeProductUseCase) : this({ barcode, mode -> lookup(barcode, mode) })
    private val state = MutableStateFlow<CameraScannerUiState>(CameraScannerUiState.Preview("", true))
    val uiState = state.asStateFlow()
    var lastBarcode: String = ""
        private set
    private var provider = FoodProviderMode.AUTOMATIC
    private val request = BarcodeLookupRequest(viewModelScope, { lookup(it, provider) }) { response ->
        if (response.product != null) state.value = CameraScannerUiState.BarcodeReady(response.product)
        else state.value = CameraScannerUiState.Empty("", response.userMessage())
    }
    fun scan(barcode: String, mode: FoodProviderMode) {
        if (state.value !is CameraScannerUiState.Preview) return
        lastBarcode = barcode
        provider = mode
        state.value = CameraScannerUiState.Processing
        request.start(barcode, BarcodeLookupTarget.FOOD_EDITOR)
    }
    fun reset() {
        request.clear()
        lastBarcode = ""
        state.value = CameraScannerUiState.Preview("", true)
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
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(product) { product?.let(onProduct) }
    key(attempt) {
        CameraScannerScreen(uiState = state, scannerMode = ScannerMode.BARCODE,
            onAnalyze = {}, onDismissError = { viewModel.reset(); attempt++ },
            onScanAgain = { viewModel.reset(); attempt++ }, onReviewItems = {},
            onReviewScaleMeasurement = {}, onBack = { viewModel.reset(); onBack() },
            onManual = { val barcode = viewModel.lastBarcode; viewModel.reset(); onManual(barcode) },
            onBarcodeScanned = { viewModel.scan(it, provider) })
    }
}
