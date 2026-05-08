package com.trainiq.ai.services

import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.security.GeminiKeyMigration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class AiUsageGate @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val geminiKeyMigration: GeminiKeyMigration,
) {
    suspend fun currentSettings(): AiPreferences {
        val legacySettings = preferencesRepository.aiPreferences.first()
        return resolveSettings(legacySettings)
    }

    suspend fun resolveSettings(legacySettings: AiPreferences): AiPreferences =
        legacySettings.copy(
            apiKey = geminiKeyMigration.currentKey(
                legacyKey = legacySettings.apiKey,
                onLegacyMigrated = { preferencesRepository.clearGeminiApiKey() },
            ).orEmpty(),
        )

    suspend fun isAiReady(): Boolean {
        val settings = currentSettings()
        return settings.enabled && settings.apiKey.isNotBlank()
    }

    suspend fun currentApiKeyOrNull(): String? {
        val settings = currentSettings()
        return settings.apiKey.takeIf { settings.enabled && it.isNotBlank() }
    }

    suspend fun saveApiKey(apiKey: String): Boolean =
        geminiKeyMigration.saveKey(apiKey)

    suspend fun clearEncryptedApiKey() {
        geminiKeyMigration.clearEncryptedKey()
    }
}
