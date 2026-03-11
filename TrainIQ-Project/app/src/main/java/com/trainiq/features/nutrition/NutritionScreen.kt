package com.trainiq.features.nutrition

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.NutritionOverview
import com.trainiq.domain.usecase.AnalyzeMealUseCase
import com.trainiq.domain.usecase.ObserveNutritionUseCase
import com.trainiq.domain.usecase.SaveScannedMealUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NutritionViewModel @Inject constructor(
    observeNutritionUseCase: ObserveNutritionUseCase,
    private val analyzeMealUseCase: AnalyzeMealUseCase,
    private val saveScannedMealUseCase: SaveScannedMealUseCase,
) : ViewModel() {
    val overview: StateFlow<NutritionOverview?> = observeNutritionUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _scanState = MutableStateFlow<List<MealScanItem>>(emptyList())
    val scanState: StateFlow<List<MealScanItem>> = _scanState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun analyze(path: String) {
        viewModelScope.launch {
            runCatching { analyzeMealUseCase(path).scannedItems }
                .onSuccess { items ->
                    _scanState.value = items
                    _message.value = if (items.isEmpty()) "Meal analysis is currently unavailable." else null
                }
                .onFailure {
                    _scanState.value = emptyList()
                    _message.value = "Meal analysis is currently unavailable."
                }
        }
    }

    fun saveScannedMeal() {
        viewModelScope.launch {
            runCatching { saveScannedMealUseCase(scanState.value) }
                .onSuccess {
                    _scanState.value = emptyList()
                    _message.value = "Meal saved."
                }
                .onFailure {
                    _message.value = "Unable to save meal right now."
                }
        }
    }

    fun setMessage(message: String?) {
        _message.value = message
    }
}

@Composable
fun NutritionRoute(viewModel: NutritionViewModel = hiltViewModel()) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val scanItems by viewModel.scanState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    NutritionScreen(
        overview = overview,
        scannedItems = scanItems,
        message = message,
        onAnalyze = viewModel::analyze,
        onSaveScan = viewModel::saveScannedMeal,
        onDismissMessage = { viewModel.setMessage(null) },
    )
}

@Composable
fun NutritionScreen(
    overview: NutritionOverview?,
    scannedItems: List<MealScanItem>,
    message: String?,
    onAnalyze: (String) -> Unit,
    onSaveScan: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scannerExpanded by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraController by remember(scannerExpanded, hasPermission) {
        mutableStateOf<LifecycleCameraController?>(null)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) {
            cameraError = "Camera access was denied. You can still log meals manually later."
        }
    }

    LaunchedEffect(scannerExpanded, hasPermission, lifecycleOwner) {
        if (scannerExpanded && hasPermission && cameraController == null) {
            cameraController = runCatching {
                LifecycleCameraController(context).apply {
                    setEnabledUseCases(CameraController.IMAGE_CAPTURE)
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    bindToLifecycle(lifecycleOwner)
                }
            }.getOrElse {
                cameraError = "Meal scanner is unavailable on this device."
                null
            }
        }
    }

    DisposableEffect(scannerExpanded) {
        onDispose {
            if (!scannerExpanded) {
                cameraController = null
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Nutrition",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (overview == null || overview.meals.isEmpty()) {
                        Text("No meals logged yet.")
                        Text("Nutrition tracking will appear here after you add your first meal.")
                    } else {
                        Text("Calories ${overview.todaysCalories} - Protein ${overview.todaysProtein}g")
                        Text("Carbs ${overview.todaysCarbs}g - Fat ${overview.todaysFat}g")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Meal Scanner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Optional feature. If camera or AI analysis is unavailable, the rest of Nutrition still works.")
                    message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    cameraError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (!scannerExpanded) {
                        OutlinedButton(
                            onClick = {
                                scannerExpanded = true
                                if (!hasPermission) {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                        ) {
                            Text("Open scanner")
                        }
                    } else {
                        if (!hasPermission) {
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                Text("Grant camera access")
                            }
                        } else if (cameraController != null) {
                            AndroidView(
                                factory = { PreviewView(it).apply { controller = cameraController } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                            )
                            Button(
                                onClick = {
                                    takePhoto(
                                        context = context,
                                        controller = cameraController,
                                        onPhotoSaved = onAnalyze,
                                        onError = { error -> cameraError = error },
                                    )
                                },
                            ) {
                                Text("Scan meal")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                scannerExpanded = false
                                cameraError = null
                                onDismissMessage()
                            },
                        ) {
                            Text("Close scanner")
                        }
                    }
                    if (scannedItems.isNotEmpty()) {
                        scannedItems.forEach { item ->
                            Text("${item.name}: ${item.calories} kcal - P${item.protein} C${item.carbs} F${item.fat}")
                        }
                        Button(onClick = onSaveScan) { Text("Confirm and save") }
                    }
                }
            }
        }
        if (overview?.meals.isNullOrEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No meal history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Saved meals will appear here once you log or scan food.")
                    }
                }
            }
        } else {
            items(overview?.meals ?: emptyList(), key = { it.id }) { meal ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${meal.calories} kcal", fontWeight = FontWeight.SemiBold)
                        Text("Protein ${meal.protein}g - Carbs ${meal.carbs}g - Fat ${meal.fat}g")
                    }
                }
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    controller: LifecycleCameraController?,
    onPhotoSaved: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val activeController = controller ?: run {
        onError("Meal scanner is not ready.")
        return
    }
    val file = File(context.cacheDir, "meal-${System.currentTimeMillis()}.jpg")
    val output = ImageCapture.OutputFileOptions.Builder(file).build()
    activeController.takePicture(
        output,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onPhotoSaved(file.absolutePath)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("TrainIQ", "Camera capture failed", exception)
                onError("Unable to capture a photo on this device.")
            }
        },
    )
}
