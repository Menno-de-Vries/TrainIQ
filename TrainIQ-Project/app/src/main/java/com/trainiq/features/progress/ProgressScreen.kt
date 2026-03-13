package com.trainiq.features.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.ui.MessageCard
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.ShimmerCardPlaceholder
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
        val parsedWeight = weight.toDoubleOrNull()
        val parsedBodyFat = bodyFat.toDoubleOrNull()
        val parsedMuscleMass = muscleMass.toDoubleOrNull()
        if (parsedWeight == null || parsedBodyFat == null || parsedMuscleMass == null) {
            _message.value = "Fill in valid numbers for all measurement fields."
            return
        }
        viewModelScope.launch {
            addMeasurementUseCase(parsedWeight, parsedBodyFat, parsedMuscleMass)
            _message.value = "Measurement saved."
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader(title = "Progress") }
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add measurement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Use manual entries to keep your body metrics up to date.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = bodyFat, onValueChange = { bodyFat = it }, label = { Text("Body fat %") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = muscleMass, onValueChange = { muscleMass = it }, label = { Text("Muscle mass") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        onAddMeasurement(weight, bodyFat, muscleMass)
                        weight = ""
                        bodyFat = ""
                        muscleMass = ""
                    }) { Text("Save measurement") }
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
                MetricCard(
                    title = "No progress data",
                    value = "Start logging",
                    subtitle = "Add body measurements and complete workouts to unlock progress analytics.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            item {
                MetricCard("Estimated 1RM", "${overview.estimatedOneRepMax.toInt()} kg", "Calculated from your logged sets", Modifier.fillMaxWidth())
            }
            item {
                MetricCard("Fatigue Index", String.format("%.2f", overview.fatigueIndex), "Weekly volume versus baseline", Modifier.fillMaxWidth())
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Measurement history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        overview.measurements.forEach { measurement ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${measurement.date.toReadableDate()}: ${measurement.weight} kg, ${measurement.bodyFat}% fat")
                                TextButton(onClick = { onDeleteMeasurement(measurement.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
            item { ChartComposable("Body weight", overview.weightTrend, Modifier.fillMaxWidth()) }
            item { ChartComposable("Body fat", overview.bodyFatTrend, Modifier.fillMaxWidth()) }
            item { ChartComposable("Strength progression", overview.strengthTrend, Modifier.fillMaxWidth()) }
            item { ChartComposable("Training volume", overview.volumeTrend, Modifier.fillMaxWidth()) }
        }
    }
}
