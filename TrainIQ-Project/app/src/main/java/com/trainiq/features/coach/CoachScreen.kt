package com.trainiq.features.coach

import com.trainiq.ai.services.toAiUserMessage

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.theme.spacing
import com.trainiq.core.theme.trainIqColors
import com.trainiq.core.ui.AppCard
import com.trainiq.core.ui.CompactSectionTabItem
import com.trainiq.core.ui.CompactSectionTabs
import com.trainiq.core.ui.MessageCard
import com.trainiq.core.ui.reloadableObservation
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.core.ui.TrainIqFormField
import com.trainiq.core.ui.TrainIqFormFieldContext
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
import com.trainiq.domain.model.GoalAdviceSource
import com.trainiq.domain.model.SavedGoalAdvice
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.model.WeeklyReportResult
import com.trainiq.ai.services.safeUserMessage
import com.trainiq.domain.model.WeeklyReportSource
import com.trainiq.domain.model.buildGoalBaseline
import com.trainiq.domain.model.goalAdviceProfileFingerprint
import com.trainiq.domain.usecase.GenerateGoalAdviceUseCase
import com.trainiq.domain.usecase.GenerateWeeklyReportUseCase
import com.trainiq.domain.usecase.ObserveCoachUseCase
import com.trainiq.domain.usecase.ObserveSavedGoalAdviceUseCase
import com.trainiq.domain.usecase.ObserveUserProfileUseCase
import com.trainiq.domain.usecase.SaveUserProfileUseCase
import com.trainiq.navigation.TrainIqWindowWidthClass
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.Serializable
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CoachUiState {
    data object Loading : CoachUiState
    data class Success(
        val overview: CoachOverview,
        val currentProfile: UserProfile?,
        val goalAdvice: GoalAdvice? = null,
        val savedGoalAdvice: SavedGoalAdvice? = null,
        val generatedReport: WeeklyReportResult? = null,
        val message: String? = null,
        val isGeneratingAdvice: Boolean = false,
        val isGeneratingReport: Boolean = false,
        val profileDraft: CoachProfileDraft,
        val isProfileDraftDirty: Boolean,
    ) : CoachUiState
    data class Error(val message: String) : CoachUiState
}

data class CoachProfileDraft(
    val name: String = "",
    val age: String = "30",
    val sex: BiologicalSex = BiologicalSex.MALE,
    val height: String = "",
    val weight: String = "",
    val bodyFat: String = "",
    val activityLevel: String = "Gemiddeld actief",
    val goal: String = "",
    val manualCalorieTarget: String = "",
) : Serializable {
    private companion object {
        const val serialVersionUID: Long = 1L
    }
}

private data class CoachProfileDraftState(
    val draft: CoachProfileDraft,
    val isDirty: Boolean,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CoachViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val observeCoachUseCase: ObserveCoachUseCase,
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val observeSavedGoalAdviceUseCase: ObserveSavedGoalAdviceUseCase,
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

    private val reloads = MutableStateFlow(0)
    private val overview = reloadableObservation(reloads) { observeCoachUseCase() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val profile = reloadableObservation(reloads) { observeUserProfileUseCase() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val savedGoalAdvice = reloadableObservation(reloads) { observeSavedGoalAdviceUseCase() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val ephemeral = MutableStateFlow(CoachEphemeralState())
    private val profileDraft = savedStateHandle.getStateFlow(
        CoachProfileDraftKey,
        savedStateHandle.get<CoachProfileDraft>(CoachProfileDraftKey) ?: CoachProfileDraft(),
    )
    private val isProfileDraftDirty = savedStateHandle.getStateFlow(CoachProfileDraftDirtyKey, false)
    private val profileDraftState = combine(profileDraft, isProfileDraftDirty, ::CoachProfileDraftState)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CoachProfileDraftState(profileDraft.value, isProfileDraftDirty.value),
        )

    init {
        viewModelScope.launch {
            profile.collect { result ->
                if (result?.isSuccess == true && !isProfileDraftDirty.value) {
                    hydrateProfileDraft(result.getOrThrow())
                }
            }
        }
    }

    val uiState: StateFlow<CoachUiState> = combine(overview, profile, savedGoalAdvice, ephemeral, profileDraftState) { currentOverview, currentProfile, persistedAdvice, temp, draftState ->
        when {
            currentOverview == null || currentProfile == null || persistedAdvice == null -> CoachUiState.Loading
            currentOverview.isFailure -> CoachUiState.Error("Coachgegevens konden niet worden geladen.")
            currentProfile.isFailure || persistedAdvice.isFailure -> CoachUiState.Error("Coachprofiel en advies konden niet worden geladen.")
            else -> CoachUiState.Success(
                overview = currentOverview.getOrThrow(),
                currentProfile = currentProfile.getOrThrow(),
                goalAdvice = temp.goalAdvice ?: persistedAdvice.getOrThrow()?.advice,
                savedGoalAdvice = persistedAdvice.getOrThrow(),
                generatedReport = temp.generatedReport,
                message = temp.message,
                isGeneratingAdvice = temp.isGeneratingAdvice,
                isGeneratingReport = temp.isGeneratingReport,
                profileDraft = draftState.draft,
                isProfileDraftDirty = draftState.isDirty,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoachUiState.Loading)

    fun retry() {
        reloads.update { it + 1 }
    }

    fun updateProfileDraft(draft: CoachProfileDraft) {
        savedStateHandle[CoachProfileDraftDirtyKey] = true
        savedStateHandle[CoachProfileDraftKey] = draft
    }

    fun generateGoalAdvice() {
        val draft = profileDraft.value
        val input = when (
            val result = validateGoalAdviceInput(
                draft.name,
                draft.height,
                draft.weight,
                draft.bodyFat,
                draft.age,
                draft.sex,
                draft.activityLevel,
                draft.goal,
                draft.manualCalorieTarget,
            )
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
                generateGoalAdviceUseCase(input.height, input.weight, input.bodyFat, input.age, input.sex, input.activityLevel, input.goal, input.manualCalorieTarget)
            }
            ephemeral.update {
                val advice = result.getOrNull()
                it.copy(
                    goalAdvice = advice,
                    goalAdviceInput = if (result.isSuccess) input else null,
                    message = when {
                        advice?.source == GoalAdviceSource.LOCAL_CALCULATION ->
                            advice.fallbackContext?.safeUserMessage() ?: "Lokale berekening gemaakt."
                        result.isSuccess -> "Advies gemaakt. Controleer het voordat je opslaat."
                        else -> result.exceptionOrNull()?.toAiUserMessage("Advies maken lukt nu niet.")
                    },
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
                val report = result.getOrNull()
                it.copy(
                    generatedReport = report,
                    message = when {
                        report?.source == WeeklyReportSource.LOCAL_FALLBACK -> report.localFallbackMessage()
                        result.isSuccess -> "Samenvatting bijgewerkt."
                        else -> result.exceptionOrNull()?.toAiUserMessage("Weekrapport maken lukt nu niet.")
                    },
                    isGeneratingReport = false,
                )
            }
        }
    }

    fun saveProfile() {
        val draft = profileDraft.value
        val input = when (
            val result = validateGoalAdviceInput(
                draft.name,
                draft.height,
                draft.weight,
                draft.bodyFat,
                draft.age,
                draft.sex,
                draft.activityLevel,
                draft.goal,
                draft.manualCalorieTarget,
            )
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
        val profile = UserProfile(
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
        )
        val savedAdvice = currentAdviceState.goalAdvice
            ?.takeIf { input == currentAdviceState.goalAdviceInput }
            ?.let { generatedAdvice ->
                SavedGoalAdvice(
                    advice = generatedAdvice,
                    profileFingerprint = profile.goalAdviceProfileFingerprint(),
                    savedAt = System.currentTimeMillis(),
                )
            }
        viewModelScope.launch {
            runCatching {
                saveUserProfileUseCase(profile, savedAdvice)
            }.onSuccess {
                if (profileDraft.value == draft) {
                    hydrateProfileDraft(profile)
                }
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

    private fun hydrateProfileDraft(profile: UserProfile?) {
        savedStateHandle[CoachProfileDraftKey] = profile.toCoachProfileDraft()
        savedStateHandle[CoachProfileDraftDirtyKey] = false
    }
}

private fun UserProfile?.toCoachProfileDraft(): CoachProfileDraft = this?.let { profile ->
    CoachProfileDraft(
        name = profile.name,
        age = profile.age.toString(),
        sex = profile.sex,
        height = profile.height.toString(),
        weight = profile.weight.toString(),
        bodyFat = profile.bodyFat.toString(),
        activityLevel = profile.activityLevel.toDutchActivityLevelLabel(),
        goal = profile.goal,
        manualCalorieTarget = profile.calorieTarget.takeIf { it > 0 }?.toString().orEmpty(),
    )
} ?: CoachProfileDraft()

private const val CoachProfileDraftKey = "coach_profile_draft"
private const val CoachProfileDraftDirtyKey = "coach_profile_draft_dirty"

private fun GoalAdviceInput.toDeterministicGoalAdvice(): GoalAdvice {
    val baseline = buildGoalBaseline(
        heightCm = height,
        weightKg = weight,
        bodyFat = bodyFat,
        age = age,
        sex = sex,
        activityLevel = activityLevel,
        goal = goal,
        manualCalorieTarget = manualCalorieTarget,
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
        calorieAdvice = "Onderhoud is berekend als BMR x activiteitsfactor. Je doelcalorieën zijn daarvan afgeleid.",
        macroAdvice = "Eiwit, vet en koolhydraten zijn verdeeld zodat de macrocalorieën rond je doel uitkomen.",
        activityExplanation = "Activiteitsfactor ${String.format(Locale.US, "%.3f", baseline.activityMultiplier)}: ${baseline.bmr} kcal x ${String.format(Locale.US, "%.3f", baseline.activityMultiplier)} = ${baseline.maintenanceCalories} kcal onderhoud.",
        attentionPoints = listOf("Dit blijft een lokale schatting totdat gewichtstrend, stappen en training genoeg context geven."),
        advice = "Gebruik deze waarden twee weken en stuur daarna bij op basis van trend en prestaties.",
        dataQuality = "Lokale berekening op basis van profieldata.",
        source = GoalAdviceSource.LOCAL_CALCULATION,
    )
}

@Composable
private fun WeekReportCard(report: WeeklyReportResult?, fallbackSummary: String) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        Text("AI-coach", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.amber, fontWeight = FontWeight.SemiBold)
        Text("Weekoverzicht", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.trainIqColors.amber.copy(alpha = 0.16f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Text(
                report?.source?.label() ?: "Lokale analyse",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        AdviceSurface {
            compactSentences(report?.summary ?: fallbackSummary, maxSentences = 2).forEach {
                Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
        }
        report?.let {
            if (it.wins.isNotEmpty()) BulletAdviceSurface("Hoogtepunten", it.wins)
            if (it.risks.isNotEmpty()) BulletAdviceSurface("Aandachtspunten", it.risks)
            AdviceSurface {
                Text("Volgende stap", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(it.nextWeekFocus, style = MaterialTheme.typography.bodyMedium)
            }
            if (it.rationaleBullets.isNotEmpty()) BulletAdviceSurface("Onderbouwing", it.rationaleBullets.take(3))
        }
    }
}

@Composable
private fun BulletAdviceSurface(title: String, items: List<String>) {
    AdviceSurface {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        items.forEach { point -> BulletText(point) }
    }
}

@Composable
private fun BulletText(point: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Text(cleanAdviceBulletText(point), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

private fun WeeklyReportResult.localFallbackMessage(): String {
    val fallbackContext = fallbackContext ?: return "Lokale samenvatting gemaakt."
    return summary
        .substringBefore(" Lokale samenvatting:", missingDelimiterValue = "")
        .ifBlank { fallbackContext.safeUserMessage() }
}

private fun WeeklyReportSource.label(): String = when (this) {
    WeeklyReportSource.GEMINI_2_5_FLASH -> "Gemini 2.5 Flash"
    WeeklyReportSource.OPENAI -> "OpenAI"
    WeeklyReportSource.LOCAL_FALLBACK -> "Lokale analyse"
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
    val manualCalorieTarget: Int? = null,
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
    manualCalorieTarget: String = "",
): GoalAdviceInput? {
    val input = buildValidatedProfileInput(name, height, weight, bodyFat, age, sex, activityLevel, goal) ?: return null
    val parsedManualCalorieTarget = parseManualCalorieTarget(manualCalorieTarget) ?: return null

    return GoalAdviceInput(
        name = input.name,
        height = input.height,
        weight = input.weight,
        bodyFat = input.bodyFat,
        age = input.age,
        sex = input.sex,
        activityLevel = input.activityLevel,
        goal = input.goal,
        manualCalorieTarget = parsedManualCalorieTarget.value,
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
    manualCalorieTarget: String = "",
): GoalAdviceInputValidationResult {
    return when (val result = validateProfileInput(name, height, weight, bodyFat, age, sex, activityLevel, goal)) {
        is ProfileInputValidationResult.Valid -> {
            val parsedManualCalorieTarget = parseManualCalorieTarget(manualCalorieTarget)
                ?: return GoalAdviceInputValidationResult.Invalid(ManualCalorieTargetErrorMessage)
            GoalAdviceInputValidationResult.Valid(
            GoalAdviceInput(
                name = result.input.name,
                height = result.input.height,
                weight = result.input.weight,
                bodyFat = result.input.bodyFat,
                age = result.input.age,
                sex = result.input.sex,
                activityLevel = result.input.activityLevel,
                goal = result.input.goal,
                manualCalorieTarget = parsedManualCalorieTarget.value,
            ),
            )
        }
        is ProfileInputValidationResult.Invalid -> GoalAdviceInputValidationResult.Invalid(result.error.message)
    }
}

private data class ParsedManualCalorieTarget(val value: Int?)

private fun parseManualCalorieTarget(value: String): ParsedManualCalorieTarget? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return ParsedManualCalorieTarget(null)
    val parsed = trimmed.toIntOrNull() ?: return null
    return parsed.takeIf { it in ManualCalorieTargetRange }?.let(::ParsedManualCalorieTarget)
}

private val ManualCalorieTargetRange = 1_200..6_000
private const val ManualCalorieTargetErrorMessage = "Calorie doel moet leeg zijn of tussen 1200 en 6000 kcal liggen."

@Composable
fun CoachRoute(
    windowWidthClass: TrainIqWindowWidthClass = TrainIqWindowWidthClass.Compact,
    viewModel: CoachViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CoachScreen(
        uiState = uiState,
        onGenerateAdvice = viewModel::generateGoalAdvice,
        onGenerateWeeklyReport = viewModel::generateWeeklyReport,
        onSaveProfile = viewModel::saveProfile,
        onProfileDraftChange = viewModel::updateProfileDraft,
        onDismissMessage = viewModel::clearMessage,
        onRetry = viewModel::retry,
    )
}

@Composable
fun CoachScreen(
    uiState: CoachUiState,
    onGenerateAdvice: () -> Unit,
    onGenerateWeeklyReport: () -> Unit,
    onSaveProfile: () -> Unit,
    onProfileDraftChange: (CoachProfileDraft) -> Unit,
    onDismissMessage: () -> Unit,
    onRetry: () -> Unit,
) {
    var manualCalorieTargetError by remember { mutableStateOf<String?>(null) }
    var profileInputError by remember { mutableStateOf<ProfileInputValidationError?>(null) }
    var selectedCoachTab by rememberSaveable { mutableStateOf(CoachSectionTab.Week.key) }
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    val draft = (uiState as? CoachUiState.Success)?.profileDraft ?: CoachProfileDraft()
    val name = draft.name
    val age = draft.age
    val sex = draft.sex
    val height = draft.height
    val weight = draft.weight
    val bodyFat = draft.bodyFat
    val activityLevel = draft.activityLevel
    val goal = draft.goal
    val manualCalorieTarget = draft.manualCalorieTarget
    val message = (uiState as? CoachUiState.Success)?.message
    LaunchedEffect(message, profileInputError) {
        val currentMessage = message ?: return@LaunchedEffect
        if (profileInputError == null) {
            snackbarHostState.showSnackbar(currentMessage)
            onDismissMessage()
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
    AnimatedContent(targetState = uiState::class, label = "coach-ui-state") {
        val state = uiState
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
                    item {
                        MessageCard(message = state.message)
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text("Opnieuw proberen") }
                    }
                }

                is CoachUiState.Success -> {
                    item {
                        CoachSectionTabSwitcher(
                            selectedTab = selectedCoachTab,
                            onSelectTab = { selectedCoachTab = it.key },
                        )
                    }
                    if (state.currentProfile == null && selectedCoachTab != CoachSectionTab.Goals.key) {
                        item {
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Text("Profiel instellen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                                Text("Vul je profiel en doel in onder Doelen. Daarna worden weekrapporten en advies zichtbaar op basis van jouw gegevens.")
                            }
                        }
                    } else if (selectedCoachTab == CoachSectionTab.Week.key) item {
                        AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) {
                                WeekReportCard(report = state.generatedReport, fallbackSummary = state.overview.weeklyReport)
                                Button(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onGenerateWeeklyReport()
                                    },
                                    enabled = !state.isGeneratingReport,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (state.isGeneratingReport) "Rapport maken..." else "Weekrapport maken")
                                }
                                if (state.isGeneratingReport) {
                                    ShimmerCardPlaceholder(lineCount = 3)
                                }
                                Text("Trainingsinzichten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                (state.overview.trainingInsights.ifEmpty { listOf("Nog geen inzichten. Sla een doel op en voltooi een workout.") }).forEach {
                                    BulletText(it)
                                }
                                Text("Voedingscoach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.trainIqColors.amber.copy(alpha = 0.14f),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ) {
                                    Text(
                                        text = state.overview.nutritionCoachMessage,
                                        modifier = Modifier.padding(MaterialTheme.spacing.medium),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                        }
                    }
                    if (selectedCoachTab == CoachSectionTab.Goals.key) item {
                        AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) {
                                Text("Doeladvies", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.amber, fontWeight = FontWeight.SemiBold)
                                Text("Profiel en doelen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                                TrainIqFormField(
                                    value = name,
                                    onValueChange = {
                                        onProfileDraftChange(draft.copy(name = it))
                                        profileInputError = null
                                    },
                                    label = "Naam",
                                    context = TrainIqFormFieldContext.Goal,
                                    isError = profileInputError.isFor(ProfileInputField.Name),
                                    errorText = profileInputError.errorTextFor(ProfileInputField.Name),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                TrainIqFormField(
                                    value = age,
                                    onValueChange = {
                                        onProfileDraftChange(draft.copy(age = it))
                                        profileInputError = null
                                    },
                                    label = "Leeftijd",
                                    context = TrainIqFormFieldContext.Goal,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = profileInputError.isFor(ProfileInputField.Age),
                                    errorText = profileInputError.errorTextFor(ProfileInputField.Age),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text("Biologische sekse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                                    BiologicalSex.entries.forEach { option ->
                                        FilterChip(
                                            modifier = Modifier.height(48.dp),
                                            selected = sex == option,
                                            onClick = {
                                                onProfileDraftChange(draft.copy(sex = option))
                                                profileInputError = null
                                            },
                                            label = { Text(option.displayLabel()) },
                                        )
                                    }
                                }
                                TrainIqFormField(
                                    value = height,
                                    onValueChange = {
                                        onProfileDraftChange(draft.copy(height = it))
                                        profileInputError = null
                                    },
                                    label = "Lengte (cm)",
                                    context = TrainIqFormFieldContext.Goal,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = profileInputError.isFor(ProfileInputField.Height),
                                    errorText = profileInputError.errorTextFor(ProfileInputField.Height),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                TrainIqFormField(
                                    value = weight,
                                    onValueChange = {
                                        onProfileDraftChange(draft.copy(weight = it))
                                        profileInputError = null
                                    },
                                    label = "Gewicht (kg)",
                                    context = TrainIqFormFieldContext.Goal,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = profileInputError.isFor(ProfileInputField.Weight),
                                    errorText = profileInputError.errorTextFor(ProfileInputField.Weight),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                TrainIqFormField(
                                    value = bodyFat,
                                    onValueChange = {
                                        onProfileDraftChange(draft.copy(bodyFat = it))
                                        profileInputError = null
                                    },
                                    label = "Vetpercentage %",
                                    context = TrainIqFormFieldContext.Goal,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = profileInputError.isFor(ProfileInputField.BodyFat),
                                    errorText = profileInputError.errorTextFor(ProfileInputField.BodyFat),
                                    modifier = Modifier.fillMaxWidth(),
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
                                            modifier = Modifier.height(48.dp),
                                            selected = activityLevel == option,
                                            onClick = {
                                                onProfileDraftChange(draft.copy(activityLevel = option))
                                                profileInputError = null
                                            },
                                            label = { Text(option) },
                                        )
                                    }
                                }
                                profileInputError.takeIf { it.isFor(ProfileInputField.ActivityLevel) }?.let { error ->
                                    Text(error.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                                TrainIqFormField(
                                    value = goal,
                                    onValueChange = {
                                        onProfileDraftChange(draft.copy(goal = it))
                                        profileInputError = null
                                    },
                                    label = "Doel",
                                    context = TrainIqFormFieldContext.Goal,
                                    isError = profileInputError.isFor(ProfileInputField.Goal),
                                    errorText = profileInputError.errorTextFor(ProfileInputField.Goal),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                TrainIqFormField(
                                    value = manualCalorieTarget,
                                    onValueChange = {
                                        onProfileDraftChange(draft.copy(manualCalorieTarget = it.take(4)))
                                        manualCalorieTargetError = null
                                        profileInputError = null
                                    },
                                    label = "Jouw calorie doel (kcal, optioneel)",
                                    context = TrainIqFormFieldContext.Nutrition,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = manualCalorieTargetError != null,
                                    errorText = manualCalorieTargetError,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    "Laat leeg voor automatisch. Vul bijvoorbeeld 2900 in als je bewust hoger wilt eten; TrainIQ berekent macro's automatisch mee.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Button(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (parseManualCalorieTarget(manualCalorieTarget) == null) {
                                            manualCalorieTargetError = ManualCalorieTargetErrorMessage
                                            onDismissMessage()
                                        } else {
                                            manualCalorieTargetError = null
                                            when (
                                                val result = validateProfileInput(name, height, weight, bodyFat, age, sex, activityLevel, goal)
                                            ) {
                                                is ProfileInputValidationResult.Valid -> {
                                                    profileInputError = null
                                                    onGenerateAdvice()
                                                }
                                                is ProfileInputValidationResult.Invalid -> {
                                                    profileInputError = result.error
                                                    onDismissMessage()
                                                }
                                            }
                                        }
                                    },
                                    enabled = !state.isGeneratingAdvice,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (state.isGeneratingAdvice) "Advies maken..." else "Advies maken")
                                }
                                if (state.isGeneratingAdvice) {
                                    ShimmerCardPlaceholder(lineCount = 4)
                                }
                                state.goalAdvice?.let {
                                    Button(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (parseManualCalorieTarget(manualCalorieTarget) == null) {
                                                manualCalorieTargetError = ManualCalorieTargetErrorMessage
                                                onDismissMessage()
                                            } else {
                                                manualCalorieTargetError = null
                                                when (
                                                    val result = validateProfileInput(name, height, weight, bodyFat, age, sex, activityLevel, goal)
                                                ) {
                                                    is ProfileInputValidationResult.Valid -> {
                                                        profileInputError = null
                                                        onSaveProfile()
                                                    }
                                                    is ProfileInputValidationResult.Invalid -> {
                                                        profileInputError = result.error
                                                        onDismissMessage()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Profiel en doelen opslaan")
                                    }
                                }
                        }
                    }
                    if (selectedCoachTab == CoachSectionTab.Advice.key) item {
                        AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) {
                            state.goalAdvice?.let { advice ->
                                GoalAdviceCard(
                                    advice = advice,
                                    activityLevel = activityLevel,
                                    isOutdated = state.savedGoalAdvice?.let { savedAdvice ->
                                        savedAdvice.profileFingerprint != state.currentProfile?.goalAdviceProfileFingerprint()
                                    } ?: false,
                                )
                            } ?: run {
                                Text("Nog geen advies", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                                Text("Maak eerst een doeladvies onder Doelen. Daarna zie je hier calorieën, macro's, actiepunten en datakwaliteit los van het formulier.")
                            }
                        }
                    }
                }
            }
        }
    }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(MaterialTheme.spacing.medium),
        )
    }
}

private enum class CoachSectionTab(val key: String, val label: String) {
    Week("week", "Week"),
    Goals("goals", "Doelen"),
    Advice("advice", "Advies"),
}

@Composable
private fun CoachSectionTabSwitcher(
    selectedTab: String,
    onSelectTab: (CoachSectionTab) -> Unit,
) {
    CompactSectionTabs(
        selectedKey = selectedTab,
        tabs = CoachSectionTab.entries.map { CompactSectionTabItem(it.key, it.label) },
        onSelectTab = { selected -> CoachSectionTab.entries.firstOrNull { it.key == selected.key }?.let(onSelectTab) },
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun GoalAdviceCard(advice: GoalAdvice, activityLevel: String, isOutdated: Boolean) {
    val difference = advice.calorieTarget - advice.maintenanceCalories
    val macroCalories = advice.proteinTarget * 4 + advice.carbsTarget * 4 + advice.fatTarget * 9
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
            Text("Voedingsadvies", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(
                    advice.source.label(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        AdviceSurface {
            compactSentences(advice.summary, maxSentences = 2).forEach { sentence ->
                Text(sentence, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            if (isOutdated) {
                Text(
                    "Dit opgeslagen advies is gebaseerd op een eerder profiel. Genereer en sla nieuw advies op om het te verversen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (advice.dataQuality.isNotBlank()) {
                Text(
                    compactSentences(advice.dataQuality, maxSentences = 1).joinToString(" "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AdviceSurface {
            Text("Calorieën", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                MetricPill("BMR", "${advice.bmr} kcal", accent = MaterialTheme.trainIqColors.amber)
                MetricPill("Berekend onderhoud", "${advice.maintenanceCalories} kcal", accent = MaterialTheme.trainIqColors.amber)
                MetricPill("Jouw doel", "${advice.calorieTarget} kcal", accent = MaterialTheme.trainIqColors.amber)
                MetricPill(goalAdviceEnergyDifferenceLabel(difference), "${kotlin.math.abs(difference)} kcal", accent = MaterialTheme.trainIqColors.amber)
            }
            compactSentences(advice.calorieAdvice.ifBlank { "Doelcalorieën zijn afgeleid van onderhoud en doel." }, maxSentences = 2).forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        AdviceSurface {
            Text("Macro's", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                MetricPill("Eiwit", "${advice.proteinTarget} g", accent = MaterialTheme.trainIqColors.amber)
                MetricPill("Koolhydraten", "${advice.carbsTarget} g", accent = MaterialTheme.trainIqColors.amber)
                MetricPill("Vet", "${advice.fatTarget} g", accent = MaterialTheme.trainIqColors.amber)
            }
            Text("Auto macro's: samen ongeveer $macroCalories kcal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            compactSentences(advice.macroAdvice.ifBlank { "Koolhydraten vullen de resterende calorieën aan als trainingsbrandstof." }, maxSentences = 2).forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        AdviceSurface {
            Text("Activiteit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            MetricPill(activityLevel.toDutchActivityLevelLabel(), "factor ${String.format(Locale.US, "%.3f", advice.activityMultiplier)}", accent = MaterialTheme.trainIqColors.amber)
            compactSentences(advice.activityExplanation.ifBlank { "Onderhoud = BMR x activiteitsfactor." }, maxSentences = 2).forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        AdviceSurface {
            Text("Advies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            compactSentences(advice.advice.ifBlank { advice.trainingFocus }, maxSentences = 2).forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            if (advice.trainingFocus.isNotBlank()) {
                Text("Trainingsfocus: ${advice.trainingFocus}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (advice.attentionPoints.isNotEmpty()) {
            AdviceSurface {
                Text("Aandachtspunten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                advice.attentionPoints.forEach { point -> BulletText(point) }
            }
        }
    }
}

@Composable
private fun AdviceSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.trainIqColors.amber.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            content = content,
        )
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun GoalAdviceSource.label(): String = when (this) {
    GoalAdviceSource.GEMINI_2_5_FLASH -> "Gemini 2.5 Flash"
    GoalAdviceSource.OPENAI -> "OpenAI"
    GoalAdviceSource.LOCAL_CALCULATION -> "Lokale berekening"
}

private fun String.toDutchActivityLevelLabel(): String = when (trim().lowercase(Locale.ROOT)) {
    "sedentary" -> "Zittend"
    "lightly active" -> "Licht actief"
    "moderately active" -> "Gemiddeld actief"
    "very active" -> "Zeer actief"
    "athlete" -> "Atleet"
    else -> this
}

internal fun goalAdviceEnergyDifferenceLabel(difference: Int): String = when {
    difference < 0 -> "Tekort"
    difference > 0 -> "Overschot"
    else -> "Balans"
}

internal fun cleanAdviceBulletText(point: String): String =
    point.trim().trimStart('-', '•', '*').trim()

private fun compactSentences(text: String, maxSentences: Int): List<String> {
    val cleaned = text.trim()
    if (cleaned.isBlank()) return emptyList()
    val sentences = cleaned
        .split(Regex("(?<=[.!?])\\s+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return sentences.take(maxSentences).ifEmpty { listOf(cleaned) }
}

private fun ProfileInputValidationError?.isFor(field: ProfileInputField): Boolean = this?.field == field

private fun BiologicalSex.displayLabel(): String = when (this) {
    BiologicalSex.MALE -> "Man"
    BiologicalSex.FEMALE -> "Vrouw"
}

private fun ProfileInputValidationError?.errorTextFor(field: ProfileInputField): String? {
    val error = takeIf { it.isFor(field) } ?: return null
    return error.message
}
