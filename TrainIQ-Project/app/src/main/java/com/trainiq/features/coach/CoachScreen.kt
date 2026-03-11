package com.trainiq.features.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.trainiq.domain.model.CoachOverview
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.usecase.GenerateGoalAdviceUseCase
import com.trainiq.domain.usecase.ObserveCoachUseCase
import com.trainiq.features.settings.SettingsSection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CoachViewModel @Inject constructor(
    observeCoachUseCase: ObserveCoachUseCase,
    private val generateGoalAdviceUseCase: GenerateGoalAdviceUseCase,
) : ViewModel() {
    val overview: StateFlow<CoachOverview?> = observeCoachUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _goalAdvice = MutableStateFlow<GoalAdvice?>(null)
    val goalAdvice: StateFlow<GoalAdvice?> = _goalAdvice

    fun generateGoalAdvice(height: Double, weight: Double, bodyFat: Double, goal: String) {
        viewModelScope.launch { _goalAdvice.value = generateGoalAdviceUseCase(height, weight, bodyFat, goal) }
    }
}

@Composable
fun CoachRoute(viewModel: CoachViewModel = hiltViewModel()) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val goalAdvice by viewModel.goalAdvice.collectAsStateWithLifecycle()
    CoachScreen(overview, goalAdvice, viewModel::generateGoalAdvice)
}

@Composable
fun CoachScreen(
    overview: CoachOverview?,
    goalAdvice: GoalAdvice?,
    onGenerateAdvice: (Double, Double, Double, String) -> Unit,
) {
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Coach", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        if (
            overview != null &&
            (overview.weeklyReport.isNotBlank() || overview.trainingInsights.isNotEmpty() || overview.nutritionCoachMessage.isNotBlank())
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Weekly AI Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (overview.weeklyReport.isNotBlank()) {
                            Text(overview.weeklyReport)
                        }
                        if (overview.trainingInsights.isNotEmpty()) {
                            Text("Training Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            overview.trainingInsights.forEach { Text("- $it") }
                        }
                        if (overview.nutritionCoachMessage.isNotBlank()) {
                            Text("Nutrition Coach: ${overview.nutritionCoachMessage}")
                        }
                    }
                }
            }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AI Coach unavailable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Add workouts, meals, and progress data to unlock weekly reports and coaching insights.")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Body Goal Advisor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it },
                        label = { Text("Height (cm)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = bodyFat,
                        onValueChange = { bodyFat = it },
                        label = { Text("Body fat %") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = goal,
                        onValueChange = { goal = it },
                        label = { Text("Goal") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            onGenerateAdvice(
                                height.toDoubleOrNull() ?: 0.0,
                                weight.toDoubleOrNull() ?: 0.0,
                                bodyFat.toDoubleOrNull() ?: 0.0,
                                goal,
                            )
                        },
                    ) {
                        Text("Generate advice")
                    }
                    goalAdvice?.let { advice ->
                        Text("Calories: ${advice.calorieTarget} kcal")
                        Text("Macros: P${advice.proteinTarget} C${advice.carbsTarget} F${advice.fatTarget}")
                        Text("Training focus: ${advice.trainingFocus}")
                        Text(advice.summary)
                    }
                }
            }
        }
        item { SettingsSection() }
    }
}
