package com.trainiq.features.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.text.KeyboardOptions
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
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.theme.ThemeMode
import com.trainiq.data.local.TrainIqLocalStore
import com.trainiq.features.profile.buildValidatedProfileInput
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
            message = "Health Connect status wordt geladen.",
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
                "AI features enabled. Requests only run after you explicitly trigger them."
            } else {
                "AI features disabled. TrainIQ will stay manual-first."
            }
        }
    }

    fun saveGeminiKey(apiKey: String) {
        viewModelScope.launch {
            if (apiKey.isBlank()) {
                _message.value = "Enter a Gemini API key first."
                return@launch
            }
            preferencesRepository.saveGeminiApiKey(apiKey)
            _message.value = "Gemini API key saved."
        }
    }

    fun clearGeminiKey() {
        viewModelScope.launch {
            preferencesRepository.clearGeminiApiKey()
            preferencesRepository.setAiEnabled(false)
            _message.value = "Gemini API key removed and AI disabled."
        }
    }

    fun setRestTimerSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRestTimerSoundEnabled(enabled)
            _message.value = if (enabled) "Rest timer sound enabled." else "Rest timer sound disabled."
        }
    }

    fun setWorkoutHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setWorkoutHapticsEnabled(enabled)
            _message.value = if (enabled) "Workout haptics enabled." else "Workout haptics disabled."
        }
    }

    fun refreshHealthConnectStatus() {
        viewModelScope.launch {
            _healthStatus.value = getHealthConnectStatusUseCase()
        }
    }

    fun saveProfile(name: String, height: String, weight: String, bodyFat: String, activityLevel: String, goal: String) {
        val currentProfile = profile.value
        val input = buildValidatedProfileInput(
            name = name,
            height = height,
            weight = weight,
            bodyFat = bodyFat,
            age = (currentProfile?.age ?: 30).toString(),
            sex = currentProfile?.sex ?: BiologicalSex.MALE,
            activityLevel = activityLevel,
            goal = goal,
        )
        if (input == null) {
            _message.value = "Complete all profile fields with valid values."
            return
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
            _message.value = "Profile saved. Dashboard targets updated."
        }
    }

    fun resetProfile() {
        viewModelScope.launch {
            localStore.clearProfile()
            _message.value = "Profile removed."
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            localStore.clearAll()
            _message.value = "Local training, meal, and profile data cleared."
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun maskedKey(): String {
        val key = aiPreferences.value.apiKey
        if (key.isBlank()) return "Not configured"
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
    HealthConnectRefreshOnResume(viewModel::refreshHealthConnectStatus)

    LaunchedEffect(Unit) {
        viewModel.refreshHealthConnectStatus()
    }

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
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                viewModel.refreshHealthConnectStatus()
            }
        },
        onOpenHealthInstall = {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"))
            val intent = if (marketIntent.resolveActivity(context.packageManager) != null) marketIntent else webIntent
            context.startActivity(intent)
        },
    )
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
    onSaveProfile: (String, String, String, String, String, String) -> Unit,
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
    var height by remember { mutableStateOf(profile?.height?.toString().orEmpty()) }
    var weight by remember { mutableStateOf(profile?.weight?.toString().orEmpty()) }
    var bodyFat by remember { mutableStateOf(profile?.bodyFat?.toString().orEmpty()) }
    var activityLevel by remember { mutableStateOf(profile?.activityLevel.orEmpty()) }
    var goal by remember { mutableStateOf(profile?.goal.orEmpty()) }

    LaunchedEffect(profile) {
        name = profile?.name.orEmpty()
        height = profile?.height?.toString().orEmpty()
        weight = profile?.weight?.toString().orEmpty()
        bodyFat = profile?.bodyFat?.toString().orEmpty()
        activityLevel = profile?.activityLevel.orEmpty()
        goal = profile?.goal.orEmpty()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
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
        item { ScreenHeader(title = "Settings", subtitle = "Profiel, Health Connect en voorkeuren") }
        item {
            SectionCard(title = "Quick Status") {
                Text("Theme: ${themeMode.name.lowercase().replaceFirstChar(Char::titlecase)}")
                Text("AI: ${if (aiPreferences.enabled && aiPreferences.apiKey.isNotBlank()) "Ready for explicit use" else "Manual-only"}")
                Text("Health Connect: ${healthStatusLabel(healthStatus)}")
            }
        }
        message?.let {
            item {
                MessageCard(message = it, onDismiss = onDismissMessage)
            }
        }
        item {
            SectionCard(title = "Appearance") {
                Text("Theme mode")
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeSelected(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar(Char::titlecase)) },
                    )
                }
            }
        }
        item {
            SectionCard(title = "Workout feedback") {
                FeedbackToggleRow(
                    title = "Rest timer sound",
                    body = "Play a short sound when rest reaches zero.",
                    checked = workoutFeedbackPreferences.restTimerSoundEnabled,
                    onCheckedChange = onToggleRestTimerSound,
                )
                FeedbackToggleRow(
                    title = "Workout haptics",
                    body = "Use subtle vibration when sets are completed or rest finishes.",
                    checked = workoutFeedbackPreferences.workoutHapticsEnabled,
                    onCheckedChange = onToggleWorkoutHaptics,
                )
            }
        }
        item {
            SectionCard(title = "AI / Gemini") {
                Text("AI is always opt-in. TrainIQ does not make background AI calls.")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Enable AI features", fontWeight = FontWeight.SemiBold)
                        Text("Only explicit actions like meal analysis, goal advice, AI report, and workout debrief can trigger requests.")
                    }
                    Switch(checked = aiPreferences.enabled, onCheckedChange = onToggleAi)
                }
                Text("Current key: $maskedApiKey")
                OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("Gemini API key") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    Button(onClick = {
                        onSaveApiKey(apiKey)
                        apiKey = ""
                    }) { Text(if (aiPreferences.apiKey.isBlank()) "Save key" else "Update key") }
                    TextButton(onClick = onClearApiKey) { Text("Remove key") }
                }
                Text("Gemini may incur API costs. Keep AI disabled unless you want to use it.")
                Text("Used by: Meal analysis, workout debrief, weekly AI report, goal advisor.")
                Text("Status: ${if (aiPreferences.enabled && aiPreferences.apiKey.isNotBlank()) "Enabled and ready" else if (aiPreferences.apiKey.isBlank()) "No key configured" else "Disabled"}")
            }
        }
        item {
            SectionCard(title = "Health Connect") {
                Text("Status: ${healthStatusLabel(healthStatus)}")
                Text(healthStatus.message)
                healthStatus.lastSyncedAt?.let {
                    Text("Last checked: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))}")
                }
                healthStatus.stepsToday?.let { steps ->
                    Text(if (steps > 0) "Today's steps available: $steps" else "Connected, but no step data was returned today yet.")
                }
                healthStatus.averageHeartRateBpm?.let { bpm ->
                    Text("Average heart rate today: $bpm bpm")
                }
                healthStatus.latestHeartRateBpm?.let { bpm ->
                    Text("Latest heart rate sample: $bpm bpm")
                }
                healthStatus.sleepMinutes?.takeIf { it > 0 }?.let { minutes ->
                    Text("Recent sleep: ${minutes / 60}h ${minutes % 60}m across ${healthStatus.sleepSessionCount} session(s)")
                }
                when (healthStatus.state) {
                    HealthConnectState.PERMISSION_REQUIRED -> Text("Grant access to let TrainIQ read your daily steps, heart rate, and sleep.")
                    HealthConnectState.PROVIDER_MISSING -> Text("Install or update Health Connect first, then return here and refresh.")
                    else -> Unit
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small), verticalAlignment = Alignment.CenterVertically) {
                    when (healthStatus.state) {
                        HealthConnectState.PROVIDER_MISSING -> Button(onClick = onOpenHealthInstall) { Text("Install / update") }
                        HealthConnectState.PERMISSION_REQUIRED -> Button(onClick = onRequestHealthPermission) { Text("Grant access") }
                        HealthConnectState.CONNECTED, HealthConnectState.NO_DATA -> Button(onClick = onOpenHealthSettings) { Text("Open Health Connect") }
                        HealthConnectState.UNSUPPORTED -> Text("Not supported on this device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HealthConnectState.ERROR -> Text("Unable to read Health Connect right now.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onRefreshHealth) { Text("Refresh") }
                }
            }
        }
        item {
            SectionCard(title = "Profile & Goals") {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                OutlinedTextField(value = bodyFat, onValueChange = { bodyFat = it }, label = { Text("Body fat %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                OutlinedTextField(value = activityLevel, onValueChange = { activityLevel = it }, label = { Text("Activity level") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                OutlinedTextField(value = goal, onValueChange = { goal = it }, label = { Text("Primary goal") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                Button(
                    onClick = { onSaveProfile(name, height, weight, bodyFat, activityLevel, goal) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save profile") }
                TextButton(onClick = onResetProfile, modifier = Modifier.fillMaxWidth()) { Text("Reset profile") }
                profile?.let {
                    Text("Current dashboard targets: ${it.calorieTarget} kcal and ${it.proteinTarget} g protein.")
                }
            }
        }
        item {
            SectionCard(title = "Data / Storage") {
                Text("Storage mode: Local JSON store")
                Text("AI key storage: local app preferences")
                Text("Health Connect cache: persisted change token plus lightweight sync snapshot")
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    TextButton(onClick = onClearApiKey) { Text("Clear AI key") }
                    TextButton(onClick = onClearAllData) { Text("Clear local data") }
                }
            }
        }
        item {
            SectionCard(title = "About") {
                Text("App version: ${BuildConfig.VERSION_NAME}")
                Text("AI enabled: ${if (aiPreferences.enabled) "Yes" else "No"}")
                Text("Health Connect: ${healthStatusLabel(healthStatus)}")
                Text("Designed as a manual-first training and nutrition MVP.")
            }
        }
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
    HealthConnectState.UNSUPPORTED -> "Not supported"
    HealthConnectState.PROVIDER_MISSING -> "Provider missing"
    HealthConnectState.PERMISSION_REQUIRED -> "Permission required"
    HealthConnectState.CONNECTED -> "Connected"
    HealthConnectState.NO_DATA -> "Connected, no data yet"
    HealthConnectState.ERROR -> "Error"
}
