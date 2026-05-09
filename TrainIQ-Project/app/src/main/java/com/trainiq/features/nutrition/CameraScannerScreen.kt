package com.trainiq.features.nutrition

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.trainiq.ai.services.AiUsageGate
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.theme.spacing
import com.trainiq.core.ui.ScreenUiState
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealAnalysisSource
import com.trainiq.domain.model.MealType
import com.trainiq.domain.usecase.AnalyzeMealUseCase
import com.trainiq.domain.usecase.ClearLastScanResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Keep
enum class ScannerMode { BARCODE, AI_MEAL }

sealed interface CameraScannerUiState {
    data class Preview(
        val contextHint: String,
        val isEnabled: Boolean,
        val message: String? = null,
    ) : CameraScannerUiState

    data object Processing : CameraScannerUiState
    data class Completed(val suggestedMealType: MealType?, val itemCount: Int = 0) : CameraScannerUiState
    data class Empty(val contextHint: String, val message: String) : CameraScannerUiState
    data class NoConfig(val contextHint: String, val message: String) : CameraScannerUiState
    data class LocalFallback(val contextHint: String, val message: String) : CameraScannerUiState
    data class Error(val contextHint: String, val message: String) : CameraScannerUiState
}

data class CameraUiContent(
    val scannerState: CameraScannerUiState,
)

internal data class CameraScannerRestorableState(
    val permissionDenied: Boolean = false,
    val cameraError: String? = null,
) {
    companion object {
        val Saver: Saver<CameraScannerRestorableState, List<Any?>> = Saver(
            save = { saveCameraScannerRestorableState(it) },
            restore = ::restoreCameraScannerRestorableState,
        )
    }
}

internal fun saveCameraScannerRestorableState(state: CameraScannerRestorableState): List<Any?> =
    listOf(state.permissionDenied, state.cameraError)

internal fun restoreCameraScannerRestorableState(saved: List<Any?>): CameraScannerRestorableState =
    CameraScannerRestorableState(
        permissionDenied = saved.getOrNull(0) as? Boolean ?: false,
        cameraError = saved.getOrNull(1) as? String,
    )

internal fun cameraScreenUiState(scannerState: CameraScannerUiState): ScreenUiState<CameraUiContent> =
    ScreenUiState.Success(CameraUiContent(scannerState))

@HiltViewModel
class CameraScannerViewModel @Inject constructor(
    preferencesRepository: UserPreferencesRepository,
    aiUsageGate: AiUsageGate,
    private val analyzeMealUseCase: AnalyzeMealUseCase,
    private val clearLastScanResultUseCase: ClearLastScanResultUseCase,
) : ViewModel() {
    private data class ScannerEphemeralState(
        val contextHint: String = "",
        val phase: Phase = Phase.Preview,
        val message: String? = null,
        val suggestedMealType: MealType? = null,
        val itemCount: Int = 0,
    )

    private enum class Phase { Preview, Processing, Completed, Empty, NoConfig, LocalFallback, Error }

    private val aiPreferences: StateFlow<AiPreferences> = preferencesRepository.aiPreferences
        .mapResolvedAiPreferences(aiUsageGate)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiPreferences(false, ""))
    private val ephemeral = MutableStateFlow(ScannerEphemeralState())

    val uiState: StateFlow<ScreenUiState<CameraUiContent>> = combine(aiPreferences, ephemeral) { ai, temp ->
        cameraScreenUiState(cameraScannerUiState(ai, temp))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScreenUiState.Loading)

    private fun cameraScannerUiState(ai: AiPreferences, temp: ScannerEphemeralState): CameraScannerUiState =
        when (temp.phase) {
            Phase.Preview -> CameraScannerUiState.Preview(
                contextHint = temp.contextHint,
                isEnabled = ai.enabled && ai.apiKey.isNotBlank(),
                message = temp.message ?: when {
                    !ai.enabled -> "AI staat uit in Instellingen. Zet AI aan voordat je scant."
                    ai.apiKey.isBlank() -> "Voeg eerst een Gemini API-sleutel toe in Instellingen."
                    else -> null
                },
            )
            Phase.Processing -> CameraScannerUiState.Processing
            Phase.Completed -> CameraScannerUiState.Completed(
                suggestedMealType = temp.suggestedMealType,
                itemCount = temp.itemCount,
            )
            Phase.Empty -> CameraScannerUiState.Empty(
                contextHint = temp.contextHint,
                message = temp.message ?: "Geen producten gevonden.",
            )
            Phase.NoConfig -> CameraScannerUiState.NoConfig(
                contextHint = temp.contextHint,
                message = temp.message ?: "AI-scan niet ingesteld. Zet AI aan en voeg een Gemini API-sleutel toe in Instellingen.",
            )
            Phase.LocalFallback -> CameraScannerUiState.LocalFallback(
                contextHint = temp.contextHint,
                message = temp.message ?: "Lokale fallback gebruikt. Voeg de maaltijd handmatig toe.",
            )
            Phase.Error -> CameraScannerUiState.Error(
                contextHint = temp.contextHint,
                message = temp.message ?: "Scan mislukt. Probeer opnieuw.",
            )
        }

    fun setContextHint(hint: String) {
        ephemeral.update { it.copy(contextHint = hint.trim(), phase = Phase.Preview, message = null) }
    }

    fun analyze(path: String) {
        if (ephemeral.value.phase == Phase.Processing) return
        val capturedAtMillis = System.currentTimeMillis()
        val contextHint = ephemeral.value.contextHint
        val ai = aiPreferences.value
        if (!ai.enabled || ai.apiKey.isBlank()) {
            ephemeral.update {
                it.copy(
                    phase = Phase.NoConfig,
                    message = "AI-scan niet ingesteld. Zet AI aan en voeg een Gemini API-sleutel toe in Instellingen.",
                )
            }
            return
        }
        viewModelScope.launch {
            ephemeral.update { it.copy(phase = Phase.Processing, message = null) }
            runCatching { analyzeMealUseCase(path, contextHint, capturedAtMillis) }
                .onSuccess { result ->
                    val resultState = classifyMealScanResultForScanner(result, contextHint)
                    ephemeral.update {
                        when (resultState) {
                            is CameraScannerUiState.Completed -> it.copy(
                                phase = Phase.Completed,
                                suggestedMealType = resultState.suggestedMealType,
                                itemCount = resultState.itemCount,
                                message = null,
                            )
                            is CameraScannerUiState.Empty -> it.copy(
                                phase = Phase.Empty,
                                suggestedMealType = result.suggestedMealType,
                                itemCount = 0,
                                message = resultState.message,
                            )
                            is CameraScannerUiState.LocalFallback -> it.copy(
                                phase = Phase.LocalFallback,
                                suggestedMealType = result.suggestedMealType,
                                itemCount = 0,
                                message = resultState.message,
                            )
                            else -> it
                        }
                    }
                }
                .onFailure {
                    ephemeral.update {
                        it.copy(
                            phase = Phase.Error,
                            message = "Scan mislukt. Probeer opnieuw.",
                        )
                    }
                }
        }
    }

    fun resetToPreview(clearScanResult: Boolean = true) {
        if (clearScanResult) clearLastScanResultUseCase()
        ephemeral.update { it.copy(phase = Phase.Preview, message = null) }
    }

    fun clearScanResult() {
        clearLastScanResultUseCase()
    }
}

private fun kotlinx.coroutines.flow.Flow<AiPreferences>.mapResolvedAiPreferences(
    aiUsageGate: AiUsageGate,
): kotlinx.coroutines.flow.Flow<AiPreferences> = map { legacySettings ->
    aiUsageGate.resolveSettings(legacySettings)
}

internal fun classifyMealScanResultForScanner(
    result: MealAnalysisResult,
    contextHint: String,
): CameraScannerUiState =
    when {
        result.source == MealAnalysisSource.LOCAL_FALLBACK -> CameraScannerUiState.LocalFallback(
            contextHint = contextHint,
            message = result.notes ?: "Lokale fallback gebruikt. Voeg de maaltijd handmatig toe.",
        )
        result.items.isEmpty() -> CameraScannerUiState.Empty(
            contextHint = contextHint,
            message = "Geen producten gevonden. Probeer opnieuw met betere belichting of voeg de maaltijd handmatig toe.",
        )
        else -> CameraScannerUiState.Completed(
            suggestedMealType = result.suggestedMealType,
            itemCount = result.items.size,
        )
    }

@Composable
fun CameraScannerRoute(
    contextHint: String,
    scannerMode: ScannerMode = ScannerMode.AI_MEAL,
    onBack: () -> Unit,
    onBarcodeScanned: (String) -> Unit = {},
    viewModel: CameraScannerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(contextHint) {
        viewModel.setContextHint(contextHint)
    }

    LaunchedEffect(uiState) {
        if ((uiState as? ScreenUiState.Success)?.content?.scannerState is CameraScannerUiState.Completed) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    CameraScannerScreen(
        uiState = uiState.cameraScannerStateOrPreview(),
        scannerMode = scannerMode,
        onAnalyze = viewModel::analyze,
        onDismissError = { viewModel.resetToPreview(clearScanResult = true) },
        onScanAgain = { viewModel.resetToPreview(clearScanResult = true) },
        onReviewItems = onBack,
        onBack = {
            viewModel.clearScanResult()
            onBack()
        },
        onBarcodeScanned = onBarcodeScanned,
    )
}

private fun ScreenUiState<CameraUiContent>.cameraScannerStateOrPreview(): CameraScannerUiState =
    when (this) {
        ScreenUiState.Loading -> CameraScannerUiState.Preview("", isEnabled = false)
        is ScreenUiState.Error -> CameraScannerUiState.Error("", message)
        is ScreenUiState.Success -> content.scannerState
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraScannerScreen(
    uiState: CameraScannerUiState,
    scannerMode: ScannerMode,
    onAnalyze: (String) -> Unit,
    onDismissError: () -> Unit,
    onScanAgain: () -> Unit,
    onReviewItems: () -> Unit,
    onBack: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val hasCameraFeature = remember(context) { isCameraFeatureAvailable(context.packageManager) }
    var hasPermission by rememberSaveable {
        mutableStateOf(
            isCameraPermissionGranted(
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA),
            ),
        )
    }
    var restorableState by rememberSaveable(stateSaver = CameraScannerRestorableState.Saver) {
        mutableStateOf(CameraScannerRestorableState())
    }
    var isCapturing by remember { mutableStateOf(false) }
    val hasDetectedBarcode = remember { AtomicBoolean(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        restorableState = restorableState.copy(permissionDenied = !it)
    }

    val controller = remember(context, scannerMode) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                if (scannerMode == ScannerMode.BARCODE) CameraController.IMAGE_ANALYSIS
                else CameraController.IMAGE_CAPTURE,
            )
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    DisposableEffect(controller, lifecycleOwner, hasPermission, hasCameraFeature, scannerMode) {
        var scanner: com.google.mlkit.vision.barcode.BarcodeScanner? = null
        if (hasPermission && hasCameraFeature) {
            val bound = runCatching { controller.bindToLifecycle(lifecycleOwner) }
                .onFailure {
                    restorableState = restorableState.copy(
                        cameraError = scannerCameraBindFailureMessage(scannerMode),
                    )
                }
                .isSuccess
            if (bound && scannerMode == ScannerMode.BARCODE) {
                val barcodeScanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build(),
                )
                scanner = barcodeScanner
                controller.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    processBarcode(imageProxy, barcodeScanner) { barcode ->
                        if (hasDetectedBarcode.compareAndSet(false, true)) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBarcodeScanned(barcode)
                        }
                    }
                }
            }
        }
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            scanner?.close()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val showSheet = hasPermission && scannerMode == ScannerMode.AI_MEAL && uiState !is CameraScannerUiState.Preview
    val showCameraFallback = shouldShowCameraFallback(
        hasPermission = hasPermission,
        hasCameraFeature = hasCameraFeature,
        cameraError = restorableState.cameraError,
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        if (!hasPermission) {
            PermissionGate(
                permissionDenied = restorableState.permissionDenied,
                onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                },
                onBack = onBack,
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!showCameraFallback) {
                    AndroidView(
                        factory = { previewContext ->
                            PreviewView(previewContext).apply {
                                this.controller = controller
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Beam: always in BARCODE mode, only during Processing in AI mode
                    val showBeam = scannerMode == ScannerMode.BARCODE || uiState is CameraScannerUiState.Processing
                    if (showBeam) FullscreenScanningBeam(modifier = Modifier.fillMaxSize())
                }

                // Top hint card
                val topHint = when {
                    showCameraFallback -> scannerCameraUnavailableMessage(scannerMode)
                    scannerMode == ScannerMode.BARCODE -> "Richt de camera op de barcode van het product."
                    uiState is CameraScannerUiState.Preview -> uiState.contextHint.ifBlank {
                        "Zet het volledige bord of de verpakking duidelijk in beeld. TrainIQ maakt er bewerkbare producten en macro's van."
                    }
                    uiState is CameraScannerUiState.Error -> uiState.contextHint.ifBlank { "" }
                    uiState is CameraScannerUiState.Empty -> uiState.contextHint.ifBlank { "" }
                    uiState is CameraScannerUiState.NoConfig -> uiState.contextHint.ifBlank { "" }
                    uiState is CameraScannerUiState.LocalFallback -> uiState.contextHint.ifBlank { "" }
                    else -> null
                }
                val topTitle = if (scannerMode == ScannerMode.BARCODE) "Barcodescanner" else "Camerascanner"
                val helperMessage = when {
                    showCameraFallback -> restorableState.cameraError
                    scannerMode == ScannerMode.AI_MEAL && uiState is CameraScannerUiState.Preview -> restorableState.cameraError ?: uiState.message
                    else -> null
                }

                if (topHint != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.large),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 8.dp,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(MaterialTheme.spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                            ) {
                                Text(topTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                                Text(topHint, style = MaterialTheme.typography.bodyMedium)
                                helperMessage?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                // Bottom actions
                when {
                    scannerMode == ScannerMode.BARCODE -> {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(MaterialTheme.spacing.large),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            OutlinedButton(onClick = onBack) {
                                Text(if (showCameraFallback) scannerManualFallbackLabel(scannerMode) else "Annuleren")
                            }
                        }
                    }
                    uiState is CameraScannerUiState.Preview -> {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(MaterialTheme.spacing.large),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(onClick = onBack) {
                                Text(if (showCameraFallback) scannerManualFallbackLabel(scannerMode) else "Terug")
                            }
                            Button(
                                onClick = {
                                    if (isCapturing) return@Button
                                    restorableState = restorableState.copy(cameraError = null)
                                    isCapturing = true
                                    takeScannerPhoto(
                                        context = context,
                                        controller = controller,
                                        onPhotoSaved = {
                                            isCapturing = false
                                            onAnalyze(it)
                                        },
                                        onError = {
                                            isCapturing = false
                                            restorableState = restorableState.copy(cameraError = it)
                                        },
                                    )
                                },
                                enabled = uiState.isEnabled && !isCapturing && !showCameraFallback,
                            ) { Text(if (isCapturing) "Foto maken..." else "Foto maken") }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (
                    uiState is CameraScannerUiState.Error ||
                    uiState is CameraScannerUiState.NoConfig ||
                    uiState is CameraScannerUiState.LocalFallback
                ) onDismissError()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            when (uiState) {
                is CameraScannerUiState.Processing -> ProcessingSheetContent()
                is CameraScannerUiState.Completed -> CompletedSheetContent(
                    itemCount = uiState.itemCount,
                    suggestedMealType = uiState.suggestedMealType,
                    onScanAgain = onScanAgain,
                    onReviewItems = onReviewItems,
                )
                is CameraScannerUiState.Empty -> EmptySheetContent(
                    message = uiState.message,
                    onRetry = onScanAgain,
                    onManual = onBack,
                )
                is CameraScannerUiState.NoConfig -> ErrorSheetContent(
                    title = "AI-scan niet ingesteld",
                    message = uiState.message,
                    onRetry = onDismissError,
                    retryLabel = scannerErrorPrimaryActionLabel(ScannerSheetErrorAction.Dismiss),
                    onBack = onBack,
                )
                is CameraScannerUiState.LocalFallback -> ErrorSheetContent(
                    title = "Lokale fallback",
                    message = uiState.message,
                    onRetry = onScanAgain,
                    retryLabel = scannerErrorPrimaryActionLabel(ScannerSheetErrorAction.ScanAgain),
                    onBack = onBack,
                )
                is CameraScannerUiState.Error -> ErrorSheetContent(
                    title = "Scan mislukt",
                    message = uiState.message,
                    onRetry = onDismissError,
                    retryLabel = scannerErrorPrimaryActionLabel(ScannerSheetErrorAction.Dismiss),
                    onBack = onBack,
                )
                else -> {}
            }
        }
    }
}

@Composable
private fun EmptySheetContent(
    message: String,
    onRetry: () -> Unit,
    onManual: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Text(scannerEmptyTitle(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Button(onClick = onManual, modifier = Modifier.fillMaxWidth()) { Text(scannerManualAddLabel()) }
            OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text(scannerRetryLabel()) }
        }
    }
}

internal fun shouldAutoRequestCameraPermissionOnEntry(): Boolean = false

internal fun isCameraFeatureAvailable(packageManager: PackageManager): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)

internal fun shouldShowCameraFallback(
    hasPermission: Boolean,
    hasCameraFeature: Boolean,
    cameraError: String?,
): Boolean = hasPermission && (!hasCameraFeature || cameraError != null)

internal fun scannerCameraUnavailableMessage(scannerMode: ScannerMode): String =
    when (scannerMode) {
        ScannerMode.AI_MEAL -> "Camera niet beschikbaar. Je kunt deze maaltijd handmatig toevoegen."
        ScannerMode.BARCODE -> "Camera niet beschikbaar. Voer de barcode handmatig in bij het product."
    }

internal fun scannerCameraBindFailureMessage(scannerMode: ScannerMode): String =
    when (scannerMode) {
        ScannerMode.AI_MEAL -> "Camera kan nu niet starten. Voeg de maaltijd handmatig toe of probeer later opnieuw."
        ScannerMode.BARCODE -> "Camera kan nu niet starten. Voer de code handmatig in of probeer later opnieuw."
    }

internal fun scannerManualFallbackLabel(scannerMode: ScannerMode): String =
    when (scannerMode) {
        ScannerMode.AI_MEAL -> "Handmatig toevoegen"
        ScannerMode.BARCODE -> "Code handmatig invoeren"
    }

internal enum class ScannerSheetErrorAction { Dismiss, ScanAgain }

internal fun scannerErrorPrimaryActionLabel(action: ScannerSheetErrorAction): String = when (action) {
    ScannerSheetErrorAction.Dismiss -> "Sluiten"
    ScannerSheetErrorAction.ScanAgain -> "Opnieuw scannen"
}

@Composable
private fun PermissionGate(
    permissionDenied: Boolean,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(MaterialTheme.spacing.large),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                Text("Cameratoegang nodig", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    scannerPermissionGateMessage(permissionDenied),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) { Text(scannerPermissionGrantLabel()) }
                    if (permissionDenied) {
                        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text(scannerPermissionSettingsLabel()) }
                    }
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(scannerPermissionBackLabel()) }
                }
            }
        }
    }
}

internal fun scannerPermissionGateMessage(permissionDenied: Boolean): String =
    if (permissionDenied) {
        "TrainIQ heeft cameratoegang nodig om maaltijden of barcodes te scannen. Als Android de vraag niet meer toont, open dan de app-instellingen."
    } else {
        "Geef cameratoegang om de scanner te gebruiken."
    }

internal fun scannerPermissionGrantLabel(): String = "Toegang geven"

internal fun scannerPermissionSettingsLabel(): String = "Instellingen openen"

internal fun scannerPermissionBackLabel(): String = "Terug"

@Composable
private fun ProcessingSheetContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        Text(scannerProcessingTitle(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            scannerProcessingMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        repeat(3) { ShimmerCardPlaceholder(lineCount = 2, modifier = Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun CompletedSheetContent(
    itemCount: Int,
    suggestedMealType: MealType?,
    onScanAgain: () -> Unit,
    onReviewItems: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Text(scannerCompletedTitle(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            buildString {
                append(scannerCompletedMessage(itemCount, suggestedMealType))
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Button(onClick = onReviewItems, modifier = Modifier.fillMaxWidth()) { Text(scannerReviewProductsLabel()) }
            OutlinedButton(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) { Text(scannerScanAgainLabel()) }
        }
    }
}

internal fun isCameraPermissionGranted(permissionResult: Int): Boolean =
    permissionResult == PackageManager.PERMISSION_GRANTED

internal fun scannerCompletedMessage(itemCount: Int, suggestedMealType: MealType?): String =
    buildString {
        append("$itemCount ${if (itemCount == 1) "product gevonden" else "producten gevonden"}.")
        suggestedMealType?.let { append(" Suggestie: ${it.dutchLabel}.") }
    }

internal fun scannerProcessingTitle(): String = "Scannen..."

internal fun scannerProcessingMessage(): String =
    "Gemini Flash herkent producten, schat porties en berekent macro's."

internal fun scannerCompletedTitle(): String = "Scan voltooid"

internal fun scannerReviewProductsLabel(): String = "Producten controleren"

internal fun scannerScanAgainLabel(): String = "Opnieuw scannen"

internal fun scannerEmptyTitle(): String = "Geen producten gevonden"

internal fun scannerManualAddLabel(): String = "Handmatig toevoegen"

internal fun scannerRetryLabel(): String = "Opnieuw proberen"

private val MealType.dutchLabel: String
    get() = when (this) {
        MealType.BREAKFAST -> "Ochtend"
        MealType.LUNCH -> "Middag"
        MealType.DINNER -> "Avond"
        MealType.SNACK -> "Snacks"
    }

@Composable
private fun ErrorSheetContent(
    title: String,
    message: String,
    onRetry: () -> Unit,
    retryLabel: String,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text(retryLabel) }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(scannerPermissionBackLabel()) }
        }
    }
}

@Composable
private fun FullscreenScanningBeam(modifier: Modifier = Modifier) {
    val beamColor = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "fullscreen-scanner")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fullscreen-scanner-progress",
    )
    Box(
        modifier = modifier.drawBehind {
            val beamHeight = 8.dp.toPx()
            val top = (size.height - beamHeight) * progress
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, beamColor.copy(alpha = 0.92f), Color.Transparent),
                ),
                topLeft = androidx.compose.ui.geometry.Offset(0f, top),
                size = androidx.compose.ui.geometry.Size(size.width, beamHeight),
            )
        },
    )
}

@androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class])
private fun processBarcode(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onDetected: (String) -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { codes -> codes.firstOrNull()?.rawValue?.let(onDetected) }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}

private fun takeScannerPhoto(
    context: Context,
    controller: LifecycleCameraController,
    onPhotoSaved: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val file = File(context.cacheDir, "meal-fullscreen-${System.currentTimeMillis()}.jpg")
    val output = ImageCapture.OutputFileOptions.Builder(file).build()
    controller.takePicture(
        output,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onPhotoSaved(file.absolutePath)
            }

            override fun onError(exception: ImageCaptureException) {
                android.util.Log.e("TrainIQ", "Camera capture failed", exception)
                onError("Kan geen foto maken op dit apparaat.")
            }
        },
    )
}
