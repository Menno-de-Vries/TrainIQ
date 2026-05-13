package com.trainiq.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trainiq.ai.services.AiProviderPreference
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

@Singleton
class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val streakKey = intPreferencesKey("streak_count")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val aiEnabledKey = booleanPreferencesKey("ai_enabled")
    private val aiProviderPreferenceKey = stringPreferencesKey("ai_provider_preference")
    private val geminiApiKey = stringPreferencesKey("gemini_api_key")
    private val telemetryOptInKey = booleanPreferencesKey("telemetry_opt_in")
    private val restTimerSoundEnabledKey = booleanPreferencesKey("rest_timer_sound_enabled")
    private val workoutHapticsEnabledKey = booleanPreferencesKey("workout_haptics_enabled")
    private val healthChangesTokenKey = stringPreferencesKey("health_connect_changes_token")
    private val healthMetricChangesTokensKey = stringPreferencesKey("health_connect_metric_changes_tokens")
    private val healthCacheStateKey = stringPreferencesKey("health_connect_cache_state")
    private val healthLastSyncedAtKey = stringPreferencesKey("health_connect_last_synced_at")

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
    val telemetryOptIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[telemetryOptInKey] ?: false
    }
    val workoutFeedbackPreferences: Flow<WorkoutFeedbackPreferences> = context.dataStore.data.map { preferences ->
        WorkoutFeedbackPreferences(
            restTimerSoundEnabled = preferences[restTimerSoundEnabledKey] ?: true,
            workoutHapticsEnabled = preferences[workoutHapticsEnabledKey] ?: true,
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

    suspend fun setTelemetryOptIn(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[telemetryOptInKey] = enabled }
    }

    suspend fun setRestTimerSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[restTimerSoundEnabledKey] = enabled }
    }

    suspend fun setWorkoutHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[workoutHapticsEnabledKey] = enabled }
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
