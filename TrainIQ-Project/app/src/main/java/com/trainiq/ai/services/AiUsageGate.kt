package com.trainiq.ai.services

import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.security.GeminiKeyMigration
import com.trainiq.core.security.OpenAiKeyStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class AiUsageGate @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val geminiKeyMigration: GeminiKeyMigration,
    private val openAiKeyStore: OpenAiKeyStore,
) {
    suspend fun currentSettings(): AiPreferences {
        val legacySettings = preferencesRepository.aiPreferences.first()
        return resolveSettings(legacySettings)
    }

    suspend fun resolveSettings(legacySettings: AiPreferences): AiPreferences =
        geminiKeyMigration.currentKey(
                legacyKey = legacySettings.apiKey,
                onLegacyMigrated = { preferencesRepository.clearGeminiApiKey() },
            ).orEmpty().let { geminiKey ->
            val openAiKey = openAiKeyStore.currentKey().orEmpty()
            legacySettings.copy(
                apiKey = geminiKey,
                geminiApiKey = geminiKey,
                openAiApiKey = openAiKey,
            )
        }

    suspend fun isAiReady(): Boolean {
        val settings = currentSettings()
        return settings.hasAnyReadyProvider()
    }

    suspend fun currentApiKeyOrNull(): String? {
        val settings = currentSettings()
        return settings.geminiApiKey.takeIf { settings.enabled && it.isNotBlank() }
    }

    suspend fun saveApiKey(apiKey: String): Boolean {
        val saved = geminiKeyMigration.saveKey(apiKey)
        if (saved) preferencesRepository.clearGeminiApiKey()
        return saved
    }

    suspend fun saveOpenAiApiKey(apiKey: String): Boolean =
        openAiKeyStore.saveKey(apiKey)

    suspend fun setProviderPreference(preference: AiProviderPreference) {
        preferencesRepository.setAiProviderPreference(preference)
    }

    suspend fun clearEncryptedApiKey() {
        geminiKeyMigration.clearEncryptedKey()
        preferencesRepository.clearGeminiApiKey()
    }

    suspend fun clearOpenAiApiKey() {
        openAiKeyStore.clearEncryptedKey()
    }

    suspend fun clearAllAiKeys() {
        geminiKeyMigration.clearEncryptedKey()
        openAiKeyStore.clearEncryptedKey()
        preferencesRepository.clearGeminiApiKey()
    }
}
