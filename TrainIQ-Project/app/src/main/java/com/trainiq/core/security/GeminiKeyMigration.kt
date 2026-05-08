package com.trainiq.core.security

import javax.inject.Inject
import javax.inject.Singleton

interface GeminiEncryptedKeyStore {
    suspend fun readKey(): String?
    suspend fun writeKey(apiKey: String): Boolean
    suspend fun clearKey()
}

@Singleton
class GeminiKeyMigration @Inject constructor(
    private val encryptedKeyStore: GeminiEncryptedKeyStore,
) {
    suspend fun currentKey(
        legacyKey: String,
        onLegacyMigrated: suspend () -> Unit = {},
    ): String? {
        encryptedKeyStore.readKey()?.takeIf { it.isNotBlank() }?.let { return it }
        val normalizedLegacy = legacyKey.trim().takeIf { it.isNotBlank() } ?: return null
        if (saveKey(normalizedLegacy)) {
            onLegacyMigrated()
            return encryptedKeyStore.readKey()?.takeIf { it.isNotBlank() }
        }
        return null
    }

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
