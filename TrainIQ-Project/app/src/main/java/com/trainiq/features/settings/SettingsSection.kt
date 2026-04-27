package com.trainiq.features.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.trainiq.core.theme.spacing
import androidx.health.connect.client.HealthConnectClient
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.BuildConfig
import com.trainiq.ai.services.GoalAdvisorService
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.WorkoutFeedbackPreferences
import com.trainiq.core.health.HealthConnectRefreshOnResume
import com.trainiq.core.health.rememberHealthConnectPermissionRequester
import com.trainiq.core.ui.MessageCard
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.SectionCard
import com.trainiq.core.ui.bringIntoViewOnFocus
import com.trainiq.core.ui.clearFocusOnScrollOrDrag
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.diagnostics.PerformanceSessionStore
import com.trainiq.core.theme.ThemeMode
import com.trainiq.data.local.TrainIqLocalStore
import com.trainiq.features.profile.DefaultProfileActivityLevel
import com.trainiq.features.profile.ProfileActivityLevels
import com.trainiq.features.profile.ProfileInputField
import com.trainiq.features.profile.ProfileInputValidationError
import com.trainiq.features.profile.ProfileInputValidationResult
import com.trainiq.features.profile.validateProfileInput
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.usecase.GetHealthConnectStatusUseCase
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
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
    private val getHealthConnectStatusUseCase: GetHealthConnectStatusUseCase,
    private val goalAdvisorService: GoalAdvisorService,
    private val localStore: TrainIqLocalStore,
    private val performanceSessionStore: PerformanceSessionStore,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
    val aiPreferences: StateFlow<AiPreferences> = preferencesRepository.aiPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiPreferences(false, ""))
    val workoutFeedbackPreferences: StateFlow<WorkoutFeedbackPreferences> = preferencesRepository.workoutFeedbackPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutFeedbackPreferences())
    val profile: StateFlow<UserProfile?> = observeUserProfileUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _healthStatus = MutableStateFlow(
        HealthConnectStatus(
            state = HealthConnectState.ERROR,
            message = "Health Connect-status wordt geladen.",
        ),
    )
    val healthStatus: StateFlow<HealthConnectStatus> = _healthStatus.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refreshHealthConnectStatus()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAiEnabled(enabled)
            _message.value = if (enabled) {
                "AI-functies ingeschakeld. Verzoeken starten alleen na jouw expliciete actie."
            } else {
                "AI-functies uitgeschakeld. TrainIQ blijft handmatig werken."
            }
        }
    }

    fun saveGeminiKey(apiKey: String) {
        viewModelScope.launch {
            if (apiKey.isBlank()) {
                _message.value = "Voer eerst een Gemini API-sleutel in."
                return@launch
            }
            preferencesRepository.saveGeminiApiKey(apiKey)
            _message.value = "Gemini API-sleutel opgeslagen."
        }
    }

    fun clearGeminiKey() {
        viewModelScope.launch {
            preferencesRepository.clearGeminiApiKey()
            preferencesRepository.setAiEnabled(false)
            _message.value = "Gemini API-sleutel verwijderd en AI uitgeschakeld."
        }
    }

    fun setRestTimerSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRestTimerSoundEnabled(enabled)
            _message.value = if (enabled) "Rusttimer-geluid ingeschakeld." else "Rusttimer-geluid uitgeschakeld."
        }
    }

    fun setWorkoutHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setWorkoutHapticsEnabled(enabled)
            _message.value = if (enabled) "Workouttrillingen ingeschakeld." else "Workouttrillingen uitgeschakeld."
        }
    }

    fun refreshHealthConnectStatus() {
        viewModelScope.launch {
            _healthStatus.value = runCatching { getHealthConnectStatusUseCase() }
                .getOrElse {
                    HealthConnectStatus(
                        state = HealthConnectState.ERROR,
                        message = "Health Connect kan nu niet worden bijgewerkt.",
                    )
                }
        }
    }

    fun saveProfile(name: String, age: String, sex: BiologicalSex, height: String, weight: String, bodyFat: String, activityLevel: String, goal: String) {
        val input = when (
            val result = validateProfileInput(
                name = name,
                height = height,
                weight = weight,
                bodyFat = bodyFat,
                age = age,
                sex = sex,
                activityLevel = activityLevel,
                goal = goal,
            )
        ) {
            is ProfileInputValidationResult.Valid -> result.input
            is ProfileInputValidationResult.Invalid -> {
                _message.value = result.error.message
                return
            }
        }
        val advice = goalAdvisorService.deterministicGoalAdvice(
            height = input.height,
            weight = input.weight,
            bodyFat = input.bodyFat,
            age = input.age,
            sex = input.sex,
            activityLevel = input.activityLevel,
            goal = input.goal,
        )
        viewModelScope.launch {
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
            _message.value = "Profiel opgeslagen. Dashboarddoelen bijgewerkt."
        }
    }

    fun resetProfile() {
        viewModelScope.launch {
            localStore.clearProfile()
            _message.value = "Profiel verwijderd."
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            localStore.clearAll()
            preferencesRepository.clearLocalPrivateData()
            performanceSessionStore.clearAll()
            _healthStatus.value = HealthConnectStatus(
                state = HealthConnectState.PERMISSION_REQUIRED,
                message = "Lokale data is gewist. Verbind Health Connect opnieuw als je die koppeling wilt gebruiken.",
            )
            _message.value = "Alle lokale appdata, AI-sleutel, voorkeuren en Health Connect-cache zijn gewist."
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun maskedKey(): String {
        val key = aiPreferences.value.apiKey
        if (key.isBlank()) return "Niet ingesteld"
        return if (key.length <= 8) "********" else "${key.take(4)}****${key.takeLast(4)}"
    }
}

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val aiPreferences by viewModel.aiPreferences.collectAsStateWithLifecycle()
    val workoutFeedbackPreferences by viewModel.workoutFeedbackPreferences.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val healthStatus by viewModel.healthStatus.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val requestHealthPermission = rememberHealthConnectPermissionRequester(viewModel::refreshHealthConnectStatus)
    HealthConnectRefreshOnResume(viewModel::refreshHealthConnectStatus, refreshOnFirstResume = false)

    SettingsScreen(
        themeMode = themeMode,
        aiPreferences = aiPreferences,
        workoutFeedbackPreferences = workoutFeedbackPreferences,
        maskedApiKey = viewModel.maskedKey(),
        profile = profile,
        healthStatus = healthStatus,
        message = message,
        onThemeSelected = viewModel::setThemeMode,
        onToggleAi = viewModel::setAiEnabled,
        onToggleRestTimerSound = viewModel::setRestTimerSoundEnabled,
        onToggleWorkoutHaptics = viewModel::setWorkoutHapticsEnabled,
        onSaveApiKey = viewModel::saveGeminiKey,
        onClearApiKey = viewModel::clearGeminiKey,
        onSaveProfile = viewModel::saveProfile,
        onResetProfile = viewModel::resetProfile,
        onClearAllData = viewModel::clearAllData,
        onDismissMessage = viewModel::clearMessage,
        onRequestHealthPermission = requestHealthPermission,
        onRefreshHealth = viewModel::refreshHealthConnectStatus,
        onOpenHealthSettings = {
            val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
            if (!context.startActivityIfResolvable(intent)) {
                viewModel.refreshHealthConnectStatus()
            }
        },
        onOpenHealthInstall = {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"))
            if (!context.startActivityIfResolvable(marketIntent) && !context.startActivityIfResolvable(webIntent)) {
                viewModel.refreshHealthConnectStatus()
            }
        },
    )
}

private enum class PendingDestructiveSettingsAction {
    CLEAR_API_KEY,
    RESET_PROFILE,
    CLEAR_ALL_DATA,
}

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    aiPreferences: AiPreferences,
    workoutFeedbackPreferences: WorkoutFeedbackPreferences,
    maskedApiKey: String,
    profile: UserProfile?,
    healthStatus: HealthConnectStatus,
    message: String?,
    onThemeSelected: (ThemeMode) -> Unit,
    onToggleAi: (Boolean) -> Unit,
    onToggleRestTimerSound: (Boolean) -> Unit,
    onToggleWorkoutHaptics: (Boolean) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onSaveProfile: (String, String, BiologicalSex, String, String, String, String, String) -> Unit,
    onResetProfile: () -> Unit,
    onClearAllData: () -> Unit,
    onDismissMessage: () -> Unit,
    onRequestHealthPermission: () -> Unit,
    onRefreshHealth: () -> Unit,
    onOpenHealthSettings: () -> Unit,
    onOpenHealthInstall: () -> Unit,
) {
    var apiKey by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(profile?.name.orEmpty()) }
    var age by remember { mutableStateOf(profile?.age?.toString() ?: "30") }
    var sex by remember { mutableStateOf(profile?.sex ?: BiologicalSex.MALE) }
    var height by remember { mutableStateOf(profile?.height?.toString().orEmpty()) }
    var weight by remember { mutableStateOf(profile?.weight?.toString().orEmpty()) }
    var bodyFat by remember { mutableStateOf(profile?.bodyFat?.toString().orEmpty()) }
    var activityLevel by remember {
        mutableStateOf(profile?.activityLevel?.takeIf { it in ProfileActivityLevels } ?: DefaultProfileActivityLevel)
    }
    var goal by remember { mutableStateOf(profile?.goal.orEmpty()) }
    var profileInputError by remember { mutableStateOf<ProfileInputValidationError?>(null) }
    var pendingDestructiveAction by remember { mutableStateOf<PendingDestructiveSettingsAction?>(null) }

    LaunchedEffect(profile) {
        name = profile?.name.orEmpty()
        age = profile?.age?.toString() ?: "30"
        sex = profile?.sex ?: BiologicalSex.MALE
        height = profile?.height?.toString().orEmpty()
        weight = profile?.weight?.toString().orEmpty()
        bodyFat = profile?.bodyFat?.toString().orEmpty()
        activityLevel = profile?.activityLevel?.takeIf { it in ProfileActivityLevels } ?: DefaultProfileActivityLevel
        goal = profile?.goal.orEmpty()
        profileInputError = null
    }

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
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
    ) {
        item { ScreenHeader(title = "Instellingen", subtitle = "Profiel, Health Connect en voorkeuren") }
        item {
            SectionCard(title = "Snelle status") {
                Text("Thema: ${themeMode.displayLabel()}")
                Text("AI: ${if (aiPreferences.enabled && aiPreferences.apiKey.isNotBlank()) "Klaar voor expliciet gebruik" else "Alleen handmatig"}")
                Text("Health Connect: ${healthStatusLabel(healthStatus)}")
            }
        }
        if (profileInputError == null) message?.let {
            item {
                MessageCard(message = it, onDismiss = onDismissMessage)
            }
        }
        item {
            SectionCard(title = "Weergave") {
                Text("Themamodus")
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeSelected(mode) },
                        label = { Text(mode.displayLabel()) },
                    )
                }
            }
        }
        item {
            SectionCard(title = "Workoutfeedback") {
                FeedbackToggleRow(
                    title = "Rusttimer-geluid",
                    body = "Speel een kort geluid wanneer de rusttijd voorbij is.",
                    checked = workoutFeedbackPreferences.restTimerSoundEnabled,
                    onCheckedChange = onToggleRestTimerSound,
                )
                FeedbackToggleRow(
                    title = "Workouttrillingen",
                    body = "Gebruik een subtiele trilling bij voltooide sets of afgelopen rusttijd.",
                    checked = workoutFeedbackPreferences.workoutHapticsEnabled,
                    onCheckedChange = onToggleWorkoutHaptics,
                )
            }
        }
        item {
            SectionCard(title = "AI / Gemini") {
                Text("AI wordt alleen gebruikt nadat jij het inschakelt. TrainIQ doet geen AI-aanvragen op de achtergrond.")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("AI-functies inschakelen", fontWeight = FontWeight.SemiBold)
                        Text("Alleen expliciete acties zoals maaltijdanalyse, doeladvies, AI-rapport en workoutterugblik kunnen verzoeken starten.")
                    }
                    Switch(checked = aiPreferences.enabled, onCheckedChange = onToggleAi)
                }
                Text("Huidige sleutel: $maskedApiKey")
                OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("Gemini API-sleutel") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    Button(onClick = {
                        onSaveApiKey(apiKey)
                        apiKey = ""
                    }) { Text(if (aiPreferences.apiKey.isBlank()) "Sleutel opslaan" else "Sleutel bijwerken") }
                    TextButton(onClick = { pendingDestructiveAction = PendingDestructiveSettingsAction.CLEAR_API_KEY }) { Text("Sleutel verwijderen") }
                }
                Text("Gemini kan API-kosten veroorzaken. Laat AI uitgeschakeld tenzij je het wilt gebruiken.")
                Text("Gebruikt door: maaltijdanalyse, workoutterugblik, wekelijks AI-rapport en doeladviseur.")
                Text("Status: ${if (aiPreferences.enabled && aiPreferences.apiKey.isNotBlank()) "Ingeschakeld en klaar" else if (aiPreferences.apiKey.isBlank()) "Geen sleutel ingesteld" else "Uitgeschakeld"}")
            }
        }
        item {
            SectionCard(title = "Health Connect") {
                Text("Status: ${healthStatusLabel(healthStatus)}")
                Text(healthStatus.message)
                healthStatus.lastSyncedAt?.let {
                    Text("Laatst gecontroleerd: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))}")
                }
                healthStatus.stepsToday?.let { steps ->
                    Text(if (steps > 0) "Stappen vandaag beschikbaar: $steps" else "Verbonden, maar er is vandaag nog geen stapdata teruggekomen.")
                }
                healthStatus.averageHeartRateBpm?.let { bpm ->
                    Text("Gemiddelde hartslag vandaag: $bpm bpm")
                }
                healthStatus.latestHeartRateBpm?.let { bpm ->
                    Text("Laatste hartslagmeting: $bpm bpm")
                }
                healthStatus.sleepMinutes?.takeIf { it > 0 }?.let { minutes ->
                    Text("Recente slaap: ${minutes / 60}u ${minutes % 60}m over ${healthStatus.sleepSessionCount} sessie(s)")
                }
                when (healthStatus.state) {
                    HealthConnectState.PERMISSION_REQUIRED -> Text("Geef toegang zodat TrainIQ je dagelijkse stappen, hartslag en slaap kan lezen.")
                    HealthConnectState.PROVIDER_MISSING -> Text("Installeer of update Health Connect eerst, kom daarna terug en vernieuw.")
                    else -> Unit
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small), verticalAlignment = Alignment.CenterVertically) {
                    when (healthStatus.state) {
                        HealthConnectState.PROVIDER_MISSING -> Button(onClick = onOpenHealthInstall) { Text("Installeren / bijwerken") }
                        HealthConnectState.PERMISSION_REQUIRED -> Button(onClick = onRequestHealthPermission) { Text("Toegang geven") }
                        HealthConnectState.CONNECTED, HealthConnectState.NO_DATA -> Button(onClick = onOpenHealthSettings) { Text("Health Connect openen") }
                        HealthConnectState.UNSUPPORTED -> Text("Niet ondersteund op dit apparaat.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HealthConnectState.ERROR -> Text("Health Connect kan nu niet worden gelezen.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onRefreshHealth) { Text("Vernieuwen") }
                }
            }
        }
        item {
            SectionCard(title = "Profiel & doelen") {
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
                    label = { Text("Hoofddoel") },
                    isError = profileInputError.isFor(ProfileInputField.Goal),
                    supportingText = profileInputError.supportingTextFor(ProfileInputField.Goal),
                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                )
                Button(
                    onClick = {
                        when (
                            val result = validateProfileInput(name, height, weight, bodyFat, age, sex, activityLevel, goal)
                        ) {
                            is ProfileInputValidationResult.Valid -> {
                                profileInputError = null
                                onSaveProfile(name, age, sex, height, weight, bodyFat, activityLevel, goal)
                            }
                            is ProfileInputValidationResult.Invalid -> {
                                profileInputError = result.error
                                onDismissMessage()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Profiel opslaan") }
                TextButton(
                    onClick = { pendingDestructiveAction = PendingDestructiveSettingsAction.RESET_PROFILE },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Profiel resetten") }
                profile?.let {
                    Text("Huidige dashboarddoelen: ${it.calorieTarget} kcal en ${it.proteinTarget} g eiwit.")
                }
            }
        }
        item {
            SectionCard(title = "Gegevens / opslag") {
                Text("Opslagmodus: lokale JSON-opslag")
                Text("AI-sleutelopslag: lokale appvoorkeuren")
                Text("Health Connect-cache: opgeslagen wijzigingstoken plus lichte syncsnapshot")
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    TextButton(onClick = { pendingDestructiveAction = PendingDestructiveSettingsAction.CLEAR_API_KEY }) { Text("AI-sleutel wissen") }
                    TextButton(onClick = { pendingDestructiveAction = PendingDestructiveSettingsAction.CLEAR_ALL_DATA }) { Text("Lokale data wissen") }
                }
            }
        }
        item {
            SectionCard(title = "Over") {
                Text("Appversie: ${BuildConfig.VERSION_NAME}")
                Text("AI ingeschakeld: ${if (aiPreferences.enabled) "Ja" else "Nee"}")
                Text("Health Connect: ${healthStatusLabel(healthStatus)}")
                Text("Ontworpen als handmatige training- en voedings-MVP.")
            }
        }
    }

    pendingDestructiveAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingDestructiveAction = null },
            title = {
                Text(
                    when (action) {
                        PendingDestructiveSettingsAction.CLEAR_API_KEY -> "API-sleutel verwijderen?"
                        PendingDestructiveSettingsAction.RESET_PROFILE -> "Profiel resetten?"
                        PendingDestructiveSettingsAction.CLEAR_ALL_DATA -> "Alle lokale appdata wissen?"
                    },
                )
            },
            text = {
                Text(
                    when (action) {
                        PendingDestructiveSettingsAction.CLEAR_API_KEY -> "Je Gemini API-sleutel wordt verwijderd en AI wordt uitgeschakeld. Deze actie kan niet automatisch ongedaan worden gemaakt."
                        PendingDestructiveSettingsAction.RESET_PROFILE -> "Je profiel en dashboarddoelen worden verwijderd. Trainingen en voeding blijven staan. Deze actie kan niet automatisch ongedaan worden gemaakt."
                        PendingDestructiveSettingsAction.CLEAR_ALL_DATA -> "TrainIQ wist lokale trainingen, voeding, profiel, AI-sleutel, voorkeuren en Health Connect-cache op dit apparaat. Health Connect-permissies zelf beheer je in Android. Deze actie kan niet automatisch ongedaan worden gemaakt."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (action) {
                            PendingDestructiveSettingsAction.CLEAR_API_KEY -> onClearApiKey()
                            PendingDestructiveSettingsAction.RESET_PROFILE -> onResetProfile()
                            PendingDestructiveSettingsAction.CLEAR_ALL_DATA -> onClearAllData()
                        }
                        pendingDestructiveAction = null
                    },
                ) { Text("Bevestigen") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDestructiveAction = null }) { Text("Annuleren") }
            },
        )
    }
}

@Composable
private fun FeedbackToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun healthStatusLabel(status: HealthConnectStatus): String = when (status.state) {
    HealthConnectState.UNSUPPORTED -> "Niet ondersteund"
    HealthConnectState.PROVIDER_MISSING -> "Provider ontbreekt"
    HealthConnectState.PERMISSION_REQUIRED -> "Toegang vereist"
    HealthConnectState.CONNECTED -> "Verbonden"
    HealthConnectState.NO_DATA -> "Verbonden, nog geen data"
    HealthConnectState.ERROR -> "Fout"
}

private fun BiologicalSex.displayLabel(): String = when (this) {
    BiologicalSex.MALE -> "Man"
    BiologicalSex.FEMALE -> "Vrouw"
}

private fun ThemeMode.displayLabel(): String = when (this) {
    ThemeMode.SYSTEM -> "Systeem"
    ThemeMode.LIGHT -> "Licht"
    ThemeMode.DARK -> "Donker"
}

private fun Context.startActivityIfResolvable(intent: Intent): Boolean {
    if (intent.resolveActivity(packageManager) == null) return false
    return runCatching {
        startActivity(intent)
    }.isSuccess
}

private fun ProfileInputValidationError?.isFor(field: ProfileInputField): Boolean = this?.field == field

private fun ProfileInputValidationError?.supportingTextFor(field: ProfileInputField): (@Composable () -> Unit)? {
    val error = takeIf { it.isFor(field) } ?: return null
    return { Text(error.message) }
}
