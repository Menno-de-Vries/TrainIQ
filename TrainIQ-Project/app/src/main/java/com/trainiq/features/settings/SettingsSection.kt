package com.trainiq.features.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.trainiq.core.theme.spacing
import androidx.health.connect.client.HealthConnectClient
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.BuildConfig
import com.trainiq.ai.services.AiProviderPreference
import com.trainiq.ai.services.AiUsageGate
import com.trainiq.ai.services.GoalAdvisorService
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.OnboardingPreferences
import com.trainiq.core.datastore.ReminderPreferences
import com.trainiq.core.datastore.WorkoutFeedbackPreferences
import com.trainiq.core.health.HealthConnectBackgroundSyncScheduler
import com.trainiq.core.health.HealthConnectRefreshOnResume
import com.trainiq.core.health.healthConnectStepSourceLabel
import com.trainiq.core.health.rememberHealthConnectPermissionRequester
import com.trainiq.core.reminders.TrainIqReminderScheduler
import com.trainiq.core.ui.MessageCard
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.SectionCard
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.core.ui.TrainIqFormField
import com.trainiq.core.ui.TrainIqFormFieldContext
import com.trainiq.core.ui.UiMessage
import com.trainiq.core.ui.reloadableObservation
import com.trainiq.core.ui.clearFocusOnScrollOrDrag
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.data.datasource.SamsungHealthDirectStepsDataSource
import com.trainiq.core.theme.ThemeMode
import com.trainiq.features.profile.ProfileInputField
import com.trainiq.features.profile.ProfileInputValidationError
import com.trainiq.features.profile.ProfileInputValidationResult
import com.trainiq.features.profile.validateProfileInput
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HealthConnectStepDataFreshness
import com.trainiq.domain.model.HealthMetricSyncState
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.usecase.AppDataImportPreview
import com.trainiq.domain.usecase.ClearAppDataUseCase
import com.trainiq.domain.usecase.ExportAppDataUseCase
import com.trainiq.domain.usecase.GetHealthConnectStatusUseCase
import com.trainiq.domain.usecase.ImportAppDataUseCase
import com.trainiq.domain.usecase.ObserveUserProfileUseCase
import com.trainiq.domain.usecase.PreviewAppDataImportUseCase
import com.trainiq.domain.usecase.ReopenOnboardingUseCase
import com.trainiq.domain.usecase.ResetProfileUseCase
import com.trainiq.domain.usecase.SaveUserProfileUseCase
import com.trainiq.features.onboarding.onboardingSetupItems
import com.trainiq.navigation.TrainIqWindowWidthClass
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(
        val themeMode: ThemeMode,
        val aiStatus: SettingsAiStatus,
        val telemetryOptIn: Boolean,
        val workoutFeedbackPreferences: WorkoutFeedbackPreferences,
        val reminderPreferences: ReminderPreferences,
        val onboardingPreferences: OnboardingPreferences = OnboardingPreferences(),
        val profile: UserProfile?,
        val healthStatus: HealthConnectStatus,
        val importPreview: AppDataImportPreview? = null,
        val isImporting: Boolean = false,
        val message: UiMessage? = null,
    ) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

data class SettingsAiStatus(
    val enabled: Boolean,
    val preferredProvider: AiProviderPreference,
    val hasGeminiKey: Boolean,
    val hasOpenAiKey: Boolean,
    val maskedGeminiKey: String,
    val maskedOpenAiKey: String,
)

private data class SettingsUiInputs(
    val themeMode: ThemeMode,
    val aiStatus: SettingsAiStatus,
    val telemetryOptIn: Boolean,
    val workoutFeedbackPreferences: WorkoutFeedbackPreferences,
    val reminderPreferences: ReminderPreferences,
    val onboardingPreferences: OnboardingPreferences,
    val profile: UserProfile?,
    val healthStatus: HealthConnectStatus,
)

private data class SettingsPreferenceInputs(
    val themeMode: ThemeMode,
    val aiStatus: SettingsAiStatus,
    val telemetryOptIn: Boolean,
    val workoutFeedbackPreferences: WorkoutFeedbackPreferences,
    val reminderPreferences: ReminderPreferences,
    val onboardingPreferences: OnboardingPreferences,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
    private val getHealthConnectStatusUseCase: GetHealthConnectStatusUseCase,
    private val samsungHealthDirectStepsDataSource: SamsungHealthDirectStepsDataSource,
    private val aiUsageGate: AiUsageGate,
    private val goalAdvisorService: GoalAdvisorService,
    private val resetProfileUseCase: ResetProfileUseCase,
    private val clearAppDataUseCase: ClearAppDataUseCase,
    private val exportAppDataUseCase: ExportAppDataUseCase,
    private val previewAppDataImportUseCase: PreviewAppDataImportUseCase,
    private val importAppDataUseCase: ImportAppDataUseCase,
    private val reopenOnboardingUseCase: ReopenOnboardingUseCase,
    private val reminderScheduler: TrainIqReminderScheduler,
    private val healthConnectBackgroundSyncScheduler: HealthConnectBackgroundSyncScheduler,
) : ViewModel() {
    private val reloads = MutableStateFlow(0)
    private val apiKeyRefreshes = MutableStateFlow(0)
    private val externalInputs = reloadableObservation(reloads) {
        val aiPreferences = combine(preferencesRepository.aiPreferences, apiKeyRefreshes) { legacySettings, _ ->
            aiUsageGate.resolveSettings(legacySettings)
        }
        val preferenceInputs = combine(
            preferencesRepository.themeMode,
            aiPreferences,
            preferencesRepository.telemetryOptIn,
            preferencesRepository.workoutFeedbackPreferences,
            preferencesRepository.reminderPreferences,
        ) { theme, ai, telemetry, feedback, reminders ->
            SettingsPreferenceInputs(theme, ai.toSettingsAiStatus(), telemetry, feedback, reminders, OnboardingPreferences())
        }
        combine(preferenceInputs, preferencesRepository.onboardingPreferences, observeUserProfileUseCase()) { preferences, onboarding, profile ->
            SettingsUiInputs(
                themeMode = preferences.themeMode,
                aiStatus = preferences.aiStatus,
                telemetryOptIn = preferences.telemetryOptIn,
                workoutFeedbackPreferences = preferences.workoutFeedbackPreferences,
                reminderPreferences = preferences.reminderPreferences,
                onboardingPreferences = onboarding,
                profile = profile,
                healthStatus = healthStatus.value,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
        externalInputs,
        healthStatus,
        _importPreview,
        _isImporting,
        _message,
    ) { externalInputs, health, importPreview, isImporting, message ->
        when {
            externalInputs == null -> SettingsUiState.Loading
            externalInputs.isFailure -> SettingsUiState.Error("Instellingen konden niet worden geladen.")
            else -> externalInputs.getOrThrow().let { inputs ->
                settingsUiState(inputs.themeMode, inputs.aiStatus, inputs.telemetryOptIn, inputs.workoutFeedbackPreferences, inputs.reminderPreferences, inputs.onboardingPreferences, inputs.profile, health, importPreview, isImporting, message)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

    init {
        refreshHealthConnectStatus()
    }

    fun retry() {
        reloads.update { it + 1 }
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

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRemindersEnabled(enabled)
            if (enabled) {
                reminderScheduler.schedule()
                emitMessage("TrainIQ-reminders ingeschakeld.")
            } else {
                reminderScheduler.cancel()
                emitMessage("TrainIQ-reminders uitgeschakeld.")
            }
        }
    }

    fun onReminderPermissionDenied() {
        viewModelScope.launch {
            preferencesRepository.setRemindersEnabled(false)
            reminderScheduler.cancel()
            emitMessage("Notificatietoestemming ontbreekt. Reminders blijven uit.")
        }
    }

    fun refreshHealthConnectStatus() {
        viewModelScope.launch {
            refreshHealthConnectStatusAndReconcile(
                loadStatus = { getHealthConnectStatusUseCase() },
                fallbackStatus = {
                    HealthConnectStatus(
                        state = HealthConnectState.ERROR,
                        message = "Health Connect kan nu niet worden bijgewerkt.",
                    )
                },
                publishStatus = { _healthStatus.value = it },
                reconcileBackgroundSync = {
                    healthConnectBackgroundSyncScheduler.scheduleIfBackgroundReadAvailable()
                },
                onReconcileFailure = { error ->
                    Log.w(SettingsLogTag, "Health Connect background sync reconciliation failed.", error)
                },
            )
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

    fun requestSamsungHealthStepPermission(activity: Activity) {
        viewModelScope.launch {
            val samsungStatus = runCatching {
                samsungHealthDirectStepsDataSource.requestTodayStepPermission(activity).status
            }.getOrElse { throwable ->
                "Samsung Health-stappentoegang kon niet worden geopend: ${throwable.message ?: throwable.javaClass.simpleName}"
            }
            _healthStatus.value = runCatching { getHealthConnectStatusUseCase() }
                .getOrElse { _healthStatus.value }
            emitMessage(samsungStatus)
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

    fun reopenOnboarding() {
        viewModelScope.launch {
            reopenOnboardingUseCase()
            emitMessage("Onboarding opnieuw geopend.")
        }
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

internal suspend fun refreshHealthConnectStatusAndReconcile(
    loadStatus: suspend () -> HealthConnectStatus,
    fallbackStatus: () -> HealthConnectStatus,
    publishStatus: (HealthConnectStatus) -> Unit,
    reconcileBackgroundSync: suspend () -> Unit,
    onReconcileFailure: (Throwable) -> Unit = {},
) {
    val refreshedStatus = try {
        loadStatus()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        fallbackStatus()
    }
    publishStatus(refreshedStatus)
    try {
        reconcileBackgroundSync()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        onReconcileFailure(error)
    }
}

@Composable
fun SettingsRoute(
    windowWidthClass: TrainIqWindowWidthClass = TrainIqWindowWidthClass.Compact,
    onOpenOnboarding: () -> Unit = {},
    onOpenProgress: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
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
    val requestNotificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.setRemindersEnabled(true)
        } else {
            viewModel.onReminderPermissionDenied()
        }
    }

    when (val state = uiState) {
        SettingsUiState.Loading -> SettingsLoadingScreen()
        is SettingsUiState.Error -> SettingsErrorScreen(state.message, viewModel::retry)
        is SettingsUiState.Success -> {
            SettingsScreen(
                themeMode = state.themeMode,
                aiStatus = state.aiStatus,
                telemetryOptIn = state.telemetryOptIn,
                workoutFeedbackPreferences = state.workoutFeedbackPreferences,
                reminderPreferences = state.reminderPreferences,
                onboardingPreferences = state.onboardingPreferences,
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
                onToggleReminders = { enabled ->
                    if (!enabled) {
                        viewModel.setRemindersEnabled(false)
                    } else if (canPostNotifications(context)) {
                        viewModel.setRemindersEnabled(true)
                    } else {
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onProviderPreferenceSelected = viewModel::setAiProviderPreference,
                onSaveApiKey = viewModel::saveGeminiKey,
                onSaveOpenAiKey = viewModel::saveOpenAiKey,
                onClearApiKey = viewModel::clearAllAiKeys,
                onSaveProfile = viewModel::saveProfile,
                onResetProfile = viewModel::resetProfile,
                onClearAllData = viewModel::clearAllData,
                onDismissMessage = viewModel::clearMessage,
                onRequestHealthPermission = requestHealthPermission,
                onRequestSamsungHealthPermission = {
                    if (activity != null) {
                        viewModel.requestSamsungHealthStepPermission(activity)
                    } else {
                        viewModel.refreshHealthConnectStatus()
                    }
                },
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
                onOpenSamsungHealth = {
                    val launchIntent = context.packageManager
                        .getLaunchIntentForPackage(SamsungHealthPackageName)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SamsungHealthPackageName"))
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$SamsungHealthPackageName"))
                    if (
                        launchIntent == null ||
                        !context.startActivityIfResolvable(launchIntent)
                    ) {
                        if (!context.startActivityIfResolvable(marketIntent) && !context.startActivityIfResolvable(webIntent)) {
                            viewModel.refreshHealthConnectStatus()
                        }
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
                onOpenOnboarding = {
                    onOpenOnboarding()
                },
                onOpenProgress = onOpenProgress,
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
private fun SettingsErrorScreen(message: String, onRetry: () -> Unit) {
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
        item {
            SectionCard(title = "Instellingen niet beschikbaar") {
                Text(message)
                Button(
                    onClick = onRetry,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Opnieuw proberen") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    aiStatus: SettingsAiStatus,
    telemetryOptIn: Boolean,
    workoutFeedbackPreferences: WorkoutFeedbackPreferences,
    reminderPreferences: ReminderPreferences,
    onboardingPreferences: OnboardingPreferences,
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
    onToggleReminders: (Boolean) -> Unit,
    onProviderPreferenceSelected: (AiProviderPreference) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onSaveOpenAiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onSaveProfile: (String, String, BiologicalSex, String, String, String, String, String) -> Unit,
    onResetProfile: () -> Unit,
    onClearAllData: () -> Unit,
    onDismissMessage: (Long) -> Unit,
    onRequestHealthPermission: () -> Unit,
    onRequestSamsungHealthPermission: () -> Unit,
    onRefreshHealth: () -> Unit,
    onOpenHealthSettings: () -> Unit,
    onOpenHealthInstall: () -> Unit,
    onOpenSamsungHealth: () -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    onConfirmImport: () -> Unit,
    onDismissImportPreview: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenProgress: () -> Unit,
) {
    var geminiKeyInput by remember { mutableStateOf("") }
    var openAiKeyInput by remember { mutableStateOf("") }
    var pendingDestructiveAction by rememberSaveable { mutableStateOf<PendingDestructiveSettingsAction?>(null) }
    var showHealthTechnicalDetails by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            geminiKeyInput = ""
            openAiKeyInput = ""
        }
    }
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val skippedSetupItems = onboardingSetupItems(onboardingPreferences)

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnScrollOrDrag()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .imePadding()
                .padding(
                    start = MaterialTheme.spacing.medium,
                    top = MaterialTheme.spacing.small,
                    end = MaterialTheme.spacing.medium,
                    bottom = 132.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        ) {
        ScreenHeader(title = "Instellingen", subtitle = "Health Connect, AI en voorkeuren")
        SectionCard(title = settingsOverflowSectionTitle()) {
                Text(settingsOverflowSectionBody())
                Text("Thema: ${themeMode.displayLabel()}")
                Text("AI: ${if (aiStatus.enabled && (aiStatus.hasGeminiKey || aiStatus.hasOpenAiKey)) "Klaar voor expliciet gebruik" else "Alleen handmatig"}")
                Text("Health Connect: ${healthStatusLabel(healthStatus)}")
                Button(
                    onClick = onOpenProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .settingsActionLabel(settingsOpenProgressActionLabel()),
                ) {
                    Text(settingsOpenProgressActionLabel())
                }
        }
        SectionCard(title = "Onboarding") {
                Text(
                    if (onboardingPreferences.completed) {
                        "Intro afgerond. Je kunt de setup opnieuw openen om doelen, Health Connect, AI en reminders rustig langs te lopen."
                    } else {
                        "Intro nog niet afgerond. Rond de basissetup af of sla onderdelen bewust over."
                    },
                )
                if (skippedSetupItems.isNotEmpty()) {
                    Text(
                        "Nog open",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    skippedSetupItems.take(3).forEach { setupItem ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(setupItem.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                setupItem.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Text(
                        "Alle eerste setup-keuzes zijn vastgelegd.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onOpenOnboarding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .settingsActionLabel("Onboarding opnieuw openen"),
                ) {
                    Text("Onboarding openen")
                }
        }
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
        SectionCard(title = "Voorkeuren") {
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
                FeedbackToggleRow(
                    title = "TrainIQ-reminders",
                    body = "Stuur lokale notificaties voor gemiste food-logs en krachttraining. Alleen na jouw toestemming.",
                    checked = reminderPreferences.enabled,
                    onCheckedChange = onToggleReminders,
                )
                Text(
                    if (reminderPreferences.enabled) {
                        "Voeding wordt ongeveer elke 4 uur gecheckt. Krachttraining krijgt na 2 dagen zonder sessie een rustige reminder."
                    } else {
                        "Reminders staan uit. TrainIQ vraagt geen notificaties zolang je dit niet inschakelt."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                        checked = aiStatus.enabled,
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
                            selected = aiStatus.preferredProvider == preference,
                            onClick = { onProviderPreferenceSelected(preference) },
                            label = { Text(preference.label) },
                        )
                    }
                }
                Text("Gemini sleutel: ${aiStatus.maskedGeminiKey}")
                TrainIqFormField(
                    value = geminiKeyInput,
                    onValueChange = { geminiKeyInput = it },
                    label = "Gemini API-sleutel",
                    modifier = Modifier.fillMaxWidth(),
                    context = TrainIqFormFieldContext.Settings,
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
                        onSaveApiKey(geminiKeyInput)
                        geminiKeyInput = ""
                    }) { Text(if (!aiStatus.hasGeminiKey) "Sleutel opslaan" else "Sleutel bijwerken") }
                }
                Text("OpenAI sleutel: ${aiStatus.maskedOpenAiKey}")
                TrainIqFormField(
                    value = openAiKeyInput,
                    onValueChange = { openAiKeyInput = it },
                    label = "OpenAI API-sleutel",
                    modifier = Modifier.fillMaxWidth(),
                    context = TrainIqFormFieldContext.Settings,
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
                        onSaveOpenAiKey(openAiKeyInput)
                        openAiKeyInput = ""
                    }) { Text(if (!aiStatus.hasOpenAiKey) "OpenAI opslaan" else "OpenAI bijwerken") }
                    TextButton(onClick = { pendingDestructiveAction = PendingDestructiveSettingsAction.CLEAR_API_KEY }) { Text("AI-sleutels verwijderen") }
                }
                Text("Gemini en OpenAI kunnen API-kosten veroorzaken. Laat AI uitgeschakeld tenzij je het wilt gebruiken.")
                Text("Gebruikt door: maaltijdanalyse, workoutterugblik, wekelijks AI-rapport en doeladviseur.")
                Text("Status: ${aiProviderStatusLabel(aiStatus)}")
        }
        SectionCard(title = "Health Connect") {
                Text("Status: ${healthStatusLabel(healthStatus)}")
                healthConnectStepsAvailabilityMessage(healthStatus)?.let { stepMessage ->
                    Text(stepMessage)
                }
                healthConnectLastCheckedMessage(healthStatus.lastSyncedAt)?.let { lastCheckedMessage ->
                    Text(
                        lastCheckedMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (
                    (healthStatus.state != HealthConnectState.CONNECTED && healthStatus.state != HealthConnectState.NO_DATA) ||
                    healthStatus.hasPartialHealthConnectAccess()
                ) {
                    Text(
                        healthConnectSettingsMessage(healthStatus),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                    if (BuildConfig.SAMSUNG_HEALTH_DATA_SDK_AAR_PRESENT) {
                        Button(
                            modifier = Modifier.settingsActionLabel("Samsung Health-stappentoegang geven"),
                            onClick = onRequestSamsungHealthPermission,
                        ) { Text("Samsung toegang geven") }
                    }
                    TextButton(
                        modifier = Modifier.settingsActionLabel("Health Connect-status vernieuwen"),
                        onClick = onRefreshHealth,
                    ) { Text("Vernieuwen") }
                }
                val technicalDetailsLabel = healthTechnicalDetailsToggleLabel(showHealthTechnicalDetails)
                TextButton(
                    modifier = Modifier.settingsActionLabel(technicalDetailsLabel),
                    onClick = { showHealthTechnicalDetails = !showHealthTechnicalDetails },
                ) { Text(technicalDetailsLabel) }
                AnimatedVisibility(visible = showHealthTechnicalDetails) {
                    HealthConnectTechnicalDetails(
                        healthStatus = healthStatus,
                        onOpenHealthSettings = onOpenHealthSettings,
                        onOpenSamsungHealth = onOpenSamsungHealth,
                        onCopyDiagnostic = { diagnostic ->
                            clipboardManager.setText(AnnotatedString(diagnostic))
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Samsung stappen-diagnose gekopieerd.")
                            }
                        },
                    )
                }
        }
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
        SectionCard(title = "Over") {
                Text("Appversie: ${BuildConfig.VERSION_NAME}")
                Text("AI ingeschakeld: ${if (aiStatus.enabled) "Ja" else "Nee"}")
                Text("Health Connect: ${healthStatusLabel(healthStatus)}")
                Text("Ontworpen als handmatige training- en voedings-MVP.")
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

@Composable
private fun HealthConnectTechnicalDetails(
    healthStatus: HealthConnectStatus,
    onOpenHealthSettings: () -> Unit,
    onOpenSamsungHealth: () -> Unit,
    onCopyDiagnostic: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    ) {
        Text(
            "Achtergrondsync wordt alleen gepland als Android-toegang beschikbaar en toegestaan is.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        healthStatus.stepDiagnostic?.let { stepDiagnostic ->
            Text(
                "Bronnen vandaag: ${stepDiagnostic.sourceSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Venster: ${stepDiagnostic.queryWindowSummary}. ${stepDiagnostic.aggregateAuthorityLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Samsung-vergelijking: ${stepDiagnostic.samsungHealthComparisonSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Stappenwaarden: ${stepDiagnostic.stepValueDebugSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Samsung-bron timing: ${stepDiagnostic.samsungSourceRecencySummary()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Samsung direct: ${stepDiagnostic.samsungHealthDirectStatus}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Pariteit: ${stepDiagnostic.parityGapSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Health Connect zichtbaar: ${stepDiagnostic.healthConnectVisibleStepSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Health Connect-prioriteit: ${stepDiagnostic.healthConnectStepPrioritySummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (stepDiagnostic.hasMultipleHealthConnectStepSources) {
                TextButton(
                    modifier = Modifier.settingsActionLabel("Health Connect App priorities openen"),
                    onClick = onOpenHealthSettings,
                ) { Text("Prioriteiten openen") }
            }
            Text(
                "Samsung Health All steps: open Samsung Health > Instellingen > Health Connect en gebruik Sync now als TrainIQ blijft afwijken.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Workout-overlap: ${stepDiagnostic.workoutWindowSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (
                !stepDiagnostic.hasSamsungHealthSource ||
                stepDiagnostic.freshness() == com.trainiq.domain.model.HealthConnectStepDiagnosticFreshness.STALE
            ) {
                Text(
                    stepDiagnostic.samsungHealthSyncGuidance(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(
                modifier = Modifier.settingsActionLabel("Samsung stappen-diagnose kopieren"),
                onClick = { onCopyDiagnostic(stepDiagnostic.samsungStepDebugClipboardText()) },
            ) { Text("Diagnose kopieren") }
            TextButton(
                modifier = Modifier.settingsActionLabel("Samsung Health openen voor Sync now"),
                onClick = onOpenSamsungHealth,
            ) { Text("Samsung Health openen") }
        }
        healthStatus.averageHeartRateBpm?.let { bpm ->
            Text("Gemiddelde hartslag vandaag: $bpm bpm")
        }
        healthStatus.latestHeartRateBpm?.let { bpm ->
            Text("Laatste hartslagmeting: $bpm bpm")
        }
        healthStatus.sleepMinutes?.takeIf { it > 0 }?.let { minutes ->
            val recordContext = if (healthStatus.sleepSessionCount > 1) {
                " · meerdere Health Connect-records gezien"
            } else {
                ""
            }
            Text("Recente slaap: ${minutes / 60}u ${minutes % 60}m$recordContext")
        }
    }
}

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

internal fun healthConnectStepsAvailabilityMessage(status: HealthConnectStatus): String? {
    val steps = status.stepsToday ?: return null
    val source = healthConnectStepSourceLabel(status)
    return when (status.stepDataFreshness) {
        HealthConnectStepDataFreshness.FRESH -> "$steps stappen via $source"
        HealthConnectStepDataFreshness.STALE_CACHE -> "Laatst bekend: $steps stappen via $source"
        else -> null
    }
}

internal fun healthConnectLastCheckedMessage(
    lastSyncedAt: Long?,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String? = lastSyncedAt?.let { timestamp ->
    val time = Instant.ofEpochMilli(timestamp)
        .atZone(zoneId)
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    "Laatst gecontroleerd om $time"
}

internal fun healthTechnicalDetailsToggleLabel(expanded: Boolean): String =
    if (expanded) "Technische details verbergen" else "Technische details tonen"

internal fun healthConnectSettingsMessage(status: HealthConnectStatus): String = when {
    status.hasPartialHealthConnectAccess() -> status.message
    else -> when (status.state) {
        HealthConnectState.UNSUPPORTED -> "Health Connect wordt niet ondersteund op dit apparaat."
        HealthConnectState.PROVIDER_MISSING -> "Installeer of update Health Connect en vernieuw daarna de status."
        HealthConnectState.PERMISSION_REQUIRED -> "Geef toegang om stappen, hartslag, slaap, actieve calorieen en workouts te synchroniseren."
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
    aiStatus: SettingsAiStatus,
    telemetryOptIn: Boolean,
    workoutFeedbackPreferences: WorkoutFeedbackPreferences,
    reminderPreferences: ReminderPreferences,
    onboardingPreferences: OnboardingPreferences = OnboardingPreferences(),
    profile: UserProfile?,
    healthStatus: HealthConnectStatus,
    importPreview: AppDataImportPreview? = null,
    isImporting: Boolean = false,
    message: UiMessage?,
): SettingsUiState = SettingsUiState.Success(
    themeMode = themeMode,
    aiStatus = aiStatus,
    telemetryOptIn = telemetryOptIn,
    workoutFeedbackPreferences = workoutFeedbackPreferences,
    reminderPreferences = reminderPreferences,
    onboardingPreferences = onboardingPreferences,
    profile = profile,
    healthStatus = healthStatus,
    importPreview = importPreview,
    isImporting = isImporting,
    message = message,
)

internal fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

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

internal fun AiPreferences.toSettingsAiStatus(): SettingsAiStatus =
    SettingsAiStatus(
        enabled = enabled,
        preferredProvider = preferredProvider,
        hasGeminiKey = geminiApiKey.isNotBlank(),
        hasOpenAiKey = openAiApiKey.isNotBlank(),
        maskedGeminiKey = maskedSettingsApiKey(geminiApiKey),
        maskedOpenAiKey = maskedSettingsApiKey(openAiApiKey),
    )

internal fun aiProviderStatusLabel(aiStatus: SettingsAiStatus): String = when {
    !aiStatus.enabled -> "Uitgeschakeld"
    aiStatus.hasGeminiKey && aiStatus.hasOpenAiKey -> buildString {
        append("Klaar: ${aiStatus.preferredProvider.label}")
        append(", tweede provider opgeslagen")
    }
    aiStatus.hasGeminiKey -> "Klaar: Gemini 2.5 Flash"
    aiStatus.hasOpenAiKey -> "Klaar: OpenAI"
    else -> "Geen AI-sleutel ingesteld"
}

internal fun settingsOverflowSectionTitle(): String = "Meer"

internal fun settingsOverflowSectionBody(): String =
    "Compacte navigatie: instellingen, voorkeuren en appbeheer staan hier. Trends en grafieken open je via Voortgang hieronder."

internal fun settingsOpenProgressActionLabel(): String = "Voortgang openen"

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

private const val SamsungHealthPackageName = "com.sec.android.app.shealth"
private const val SettingsLogTag = "TrainIQSettings"

private fun ProfileInputValidationError?.isFor(field: ProfileInputField): Boolean = this?.field == field

private fun ProfileInputValidationError?.supportingTextFor(field: ProfileInputField): (@Composable () -> Unit)? {
    val error = takeIf { it.isFor(field) } ?: return null
    return { Text(error.message) }
}
