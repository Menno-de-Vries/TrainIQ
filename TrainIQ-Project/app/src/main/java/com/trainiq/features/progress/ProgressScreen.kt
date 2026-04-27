package com.trainiq.features.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.trainiq.core.ui.EmptyStateCard
import com.trainiq.core.ui.PrimaryActionButton
import com.trainiq.core.ui.clearFocusOnScrollOrDrag
import com.trainiq.core.theme.spacing
import com.trainiq.core.theme.trainIqColors
import com.trainiq.core.util.ChartComposable
import com.trainiq.core.util.MetricCard
import com.trainiq.core.util.toReadableDate
import com.trainiq.domain.model.ProgressOverview
import com.trainiq.domain.usecase.AddMeasurementUseCase
import com.trainiq.domain.usecase.DeleteMeasurementUseCase
import com.trainiq.domain.usecase.ObserveProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val addMeasurementUseCase: AddMeasurementUseCase,
    private val deleteMeasurementUseCase: DeleteMeasurementUseCase,
) : ViewModel() {
    val overview: StateFlow<ProgressOverview?> = observeProgressUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun addMeasurement(weight: String, bodyFat: String, muscleMass: String) {
        when (val validation = validateProgressMeasurementInput(weight, bodyFat, muscleMass)) {
            is ProgressMeasurementValidationResult.Invalid -> {
                _message.value = validation.error.message
                return
            }
            is ProgressMeasurementValidationResult.Valid -> {
                viewModelScope.launch {
                    val measurement = validation.measurement
                    addMeasurementUseCase(measurement.weight, measurement.bodyFat, measurement.muscleMass)
                    _message.value = "Meting opgeslagen."
                }
            }
        }
    }

    fun deleteMeasurement(measurementId: Long) {
        if (overview.value?.measurements?.none { it.id == measurementId } != false) {
            _message.value = "Measurement could not be found."
            return
        }
        viewModelScope.launch {
            deleteMeasurementUseCase(measurementId)
            _message.value = "Measurement deleted."
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

@Composable
fun ProgressRoute(viewModel: ProgressViewModel = hiltViewModel()) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    ProgressScreen(
        overview = overview,
        message = message,
        onAddMeasurement = viewModel::addMeasurement,
        onDeleteMeasurement = viewModel::deleteMeasurement,
        onDismissMessage = viewModel::clearMessage,
    )
}

@Composable
fun ProgressScreen(
    overview: ProgressOverview?,
    message: String?,
    onAddMeasurement: (String, String, String) -> Unit,
    onDeleteMeasurement: (Long) -> Unit,
    onDismissMessage: () -> Unit,
) {
    var weight by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var muscleMass by remember { mutableStateOf("") }
    var weightTouched by remember { mutableStateOf(false) }
    var bodyFatTouched by remember { mutableStateOf(false) }
    var muscleMassTouched by remember { mutableStateOf(false) }

    val weightError = validateProgressMeasurementField(weight, weightSpec).takeIf { weightTouched }
    val bodyFatError = validateProgressMeasurementField(bodyFat, bodyFatSpec).takeIf { bodyFatTouched }
    val muscleMassError = validateProgressMeasurementField(muscleMass, muscleMassSpec).takeIf { muscleMassTouched }
    val measurementValidation = validateProgressMeasurementInput(weight, bodyFat, muscleMass)
    val canSaveMeasurement = measurementValidation is ProgressMeasurementValidationResult.Valid

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
        item { ScreenHeader(title = "Progress", subtitle = "Metingen, grafieken en kracht-trends") }
        message?.let {
            item { MessageCard(message = it, onDismiss = onDismissMessage) }
        }
        if (overview == null) {
            item { ShimmerCardPlaceholder(lineCount = 4) }
            item { ShimmerCardPlaceholder(lineCount = 3) }
            item { ShimmerCardPlaceholder(lineCount = 5) }
            return@LazyColumn
        }
        item {
            AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.purple) {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    Text("Lichaamssamenstelling", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("Vetpercentage, spiermassa en gewicht naast elkaar.", color = MaterialTheme.trainIqColors.mutedText)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        MeasurementTextField(
                            value = weight,
                            onValueChange = {
                                weight = it
                                weightTouched = true
                            },
                            label = "Gewicht (kg)",
                            error = weightError,
                            modifier = Modifier.weight(1f),
                        )
                        MeasurementTextField(
                            value = bodyFat,
                            onValueChange = {
                                bodyFat = it
                                bodyFatTouched = true
                            },
                            label = "Vetpercentage (%)",
                            error = bodyFatError,
                            modifier = Modifier.weight(1f),
                        )
                    }
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
                            weight = ""
                            bodyFat = ""
                            muscleMass = ""
                            weightTouched = false
                            bodyFatTouched = false
                            muscleMassTouched = false
                        }
                    }, enabled = canSaveMeasurement, accent = MaterialTheme.trainIqColors.purple) { Text("Meting opslaan") }
                }
            }
        }
        if (
            overview.weightTrend.isEmpty() &&
            overview.bodyFatTrend.isEmpty() &&
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
            item {
                MetricCard("Krachtscore", "${overview.estimatedOneRepMax.toInt()} kg", "Volume en geschatte 1RM samengevoegd", Modifier.fillMaxWidth())
            }
            item {
                MetricCard("Vermoeidheidsindex", String.format(Locale.getDefault(), "%.2f", overview.fatigueIndex), "Waarschuwing bij snelle volume + RPE stijging", Modifier.fillMaxWidth())
            }
            item {
                AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.purple) {
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                        Text("Meetgeschiedenis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        overview.measurements.forEach { measurement ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${measurement.date.toReadableDate()}: ${measurement.weight} kg, ${measurement.bodyFat}% vet", color = MaterialTheme.trainIqColors.mutedText)
                                TextButton(onClick = { onDeleteMeasurement(measurement.id) }) { Text("Verwijderen") }
                            }
                        }
                    }
                }
            }
            item { ChartComposable("Lichaamsgewicht", overview.weightTrend, Modifier.fillMaxWidth()) }
            item { ChartComposable("Vetpercentage", overview.bodyFatTrend, Modifier.fillMaxWidth()) }
            item { ChartComposable("Krachtprogressie", overview.strengthTrend, Modifier.fillMaxWidth()) }
            item { ChartComposable("Trainingsvolume", overview.volumeTrend, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun MeasurementTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: ProgressMeasurementValidationError?,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = error != null,
        supportingText = error?.let { { Text(it.message) } },
    )
}
