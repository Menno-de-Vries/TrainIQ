package com.trainiq.features.progress

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.ui.MessageCard
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.core.ui.AppCard
import com.trainiq.core.ui.AppChip
import com.trainiq.core.ui.CompactSectionTabItem
import com.trainiq.core.ui.CompactSectionTabs
import com.trainiq.core.ui.EmptyStateCard
import com.trainiq.core.ui.PrimaryActionButton
import com.trainiq.core.ui.TrainIqFormField
import com.trainiq.core.ui.TrainIqFormFieldContext
import com.trainiq.core.ui.UiMessage
import com.trainiq.core.ui.clearFocusOnScrollOrDrag
import com.trainiq.core.ui.reloadableObservation
import com.trainiq.core.theme.spacing
import com.trainiq.core.theme.trainIqColors
import com.trainiq.core.util.ChartComposable
import com.trainiq.core.util.MetricCard
import com.trainiq.core.util.toReadableDate
import com.trainiq.domain.model.BodyMeasurement
import com.trainiq.domain.model.BodyMeasurementPhotoSource
import com.trainiq.domain.model.ProgressOverview
import com.trainiq.domain.usecase.AddMeasurementUseCase
import com.trainiq.domain.usecase.AnalyzeBodyMeasurementPhotoUseCase
import com.trainiq.domain.usecase.DeleteMeasurementUseCase
import com.trainiq.domain.usecase.ObserveProgressUseCase
import com.trainiq.features.nutrition.copyScannerImageFromUri
import com.trainiq.features.nutrition.scalePhotoImportLabel
import com.trainiq.navigation.TrainIqWindowWidthClass
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.util.Locale

enum class ProgressMeasurementField {
    Weight,
    BodyFat,
    MuscleMass,
}

data class ValidatedProgressMeasurement(
    val weight: Double,
    val bodyFat: Double,
    val muscleMass: Double,
)

data class ProgressMeasurementValidationError(
    val field: ProgressMeasurementField,
    val message: String,
)

sealed interface ProgressMeasurementValidationResult {
    data class Valid(val measurement: ValidatedProgressMeasurement) : ProgressMeasurementValidationResult
    data class Invalid(val error: ProgressMeasurementValidationError) : ProgressMeasurementValidationResult
}

sealed interface ProgressUiState {
    data object Loading : ProgressUiState
    data class Success(
        val overview: ProgressOverview,
        val message: UiMessage? = null,
        val isSaving: Boolean = false,
        val savedMeasurement: ValidatedProgressMeasurement? = null,
    ) : ProgressUiState
    data class Error(val message: String) : ProgressUiState
}

private data class ProgressMeasurementFieldSpec(
    val field: ProgressMeasurementField,
    val minimum: Double,
    val maximum: Double,
    val message: String,
)

private val weightSpec = ProgressMeasurementFieldSpec(
    field = ProgressMeasurementField.Weight,
    minimum = 30.0,
    maximum = 300.0,
    message = "Gewicht moet tussen 30 en 300 kg zijn.",
)

private val bodyFatSpec = ProgressMeasurementFieldSpec(
    field = ProgressMeasurementField.BodyFat,
    minimum = 0.0,
    maximum = 100.0,
    message = "Vetpercentage moet tussen 0 en 100% zijn.",
)

private val muscleMassSpec = ProgressMeasurementFieldSpec(
    field = ProgressMeasurementField.MuscleMass,
    minimum = 1.0,
    maximum = 200.0,
    message = "Spiermassa moet tussen 1 en 200 kg zijn.",
)

fun validateProgressMeasurementInput(
    weight: String,
    bodyFat: String,
    muscleMass: String,
): ProgressMeasurementValidationResult {
    val parsedWeight = parseProgressMeasurementField(weight, weightSpec)
        ?: return ProgressMeasurementValidationResult.Invalid(progressMeasurementError(weightSpec))
    val parsedBodyFat = parseProgressMeasurementField(bodyFat, bodyFatSpec)
        ?: return ProgressMeasurementValidationResult.Invalid(progressMeasurementError(bodyFatSpec))
    val parsedMuscleMass = parseProgressMeasurementField(muscleMass, muscleMassSpec)
        ?: return ProgressMeasurementValidationResult.Invalid(progressMeasurementError(muscleMassSpec))

    return ProgressMeasurementValidationResult.Valid(
        ValidatedProgressMeasurement(
            weight = parsedWeight,
            bodyFat = parsedBodyFat,
            muscleMass = parsedMuscleMass,
        ),
    )
}

private fun validateProgressMeasurementField(
    value: String,
    spec: ProgressMeasurementFieldSpec,
): ProgressMeasurementValidationError? =
    if (parseProgressMeasurementField(value, spec) == null) progressMeasurementError(spec) else null

private fun parseProgressMeasurementField(value: String, spec: ProgressMeasurementFieldSpec): Double? {
    val parsed = value.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return parsed.takeIf { it.isFinite() && it in spec.minimum..spec.maximum }
}

private fun progressMeasurementError(spec: ProgressMeasurementFieldSpec) = ProgressMeasurementValidationError(
    field = spec.field,
    message = spec.message,
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    observeProgressUseCase: ObserveProgressUseCase,
    private val analyzeBodyMeasurementPhotoUseCase: AnalyzeBodyMeasurementPhotoUseCase,
    private val addMeasurementUseCase: AddMeasurementUseCase,
    private val deleteMeasurementUseCase: DeleteMeasurementUseCase,
) : ViewModel() {
    private val reloads = MutableStateFlow(0)
    private val overview: StateFlow<Result<ProgressOverview>?> =
        reloadableObservation(reloads) { observeProgressUseCase() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<UiMessage?>(null)
    private data class SaveState(val isSaving: Boolean = false, val saved: ValidatedProgressMeasurement? = null)
    private val saveState = MutableStateFlow(SaveState())
    val uiState: StateFlow<ProgressUiState> = combine(overview, _message, saveState) { current, message, save ->
        when (val state = progressUiState(current, message)) {
            is ProgressUiState.Success -> state.copy(isSaving = save.isSaving, savedMeasurement = save.saved)
            else -> state
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState.Loading)

    fun addMeasurement(weight: String, bodyFat: String, muscleMass: String) {
        if (saveState.value.isSaving) return
        when (val validation = validateProgressMeasurementInput(weight, bodyFat, muscleMass)) {
            is ProgressMeasurementValidationResult.Invalid -> {
                emitMessage(validation.error.message)
                return
            }
            is ProgressMeasurementValidationResult.Valid -> {
                saveState.value = SaveState(isSaving = true)
                viewModelScope.launch {
                    try {
                        val measurement = validation.measurement
                        addMeasurementUseCase(measurement.weight, measurement.bodyFat, measurement.muscleMass)
                        saveState.value = SaveState(saved = measurement)
                        emitMessage("Meting opgeslagen.")
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        emitMessage("Meting opslaan mislukt. Probeer opnieuw.")
                    } finally {
                        saveState.update { it.copy(isSaving = false) }
                    }
                }
            }
        }
    }

    fun deleteMeasurement(measurementId: Long) {
        if (overview.value?.getOrNull()?.measurements?.none { it.id == measurementId } != false) {
            emitMessage("Meting kon niet worden gevonden.")
            return
        }
        viewModelScope.launch {
            runCatching { deleteMeasurementUseCase(measurementId) }
                .onSuccess { emitMessage("Meting verwijderd.") }
                .onFailure { emitMessage("Meting verwijderen mislukt. Probeer opnieuw.") }
        }
    }

    fun analyzeScalePhoto(path: String, context: String, onResult: (com.trainiq.domain.model.BodyMeasurementPhotoResult) -> Unit) {
        viewModelScope.launch {
            runCatching { analyzeBodyMeasurementPhotoUseCase(path, context) }
                .onSuccess { result ->
                    if (result.source == BodyMeasurementPhotoSource.LOCAL_FALLBACK || result.weight <= 0.0) {
                        emitMessage(result.notes ?: "Geen betrouwbare meting gevonden. Vul de waarden handmatig in.")
                    } else {
                        onResult(result)
                        emitMessage(result.notes ?: "Weegfoto geimporteerd. Controleer de waarden voor opslaan.")
                    }
                }
                .onFailure { emitMessage("Weegfoto analyseren mislukt. Probeer opnieuw of vul handmatig in.") }
        }
    }

    private fun emitMessage(text: String) {
        _message.value = UiMessage(text)
    }

    fun retry() {
        reloads.update { it + 1 }
    }

    fun clearMessage(id: Long? = null) {
        if (id == null || _message.value?.id == id) {
            _message.value = null
        }
    }
}

@Composable
fun ProgressRoute(
    windowWidthClass: TrainIqWindowWidthClass = TrainIqWindowWidthClass.Compact,
    pendingScaleWeight: String? = null,
    pendingScaleBodyFat: String? = null,
    pendingScaleMuscleMass: String? = null,
    pendingScaleNotes: String? = null,
    onScaleResultConsumed: () -> Unit = {},
    onOpenScaleScanner: () -> Unit = {},
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(
        uiState = uiState,
        onAddMeasurement = viewModel::addMeasurement,
        onDeleteMeasurement = viewModel::deleteMeasurement,
        onRetry = viewModel::retry,
        onDismissMessage = viewModel::clearMessage,
        pendingScaleWeight = pendingScaleWeight,
        pendingScaleBodyFat = pendingScaleBodyFat,
        pendingScaleMuscleMass = pendingScaleMuscleMass,
        pendingScaleNotes = pendingScaleNotes,
        onScaleResultConsumed = onScaleResultConsumed,
        onOpenScaleScanner = onOpenScaleScanner,
        onAnalyzeImportedScalePhoto = { path, onResult ->
            viewModel.analyzeScalePhoto(
                path = path,
                context = "Lees gewicht, vetpercentage en spiermassa uit van de geimporteerde smart-weegschaalfoto.",
                onResult = onResult,
            )
        },
    )
}

@Composable
fun ProgressScreen(
    uiState: ProgressUiState,
    onAddMeasurement: (String, String, String) -> Unit,
    onDeleteMeasurement: (Long) -> Unit,
    onRetry: () -> Unit,
    onDismissMessage: (Long) -> Unit,
    pendingScaleWeight: String? = null,
    pendingScaleBodyFat: String? = null,
    pendingScaleMuscleMass: String? = null,
    pendingScaleNotes: String? = null,
    onScaleResultConsumed: () -> Unit = {},
    onOpenScaleScanner: () -> Unit = {},
    onAnalyzeImportedScalePhoto: (String, (com.trainiq.domain.model.BodyMeasurementPhotoResult) -> Unit) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    var weight by rememberSaveable { mutableStateOf("") }
    var bodyFat by rememberSaveable { mutableStateOf("") }
    var muscleMass by rememberSaveable { mutableStateOf("") }
    var weightTouched by rememberSaveable { mutableStateOf(false) }
    var bodyFatTouched by rememberSaveable { mutableStateOf(false) }
    var muscleMassTouched by rememberSaveable { mutableStateOf(false) }
    var selectedProgressTab by rememberSaveable { mutableStateOf(ProgressSectionTab.Body.key) }
    var scalePhotoNote by rememberSaveable { mutableStateOf<String?>(null) }

    val weightError = validateProgressMeasurementField(weight, weightSpec).takeIf { weightTouched }
    val bodyFatError = validateProgressMeasurementField(bodyFat, bodyFatSpec).takeIf { bodyFatTouched }
    val muscleMassError = validateProgressMeasurementField(muscleMass, muscleMassSpec).takeIf { muscleMassTouched }
    val measurementValidation = validateProgressMeasurementInput(weight, bodyFat, muscleMass)
    val successState = uiState as? ProgressUiState.Success
    val canSaveMeasurement = measurementValidation is ProgressMeasurementValidationResult.Valid && successState?.isSaving != true
    val overview = successState?.overview
    val message = successState?.message
    val snackbarHostState = remember { SnackbarHostState() }
    val photoImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val path = copyScannerImageFromUri(context, uri) ?: return@rememberLauncherForActivityResult
        onAnalyzeImportedScalePhoto(path) { result ->
            weight = result.weight.takeIf { it > 0.0 }?.let(::oneDecimal).orEmpty()
            bodyFat = result.bodyFat.takeIf { it > 0.0 }?.let(::oneDecimal).orEmpty()
            muscleMass = result.muscleMass.takeIf { it > 0.0 }?.let(::oneDecimal).orEmpty()
            weightTouched = weight.isNotBlank()
            bodyFatTouched = bodyFat.isNotBlank()
            muscleMassTouched = muscleMass.isNotBlank()
            scalePhotoNote = result.notes
        }
    }

    LaunchedEffect(message?.id) {
        val currentMessage = message
        if (currentMessage?.text == "Meting opgeslagen." &&
            (measurementValidation as? ProgressMeasurementValidationResult.Valid)?.measurement == successState?.savedMeasurement
        ) {
            weight = ""
            bodyFat = ""
            muscleMass = ""
            weightTouched = false
            bodyFatTouched = false
            muscleMassTouched = false
        }
        if (currentMessage != null) {
            snackbarHostState.showSnackbar(currentMessage.text)
            onDismissMessage(currentMessage.id)
        }
    }

    LaunchedEffect(pendingScaleWeight, pendingScaleBodyFat, pendingScaleMuscleMass) {
        if (pendingScaleWeight != null || pendingScaleBodyFat != null || pendingScaleMuscleMass != null) {
            pendingScaleWeight?.let {
                weight = it
                weightTouched = true
            }
            pendingScaleBodyFat?.let {
                bodyFat = it
                bodyFatTouched = true
            }
            pendingScaleMuscleMass?.let {
                muscleMass = it
                muscleMassTouched = true
            }
            scalePhotoNote = pendingScaleNotes
            onScaleResultConsumed()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { _ ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnScrollOrDrag()
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                top = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                bottom = 132.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        ) {
            item { ScreenHeader(title = "Trend", subtitle = "Metingen, grafieken en krachttrends") }
            when (uiState) {
            ProgressUiState.Loading -> {
                item { ShimmerCardPlaceholder(lineCount = 4) }
                item { ShimmerCardPlaceholder(lineCount = 3) }
                item { ShimmerCardPlaceholder(lineCount = 5) }
                return@LazyColumn
            }
            is ProgressUiState.Error -> {
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Voortgang niet beschikbaar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(uiState.message, color = MaterialTheme.trainIqColors.mutedText)
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text("Opnieuw proberen") }
                    }
                }
                return@LazyColumn
            }
            is ProgressUiState.Success -> Unit
        }
        if (overview == null) {
            item { ShimmerCardPlaceholder(lineCount = 4) }
            item { ShimmerCardPlaceholder(lineCount = 3) }
            item { ShimmerCardPlaceholder(lineCount = 5) }
            return@LazyColumn
        }
        item {
            ProgressSectionTabSwitcher(
                selectedTab = selectedProgressTab,
                onSelectTab = { selectedProgressTab = it.key },
            )
        }
        if (selectedProgressTab == ProgressSectionTab.Body.key) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    Text("Lichaamssamenstelling", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.amber, fontWeight = FontWeight.SemiBold)
                    Text(
                        latestBodyWeightText(overview.measurements),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.trainIqColors.amber,
                    )
                    Text("Vetpercentage, spiermassa en gewicht naast elkaar.", color = MaterialTheme.trainIqColors.mutedText)
                    scalePhotoNote?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = MaterialTheme.trainIqColors.mutedText, style = MaterialTheme.typography.bodySmall)
                    }
                    ProgressMetricRow(
                        bodyFat = latestBodyFatText(overview.measurements),
                        muscleMass = latestMuscleMassText(overview.measurements),
                    )
                    OutlinedButton(onClick = onOpenScaleScanner, modifier = Modifier.fillMaxWidth()) {
                        Text("Smart-weegschaal foto maken")
                    }
                    OutlinedButton(
                        onClick = { photoImportLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(scalePhotoImportLabel())
                    }
                    MeasurementTextField(
                        value = weight,
                        onValueChange = {
                            weight = it
                            weightTouched = true
                        },
                        label = "Gewicht (kg)",
                        error = weightError,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MeasurementTextField(
                        value = bodyFat,
                        onValueChange = {
                            bodyFat = it
                            bodyFatTouched = true
                        },
                        label = "Vetpercentage (%)",
                        error = bodyFatError,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MeasurementTextField(
                        value = muscleMass,
                        onValueChange = {
                            muscleMass = it
                            muscleMassTouched = true
                        },
                        label = "Spiermassa (kg)",
                        error = muscleMassError,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PrimaryActionButton(onClick = {
                        if (measurementValidation is ProgressMeasurementValidationResult.Valid) {
                            onAddMeasurement(weight, bodyFat, muscleMass)
                        }
                    }, enabled = canSaveMeasurement, modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) {
                        Text(if (successState?.isSaving == true) "Meting opslaan..." else "Meting opslaan")
                    }
                }
            }
            }
        }
        if (
            overview.weightTrend.isEmpty() &&
            overview.bodyFatTrend.isEmpty() &&
            overview.muscleMassTrend.isEmpty() &&
            overview.strengthTrend.isEmpty() &&
            overview.volumeTrend.isEmpty()
        ) {
            item {
                EmptyStateCard(
                    title = "Nog geen voortgangsdata",
                    body = "Voeg lichaamsmetingen toe en rond trainingen af om voortgangsanalyse te zien.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            val hasTrainingProgress = overview.strengthTrend.isNotEmpty() || overview.volumeTrend.isNotEmpty()
            if (selectedProgressTab == ProgressSectionTab.Strength.key && !hasTrainingProgress) {
                item {
                    EmptyStateCard(
                        title = "Nog geen trainingsprogressie",
                        body = "Rond een workout af om geschatte 1RM, weekbelasting en trainingsvolume te zien.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (selectedProgressTab == ProgressSectionTab.Strength.key && hasTrainingProgress) {
                item {
                    MetricCard("Geschatte 1RM", estimatedOneRepMaxText(overview.estimatedOneRepMax), "Beste geschatte maximale kracht uit je gelogde sets", Modifier.fillMaxWidth())
                }
                item {
                    MetricCard(
                        "Weekbelasting",
                        weeklyLoadRatioText(overview.weeklyLoadRatio),
                        weeklyLoadRatioSupportingText(overview.weeklyLoadRatio),
                        Modifier.fillMaxWidth(),
                    )
                }
                item { ChartComposable("Krachtprogressie", overview.strengthTrend, Modifier.fillMaxWidth()) }
                item { ChartComposable("Trainingsvolume", overview.volumeTrend, Modifier.fillMaxWidth()) }
            }
            if (selectedProgressTab == ProgressSectionTab.History.key) item {
                AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) {
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                        Text("Meetgeschiedenis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        val measurements = sortedMeasurementsForHistory(overview.measurements)
                        measurements.forEachIndexed { index, measurement ->
                            val previous = measurements.getOrNull(index + 1)
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Column {
                                    Text(
                                        "${measurement.date.toReadableDate()}: ${measurement.weight} kg, ${measurement.bodyFat}% vet, ${measurement.muscleMass} kg spier",
                                        color = MaterialTheme.trainIqColors.mutedText,
                                    )
                                    previous?.let {
                                        Text(
                                            measurementDeltaText(measurement, it),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                TextButton(onClick = { onDeleteMeasurement(measurement.id) }, modifier = Modifier.fillMaxWidth()) { Text(deleteMeasurementActionLabel()) }
                            }
                        }
                    }
                }
            }
            if (selectedProgressTab == ProgressSectionTab.Body.key) {
                item { ChartComposable("Lichaamsgewicht", overview.weightTrend, Modifier.fillMaxWidth()) }
                item { ChartComposable("Vetpercentage", overview.bodyFatTrend, Modifier.fillMaxWidth()) }
                item { ChartComposable("Spiermassa", overview.muscleMassTrend, Modifier.fillMaxWidth()) }
            }
            }
        }
    }
}

internal fun progressUiState(observation: Result<ProgressOverview>?, message: UiMessage?): ProgressUiState {
    val currentObservation = observation ?: return ProgressUiState.Loading
    return when {
        currentObservation.isFailure -> ProgressUiState.Error("Voortgang kon niet worden geladen.")
        else -> ProgressUiState.Success(overview = currentObservation.getOrThrow(), message = message)
    }
}

internal fun weeklyLoadRatioText(ratio: Double?): String =
    ratio?.takeIf { it.isFinite() && it >= 0.0 }
        ?.let { String.format(Locale.US, "%.2f×", it) }
        ?: "Nog geen vergelijking"

internal fun weeklyLoadRatioSupportingText(ratio: Double?): String =
    if (ratio == null || !ratio.isFinite() || ratio < 0.0) {
        "Minstens twee trainingsweken nodig"
    } else {
        "Laatste trainingsweek versus gemiddeld volume van maximaal drie voorgaande trainingsweken"
    }

private enum class ProgressSectionTab(val key: String, val label: String) {
    Body("body", "Lichaam"),
    Strength("strength", "Kracht"),
    History("history", "Historie"),
}

@Composable
private fun ProgressSectionTabSwitcher(
    selectedTab: String,
    onSelectTab: (ProgressSectionTab) -> Unit,
) {
    CompactSectionTabs(
        selectedKey = selectedTab,
        tabs = ProgressSectionTab.entries.map { CompactSectionTabItem(it.key, it.label) },
        onSelectTab = { selected -> ProgressSectionTab.entries.firstOrNull { it.key == selected.key }?.let(onSelectTab) },
    )
}

@Composable
private fun MeasurementTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: ProgressMeasurementValidationError?,
    modifier: Modifier = Modifier,
) {
    TrainIqFormField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        context = TrainIqFormFieldContext.Progress,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = error != null,
        errorText = error?.message,
    )
}

private fun measurementDeltaText(
    current: com.trainiq.domain.model.BodyMeasurement,
    previous: com.trainiq.domain.model.BodyMeasurement,
): String {
    val weight = current.weight - previous.weight
    val bodyFat = current.bodyFat - previous.bodyFat
    val muscle = current.muscleMass - previous.muscleMass
    return "Sinds vorige: gewicht ${signedOneDecimal(weight)} kg, vet ${signedOneDecimal(bodyFat)} pp, spier ${signedOneDecimal(muscle)} kg"
}

internal fun estimatedOneRepMaxText(value: Double): String = "${String.format(Locale.US, "%.1f", value)} kg"

internal fun sortedMeasurementsForHistory(measurements: List<BodyMeasurement>): List<BodyMeasurement> =
    measurements.sortedByDescending { it.date }

internal fun deleteMeasurementActionLabel(): String = "Verwijderen"

internal fun latestBodyWeightText(measurements: List<BodyMeasurement>): String =
    sortedMeasurementsForHistory(measurements).firstOrNull()?.let { "${oneDecimal(it.weight)} kg" } ?: "-- kg"

internal fun latestBodyFatText(measurements: List<BodyMeasurement>): String =
    sortedMeasurementsForHistory(measurements).firstOrNull()?.let { "${oneDecimal(it.bodyFat)}%" } ?: "--%"

internal fun latestMuscleMassText(measurements: List<BodyMeasurement>): String =
    sortedMeasurementsForHistory(measurements).firstOrNull()?.let { "${oneDecimal(it.muscleMass)} kg" } ?: "-- kg"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressMetricRow(bodyFat: String, muscleMass: String) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        AppChip(label = "Vet $bodyFat", accent = MaterialTheme.trainIqColors.amber)
        AppChip(label = "Spier $muscleMass", accent = MaterialTheme.trainIqColors.amber)
    }
}

private fun signedOneDecimal(value: Double): String =
    String.format(Locale.getDefault(), "%+.1f", value)

private fun oneDecimal(value: Double): String =
    String.format(Locale.US, "%.1f", value)
