package com.trainiq.features.nutrition

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.theme.spacing
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.domain.model.MealType
import com.trainiq.domain.usecase.AnalyzeMealUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScannerMode { BARCODE, AI_MEAL }

sealed interface CameraScannerUiState {
    data class Preview(
        val contextHint: String,
        val isEnabled: Boolean,
        val message: String? = null,
    ) : CameraScannerUiState

    data object Processing : CameraScannerUiState
    data class Completed(val suggestedMealType: MealType?, val itemCount: Int = 0) : CameraScannerUiState
    data class Error(val contextHint: String, val message: String) : CameraScannerUiState
}

@HiltViewModel
class CameraScannerViewModel @Inject constructor(
    preferencesRepository: UserPreferencesRepository,
    private val analyzeMealUseCase: AnalyzeMealUseCase,
) : ViewModel() {
    private data class ScannerEphemeralState(
        val contextHint: String = "",
        val phase: Phase = Phase.Preview,
        val message: String? = null,
        val suggestedMealType: MealType? = null,
        val itemCount: Int = 0,
    )

    private enum class Phase { Preview, Processing, Completed, Error }

    private val aiPreferences: StateFlow<AiPreferences> = preferencesRepository.aiPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiPreferences(false, ""))
    private val ephemeral = MutableStateFlow(ScannerEphemeralState())

    val uiState: StateFlow<CameraScannerUiState> = combine(aiPreferences, ephemeral) { ai, temp ->
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
            Phase.Error -> CameraScannerUiState.Error(
                contextHint = temp.contextHint,
                message = temp.message ?: "Deze foto kan nu niet geanalyseerd worden.",
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CameraScannerUiState.Preview("", false))

    fun setContextHint(hint: String) {
        ephemeral.update { it.copy(contextHint = hint.trim(), phase = Phase.Preview, message = null) }
    }

    fun analyze(path: String) {
        val capturedAtMillis = System.currentTimeMillis()
        val contextHint = ephemeral.value.contextHint
        val ai = aiPreferences.value
        if (!ai.enabled || ai.apiKey.isBlank()) {
            ephemeral.update {
                it.copy(
                    phase = Phase.Error,
                    message = if (!ai.enabled) "Zet AI aan in Instellingen voordat je scant." else "Voeg eerst een Gemini API-sleutel toe.",
                )
            }
            return
        }
        viewModelScope.launch {
            ephemeral.update { it.copy(phase = Phase.Processing, message = null) }
            runCatching { analyzeMealUseCase(path, contextHint, capturedAtMillis) }
                .onSuccess { result ->
                    ephemeral.update {
                        it.copy(
                            phase = Phase.Completed,
                            suggestedMealType = result.suggestedMealType,
                            itemCount = result.items.size,
                            message = null,
                        )
                    }
                }
                .onFailure {
                    ephemeral.update {
                        it.copy(
                            phase = Phase.Error,
                            message = "Maaltijdanalyse mislukt. Maak opnieuw een foto of voer de maaltijd handmatig in.",
                        )
                    }
                }
        }
    }

    fun dismissError() {
        ephemeral.update { it.copy(phase = Phase.Preview, message = null) }
    }
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
        if (uiState is CameraScannerUiState.Completed) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    CameraScannerScreen(
        uiState = uiState,
        scannerMode = scannerMode,
        onAnalyze = viewModel::analyze,
        onDismissError = viewModel::dismissError,
        onScanAgain = viewModel::dismissError,
        onReviewItems = onBack,
        onBack = onBack,
        onBarcodeScanned = onBarcodeScanned,
    )
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
    var hasPermission by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val hasDetectedBarcode = remember { AtomicBoolean(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    val controller = remember(context, scannerMode) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                if (scannerMode == ScannerMode.BARCODE) CameraController.IMAGE_ANALYSIS
                else CameraController.IMAGE_CAPTURE,
            )
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    DisposableEffect(controller, lifecycleOwner, hasPermission, scannerMode) {
        var scanner: com.google.mlkit.vision.barcode.BarcodeScanner? = null
        if (hasPermission) {
            controller.bindToLifecycle(lifecycleOwner)
            if (scannerMode == ScannerMode.BARCODE) {
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        if (!hasPermission) {
            PermissionGate(onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) }, onBack = onBack)
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
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

                // Top hint card
                val topHint = when {
                    scannerMode == ScannerMode.BARCODE -> "Richt de camera op de barcode van het product."
                    uiState is CameraScannerUiState.Preview -> uiState.contextHint.ifBlank {
                        "Zet het volledige bord of de verpakking duidelijk in beeld. TrainIQ maakt er bewerkbare producten en macro's van."
                    }
                    uiState is CameraScannerUiState.Error -> uiState.contextHint.ifBlank { "" }
                    else -> null
                }
                val topTitle = if (scannerMode == ScannerMode.BARCODE) "Barcodescanner" else "Camerascanner"
                val helperMessage = when {
                    scannerMode == ScannerMode.AI_MEAL && uiState is CameraScannerUiState.Preview -> cameraError ?: uiState.message
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
                                .padding(MaterialTheme.spacing.large),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            OutlinedButton(onClick = onBack) { Text("Annuleren") }
                        }
                    }
                    uiState is CameraScannerUiState.Preview -> {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(MaterialTheme.spacing.large),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(onClick = onBack) { Text("Terug") }
                            Button(
                                onClick = {
                                    cameraError = null
                                    takeScannerPhoto(context, controller, onAnalyze) { cameraError = it }
                                },
                                enabled = uiState.isEnabled,
                            ) { Text("Foto maken") }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (uiState is CameraScannerUiState.Error) onDismissError()
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
                is CameraScannerUiState.Error -> ErrorSheetContent(
                    message = uiState.message,
                    onRetry = onDismissError,
                    onBack = onBack,
                )
                else -> {}
            }
        }
    }
}

@Composable
private fun PermissionGate(onGrant: () -> Unit, onBack: () -> Unit) {
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
                Text("Geef cameratoegang om de scanner te gebruiken.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onGrant) { Text("Toegang geven") }
                OutlinedButton(onClick = onBack) { Text("Terug") }
            }
        }
    }
}

@Composable
private fun ProcessingSheetContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        Text("Scannen...", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Gemini Flash herkent producten, schat porties en berekent macro's.",
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
        Text("Scan voltooid", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            buildString {
                append("$itemCount ${if (itemCount == 1) "item gevonden" else "items gevonden"}.")
                suggestedMealType?.let { append(" Suggestie: ${it.dutchLabel}.") }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            OutlinedButton(onClick = onScanAgain) { Text("Opnieuw scannen") }
            Button(onClick = onReviewItems, modifier = Modifier.weight(1f)) { Text("Items controleren") }
        }
    }
}

private val MealType.dutchLabel: String
    get() = when (this) {
        MealType.BREAKFAST -> "Ochtend"
        MealType.LUNCH -> "Middag"
        MealType.DINNER -> "Avond"
        MealType.SNACK -> "Snacks"
    }

@Composable
private fun ErrorSheetContent(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Text("Scan niet beschikbaar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            OutlinedButton(onClick = onBack) { Text("Terug") }
            Button(onClick = onRetry, modifier = Modifier.weight(1f)) { Text("Opnieuw proberen") }
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
