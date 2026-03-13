package com.trainiq.ai.services

import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class AiUsageGate @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
) {
    suspend fun currentSettings(): AiPreferences = preferencesRepository.aiPreferences.first()

    suspend fun isAiReady(): Boolean {
        val settings = currentSettings()
        return settings.enabled && settings.apiKey.isNotBlank()
    }

    suspend fun currentApiKeyOrNull(): String? {
        val settings = currentSettings()
        return settings.apiKey.takeIf { settings.enabled && it.isNotBlank() }
    }
}
