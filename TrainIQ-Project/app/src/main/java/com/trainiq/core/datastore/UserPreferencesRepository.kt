package com.trainiq.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trainiq.ai.services.AiProviderPreference
import com.trainiq.ai.services.OpenAiVerificationSnapshot
import com.trainiq.ai.services.OpenAiVerificationSnapshotCodec
import com.trainiq.core.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "trainiq_preferences")

data class AiPreferences(
    val enabled: Boolean,
    val apiKey: String,
    val preferredProvider: AiProviderPreference = AiProviderPreference.GEMINI_FIRST,
    val geminiApiKey: String = apiKey,
    val openAiApiKey: String = "",
    val allowCrossProviderFallback: Boolean = false,
)

data class WorkoutFeedbackPreferences(
    val restTimerSoundEnabled: Boolean = true,
    val workoutHapticsEnabled: Boolean = true,
)

data class HealthConnectSyncPreferences(
    val changesToken: String,
    val cacheStateJson: String,
    val lastSyncedAt: Long,
    val changesTokensJson: String = "",
)

data class OnboardingPreferences(
    val completed: Boolean = false,
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
    val aiSetupDeferred: Boolean = false,
    val remindersEnabled: Boolean = false,
    val privacyAcknowledged: Boolean = false,
    val guidedTourCompleted: Boolean = false,
    val guidedTourSkipped: Boolean = false,
)

data class ReminderPreferences(
    val enabled: Boolean = false,
    val lastMealReminderAt: Long = 0L,
    val lastWorkoutReminderAt: Long = 0L,
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val streakKey = intPreferencesKey("streak_count")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val aiEnabledKey = booleanPreferencesKey("ai_enabled")
    private val aiProviderPreferenceKey = stringPreferencesKey("ai_provider_preference")
    private val geminiApiKey = stringPreferencesKey("gemini_api_key")
    private val openAiVerificationSnapshotKey = stringPreferencesKey("openai_verification_snapshot")
    private val telemetryOptInKey = booleanPreferencesKey("telemetry_opt_in")
    private val restTimerSoundEnabledKey = booleanPreferencesKey("rest_timer_sound_enabled")
    private val workoutHapticsEnabledKey = booleanPreferencesKey("workout_haptics_enabled")
    private val healthChangesTokenKey = stringPreferencesKey("health_connect_changes_token")
    private val healthMetricChangesTokensKey = stringPreferencesKey("health_connect_metric_changes_tokens")
    private val healthCacheStateKey = stringPreferencesKey("health_connect_cache_state")
    private val healthLastSyncedAtKey = stringPreferencesKey("health_connect_last_synced_at")
    private val remindersEnabledKey = booleanPreferencesKey("reminders_enabled")
    private val lastMealReminderAtKey = longPreferencesKey("last_meal_reminder_at")
    private val lastWorkoutReminderAtKey = longPreferencesKey("last_workout_reminder_at")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val onboardingGoalKey = stringPreferencesKey("onboarding_goal")
    private val onboardingExperienceKey = stringPreferencesKey("onboarding_experience")
    private val onboardingTrainingDaysKey = intPreferencesKey("onboarding_training_days")
    private val onboardingEquipmentKey = stringPreferencesKey("onboarding_equipment")
    private val onboardingSessionLengthMinutesKey = intPreferencesKey("onboarding_session_length_minutes")
    private val onboardingConstraintsKey = stringPreferencesKey("onboarding_constraints")
    private val onboardingHealthConnectAcceptedKey = booleanPreferencesKey("onboarding_health_connect_accepted")
    private val onboardingHealthConnectSkippedKey = booleanPreferencesKey("onboarding_health_connect_skipped")
    private val onboardingAiAcceptedKey = booleanPreferencesKey("onboarding_ai_accepted")
    private val onboardingAiSkippedKey = booleanPreferencesKey("onboarding_ai_skipped")
    private val onboardingAiSetupDeferredKey = booleanPreferencesKey("onboarding_ai_setup_deferred")
    private val onboardingRemindersEnabledKey = booleanPreferencesKey("onboarding_reminders_enabled")
    private val onboardingPrivacyAcknowledgedKey = booleanPreferencesKey("onboarding_privacy_acknowledged")
    private val onboardingGuidedTourCompletedKey = booleanPreferencesKey("onboarding_guided_tour_completed")
    private val onboardingGuidedTourSkippedKey = booleanPreferencesKey("onboarding_guided_tour_skipped")

    val streakCount: Flow<Int> = context.dataStore.data.map { preferences -> preferences[streakKey] ?: 0 }
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        preferences[themeModeKey]?.let(ThemeMode::fromStorageValue) ?: ThemeMode.SYSTEM
    }
    val aiPreferences: Flow<AiPreferences> = context.dataStore.data.map { preferences ->
        AiPreferences(
            enabled = preferences[aiEnabledKey] ?: false,
            apiKey = preferences[geminiApiKey].orEmpty(),
            preferredProvider = preferences[aiProviderPreferenceKey]
                ?.let(AiProviderPreference::fromStorageValue)
                ?: AiProviderPreference.GEMINI_FIRST,
        )
    }
    val openAiVerificationSnapshot: Flow<OpenAiVerificationSnapshot?> = context.dataStore.data.map { preferences ->
        OpenAiVerificationSnapshotCodec.decode(preferences[openAiVerificationSnapshotKey])
    }
    val telemetryOptIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[telemetryOptInKey] ?: false
    }
    val workoutFeedbackPreferences: Flow<WorkoutFeedbackPreferences> = context.dataStore.data.map { preferences ->
        WorkoutFeedbackPreferences(
            restTimerSoundEnabled = preferences[restTimerSoundEnabledKey] ?: true,
            workoutHapticsEnabled = preferences[workoutHapticsEnabledKey] ?: true,
        )
    }
    val reminderPreferences: Flow<ReminderPreferences> = context.dataStore.data.map { preferences ->
        ReminderPreferences(
            enabled = preferences[remindersEnabledKey] ?: false,
            lastMealReminderAt = preferences[lastMealReminderAtKey] ?: 0L,
            lastWorkoutReminderAt = preferences[lastWorkoutReminderAtKey] ?: 0L,
        )
    }
    val onboardingPreferences: Flow<OnboardingPreferences> = context.dataStore.data.map { preferences ->
        OnboardingPreferences(
            completed = preferences[onboardingCompletedKey] ?: false,
            goal = preferences[onboardingGoalKey].orEmpty(),
            experience = preferences[onboardingExperienceKey].orEmpty(),
            trainingDays = preferences[onboardingTrainingDaysKey] ?: 3,
            equipment = preferences[onboardingEquipmentKey].orEmpty(),
            sessionLengthMinutes = preferences[onboardingSessionLengthMinutesKey] ?: 60,
            constraints = preferences[onboardingConstraintsKey].orEmpty(),
            healthConnectAccepted = preferences[onboardingHealthConnectAcceptedKey] ?: false,
            healthConnectSkipped = preferences[onboardingHealthConnectSkippedKey] ?: false,
            aiAccepted = preferences[onboardingAiAcceptedKey] ?: false,
            aiSkipped = preferences[onboardingAiSkippedKey] ?: false,
            aiSetupDeferred = preferences[onboardingAiSetupDeferredKey] ?: false,
            remindersEnabled = preferences[onboardingRemindersEnabledKey] ?: false,
            privacyAcknowledged = preferences[onboardingPrivacyAcknowledgedKey] ?: false,
            guidedTourCompleted = preferences[onboardingGuidedTourCompletedKey] ?: false,
            guidedTourSkipped = preferences[onboardingGuidedTourSkippedKey] ?: false,
        )
    }

    suspend fun setStreak(count: Int) {
        context.dataStore.edit { preferences -> preferences[streakKey] = count }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences -> preferences[themeModeKey] = mode.storageValue }
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[aiEnabledKey] = enabled }
    }

    suspend fun setAiProviderPreference(preference: AiProviderPreference) {
        context.dataStore.edit { preferences -> preferences[aiProviderPreferenceKey] = preference.storageValue }
    }

    suspend fun clearGeminiApiKey() {
        context.dataStore.edit { preferences -> preferences.remove(geminiApiKey) }
    }

    suspend fun saveOpenAiVerificationSnapshot(snapshot: OpenAiVerificationSnapshot) {
        context.dataStore.edit { preferences ->
            preferences[openAiVerificationSnapshotKey] = OpenAiVerificationSnapshotCodec.encode(snapshot)
        }
    }

    suspend fun clearOpenAiVerificationSnapshot() {
        context.dataStore.edit { preferences -> preferences.remove(openAiVerificationSnapshotKey) }
    }

    suspend fun setTelemetryOptIn(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[telemetryOptInKey] = enabled }
    }

    suspend fun setRestTimerSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[restTimerSoundEnabledKey] = enabled }
    }

    suspend fun setWorkoutHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[workoutHapticsEnabledKey] = enabled }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[remindersEnabledKey] = enabled }
    }

    suspend fun getReminderPreferences(): ReminderPreferences = context.dataStore.data
        .map { preferences ->
            ReminderPreferences(
                enabled = preferences[remindersEnabledKey] ?: false,
                lastMealReminderAt = preferences[lastMealReminderAtKey] ?: 0L,
                lastWorkoutReminderAt = preferences[lastWorkoutReminderAtKey] ?: 0L,
            )
        }
        .first()

    suspend fun markMealReminderShown(atMillis: Long) {
        context.dataStore.edit { preferences -> preferences[lastMealReminderAtKey] = atMillis }
    }

    suspend fun markWorkoutReminderShown(atMillis: Long) {
        context.dataStore.edit { preferences -> preferences[lastWorkoutReminderAtKey] = atMillis }
    }

    suspend fun getOnboardingPreferences(): OnboardingPreferences = onboardingPreferences.first()

    suspend fun saveOnboardingPreferences(onboardingPreferences: OnboardingPreferences) {
        context.dataStore.edit { preferences ->
            preferences[onboardingCompletedKey] = onboardingPreferences.completed
            preferences[onboardingGoalKey] = onboardingPreferences.goal
            preferences[onboardingExperienceKey] = onboardingPreferences.experience
            preferences[onboardingTrainingDaysKey] = onboardingPreferences.trainingDays
            preferences[onboardingEquipmentKey] = onboardingPreferences.equipment
            preferences[onboardingSessionLengthMinutesKey] = onboardingPreferences.sessionLengthMinutes
            preferences[onboardingConstraintsKey] = onboardingPreferences.constraints
            preferences[onboardingHealthConnectAcceptedKey] = onboardingPreferences.healthConnectAccepted
            preferences[onboardingHealthConnectSkippedKey] = onboardingPreferences.healthConnectSkipped
            preferences[onboardingAiAcceptedKey] = onboardingPreferences.aiAccepted
            preferences[onboardingAiSkippedKey] = onboardingPreferences.aiSkipped
            preferences[onboardingAiSetupDeferredKey] = onboardingPreferences.aiSetupDeferred
            preferences[onboardingRemindersEnabledKey] = onboardingPreferences.remindersEnabled
            preferences[onboardingPrivacyAcknowledgedKey] = onboardingPreferences.privacyAcknowledged
            preferences[onboardingGuidedTourCompletedKey] = onboardingPreferences.guidedTourCompleted
            preferences[onboardingGuidedTourSkippedKey] = onboardingPreferences.guidedTourSkipped
            if (onboardingPreferences.remindersEnabled) {
                preferences[remindersEnabledKey] = true
            }
        }
    }

    suspend fun completeOnboarding(onboardingPreferences: OnboardingPreferences) {
        saveOnboardingPreferences(onboardingPreferences.copy(completed = true, privacyAcknowledged = true))
    }

    suspend fun reopenOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[onboardingGuidedTourCompletedKey] = false
            preferences[onboardingGuidedTourSkippedKey] = false
        }
    }

    suspend fun markGuidedTourCompleted() {
        context.dataStore.edit { preferences ->
            preferences[onboardingGuidedTourCompletedKey] = true
            preferences[onboardingGuidedTourSkippedKey] = false
        }
    }

    suspend fun markGuidedTourSkipped() {
        context.dataStore.edit { preferences ->
            preferences[onboardingGuidedTourCompletedKey] = false
            preferences[onboardingGuidedTourSkippedKey] = true
        }
    }

    suspend fun getHealthConnectSyncPreferences(): HealthConnectSyncPreferences {
        val preferences = context.dataStore.data.first()
        return HealthConnectSyncPreferences(
            changesToken = preferences[healthChangesTokenKey].orEmpty(),
            cacheStateJson = preferences[healthCacheStateKey].orEmpty(),
            lastSyncedAt = preferences[healthLastSyncedAtKey]?.toLongOrNull() ?: 0L,
            changesTokensJson = preferences[healthMetricChangesTokensKey].orEmpty(),
        )
    }

    suspend fun saveHealthConnectSyncPreferences(
        changesToken: String,
        cacheStateJson: String,
        lastSyncedAt: Long,
        changesTokensJson: String = "",
    ) {
        context.dataStore.edit { preferences ->
            preferences[healthChangesTokenKey] = changesToken
            preferences[healthCacheStateKey] = cacheStateJson
            preferences[healthLastSyncedAtKey] = lastSyncedAt.toString()
            if (changesTokensJson.isBlank()) {
                preferences.remove(healthMetricChangesTokensKey)
            } else {
                preferences[healthMetricChangesTokensKey] = changesTokensJson
            }
        }
    }

    suspend fun clearHealthConnectSyncPreferences() {
        context.dataStore.edit { preferences ->
            preferences.remove(healthChangesTokenKey)
            preferences.remove(healthMetricChangesTokensKey)
            preferences.remove(healthCacheStateKey)
            preferences.remove(healthLastSyncedAtKey)
        }
    }

    suspend fun clearLocalPrivateData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
