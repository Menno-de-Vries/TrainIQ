package com.trainiq.features.coach

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.theme.spacing
import com.trainiq.core.ui.MessageCard
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.CoachOverview
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.model.WeeklyReportResult
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val CoachActivityLevels = listOf(
    "Sedentary",
    "Lightly active",
    "Moderately active",
    "Very active",
    "Athlete",
)

sealed interface CoachUiState {
    data object Loading : CoachUiState
    data class Success(
        val overview: CoachOverview,
        val currentProfile: UserProfile?,
        val goalAdvice: GoalAdvice? = null,
        val generatedReport: WeeklyReportResult? = null,
        val message: String? = null,
        val isGeneratingAdvice: Boolean = false,
        val isGeneratingReport: Boolean = false,
    ) : CoachUiState
    data class Error(val message: String) : CoachUiState
}

@HiltViewModel
class CoachViewModel @Inject constructor(
    observeCoachUseCase: ObserveCoachUseCase,
    observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val generateGoalAdviceUseCase: GenerateGoalAdviceUseCase,
    private val generateWeeklyReportUseCase: GenerateWeeklyReportUseCase,
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
) : ViewModel() {
    private data class CoachEphemeralState(
        val goalAdvice: GoalAdvice? = null,
        val generatedReport: WeeklyReportResult? = null,
        val message: String? = null,
        val isGeneratingAdvice: Boolean = false,
        val isGeneratingReport: Boolean = false,
    )

    private val overview = observeCoachUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val profile = observeUserProfileUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val ephemeral = MutableStateFlow(CoachEphemeralState())

    val uiState: StateFlow<CoachUiState> = combine(overview, profile, ephemeral) { currentOverview, currentProfile, temp ->
        when {
            currentOverview == null -> CoachUiState.Loading
            else -> CoachUiState.Success(
                overview = currentOverview,
                currentProfile = currentProfile,
                goalAdvice = temp.goalAdvice,
                generatedReport = temp.generatedReport,
                message = temp.message,
                isGeneratingAdvice = temp.isGeneratingAdvice,
                isGeneratingReport = temp.isGeneratingReport,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoachUiState.Loading)

    fun generateGoalAdvice(
        name: String,
        height: String,
        weight: String,
        bodyFat: String,
        age: String,
        sex: BiologicalSex,
        activityLevel: String,
        goal: String,
    ) {
        val parsedHeight = height.toDoubleOrNull()
        val parsedWeight = weight.toDoubleOrNull()
        val parsedBodyFat = bodyFat.toDoubleOrNull()
        val parsedAge = age.toIntOrNull() ?: 30
        if (name.isBlank() || activityLevel.isBlank() || goal.isBlank() || parsedHeight == null || parsedWeight == null || parsedBodyFat == null) {
            ephemeral.update { it.copy(message = "Complete all profile fields before generating advice.") }
            return
        }
        viewModelScope.launch {
            ephemeral.update { it.copy(isGeneratingAdvice = true, message = null) }
            val result = runCatching {
                generateGoalAdviceUseCase(parsedHeight, parsedWeight, parsedBodyFat, parsedAge, sex, activityLevel, goal)
            }
            ephemeral.update {
                it.copy(
                    goalAdvice = result.getOrNull(),
                    message = if (result.isSuccess) "Advice generated. Review it before saving." else "Unable to generate advice right now.",
                    isGeneratingAdvice = false,
                )
            }
        }
    }

    fun generateWeeklyReport() {
        viewModelScope.launch {
            ephemeral.update { it.copy(isGeneratingReport = true, message = null) }
            val result: Result<WeeklyReportResult> = runCatching { generateWeeklyReportUseCase() }
            ephemeral.update {
                it.copy(
                    generatedReport = result.getOrNull(),
                    message = if (result.isSuccess) "Weekly report generated." else "Unable to generate the weekly report right now.",
                    isGeneratingReport = false,
                )
            }
        }
    }

    fun saveProfile(
        name: String,
        height: String,
        weight: String,
        bodyFat: String,
        age: String,
        sex: BiologicalSex,
        activityLevel: String,
        goal: String,
    ) {
        val parsedHeight = height.toDoubleOrNull()
        val parsedWeight = weight.toDoubleOrNull()
        val parsedBodyFat = bodyFat.toDoubleOrNull()
        val parsedAge = age.toIntOrNull() ?: 30
        val current = uiState.value as? CoachUiState.Success
        val advice = current?.goalAdvice
        if (name.isBlank() || activityLevel.isBlank() || goal.isBlank() || parsedHeight == null || parsedWeight == null || parsedBodyFat == null || advice == null) {
            ephemeral.update { it.copy(message = "Generate advice first and then save the profile.") }
            return
        }
        viewModelScope.launch {
            saveUserProfileUseCase(
                UserProfile(
                    id = 1L,
                    name = name.trim(),
                    age = parsedAge,
                    sex = sex,
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
            ephemeral.update { it.copy(message = "Profile and goals saved.") }
        }
    }

    fun clearMessage() {
        ephemeral.update { it.copy(message = null) }
    }
}

@Composable
fun CoachRoute(viewModel: CoachViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CoachScreen(
        uiState = uiState,
        onGenerateAdvice = viewModel::generateGoalAdvice,
        onGenerateWeeklyReport = viewModel::generateWeeklyReport,
        onSaveProfile = viewModel::saveProfile,
        onDismissMessage = viewModel::clearMessage,
    )
}

@Composable
fun CoachScreen(
    uiState: CoachUiState,
    onGenerateAdvice: (String, String, String, String, String, BiologicalSex, String, String) -> Unit,
    onGenerateWeeklyReport: () -> Unit,
    onSaveProfile: (String, String, String, String, String, BiologicalSex, String, String) -> Unit,
    onDismissMessage: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("30") }
    var sex by remember { mutableStateOf(BiologicalSex.MALE) }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var activityLevel by remember { mutableStateOf("Moderately active") }
    var goal by remember { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current

    val profile = (uiState as? CoachUiState.Success)?.currentProfile
    LaunchedEffect(profile) {
        profile?.let {
            name = it.name
            age = it.age.toString()
            sex = it.sex
            height = it.height.toString()
            weight = it.weight.toString()
            bodyFat = it.bodyFat.toString()
            activityLevel = it.activityLevel
            goal = it.goal
        }
    }

    AnimatedContent(targetState = uiState, label = "coach-ui-state") { state ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        ) {
            item { ScreenHeader(title = "Coach") }

            when (state) {
                CoachUiState.Loading -> {
                    item { ShimmerCardPlaceholder(lineCount = 4) }
                    item { ShimmerCardPlaceholder(lineCount = 5) }
                }

                is CoachUiState.Error -> {
                    item { MessageCard(message = state.message) }
                }

                is CoachUiState.Success -> {
                    state.message?.let { message ->
                        item { MessageCard(message = message, onDismiss = onDismissMessage) }
                    }
                    item {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                            ) {
                                Text("Weekly summary", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ) {
                                    Text(
                                        text = state.generatedReport?.summary ?: state.overview.weeklyReport,
                                        modifier = Modifier.padding(MaterialTheme.spacing.medium),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                Button(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onGenerateWeeklyReport()
                                    },
                                    enabled = !state.isGeneratingReport,
                                ) {
                                    Text(if (state.isGeneratingReport) "Thinking..." else "Generate AI report")
                                }
                                if (state.isGeneratingReport) {
                                    ShimmerCardPlaceholder(lineCount = 3)
                                }
                                state.generatedReport?.let { report ->
                                    if (report.wins.isNotEmpty()) {
                                        Text("Wins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        report.wins.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                                    }
                                    if (report.risks.isNotEmpty()) {
                                        Text("Risks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        report.risks.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                                    }
                                    Text("Next week focus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(report.nextWeekFocus, style = MaterialTheme.typography.bodyMedium)
                                    if (report.thinkingProcess.isNotEmpty()) {
                                        Text("Reasoning trace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        report.thinkingProcess.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                                    }
                                }
                                Text("Training insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                (state.overview.trainingInsights.ifEmpty { listOf("No insights yet. Save a goal and complete a workout.") }).forEach {
                                    Text("• $it", style = MaterialTheme.typography.bodyMedium)
                                }
                                Text("Nutrition coach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ) {
                                    Text(
                                        text = state.overview.nutritionCoachMessage,
                                        modifier = Modifier.padding(MaterialTheme.spacing.medium),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                            ) {
                                Text("Goal advisor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.fillMaxWidth())
                                Text("Biological sex", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                                    BiologicalSex.entries.forEach { option ->
                                        FilterChip(
                                            selected = sex == option,
                                            onClick = { sex = option },
                                            label = { Text(option.name.lowercase().replaceFirstChar(Char::titlecase)) },
                                        )
                                    }
                                }
                                OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (cm)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = bodyFat, onValueChange = { bodyFat = it }, label = { Text("Body fat %") }, modifier = Modifier.fillMaxWidth())
                                Text("Activity level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                                ) {
                                    CoachActivityLevels.forEach { option ->
                                        FilterChip(
                                            selected = activityLevel == option,
                                            onClick = { activityLevel = option },
                                            label = { Text(option) },
                                        )
                                    }
                                }
                                OutlinedTextField(value = goal, onValueChange = { goal = it }, label = { Text("Goal") }, modifier = Modifier.fillMaxWidth())
                                Button(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onGenerateAdvice(name, height, weight, bodyFat, age, sex, activityLevel, goal)
                                    },
                                    enabled = !state.isGeneratingAdvice,
                                ) {
                                    Text(if (state.isGeneratingAdvice) "Generating..." else "Generate advice")
                                }
                                if (state.isGeneratingAdvice) {
                                    ShimmerCardPlaceholder(lineCount = 4)
                                }
                                state.goalAdvice?.let { advice ->
                                    Text("BMR: ${advice.bmr} kcal", style = MaterialTheme.typography.bodyMedium)
                                    Text("Maintenance: ${advice.maintenanceCalories} kcal", style = MaterialTheme.typography.bodyMedium)
                                    Text("Activity multiplier: ${"%.3f".format(advice.activityMultiplier)}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Calories: ${advice.calorieTarget} kcal", style = MaterialTheme.typography.bodyMedium)
                                    Text("Macros: P${advice.proteinTarget} C${advice.carbsTarget} F${advice.fatTarget}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Training focus: ${advice.trainingFocus}", style = MaterialTheme.typography.bodyMedium)
                                    Text(advice.summary, style = MaterialTheme.typography.bodyMedium)
                                    Button(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSaveProfile(name, height, weight, bodyFat, age, sex, activityLevel, goal)
                                        },
                                    ) {
                                        Text("Save profile")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
