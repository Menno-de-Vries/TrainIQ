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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.usecase.GenerateGoalAdviceUseCase
import com.trainiq.domain.usecase.GenerateWeeklyReportUseCase
import com.trainiq.domain.usecase.ObserveCoachUseCase
import com.trainiq.domain.usecase.ObserveUserProfileUseCase
import com.trainiq.domain.usecase.SaveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CoachViewModel @Inject constructor(
    observeCoachUseCase: ObserveCoachUseCase,
    observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val generateGoalAdviceUseCase: GenerateGoalAdviceUseCase,
    private val generateWeeklyReportUseCase: GenerateWeeklyReportUseCase,
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
) : ViewModel() {
    val overview: StateFlow<CoachOverview?> = observeCoachUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val profile: StateFlow<UserProfile?> = observeUserProfileUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _goalAdvice = MutableStateFlow<GoalAdvice?>(null)
    val goalAdvice: StateFlow<GoalAdvice?> = _goalAdvice.asStateFlow()

    private val _generatedReport = MutableStateFlow<String?>(null)
    val generatedReport: StateFlow<String?> = _generatedReport.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun generateGoalAdvice(name: String, height: String, weight: String, bodyFat: String, activityLevel: String, goal: String) {
        val parsedHeight = height.toDoubleOrNull()
        val parsedWeight = weight.toDoubleOrNull()
        val parsedBodyFat = bodyFat.toDoubleOrNull()
        if (name.isBlank() || activityLevel.isBlank() || goal.isBlank() || parsedHeight == null || parsedWeight == null || parsedBodyFat == null) {
            _message.value = "Complete all profile fields before generating advice."
            return
        }
        viewModelScope.launch {
            _goalAdvice.value = generateGoalAdviceUseCase(parsedHeight, parsedWeight, parsedBodyFat, goal)
            _message.value = "Advice generated. Save it if you want to update your dashboard targets."
        }
    }

    fun generateWeeklyReport() {
        viewModelScope.launch {
            _generatedReport.value = generateWeeklyReportUseCase()
            _message.value = "Weekly report generated."
        }
    }

    fun saveProfile(name: String, height: String, weight: String, bodyFat: String, activityLevel: String, goal: String) {
        val parsedHeight = height.toDoubleOrNull()
        val parsedWeight = weight.toDoubleOrNull()
        val parsedBodyFat = bodyFat.toDoubleOrNull()
        val advice = goalAdvice.value
        if (name.isBlank() || activityLevel.isBlank() || goal.isBlank() || parsedHeight == null || parsedWeight == null || parsedBodyFat == null || advice == null) {
            _message.value = "Generate advice first and then save the profile."
            return
        }
        viewModelScope.launch {
            saveUserProfileUseCase(
                UserProfile(
                    id = 1L,
                    name = name.trim(),
                    height = parsedHeight,
                    weight = parsedWeight,
                    bodyFat = parsedBodyFat,
                    activityLevel = activityLevel.trim(),
                    goal = goal.trim(),
                    calorieTarget = advice.calorieTarget,
                    proteinTarget = advice.proteinTarget,
                    carbsTarget = advice.carbsTarget,
                    fatTarget = advice.fatTarget,
                    trainingFocus = advice.trainingFocus,
                ),
            )
            _message.value = "Profile and goals saved."
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

@Composable
fun CoachRoute(viewModel: CoachViewModel = hiltViewModel()) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val currentProfile by viewModel.profile.collectAsStateWithLifecycle()
    val goalAdvice by viewModel.goalAdvice.collectAsStateWithLifecycle()
    val generatedReport by viewModel.generatedReport.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    CoachScreen(
        overview = overview,
        currentProfile = currentProfile,
        goalAdvice = goalAdvice,
        generatedReport = generatedReport,
        message = message,
        onGenerateAdvice = viewModel::generateGoalAdvice,
        onGenerateWeeklyReport = viewModel::generateWeeklyReport,
        onSaveProfile = viewModel::saveProfile,
        onDismissMessage = viewModel::clearMessage,
    )
}

@Composable
fun CoachScreen(
    overview: CoachOverview?,
    currentProfile: UserProfile?,
    goalAdvice: GoalAdvice?,
    generatedReport: String?,
    message: String?,
    onGenerateAdvice: (String, String, String, String, String, String) -> Unit,
    onGenerateWeeklyReport: () -> Unit,
    onSaveProfile: (String, String, String, String, String, String) -> Unit,
    onDismissMessage: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var activityLevel by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }

    LaunchedEffect(currentProfile) {
        currentProfile?.let { profile ->
            name = profile.name
            height = profile.height.toString()
            weight = profile.weight.toString()
            bodyFat = profile.bodyFat.toString()
            activityLevel = profile.activityLevel
            goal = profile.goal
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Coach", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        message?.let {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(it)
                        TextButton(onClick = onDismissMessage) { Text("Dismiss") }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Weekly summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(overview?.weeklyReport ?: "Complete your setup to start receiving coaching.")
                    Button(onClick = onGenerateWeeklyReport) { Text("Generate AI report") }
                    Text(generatedReport ?: "AI report is only generated when you tap the button above.")
                    Text("Training insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    (overview?.trainingInsights ?: listOf("No insights yet. Save a goal and complete a workout.")).forEach { Text("- $it") }
                    Text("Nutrition coach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(overview?.nutritionCoachMessage ?: "Nutrition feedback will appear once you log meals.")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Goal Advisor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("This action is always user-triggered. If AI is disabled, TrainIQ uses deterministic advice instead.")
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (cm)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bodyFat, onValueChange = { bodyFat = it }, label = { Text("Body fat %") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = activityLevel, onValueChange = { activityLevel = it }, label = { Text("Activity level") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = goal, onValueChange = { goal = it }, label = { Text("Goal") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onGenerateAdvice(name, height, weight, bodyFat, activityLevel, goal) }) {
                        Text("Generate advice")
                    }
                    goalAdvice?.let { advice ->
                        Text("Calories: ${advice.calorieTarget} kcal")
                        Text("Macros: P${advice.proteinTarget} C${advice.carbsTarget} F${advice.fatTarget}")
                        Text("Training focus: ${advice.trainingFocus}")
                        Text(advice.summary)
                        Button(onClick = { onSaveProfile(name, height, weight, bodyFat, activityLevel, goal) }) {
                            Text("Save profile")
                        }
                    }
                }
            }
        }
    }
}
