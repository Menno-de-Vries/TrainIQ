package com.trainiq.features.coach

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.theme.spacing
import com.trainiq.core.ui.MessageCard
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.core.ui.bringIntoViewOnFocus
import com.trainiq.core.ui.clearFocusOnScrollOrDrag
import com.trainiq.features.profile.ProfileActivityLevels
import com.trainiq.features.profile.ProfileInputField
import com.trainiq.features.profile.ProfileInputValidationError
import com.trainiq.features.profile.ProfileInputValidationResult
import com.trainiq.features.profile.buildValidatedProfileInput
import com.trainiq.features.profile.validateProfileInput
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.CoachOverview
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.model.WeeklyReportResult
import com.trainiq.domain.model.buildGoalBaseline
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
        val goalAdviceInput: GoalAdviceInput? = null,
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
        val input = when (
            val result = validateGoalAdviceInput(name, height, weight, bodyFat, age, sex, activityLevel, goal)
        ) {
            is GoalAdviceInputValidationResult.Valid -> result.input
            is GoalAdviceInputValidationResult.Invalid -> {
                ephemeral.update { it.copy(message = result.message) }
                return
            }
        }
        viewModelScope.launch {
            ephemeral.update {
                it.copy(
                    goalAdvice = null,
                    goalAdviceInput = null,
                    isGeneratingAdvice = true,
                    message = null,
                )
            }
            val result = runCatching {
                generateGoalAdviceUseCase(input.height, input.weight, input.bodyFat, input.age, input.sex, input.activityLevel, input.goal)
            }
            ephemeral.update {
                it.copy(
                    goalAdvice = result.getOrNull(),
                    goalAdviceInput = if (result.isSuccess) input else null,
                    message = if (result.isSuccess) "Advies gemaakt. Controleer het voordat je opslaat." else "Advies maken lukt nu niet.",
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
                    message = if (result.isSuccess) "Samenvatting bijgewerkt." else "Weekrapport maken lukt nu niet.",
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
        val input = when (
            val result = validateGoalAdviceInput(name, height, weight, bodyFat, age, sex, activityLevel, goal)
        ) {
            is GoalAdviceInputValidationResult.Valid -> result.input
            is GoalAdviceInputValidationResult.Invalid -> {
                ephemeral.update { it.copy(message = result.message) }
                return
            }
        }
        val currentAdviceState = ephemeral.value
        val advice = currentAdviceState.goalAdvice
            ?.takeIf { input == currentAdviceState.goalAdviceInput }
            ?: input.toDeterministicGoalAdvice()
        viewModelScope.launch {
            runCatching {
                saveUserProfileUseCase(
                    UserProfile(
                        id = 1L,
                        name = input.name,
                        age = input.age,
                        sex = input.sex,
                        height = input.height,
                        weight = input.weight,
                        bodyFat = input.bodyFat,
                        activityLevel = input.activityLevel,
                        goal = input.goal,
                        calorieTarget = advice.calorieTarget,
                        proteinTarget = advice.proteinTarget,
                        carbsTarget = advice.carbsTarget,
                        fatTarget = advice.fatTarget,
                        trainingFocus = advice.trainingFocus,
                    ),
                )
            }.onSuccess {
                ephemeral.update {
                    it.copy(
                        goalAdvice = advice,
                        goalAdviceInput = input,
                        message = "Profiel en doelen opgeslagen.",
                    )
                }
            }.onFailure {
                ephemeral.update { it.copy(message = "Profiel opslaan mislukt. Probeer opnieuw.") }
            }
        }
    }

    fun clearMessage() {
        ephemeral.update { it.copy(message = null) }
    }
}

private fun GoalAdviceInput.toDeterministicGoalAdvice(): GoalAdvice {
    val baseline = buildGoalBaseline(
        heightCm = height,
        weightKg = weight,
        bodyFat = bodyFat,
        age = age,
        sex = sex,
        activityLevel = activityLevel,
        goal = goal,
    )
    val trainingFocus = when {
        goal.contains("bulk", ignoreCase = true) -> "Progressieve overload op compoundoefeningen"
        goal.contains("cut", ignoreCase = true) || goal.contains("fat", ignoreCase = true) -> "Consistentie, stappen en herstel"
        bodyFat > 20 -> "Body recomposition met consistente krachttraining"
        else -> "Gebalanceerde krachtopbouw en herstel"
    }
    return GoalAdvice(
        bmr = baseline.bmr,
        maintenanceCalories = baseline.maintenanceCalories,
        activityMultiplier = baseline.activityMultiplier,
        calorieTarget = baseline.targetCalories,
        proteinTarget = baseline.proteinTarget,
        carbsTarget = baseline.carbsTarget,
        fatTarget = baseline.fatTarget,
        trainingFocus = trainingFocus,
        summary = "Lokale berekening opgeslagen. Je kunt AI-advies later gebruiken om dit te verfijnen.",
    )
}

internal data class GoalAdviceInput(
    val name: String,
    val height: Double,
    val weight: Double,
    val bodyFat: Double,
    val age: Int,
    val sex: BiologicalSex,
    val activityLevel: String,
    val goal: String,
)

private sealed interface GoalAdviceInputValidationResult {
    data class Valid(val input: GoalAdviceInput) : GoalAdviceInputValidationResult
    data class Invalid(val message: String) : GoalAdviceInputValidationResult
}

internal fun buildGoalAdviceInput(
    name: String,
    height: String,
    weight: String,
    bodyFat: String,
    age: String,
    sex: BiologicalSex,
    activityLevel: String,
    goal: String,
): GoalAdviceInput? {
    val input = buildValidatedProfileInput(name, height, weight, bodyFat, age, sex, activityLevel, goal) ?: return null

    return GoalAdviceInput(
        name = input.name,
        height = input.height,
        weight = input.weight,
        bodyFat = input.bodyFat,
        age = input.age,
        sex = input.sex,
        activityLevel = input.activityLevel,
        goal = input.goal,
    )
}

private fun validateGoalAdviceInput(
    name: String,
    height: String,
    weight: String,
    bodyFat: String,
    age: String,
    sex: BiologicalSex,
    activityLevel: String,
    goal: String,
): GoalAdviceInputValidationResult = when (
    val result = validateProfileInput(name, height, weight, bodyFat, age, sex, activityLevel, goal)
) {
    is ProfileInputValidationResult.Valid -> GoalAdviceInputValidationResult.Valid(
        GoalAdviceInput(
            name = result.input.name,
            height = result.input.height,
            weight = result.input.weight,
            bodyFat = result.input.bodyFat,
            age = result.input.age,
            sex = result.input.sex,
            activityLevel = result.input.activityLevel,
            goal = result.input.goal,
        ),
    )
    is ProfileInputValidationResult.Invalid -> GoalAdviceInputValidationResult.Invalid(result.error.message)
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
    var profileInputError by remember { mutableStateOf<ProfileInputValidationError?>(null) }
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
            item { ScreenHeader(title = "Coach", subtitle = "Advies op basis van training, voeding en profiel") }

            when (state) {
                CoachUiState.Loading -> {
                    item { ShimmerCardPlaceholder(lineCount = 4) }
                    item { ShimmerCardPlaceholder(lineCount = 5) }
                }

                is CoachUiState.Error -> {
                    item { MessageCard(message = state.message) }
                }

                is CoachUiState.Success -> {
                    if (profileInputError == null) state.message?.let { message ->
                        item { MessageCard(message = message, onDismiss = onDismissMessage) }
                    }
                    if (state.currentProfile == null) {
                        item {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(MaterialTheme.spacing.medium),
                                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                                ) {
                                    Text("Profiel instellen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                                    Text("Vul eerst je profiel en doel in. Daarna worden weekrapporten en voedingsadvies zichtbaar op basis van jouw gegevens.")
                                }
                            }
                        }
                    } else item {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                            ) {
                                Text("Weekoverzicht", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
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
                                    Text(if (state.isGeneratingReport) "Rapport maken..." else "Weekrapport maken")
                                }
                                if (state.isGeneratingReport) {
                                    ShimmerCardPlaceholder(lineCount = 3)
                                }
                                state.generatedReport?.let { report ->
                                    if (report.wins.isNotEmpty()) {
                                        Text("Successen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        report.wins.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                                    }
                                    if (report.risks.isNotEmpty()) {
                                        Text("Risico's", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        report.risks.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                                    }
                                    Text("Focus voor volgende week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(report.nextWeekFocus, style = MaterialTheme.typography.bodyMedium)
                                    if (report.thinkingProcess.isNotEmpty()) {
                                        Text("Waarom dit advies?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        report.thinkingProcess.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                                    }
                                }
                                Text("Trainingsinzichten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                (state.overview.trainingInsights.ifEmpty { listOf("Nog geen inzichten. Sla een doel op en voltooi een workout.") }).forEach {
                                    Text("• $it", style = MaterialTheme.typography.bodyMedium)
                                }
                                Text("Voedingscoach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                                Text("Doeladvies", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = {
                                        name = it
                                        profileInputError = null
                                    },
                                    label = { Text("Naam") },
                                    isError = profileInputError.isFor(ProfileInputField.Name),
                                    supportingText = profileInputError.supportingTextFor(ProfileInputField.Name),
                                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                                )
                                OutlinedTextField(
                                    value = age,
                                    onValueChange = {
                                        age = it
                                        profileInputError = null
                                    },
                                    label = { Text("Leeftijd") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = profileInputError.isFor(ProfileInputField.Age),
                                    supportingText = profileInputError.supportingTextFor(ProfileInputField.Age),
                                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                                )
                                Text("Biologische sekse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                                    BiologicalSex.entries.forEach { option ->
                                        FilterChip(
                                            selected = sex == option,
                                            onClick = {
                                                sex = option
                                                profileInputError = null
                                            },
                                            label = { Text(option.displayLabel()) },
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = height,
                                    onValueChange = {
                                        height = it
                                        profileInputError = null
                                    },
                                    label = { Text("Lengte (cm)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = profileInputError.isFor(ProfileInputField.Height),
                                    supportingText = profileInputError.supportingTextFor(ProfileInputField.Height),
                                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                                )
                                OutlinedTextField(
                                    value = weight,
                                    onValueChange = {
                                        weight = it
                                        profileInputError = null
                                    },
                                    label = { Text("Gewicht (kg)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = profileInputError.isFor(ProfileInputField.Weight),
                                    supportingText = profileInputError.supportingTextFor(ProfileInputField.Weight),
                                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                                )
                                OutlinedTextField(
                                    value = bodyFat,
                                    onValueChange = {
                                        bodyFat = it
                                        profileInputError = null
                                    },
                                    label = { Text("Vetpercentage %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = profileInputError.isFor(ProfileInputField.BodyFat),
                                    supportingText = profileInputError.supportingTextFor(ProfileInputField.BodyFat),
                                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                                )
                                Text("Activiteitsniveau", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                                ) {
                                    ProfileActivityLevels.forEach { option ->
                                        FilterChip(
                                            selected = activityLevel == option,
                                            onClick = {
                                                activityLevel = option
                                                profileInputError = null
                                            },
                                            label = { Text(option) },
                                        )
                                    }
                                }
                                profileInputError.takeIf { it.isFor(ProfileInputField.ActivityLevel) }?.let { error ->
                                    Text(error.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                                OutlinedTextField(
                                    value = goal,
                                    onValueChange = {
                                        goal = it
                                        profileInputError = null
                                    },
                                    label = { Text("Doel") },
                                    isError = profileInputError.isFor(ProfileInputField.Goal),
                                    supportingText = profileInputError.supportingTextFor(ProfileInputField.Goal),
                                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                                )
                                Button(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        when (
                                            val result = validateProfileInput(name, height, weight, bodyFat, age, sex, activityLevel, goal)
                                        ) {
                                            is ProfileInputValidationResult.Valid -> {
                                                profileInputError = null
                                                onGenerateAdvice(name, height, weight, bodyFat, age, sex, activityLevel, goal)
                                            }
                                            is ProfileInputValidationResult.Invalid -> {
                                                profileInputError = result.error
                                                onDismissMessage()
                                            }
                                        }
                                    },
                                    enabled = !state.isGeneratingAdvice,
                                ) {
                                    Text(if (state.isGeneratingAdvice) "Advies maken..." else "Advies maken")
                                }
                                if (state.isGeneratingAdvice) {
                                    ShimmerCardPlaceholder(lineCount = 4)
                                }
                                state.goalAdvice?.let { advice ->
                                    Text("BMR: ${advice.bmr} kcal", style = MaterialTheme.typography.bodyMedium)
                                    Text("Onderhoud: ${advice.maintenanceCalories} kcal", style = MaterialTheme.typography.bodyMedium)
                                    Text("Activiteitsfactor: ${"%.3f".format(advice.activityMultiplier)}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Calorieën: ${advice.calorieTarget} kcal", style = MaterialTheme.typography.bodyMedium)
                                    Text("Macro's: P${advice.proteinTarget} C${advice.carbsTarget} F${advice.fatTarget}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Trainingsfocus: ${advice.trainingFocus}", style = MaterialTheme.typography.bodyMedium)
                                    Text(advice.summary, style = MaterialTheme.typography.bodyMedium)
                                    Button(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            when (
                                                val result = validateProfileInput(name, height, weight, bodyFat, age, sex, activityLevel, goal)
                                            ) {
                                                is ProfileInputValidationResult.Valid -> {
                                                    profileInputError = null
                                                    onSaveProfile(name, height, weight, bodyFat, age, sex, activityLevel, goal)
                                                }
                                                is ProfileInputValidationResult.Invalid -> {
                                                    profileInputError = result.error
                                                    onDismissMessage()
                                                }
                                            }
                                        },
                                    ) {
                                        Text("Profiel opslaan")
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

private fun ProfileInputValidationError?.isFor(field: ProfileInputField): Boolean = this?.field == field

private fun BiologicalSex.displayLabel(): String = when (this) {
    BiologicalSex.MALE -> "Man"
    BiologicalSex.FEMALE -> "Vrouw"
}

private fun ProfileInputValidationError?.supportingTextFor(field: ProfileInputField): (@Composable () -> Unit)? {
    val error = takeIf { it.isFor(field) } ?: return null
    return { Text(error.message) }
}
