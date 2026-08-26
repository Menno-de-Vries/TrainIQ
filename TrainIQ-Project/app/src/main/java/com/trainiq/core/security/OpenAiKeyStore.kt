package com.trainiq.core.security

import javax.inject.Inject
import javax.inject.Singleton

interface OpenAiEncryptedKeyStore {
    suspend fun readKey(): String?
    suspend fun writeKey(apiKey: String): Boolean
    suspend fun clearKey()
}

@Singleton
class OpenAiKeyStore @Inject constructor(
    private val encryptedKeyStore: OpenAiEncryptedKeyStore,
) {
    suspend fun currentKey(): String? =
        encryptedKeyStore.readKey()?.takeIf { it.isNotBlank() }

    suspend fun saveKey(apiKey: String): Boolean {
        val normalized = apiKey.trim()
        if (normalized.isBlank()) return false
        if (!encryptedKeyStore.writeKey(normalized)) return false
        return encryptedKeyStore.readKey() == normalized
    }

    suspend fun clearEncryptedKey() {
        encryptedKeyStore.clearKey()
    }
}
