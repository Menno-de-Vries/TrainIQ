package com.trainiq.features.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.datastore.OnboardingPreferences
import com.trainiq.core.health.rememberHealthConnectPermissionRequester
import com.trainiq.core.theme.spacing
import com.trainiq.core.theme.trainIqColors
import com.trainiq.core.ui.AppCard
import com.trainiq.core.ui.PrimaryActionButton
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.SecondaryActionButton
import com.trainiq.core.ui.TrainIqFormField
import com.trainiq.core.ui.TrainIqFormFieldContext
import com.trainiq.domain.usecase.CompleteOnboardingUseCase
import com.trainiq.domain.usecase.ObserveOnboardingPreferencesUseCase
import com.trainiq.domain.usecase.SaveOnboardingPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OnboardingStep(val title: String, val subtitle: String) {
    WELCOME("Welkom bij TrainIQ", "Een rustige coachlaag boven je Health Connect-, training- en voedingsdata."),
    GOAL_TRAINING("Doel en training", "Kies genoeg context voor een eerste lokale coachingbasis."),
    HEALTH_CONNECT("Health Connect", "Stappen blijven Health Connect-first en worden live ververst zodra Home opent."),
    AI_PRIVACY("AI en privacy", "AI blijft opt-in, lokaal beheerd en alleen actief na jouw expliciete actie."),
    REMINDERS("Afronden", "Rond af met reminders en privacyverwachtingen. Alles blijft later aanpasbaar."),
}

data class OnboardingDraft(
    val goal: String = "",
    val experience: String = "",
    val trainingDays: Int = 3,
    val equipment: String = "",
    val sessionLengthMinutes: Int = 60,
    val constraints: String = "",
    val healthConnectAccepted: Boolean = false,
    val healthConnectSkipped: Boolean = false,
    val aiAccepted: Boolean = false,
    val aiSkipped: Boolean = false,
    val remindersEnabled: Boolean = false,
    val privacyAcknowledged: Boolean = false,
)

data class OnboardingContentState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val draft: OnboardingDraft = OnboardingDraft(),
) {
    val canGoBack: Boolean = step.ordinal > 0
    val canComplete: Boolean = draft.privacyAcknowledged
    val progress: Float = (step.ordinal + 1).toFloat() / OnboardingStep.entries.size.toFloat()
}

sealed interface OnboardingUiState {
    data object Loading : OnboardingUiState
    data class Success(val content: OnboardingContentState) : OnboardingUiState
    data class Error(val message: String) : OnboardingUiState
}

sealed interface OnboardingEvent {
    data object Next : OnboardingEvent
    data object Back : OnboardingEvent
    data class SelectGoal(val goal: String) : OnboardingEvent
    data class SelectExperience(val experience: String) : OnboardingEvent
    data class SetTrainingDays(val days: Int) : OnboardingEvent
    data class SelectEquipment(val equipment: String) : OnboardingEvent
    data class SetSessionLength(val minutes: Int) : OnboardingEvent
    data class SetConstraints(val constraints: String) : OnboardingEvent
    data object AcceptHealthConnect : OnboardingEvent
    data object SkipHealthConnect : OnboardingEvent
    data object AcceptAi : OnboardingEvent
    data object SkipAi : OnboardingEvent
    data class SetRemindersEnabled(val enabled: Boolean) : OnboardingEvent
    data object AcceptPrivacy : OnboardingEvent
    data object SkipAll : OnboardingEvent
}

fun reduceOnboardingState(
    state: OnboardingContentState,
    event: OnboardingEvent,
): OnboardingContentState = when (event) {
    OnboardingEvent.Next -> state.copy(step = OnboardingStep.entries.getOrElse(state.step.ordinal + 1) { state.step })
    OnboardingEvent.Back -> state.copy(step = OnboardingStep.entries.getOrElse(state.step.ordinal - 1) { state.step })
    is OnboardingEvent.SelectGoal -> state.copy(draft = state.draft.copy(goal = event.goal))
    is OnboardingEvent.SelectExperience -> state.copy(draft = state.draft.copy(experience = event.experience))
    is OnboardingEvent.SetTrainingDays -> state.copy(draft = state.draft.copy(trainingDays = event.days.coerceIn(1, 7)))
    is OnboardingEvent.SelectEquipment -> state.copy(draft = state.draft.copy(equipment = event.equipment))
    is OnboardingEvent.SetSessionLength -> state.copy(draft = state.draft.copy(sessionLengthMinutes = event.minutes.coerceIn(20, 120)))
    is OnboardingEvent.SetConstraints -> state.copy(draft = state.draft.copy(constraints = event.constraints.take(240)))
    OnboardingEvent.AcceptHealthConnect -> state.copy(draft = state.draft.copy(healthConnectAccepted = true, healthConnectSkipped = false))
    OnboardingEvent.SkipHealthConnect -> state.copy(draft = state.draft.copy(healthConnectAccepted = false, healthConnectSkipped = true))
    OnboardingEvent.AcceptAi -> state.copy(draft = state.draft.copy(aiAccepted = true, aiSkipped = false))
    OnboardingEvent.SkipAi -> state.copy(draft = state.draft.copy(aiAccepted = false, aiSkipped = true))
    is OnboardingEvent.SetRemindersEnabled -> state.copy(draft = state.draft.copy(remindersEnabled = event.enabled))
    OnboardingEvent.AcceptPrivacy -> state.copy(draft = state.draft.copy(privacyAcknowledged = true))
    OnboardingEvent.SkipAll -> state.copy(
        step = OnboardingStep.REMINDERS,
        draft = state.draft.copy(
            healthConnectSkipped = state.draft.healthConnectSkipped || !state.draft.healthConnectAccepted,
            aiSkipped = state.draft.aiSkipped || !state.draft.aiAccepted,
            privacyAcknowledged = true,
        ),
    )
}

fun OnboardingPreferences.toOnboardingContentState(): OnboardingContentState =
    OnboardingContentState(
        step = OnboardingStep.WELCOME,
        draft = OnboardingDraft(
            goal = goal,
            experience = experience,
            trainingDays = trainingDays,
            equipment = equipment,
            sessionLengthMinutes = sessionLengthMinutes,
            constraints = constraints,
            healthConnectAccepted = healthConnectAccepted,
            healthConnectSkipped = healthConnectSkipped,
            aiAccepted = aiAccepted,
            aiSkipped = aiSkipped,
            remindersEnabled = remindersEnabled,
            privacyAcknowledged = privacyAcknowledged,
        ),
    )

fun OnboardingContentState.toPreferences(completed: Boolean = false): OnboardingPreferences =
    OnboardingPreferences(
        completed = completed,
        goal = draft.goal,
        experience = draft.experience,
        trainingDays = draft.trainingDays,
        equipment = draft.equipment,
        sessionLengthMinutes = draft.sessionLengthMinutes,
        constraints = draft.constraints,
        healthConnectAccepted = draft.healthConnectAccepted,
        healthConnectSkipped = draft.healthConnectSkipped,
        aiAccepted = draft.aiAccepted,
        aiSkipped = draft.aiSkipped,
        remindersEnabled = draft.remindersEnabled,
        privacyAcknowledged = draft.privacyAcknowledged,
    )

data class OnboardingSetupItem(
    val title: String,
    val body: String,
)

fun onboardingSetupItems(preferences: OnboardingPreferences): List<OnboardingSetupItem> = buildList {
    if (preferences.goal.isBlank()) {
        add(OnboardingSetupItem("Doel kiezen", "Leg je trainingsdoel vast voor betere lokale adviezen."))
    }
    if (preferences.healthConnectSkipped || !preferences.healthConnectAccepted) {
        add(OnboardingSetupItem("Health Connect koppelen", "Lees stappen, slaap en training wanneer jij toestemming geeft."))
    }
    if (preferences.aiSkipped || !preferences.aiAccepted) {
        add(OnboardingSetupItem("AI-coach instellen", "Schakel Gemini/OpenAI alleen in als je expliciete AI-acties wilt gebruiken."))
    }
    if (!preferences.remindersEnabled) {
        add(OnboardingSetupItem("Herinneringen kiezen", "Laat TrainIQ je rustig herinneren aan food-logs en trainingen."))
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val observeOnboardingPreferencesUseCase: ObserveOnboardingPreferencesUseCase,
    private val saveOnboardingPreferencesUseCase: SaveOnboardingPreferencesUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Loading)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = runCatching {
                OnboardingUiState.Success(observeOnboardingPreferencesUseCase().first().toOnboardingContentState())
            }.getOrElse { throwable ->
                OnboardingUiState.Error(throwable.message ?: "Onboarding kan nu niet worden geladen.")
            }
        }
    }

    fun dispatch(event: OnboardingEvent) {
        val current = (_uiState.value as? OnboardingUiState.Success)?.content ?: return
        val updated = reduceOnboardingState(current, event)
        _uiState.value = OnboardingUiState.Success(updated)
        viewModelScope.launch(Dispatchers.IO) {
            saveOnboardingPreferencesUseCase(updated.toPreferences(completed = false))
        }
    }

    fun complete(onComplete: () -> Unit) {
        val current = (_uiState.value as? OnboardingUiState.Success)?.content ?: return
        val completed = current.copy(draft = current.draft.copy(privacyAcknowledged = true))
        _uiState.value = OnboardingUiState.Success(completed)
        viewModelScope.launch(Dispatchers.IO) {
            completeOnboardingUseCase(completed.toPreferences(completed = true))
            launch(Dispatchers.Main) { onComplete() }
        }
    }
}

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val requestHealthPermission = rememberHealthConnectPermissionRequester {
        viewModel.dispatch(OnboardingEvent.AcceptHealthConnect)
    }

    OnboardingScreen(
        uiState = uiState,
        onEvent = viewModel::dispatch,
        onRequestHealthConnect = {
            viewModel.dispatch(OnboardingEvent.AcceptHealthConnect)
            requestHealthPermission()
        },
        onFinish = { viewModel.complete(onFinished) },
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onEvent: (OnboardingEvent) -> Unit,
    onRequestHealthConnect: () -> Unit,
    onFinish: () -> Unit,
) {
    when (uiState) {
        OnboardingUiState.Loading -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(MaterialTheme.spacing.medium),
            ) {
                item { ScreenHeader(title = "TrainIQ", subtitle = "Onboarding laden") }
                item { AppCard { Text("Je setup wordt voorbereid.") } }
            }
        }
        is OnboardingUiState.Error -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(MaterialTheme.spacing.medium),
            ) {
                item { ScreenHeader(title = "TrainIQ", subtitle = "Onboarding") }
                item { AppCard(accent = MaterialTheme.colorScheme.error) { Text(uiState.message) } }
            }
        }
        is OnboardingUiState.Success -> {
            OnboardingContent(
                state = uiState.content,
                onEvent = onEvent,
                onRequestHealthConnect = onRequestHealthConnect,
                onFinish = onFinish,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingContent(
    state: OnboardingContentState,
    onEvent: (OnboardingEvent) -> Unit,
    onRequestHealthConnect: () -> Unit,
    onFinish: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.medium,
            top = MaterialTheme.spacing.medium,
            end = MaterialTheme.spacing.medium,
            bottom = 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        item {
            ScreenHeader(title = "TrainIQ", subtitle = "Eerste setup")
        }
        item {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(220)) togetherWith
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(220))
                },
                label = "onboarding-step",
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep()
                    OnboardingStep.GOAL_TRAINING -> GoalTrainingStep(state.draft, onEvent)
                    OnboardingStep.HEALTH_CONNECT -> HealthConnectStep(state.draft, onEvent, onRequestHealthConnect)
                    OnboardingStep.AI_PRIVACY -> AiPrivacyStep(state.draft, onEvent)
                    OnboardingStep.REMINDERS -> RemindersStep(state.draft, onEvent)
                }
            }
        }
        item {
            OnboardingActions(
                state = state,
                onEvent = onEvent,
                onFinish = onFinish,
            )
        }
    }
}

@Composable
private fun WelcomeStep() {
    StepCard(
        icon = Icons.Default.AutoAwesome,
        title = OnboardingStep.WELCOME.title,
        subtitle = OnboardingStep.WELCOME.subtitle,
        accent = MaterialTheme.trainIqColors.amber,
    ) {
        Text("TrainIQ blijft local-first: jij logt training en voeding, Health Connect levert passieve signalen, en AI blijft opt-in.")
        Text("Deze setup is volledig skippable. Overgeslagen onderdelen blijven later zichtbaar in Instellingen.")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalTrainingStep(
    draft: OnboardingDraft,
    onEvent: (OnboardingEvent) -> Unit,
) {
    StepCard(
        icon = Icons.Default.FitnessCenter,
        title = OnboardingStep.GOAL_TRAINING.title,
        subtitle = OnboardingStep.GOAL_TRAINING.subtitle,
        accent = MaterialTheme.colorScheme.primary,
    ) {
        ChoiceChips(
            label = "Doel",
            options = listOf("Spieropbouw", "Vetverlies", "Recomp", "Algemene gezondheid"),
            selected = draft.goal,
            onSelected = { onEvent(OnboardingEvent.SelectGoal(it)) },
        )
        ChoiceChips(
            label = "Ervaring",
            options = listOf("Beginner", "Gemiddeld", "Gevorderd"),
            selected = draft.experience,
            onSelected = { onEvent(OnboardingEvent.SelectExperience(it)) },
        )
        ChoiceChips(
            label = "Dagen per week",
            options = listOf("2", "3", "4", "5", "6"),
            selected = draft.trainingDays.toString(),
            onSelected = { onEvent(OnboardingEvent.SetTrainingDays(it.toInt())) },
        )
        ChoiceChips(
            label = "Materiaal",
            options = listOf("Gym", "Thuis", "Dumbbells", "Bodyweight"),
            selected = draft.equipment,
            onSelected = { onEvent(OnboardingEvent.SelectEquipment(it)) },
        )
        ChoiceChips(
            label = "Sessieduur",
            options = listOf("45", "60", "75", "90"),
            selected = draft.sessionLengthMinutes.toString(),
            onSelected = { onEvent(OnboardingEvent.SetSessionLength(it.toInt())) },
        )
        TrainIqFormField(
            value = draft.constraints,
            onValueChange = { onEvent(OnboardingEvent.SetConstraints(it)) },
            label = "Blessures, voorkeuren of beperkingen",
            modifier = Modifier.fillMaxWidth(),
            context = TrainIqFormFieldContext.Goal,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = false,
        )
    }
}

@Composable
private fun HealthConnectStep(
    draft: OnboardingDraft,
    onEvent: (OnboardingEvent) -> Unit,
    onRequestHealthConnect: () -> Unit,
) {
    StepCard(
        icon = Icons.Default.HealthAndSafety,
        title = OnboardingStep.HEALTH_CONNECT.title,
        subtitle = OnboardingStep.HEALTH_CONNECT.subtitle,
        accent = MaterialTheme.trainIqColors.mint,
    ) {
        Text("TrainIQ leest stappen via Health Connect aggregate steps. Als Samsung Health meer stappen toont, moet Samsung Health die data eerst naar Health Connect synchroniseren.")
        Text("Je kunt later altijd via Instellingen terug naar Health Connect en app-permissies.")
        PrimaryActionButton(onClick = onRequestHealthConnect, modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.mint) {
            Text(if (draft.healthConnectAccepted) "Health Connect opnieuw openen" else "Health Connect koppelen")
        }
        SecondaryActionButton(onClick = { onEvent(OnboardingEvent.SkipHealthConnect) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (draft.healthConnectSkipped) "Overgeslagen" else "Nu overslaan")
        }
    }
}

@Composable
private fun AiPrivacyStep(
    draft: OnboardingDraft,
    onEvent: (OnboardingEvent) -> Unit,
) {
    StepCard(
        icon = Icons.Default.PrivacyTip,
        title = OnboardingStep.AI_PRIVACY.title,
        subtitle = OnboardingStep.AI_PRIVACY.subtitle,
        accent = MaterialTheme.colorScheme.tertiary,
    ) {
        Text("AI staat standaard uit. Als je later een sleutel opslaat, starten verzoeken alleen door expliciete acties zoals advies, rapporten of scans.")
        Text("TrainIQ gebruikt JSON-contracten en lokale fallback wanneer AI uitstaat, offline is of geen geldige output geeft.")
        PrimaryActionButton(onClick = { onEvent(OnboardingEvent.AcceptAi) }, modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.tertiary) {
            Text(if (draft.aiAccepted) "AI later instellen gekozen" else "AI later instellen")
        }
        SecondaryActionButton(onClick = { onEvent(OnboardingEvent.SkipAi) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (draft.aiSkipped) "AI overgeslagen" else "AI overslaan")
        }
    }
}

@Composable
private fun RemindersStep(
    draft: OnboardingDraft,
    onEvent: (OnboardingEvent) -> Unit,
) {
    StepCard(
        icon = Icons.Default.Notifications,
        title = OnboardingStep.REMINDERS.title,
        subtitle = OnboardingStep.REMINDERS.subtitle,
        accent = MaterialTheme.colorScheme.secondary,
    ) {
        Text("Reminders zijn lokale hints voor food-logs en trainingen. Geen cloudaccount, geen gezondheidsupload.")
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            FilterChip(
                selected = draft.remindersEnabled,
                onClick = { onEvent(OnboardingEvent.SetRemindersEnabled(true)) },
                label = { Text("Reminders aan") },
            )
            FilterChip(
                selected = !draft.remindersEnabled,
                onClick = { onEvent(OnboardingEvent.SetRemindersEnabled(false)) },
                label = { Text("Reminders uit") },
            )
        }
        SecondaryActionButton(
            onClick = { onEvent(OnboardingEvent.AcceptPrivacy) },
            modifier = Modifier.fillMaxWidth(),
            accent = if (draft.privacyAcknowledged) MaterialTheme.trainIqColors.mint else MaterialTheme.colorScheme.primary,
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Text(if (draft.privacyAcknowledged) "Privacyverwachting bevestigd" else "Local-first privacy bevestigen")
        }
    }
}

@Composable
private fun StepCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppCard(accent = accent, elevated = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, contentDescription = null, tint = accent)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            content = content,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceChips(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option) },
                    modifier = Modifier.heightIn(min = 44.dp),
                )
            }
        }
    }
}

@Composable
private fun OnboardingActions(
    state: OnboardingContentState,
    onEvent: (OnboardingEvent) -> Unit,
    onFinish: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        PrimaryActionButton(
            onClick = {
                if (state.step == OnboardingStep.REMINDERS) {
                    onFinish()
                } else {
                    onEvent(OnboardingEvent.Next)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.step != OnboardingStep.REMINDERS || state.canComplete,
        ) {
            Text(if (state.step == OnboardingStep.REMINDERS) "Setup afronden" else "Verder")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            OutlinedButton(
                onClick = { onEvent(OnboardingEvent.Back) },
                enabled = state.canGoBack,
                modifier = Modifier.weight(1f),
            ) {
                Text("Terug")
            }
            TextButton(
                onClick = {
                    onEvent(OnboardingEvent.SkipAll)
                    onFinish()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (state.step == OnboardingStep.REMINDERS) "Skip en afronden" else "Overslaan")
            }
        }
    }
}
