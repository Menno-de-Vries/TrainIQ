package com.trainiq.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trainiq.core.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "trainiq_preferences")

data class AiPreferences(
    val enabled: Boolean,
    val apiKey: String,
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val streakKey = intPreferencesKey("streak_count")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val aiEnabledKey = booleanPreferencesKey("ai_enabled")
    private val geminiApiKey = stringPreferencesKey("gemini_api_key")

    val streakCount: Flow<Int> = context.dataStore.data.map { preferences -> preferences[streakKey] ?: 0 }
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        preferences[themeModeKey]?.let(ThemeMode::fromStorageValue) ?: ThemeMode.SYSTEM
    }
    val aiPreferences: Flow<AiPreferences> = context.dataStore.data.map { preferences ->
        AiPreferences(
            enabled = preferences[aiEnabledKey] ?: false,
            apiKey = preferences[geminiApiKey].orEmpty(),
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

    suspend fun saveGeminiApiKey(apiKey: String) {
        context.dataStore.edit { preferences -> preferences[geminiApiKey] = apiKey.trim() }
    }

    suspend fun clearGeminiApiKey() {
        context.dataStore.edit { preferences -> preferences.remove(geminiApiKey) }
    }
}
