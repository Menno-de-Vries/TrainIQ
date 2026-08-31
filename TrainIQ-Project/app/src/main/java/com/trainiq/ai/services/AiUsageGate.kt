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
    private val openAiVerificationRecorder: OpenAiVerificationRecorder,
) {
    val openAiVerificationSnapshots = openAiVerificationRecorder.snapshots

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
        return currentReadiness() == AiReadiness.CONFIGURED
    }

    suspend fun currentReadiness(): AiReadiness = currentSettings().readiness()

    suspend fun currentApiKeyOrNull(): String? {
        val settings = currentSettings()
        return settings.geminiApiKey.takeIf { settings.enabled && it.isNotBlank() }
    }

    suspend fun saveApiKey(apiKey: String): Boolean {
        val saved = geminiKeyMigration.saveKey(apiKey)
        if (saved) preferencesRepository.clearGeminiApiKey()
        return saved
    }

    suspend fun saveOpenAiApiKey(apiKey: String): Boolean {
        val saved = openAiKeyStore.saveKey(apiKey)
        if (saved) openAiVerificationRecorder.clear()
        return saved
    }

    suspend fun setProviderPreference(preference: AiProviderPreference) {
        preferencesRepository.setAiProviderPreference(preference)
    }

    suspend fun clearEncryptedApiKey() {
        geminiKeyMigration.clearEncryptedKey()
        preferencesRepository.clearGeminiApiKey()
    }

    suspend fun clearOpenAiApiKey() {
        openAiKeyStore.clearEncryptedKey()
        openAiVerificationRecorder.clear()
    }

    suspend fun clearAllAiKeys() {
        geminiKeyMigration.clearEncryptedKey()
        openAiKeyStore.clearEncryptedKey()
        openAiVerificationRecorder.clear()
        preferencesRepository.clearGeminiApiKey()
    }

    suspend fun recordOpenAiVerificationSuccess(feature: AiFeature) {
        openAiVerificationRecorder.recordSuccess(feature)
    }

    internal suspend fun recordOpenAiVerificationFailure(failure: AiProviderRequestException) {
        openAiVerificationRecorder.recordFailure(failure)
    }
}
