package com.trainiq.core.security

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DurableKeyRemovalTest {
    @Test fun successfulRemovalCommitsOnce() {
        var commits = 0
        commitEncryptedKeyRemoval { commits++; true }
        assertEquals(1, commits)
    }

    @Test fun failedCommitCannotReportRemovalSuccess() {
        assertThrows(IllegalStateException::class.java) {
            commitEncryptedKeyRemoval { false }
        }
    }

    @Test fun storageExceptionRemainsFailure() {
        val error = IllegalStateException("Synthetic storage failure")
        assertSame(error, assertThrows(IllegalStateException::class.java) {
            commitEncryptedKeyRemoval { throw error }
        })
    }

    @Test fun bothProviderFacadesPropagateDurableRemovalFailure() = runTest {
        var successReported = false
        val gemini = GeminiKeyMigration(object : GeminiEncryptedKeyStore {
            override suspend fun readKey(): String? = null
            override suspend fun writeKey(apiKey: String) = false
            override suspend fun clearKey() = commitEncryptedKeyRemoval { false }
        })
        val openAi = OpenAiKeyStore(object : OpenAiEncryptedKeyStore {
            override suspend fun readKey(): String? = null
            override suspend fun writeKey(apiKey: String) = false
            override suspend fun clearKey() = commitEncryptedKeyRemoval { false }
        })
        for (clear in listOf<suspend () -> Unit>(gemini::clearEncryptedKey, openAi::clearEncryptedKey)) {
            try {
                clear()
                successReported = true
                fail("A failed durable clear must propagate")
            } catch (_: IllegalStateException) {
                assertFalse(successReported)
            }
        }
    }
}
