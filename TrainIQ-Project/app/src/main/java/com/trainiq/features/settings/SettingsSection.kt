package com.trainiq.features.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.trainiq.core.theme.spacing
import androidx.health.connect.client.HealthConnectClient
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.BuildConfig
import com.trainiq.ai.services.AiProviderPreference
import com.trainiq.ai.services.hasAnyReadyProvider
import com.trainiq.ai.services.AiUsageGate
import com.trainiq.ai.services.GoalAdvisorService
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.WorkoutFeedbackPreferences
import com.trainiq.core.health.HealthConnectRefreshOnResume
import com.trainiq.core.health.rememberHealthConnectPermissionRequester
import com.trainiq.core.ui.MessageCard
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.SectionCard
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.core.ui.UiMessage
import com.trainiq.core.ui.bringIntoViewOnFocus
import com.trainiq.core.ui.clearFocusOnScrollOrDrag
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.theme.ThemeMode
import com.trainiq.features.profile.ProfileInputField
import com.trainiq.features.profile.ProfileInputValidationError
import com.trainiq.features.profile.ProfileInputValidationResult
import com.trainiq.features.profile.validateProfileInput
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HealthMetricSyncState
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.usecase.AppDataImportPreview
import com.trainiq.domain.usecase.ClearAppDataUseCase
import com.trainiq.domain.usecase.ExportAppDataUseCase
import com.trainiq.domain.usecase.GetHealthConnectStatusUseCase
import com.trainiq.domain.usecase.ImportAppDataUseCase
import com.trainiq.domain.usecase.ObserveUserProfileUseCase
import com.trainiq.domain.usecase.PreviewAppDataImportUseCase
import com.trainiq.domain.usecase.ResetProfileUseCase
import com.trainiq.domain.usecase.SaveUserProfileUseCase
import com.trainiq.navigation.TrainIqWindowWidthClass
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(
        val themeMode: ThemeMode,
        val aiPreferences: AiPreferences,
        val telemetryOptIn: Boolean,
        val workoutFeedbackPreferences: WorkoutFeedbackPreferences,
        val profile: UserProfile?,
        val healthStatus: HealthConnectStatus,
        val importPreview: AppDataImportPreview? = null,
        val isImporting: Boolean = false,
        val message: UiMessage? = null,
        val maskedApiKey: String = maskedSettingsApiKey(aiPreferences.apiKey),
    ) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

private data class SettingsUiInputs(
    val themeMode: ThemeMode,
    val aiPreferences: AiPreferences,
    val telemetryOptIn: Boolean,
    val workoutFeedbackPreferences: WorkoutFeedbackPreferences,
    val profile: UserProfile?,
    val healthStatus: HealthConnectStatus,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
    private val getHealthConnectStatusUseCase: GetHealthConnectStatusUseCase,
    private val aiUsageGate: AiUsageGate,
    private val goalAdvisorService: GoalAdvisorService,
    private val resetProfileUseCase: ResetProfileUseCase,
    private val clearAppDataUseCase: ClearAppDataUseCase,
    private val exportAppDataUseCase: ExportAppDataUseCase,
    private val previewAppDataImportUseCase: PreviewAppDataImportUseCase,
    private val importAppDataUseCase: ImportAppDataUseCase,
) : ViewModel() {
    private val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
    private val apiKeyRefreshes = MutableStateFlow(0)
    private val aiPreferences: StateFlow<AiPreferences> = combine(
        preferencesRepository.aiPreferences,
        apiKeyRefreshes,
    ) { legacySettings, _ ->
        aiUsageGate.resolveSettings(legacySettings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiPreferences(false, ""))
    private val telemetryOptIn: StateFlow<Boolean> = preferencesRepository.telemetryOptIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    private val workoutFeedbackPreferences: StateFlow<WorkoutFeedbackPreferences> = preferencesRepository.workoutFeedbackPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutFeedbackPreferences())
    private val profile: StateFlow<UserProfile?> = observeUserProfileUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _healthStatus = MutableStateFlow(
        HealthConnectStatus(
            state = HealthConnectState.ERROR,
            message = "Health Connect-status wordt geladen.",
        ),
    )
    private val healthStatus: StateFlow<HealthConnectStatus> = _healthStatus.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    private val _importPreview = MutableStateFlow<AppDataImportPreview?>(null)
    private val _isImporting = MutableStateFlow(false)
    private var pendingImportJson: String? = null
    val uiState: StateFlow<SettingsUiState> = combine(
        combine(themeMode, aiPreferences, telemetryOptIn, workoutFeedbackPreferences, profile) { theme, ai, telemetry, feedback, userProfile ->
            SettingsUiInputs(
                themeMode = theme,
                aiPreferences = ai,
                telemetryOptIn = telemetry,
                workoutFeedbackPreferences = feedback,
                profile = userProfile,
                healthStatus = healthStatus.value,
            )
        },
        healthStatus,
        _importPreview,
        _isImporting,
        _message,
    ) { inputs, health, importPreview, isImporting, message ->
        settingsUiState(
            themeMode = inputs.themeMode,
            aiPreferences = inputs.aiPreferences,
            telemetryOptIn = inputs.telemetryOptIn,
            workoutFeedbackPreferences = inputs.workoutFeedbackPreferences,
            profile = inputs.profile,
            healthStatus = health,
            importPreview = importPreview,
            isImporting = isImporting,
            message = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

    init {
        refreshHealthConnectStatus()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAiEnabled(enabled)
            apiKeyRefreshes.update { it + 1 }
            emitMessage(if (enabled) {
                "AI-functies ingeschakeld. Verzoeken starten alleen na jouw expliciete actie."
            } else {
                "AI-functies uitgeschakeld. TrainIQ blijft handmatig werken."
            })
        }
    }

    fun setTelemetryOptIn(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setTelemetryOptIn(enabled)
            emitMessage(if (enabled) {
                "Privacyveilige technische telemetrie ingeschakeld."
            } else {
                "Telemetrie uitgeschakeld. Er worden geen technische events geupload."
            })
        }
    }

    fun saveGeminiKey(apiKey: String) {
        viewModelScope.launch {
            if (apiKey.isBlank()) {
                emitMessage("Voer eerst een Gemini API-sleutel in.")
                return@launch
            }
            val encrypted = aiUsageGate.saveApiKey(apiKey)
            emitMessage(if (encrypted) {
                apiKeyRefreshes.update { it + 1 }
                "Gemini API-sleutel versleuteld opgeslagen."
            } else {
                "Gemini API-sleutel opslaan mislukt. Bestaande sleutel blijft behouden."
            })
        }
    }

    fun saveOpenAiKey(apiKey: String) {
        viewModelScope.launch {
            if (apiKey.isBlank()) {
                emitMessage("Voer eerst een OpenAI API-sleutel in.")
                return@launch
            }
            val encrypted = aiUsageGate.saveOpenAiApiKey(apiKey)
            emitMessage(if (encrypted) {
                apiKeyRefreshes.update { it + 1 }
                "OpenAI API-sleutel versleuteld opgeslagen."
            } else {
                "OpenAI API-sleutel opslaan mislukt. Bestaande sleutel blijft behouden."
            })
        }
    }

    fun setAiProviderPreference(preference: AiProviderPreference) {
        viewModelScope.launch {
            aiUsageGate.setProviderPreference(preference)
            apiKeyRefreshes.update { it + 1 }
            emitMessage("${preference.label} ingesteld voor AI-acties.")
        }
    }

    fun clearGeminiKey() {
        viewModelScope.launch {
            aiUsageGate.clearEncryptedApiKey()
            preferencesRepository.clearGeminiApiKey()
            preferencesRepository.setAiEnabled(false)
            apiKeyRefreshes.update { it + 1 }
            emitMessage("Gemini API-sleutel verwijderd en AI uitgeschakeld.")
        }
    }

    fun clearAllAiKeys() {
        viewModelScope.launch {
            aiUsageGate.clearAllAiKeys()
            preferencesRepository.clearGeminiApiKey()
            preferencesRepository.setAiEnabled(false)
            apiKeyRefreshes.update { it + 1 }
            emitMessage("AI-sleutels verwijderd en AI uitgeschakeld.")
        }
    }

    fun setRestTimerSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRestTimerSoundEnabled(enabled)
            emitMessage(if (enabled) "Rusttimer-geluid ingeschakeld." else "Rusttimer-geluid uitgeschakeld.")
        }
    }

    fun setWorkoutHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setWorkoutHapticsEnabled(enabled)
            emitMessage(if (enabled) "Workouttrillingen ingeschakeld." else "Workouttrillingen uitgeschakeld.")
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
                emitMessage(result.error.message)
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
                emitMessage("Profiel opgeslagen. Dashboarddoelen bijgewerkt.")
            }.onFailure {
                emitMessage("Profiel opslaan mislukt. Probeer opnieuw.")
            }
        }
    }

    fun resetProfile() {
        viewModelScope.launch {
            runCatching { resetProfileUseCase() }
                .onSuccess { emitMessage("Profiel verwijderd.") }
                .onFailure { emitMessage("Profiel verwijderen mislukt. Probeer opnieuw.") }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            runCatching {
                clearAppDataUseCase()
            }.onSuccess {
                _healthStatus.value = HealthConnectStatus(
                    state = HealthConnectState.NO_DATA,
                    message = "Lokale Health Connect-cache is gewist. Android-toegang is niet ingetrokken; beheer toegang in Health Connect.",
                )
                emitMessage("Alle lokale appdata, AI-sleutel, voorkeuren en Health Connect-cache zijn gewist. Health Connect-toegang beheer je apart in Android.")
                refreshHealthConnectStatus()
            }.onFailure {
                emitMessage("Lokale data wissen mislukt. Probeer opnieuw.")
            }
        }
    }

    private fun emitMessage(text: String) {
        _message.value = UiMessage(text)
    }

    fun clearMessage(id: Long? = null) {
        if (id == null || _message.value?.id == id) {
            _message.value = null
        }
    }

    suspend fun exportAppDataJson(): String = exportAppDataUseCase()

    fun previewImportJson(json: String) {
        viewModelScope.launch {
            runCatching {
                previewAppDataImportUseCase(json)
            }.onSuccess { preview ->
                pendingImportJson = json
                _importPreview.value = preview
                emitMessage("Importbestand gecontroleerd. Bevestig om lokale data te vervangen.")
            }.onFailure { throwable ->
                pendingImportJson = null
                _importPreview.value = null
                emitMessage(throwable.message ?: "Importbestand kon niet worden gelezen.")
            }
        }
    }

    fun confirmImport() {
        val json = pendingImportJson ?: run {
            emitMessage("Kies eerst een geldig importbestand.")
            return
        }
        viewModelScope.launch {
            _isImporting.value = true
            runCatching {
                importAppDataUseCase(json)
            }.onSuccess { result ->
                pendingImportJson = null
                _importPreview.value = null
                emitMessage("TrainIQ-data geimporteerd: ${result.importedRowCount} rijen hersteld.")
                refreshHealthConnectStatus()
            }.onFailure { throwable ->
                emitMessage(throwable.message ?: "Data importeren mislukt. Probeer opnieuw.")
            }
            _isImporting.value = false
        }
    }

    fun dismissImportPreview() {
        pendingImportJson = null
        _importPreview.value = null
    }

    fun setExportMessage(success: Boolean) {
        emitMessage(if (success) {
            "TrainIQ-data geexporteerd als JSON."
        } else {
            "Data exporteren mislukt. Probeer opnieuw."
        })
    }

    fun setImportFileReadMessage(success: Boolean) {
        emitMessage(if (success) {
            "Importbestand gelezen."
        } else {
            "Importbestand openen mislukt. Probeer opnieuw."
        })
    }

}

@Composable
fun SettingsRoute(
    windowWidthClass: TrainIqWindowWidthClass = TrainIqWindowWidthClass.Compact,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val requestHealthPermission = rememberHealthConnectPermissionRequester(viewModel::refreshHealthConnectStatus)
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val success = runCatching {
                val json = viewModel.exportAppDataJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                        writer.write(json)
                    } ?: error("Kon exportbestand niet openen.")
                }
            }.isSuccess
            viewModel.setExportMessage(success)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val json = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        readTrainIqImportJson(reader)
                    } ?: error("Kon importbestand niet openen.")
                }
            }.getOrElse {
                viewModel.setImportFileReadMessage(false)
                return@launch
            }
            viewModel.previewImportJson(json)
        }
    }
    HealthConnectRefreshOnResume(viewModel::refreshHealthConnectStatus, refreshOnFirstResume = false)

    when (val state = uiState) {
        SettingsUiState.Loading -> SettingsLoadingScreen()
        is SettingsUiState.Error -> SettingsErrorScreen(state.message)
        is SettingsUiState.Success -> {
            SettingsScreen(
                themeMode = state.themeMode,
                aiPreferences = state.aiPreferences,
                telemetryOptIn = state.telemetryOptIn,
                workoutFeedbackPreferences = state.workoutFeedbackPreferences,
                maskedApiKey = state.maskedApiKey,
                profile = state.profile,
                healthStatus = state.healthStatus,
                importPreview = state.importPreview,
                isImporting = state.isImporting,
                message = state.message,
                onThemeSelected = viewModel::setThemeMode,
                onToggleAi = viewModel::setAiEnabled,
                onToggleTelemetry = viewModel::setTelemetryOptIn,
                onToggleRestTimerSound = viewModel::setRestTimerSoundEnabled,
                onToggleWorkoutHaptics = viewModel::setWorkoutHapticsEnabled,
                onProviderPreferenceSelected = viewModel::setAiProviderPreference,
                onSaveApiKey = viewModel::saveGeminiKey,
                onSaveOpenAiKey = viewModel::saveOpenAiKey,
                onClearApiKey = viewModel::clearAllAiKeys,
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
                onExportData = {
                    exportLauncher.launch("trainiq-export-${System.currentTimeMillis()}.json")
                },
                onImportData = {
                    importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                },
                onConfirmImport = viewModel::confirmImport,
                onDismissImportPreview = viewModel::dismissImportPreview,
            )
        }
    }
}

internal enum class PendingDestructiveSettingsAction {
    CLEAR_API_KEY,
    RESET_PROFILE,
    CLEAR_ALL_DATA,
}

@Composable
private fun SettingsLoadingScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.medium,
            top = MaterialTheme.spacing.medium,
            end = MaterialTheme.spacing.medium,
            bottom = 132.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
    ) {
        item { ScreenHeader(title = "Instellingen", subtitle = "Profiel, Health Connect en voorkeuren") }
        item { ShimmerCardPlaceholder(lineCount = 4) }
        item { ShimmerCardPlaceholder(lineCount = 3) }
        item { ShimmerCardPlaceholder(lineCount = 5) }
    }
}

@Composable
private fun SettingsErrorScreen(message: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.medium,
            top = MaterialTheme.spacing.medium,
            end = MaterialTheme.spacing.medium,
            bottom = 132.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
    ) {
        item { ScreenHeader(title = "Instellingen", subtitle = "Profiel, Health Connect en voorkeuren") }
        item { SectionCard(title = "Instellingen niet beschikbaar") { Text(message) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    aiPreferences: AiPreferences,
    telemetryOptIn: Boolean,
    workoutFeedbackPreferences: WorkoutFeedbackPreferences,
    maskedApiKey: String,
    profile: UserProfile?,
    healthStatus: HealthConnectStatus,
    importPreview: AppDataImportPreview?,
    isImporting: Boolean,
    message: UiMessage?,
    onThemeSelected: (ThemeMode) -> Unit,
    onToggleAi: (Boolean) -> Unit,
    onToggleTelemetry: (Boolean) -> Unit,
    onToggleRestTimerSound: (Boolean) -> Unit,
    onToggleWorkoutHaptics: (Boolean) -> Unit,
    onProviderPreferenceSelected: (AiProviderPreference) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onSaveOpenAiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onSaveProfile: (String, String, BiologicalSex, String, String, String, String, String) -> Unit,
    onResetProfile: () -> Unit,
    onClearAllData: () -> Unit,
    onDismissMessage: (Long) -> Unit,
    onRequestHealthPermission: () -> Unit,
    onRefreshHealth: () -> Unit,
    onOpenHealthSettings: () -> Unit,
    onOpenHealthInstall: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    onConfirmImport: () -> Unit,
    onDismissImportPreview: () -> Unit,
) {
    var apiKey by rememberSaveable { mutableStateOf("") }
    var openAiKey by rememberSaveable { mutableStateOf("") }
    var pendingDestructiveAction by rememberSaveable { mutableStateOf<PendingDestructiveSettingsAction?>(null) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message?.id) {
        val currentMessage = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(currentMessage.text)
        onDismissMessage(currentMessage.id)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { _ ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnScrollOrDrag()
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                top = MaterialTheme.spacing.small,
                end = MaterialTheme.spacing.medium,
                bottom = 132.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        ) {
        item { ScreenHeader(title = "Instellingen", subtitle = "Health Connect, AI en voorkeuren") }
        item {
            SectionCard(title = settingsOverflowSectionTitle()) {
                Text(settingsOverflowSectionBody())
                Text("Thema: ${themeMode.displayLabel()}")
                Text("AI: ${if (aiPreferences.enabled && aiPreferences.apiKey.isNotBlank()) "Klaar voor expliciet gebruik" else "Alleen handmatig"}")
                Text("Health Connect: ${healthStatusLabel(healthStatus)}")
            }
        }
        item {
            SectionCard(title = "Weergave") {
                Text("Themamodus")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            modifier = Modifier
                                .settingsActionLabel(themeModeAccessibilityLabel(mode))
                                .height(48.dp),
                            selected = themeMode == mode,
                            onClick = { onThemeSelected(mode) },
                            label = { Text(mode.displayLabel()) },
                        )
                    }
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
            SectionCard(title = "Privacy en telemetrie") {
                FeedbackToggleRow(
                    title = "Technische telemetrie delen",
                    body = "Deel alleen privacyveilige technische events en prestatie-samenvattingen. Gezondheidsdata, notities en sleutels worden nooit geupload.",
                    checked = telemetryOptIn,
                    onCheckedChange = onToggleTelemetry,
                )
                Text(
                    if (telemetryOptIn) {
                        "Telemetrie staat aan, maar upload gebeurt alleen wanneer de build een endpoint en sampling configureert."
                    } else {
                        "Telemetrie staat uit. Lokale crash- en prestatiediagnostiek blijft beschikbaar."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionCard(title = "AI / Providers") {
                Text("AI wordt alleen gebruikt nadat jij het inschakelt. TrainIQ doet geen AI-aanvragen op de achtergrond.")
                Text("Bij een expliciete AI-actie stuurt TrainIQ de benodigde prompt, context en eventueel gekozen foto naar je gekozen provider met jouw lokaal opgeslagen API-sleutel.")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("AI-functies inschakelen", fontWeight = FontWeight.SemiBold)
                        Text("Alleen expliciete acties zoals maaltijdanalyse, doeladvies, AI-rapport en workoutterugblik kunnen verzoeken starten.")
                    }
                    Switch(
                        checked = aiPreferences.enabled,
                        onCheckedChange = onToggleAi,
                        modifier = Modifier.settingsActionLabel("AI-functies inschakelen"),
                    )
                }
                Text("Provider-volgorde", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                ) {
                    AiProviderPreference.entries.forEach { preference ->
                        FilterChip(
                            selected = aiPreferences.preferredProvider == preference,
                            onClick = { onProviderPreferenceSelected(preference) },
                            label = { Text(preference.label) },
                        )
                    }
                }
                Text("Gemini sleutel: $maskedApiKey")
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Gemini API-sleutel") },
                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                    visualTransformation = if (shouldMaskGeminiApiKeyInput()) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                )
                Text(
                    geminiApiKeySetupHelpText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${geminiApiKeySourceLabel()}: ${geminiApiKeySourceUrl()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                ) {
                    Button(onClick = {
                        onSaveApiKey(apiKey)
                        apiKey = ""
                    }) { Text(if (aiPreferences.apiKey.isBlank()) "Sleutel opslaan" else "Sleutel bijwerken") }
                }
                Text("OpenAI sleutel: ${maskedSettingsApiKey(aiPreferences.openAiApiKey)}")
                OutlinedTextField(
                    value = openAiKey,
                    onValueChange = { openAiKey = it },
                    label = { Text("OpenAI API-sleutel") },
                    modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
                    visualTransformation = if (shouldMaskGeminiApiKeyInput()) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                )
                Text(
                    openAiApiKeySetupHelpText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${openAiApiKeySourceLabel()}: ${openAiApiKeySourceUrl()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                ) {
                    Button(onClick = {
                        onSaveOpenAiKey(openAiKey)
                        openAiKey = ""
                    }) { Text(if (aiPreferences.openAiApiKey.isBlank()) "OpenAI opslaan" else "OpenAI bijwerken") }
                    TextButton(onClick = { pendingDestructiveAction = PendingDestructiveSettingsAction.CLEAR_API_KEY }) { Text("AI-sleutels verwijderen") }
                }
                Text("Gemini en OpenAI kunnen API-kosten veroorzaken. Laat AI uitgeschakeld tenzij je het wilt gebruiken.")
                Text("Gebruikt door: maaltijdanalyse, workoutterugblik, wekelijks AI-rapport en doeladviseur.")
                Text("Status: ${aiProviderStatusLabel(aiPreferences)}")
            }
        }
        item {
            SectionCard(title = "Health Connect") {
                Text("Status: ${healthStatusLabel(healthStatus)}")
                Text(
                    healthConnectSettingsMessage(healthStatus),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                ) {
                    when (healthStatus.state) {
                        HealthConnectState.PROVIDER_MISSING -> Button(
                            modifier = Modifier.settingsActionLabel("Health Connect installeren of bijwerken"),
                            onClick = onOpenHealthInstall,
                        ) { Text("Installeren / bijwerken") }
                        HealthConnectState.PERMISSION_REQUIRED -> Button(
                            modifier = Modifier.settingsActionLabel("Health Connect-toegang geven"),
                            onClick = onRequestHealthPermission,
                        ) { Text("Toegang geven") }
                        HealthConnectState.CONNECTED, HealthConnectState.NO_DATA -> Button(
                            modifier = Modifier.settingsActionLabel("Health Connect-instellingen openen"),
                            onClick = onOpenHealthSettings,
                        ) { Text("Health Connect openen") }
                        HealthConnectState.UNSUPPORTED -> Text("Niet ondersteund op dit apparaat.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HealthConnectState.ERROR -> Text("Health Connect kan nu niet worden gelezen.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        modifier = Modifier.settingsActionLabel("Health Connect-status vernieuwen"),
                        onClick = onRefreshHealth,
                    ) { Text("Vernieuwen") }
                }
            }
        }
        item {
            SectionCard(title = "Gegevens / opslag") {
                Text("Lokale data: profiel, routines, workouts, voeding, recepten en metingen.")
                Text("AI-sleutel: lokaal versleuteld opgeslagen via Android Keystore.")
                Text("Health Connect-cache: sync-token en recente stappen, hartslag, slaap, calorieën en gewicht.")
                Text("Toegang intrekken doe je in Android Health Connect.")
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    Button(
                        onClick = onExportData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .settingsActionLabel("TrainIQ-data exporteren als JSON"),
                    ) { Text("Data exporteren als JSON") }
                    Button(
                        onClick = onImportData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .settingsActionLabel("TrainIQ-data importeren uit JSON"),
                        enabled = !isImporting,
                    ) { Text(if (isImporting) "Importeren..." else "Data importeren uit JSON") }
                    TextButton(
                        onClick = { pendingDestructiveAction = PendingDestructiveSettingsAction.CLEAR_API_KEY },
                        modifier = Modifier
                            .fillMaxWidth()
                            .settingsActionLabel("AI-sleutels wissen"),
                    ) { Text("AI-sleutel wissen") }
                    TextButton(
                        onClick = { pendingDestructiveAction = PendingDestructiveSettingsAction.CLEAR_ALL_DATA },
                        modifier = Modifier
                            .fillMaxWidth()
                            .settingsActionLabel("Lokale TrainIQ-data wissen"),
                    ) { Text("Lokale data wissen") }
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
    }

    pendingDestructiveAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingDestructiveAction = null },
            title = {
                Text(destructiveSettingsActionTitle(action))
            },
            text = {
                Text(destructiveSettingsActionBody(action))
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

    importPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = onDismissImportPreview,
            title = { Text("JSON-import bevestigen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
                    Text("Deze import vervangt je lokale TrainIQ-data. Health Connect-toegang en AI-sleutels beheer je apart.")
                    Text(importPreviewSummary(preview))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmImport,
                    enabled = !isImporting,
                ) { Text(if (isImporting) "Importeren..." else "Importeren en vervangen") }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissImportPreview,
                    enabled = !isImporting,
                ) { Text("Annuleren") }
            },
        )
    }
}

private fun Modifier.settingsActionLabel(label: String): Modifier =
    semantics { contentDescription = label }

internal fun destructiveSettingsActionTitle(action: PendingDestructiveSettingsAction): String =
    when (action) {
        PendingDestructiveSettingsAction.CLEAR_API_KEY -> "API-sleutel verwijderen?"
        PendingDestructiveSettingsAction.RESET_PROFILE -> "Profiel resetten?"
        PendingDestructiveSettingsAction.CLEAR_ALL_DATA -> "Alle lokale appdata wissen?"
    }

internal fun destructiveSettingsActionBody(action: PendingDestructiveSettingsAction): String =
    when (action) {
        PendingDestructiveSettingsAction.CLEAR_API_KEY ->
            "Je Gemini en OpenAI API-sleutels worden verwijderd en AI wordt uitgeschakeld. Deze actie kan niet automatisch ongedaan worden gemaakt."
        PendingDestructiveSettingsAction.RESET_PROFILE ->
            "Je profiel en dashboarddoelen worden verwijderd. Trainingen en voeding blijven staan. Deze actie kan niet automatisch ongedaan worden gemaakt."
        PendingDestructiveSettingsAction.CLEAR_ALL_DATA ->
            "TrainIQ wist lokale trainingen, voeding, profiel, AI-sleutel, voorkeuren en Health Connect-cache op dit apparaat. Health Connect-permissies zelf beheer je in Android. Deze actie kan niet automatisch ongedaan worden gemaakt."
    }

@Composable
private fun FeedbackToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            )
            .settingsActionLabel(settingsToggleAccessibilityLabel(title, checked)),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clearAndSetSemantics { },
        )
    }

}

internal fun settingsToggleAccessibilityLabel(title: String, checked: Boolean): String =
    "$title: ${if (checked) "ingeschakeld" else "uitgeschakeld"}"

internal fun healthStatusLabel(status: HealthConnectStatus): String = when {
    status.hasPartialHealthConnectAccess() -> "Gedeeltelijk verbonden"
    else -> when (status.state) {
        HealthConnectState.UNSUPPORTED -> "Niet ondersteund"
        HealthConnectState.PROVIDER_MISSING -> "Provider ontbreekt"
        HealthConnectState.PERMISSION_REQUIRED -> "Toegang vereist"
        HealthConnectState.CONNECTED -> "Verbonden"
        HealthConnectState.NO_DATA -> "Verbonden, nog geen data"
        HealthConnectState.ERROR -> "Fout"
    }
}

internal fun healthConnectSettingsMessage(status: HealthConnectStatus): String = when {
    status.hasPartialHealthConnectAccess() -> status.message
    else -> when (status.state) {
        HealthConnectState.UNSUPPORTED -> "Health Connect wordt niet ondersteund op dit apparaat."
        HealthConnectState.PROVIDER_MISSING -> "Installeer of update Health Connect en vernieuw daarna de status."
        HealthConnectState.PERMISSION_REQUIRED -> "Geef toegang om stappen, hartslag, slaap, calorieen, gewicht en workouts te synchroniseren."
        HealthConnectState.CONNECTED -> "Verbonden. TrainIQ synchroniseert toegestane metrics wanneer data beschikbaar is."
        HealthConnectState.NO_DATA -> "Verbonden, maar er is nog geen recente Health Connect-data gevonden."
        HealthConnectState.ERROR -> "Health Connect kan nu niet worden gelezen. Vernieuw straks opnieuw."
    }
}

internal fun HealthConnectStatus.hasPartialHealthConnectAccess(): Boolean {
    if (metricStatuses.isEmpty()) return false
    val hasDeniedMetric = metricStatuses.any { it.state == HealthMetricSyncState.DENIED }
    val hasGrantedMetric = metricStatuses.any {
        it.state != HealthMetricSyncState.DENIED && it.state != HealthMetricSyncState.UNAVAILABLE
    }
    return hasDeniedMetric && hasGrantedMetric
}

internal fun settingsUiState(
    themeMode: ThemeMode,
    aiPreferences: AiPreferences,
    telemetryOptIn: Boolean,
    workoutFeedbackPreferences: WorkoutFeedbackPreferences,
    profile: UserProfile?,
    healthStatus: HealthConnectStatus,
    importPreview: AppDataImportPreview? = null,
    isImporting: Boolean = false,
    message: UiMessage?,
): SettingsUiState = SettingsUiState.Success(
    themeMode = themeMode,
    aiPreferences = aiPreferences,
    telemetryOptIn = telemetryOptIn,
    workoutFeedbackPreferences = workoutFeedbackPreferences,
    profile = profile,
    healthStatus = healthStatus,
    importPreview = importPreview,
    isImporting = isImporting,
    message = message,
)

internal fun importPreviewSummary(preview: AppDataImportPreview): String =
    buildString {
        append("Formaat: ${preview.format}")
        preview.version?.let { append(" v$it") }
        preview.exportedAt?.let { append("\nExportdatum: $it") }
        append("\nRijen: ${preview.rowCount}")
        append("\nRoutines: ${preview.routineCount} | Workouts: ${preview.workoutCount}")
        append("\nMaaltijden: ${preview.mealCount} | Producten: ${preview.foodCount}")
        append("\nMetingen: ${preview.measurementCount}")
    }

internal const val MaxTrainIqImportJsonChars: Int = 5 * 1024 * 1024

internal fun readTrainIqImportJson(
    reader: java.io.Reader,
    maxChars: Int = MaxTrainIqImportJsonChars,
): String {
    val buffer = CharArray(8 * 1024)
    val output = StringBuilder()
    while (true) {
        val read = reader.read(buffer)
        if (read == -1) return output.toString()
        if (output.length + read > maxChars) {
            error("Importbestand is te groot. Exporteer opnieuw of kies een kleiner TrainIQ JSON-bestand.")
        }
        output.append(buffer, 0, read)
    }
}

internal fun maskedSettingsApiKey(key: String): String {
    if (key.isBlank()) return "Niet ingesteld"
    return if (key.length <= 8) "********" else "${key.take(4)}****${key.takeLast(4)}"
}

internal fun shouldMaskGeminiApiKeyInput(): Boolean = true

internal fun geminiApiKeySourceLabel(): String = "Google AI Studio API Keys"

internal fun geminiApiKeySourceUrl(): String = "https://aistudio.google.com/app/apikey"

internal fun geminiApiKeySetupHelpText(): String =
    "Maak of bekijk je sleutel in Google AI Studio, plak hem hier en zet AI aan. Deel je sleutel niet en commit hem nooit."

internal fun openAiApiKeySourceLabel(): String = "OpenAI API Keys"

internal fun openAiApiKeySourceUrl(): String = "https://platform.openai.com/api-keys"

internal fun openAiApiKeySetupHelpText(): String =
    "Maak of bekijk je sleutel in het OpenAI Platform, plak hem hier en zet AI aan. Deel je sleutel niet en commit hem nooit."

internal fun aiProviderStatusLabel(aiPreferences: AiPreferences): String = when {
    !aiPreferences.enabled -> "Uitgeschakeld"
    aiPreferences.hasAnyReadyProvider() -> buildString {
        append("Klaar: ${aiPreferences.preferredProvider.label}")
        if (aiPreferences.geminiApiKey.isNotBlank() && aiPreferences.openAiApiKey.isNotBlank()) {
            append(", tweede provider opgeslagen")
        }
    }
    else -> "Geen AI-sleutel ingesteld"
}

internal fun settingsOverflowSectionTitle(): String = "Meer"

internal fun settingsOverflowSectionBody(): String =
    "Compacte navigatie: instellingen, voorkeuren en appbeheer staan hier. Trends en grafieken staan direct in de tab Trend."

internal fun themeModeAccessibilityLabel(mode: ThemeMode): String = "Themamodus: ${mode.displayLabel()}"

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
