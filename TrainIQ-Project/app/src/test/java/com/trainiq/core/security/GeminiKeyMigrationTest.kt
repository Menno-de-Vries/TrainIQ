package com.trainiq.core.security

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiKeyMigrationTest {
    @Test
    fun oldKeyPresent_encryptedMigrationSucceedsAndReadbackWins() = runTest {
        val store = FakeGeminiEncryptedKeyStore()
        val migration = GeminiKeyMigration(store)

        var clearedLegacy = false
        val key = migration.currentKey(
            legacyKey = "legacy-key",
            onLegacyMigrated = { clearedLegacy = true },
        )

        assertEquals("legacy-key", key)
        assertEquals("legacy-key", store.readKey())
        assertEquals(1, store.writeCount)
        assertTrue(clearedLegacy)
    }

    @Test
    fun encryptedReadbackSucceeds_withoutLegacyKey() = runTest {
        val store = FakeGeminiEncryptedKeyStore(initialKey = "encrypted-key")
        val migration = GeminiKeyMigration(store)

        val key = migration.currentKey(legacyKey = "")

        assertEquals("encrypted-key", key)
        assertEquals(0, store.writeCount)
    }

    @Test
    fun migrationFailureDoesNotUseOldPlaintextKey() = runTest {
        val store = FakeGeminiEncryptedKeyStore(writeSucceeds = false)
        val migration = GeminiKeyMigration(store)

        val key = migration.currentKey(legacyKey = "legacy-key")

        assertNull(key)
        assertFalse(store.clearCalled)
    }

    @Test
    fun noKey_returnsNullWithoutWriting() = runTest {
        val store = FakeGeminiEncryptedKeyStore()
        val migration = GeminiKeyMigration(store)

        assertNull(migration.currentKey(legacyKey = ""))
        assertEquals(0, store.writeCount)
    }

    @Test
    fun saveKey_usesEncryptedStoreAndVerifiesReadback() = runTest {
        val store = FakeGeminiEncryptedKeyStore()
        val migration = GeminiKeyMigration(store)

        assertTrue(migration.saveKey("new-key"))
        assertEquals("new-key", store.readKey())
    }

    @Test
    fun failedSaveDoesNotOverwriteExistingEncryptedKey() = runTest {
        val store = FakeGeminiEncryptedKeyStore(initialKey = "existing-key", writeSucceeds = false)
        val migration = GeminiKeyMigration(store)

        assertFalse(migration.saveKey("new-key"))
        assertEquals("existing-key", store.readKey())
    }

    @Test
    fun openAiSaveKey_usesEncryptedStoreAndVerifiesReadback() = runTest {
        val store = FakeOpenAiEncryptedKeyStore()
        val openAiKeyStore = OpenAiKeyStore(store)

        assertTrue(openAiKeyStore.saveKey(" sk-test-openai "))
        assertEquals("sk-test-openai", openAiKeyStore.currentKey())

        openAiKeyStore.clearEncryptedKey()

        assertNull(openAiKeyStore.currentKey())
        assertTrue(store.clearCalled)
    }

    @Test
    fun openAiFailedSaveDoesNotOverwriteExistingEncryptedKey() = runTest {
        val store = FakeOpenAiEncryptedKeyStore(initialKey = "existing-openai-key", writeSucceeds = false)
        val openAiKeyStore = OpenAiKeyStore(store)

        assertFalse(openAiKeyStore.saveKey("new-openai-key"))

        assertEquals("existing-openai-key", openAiKeyStore.currentKey())
    }

    private class FakeGeminiEncryptedKeyStore(
        initialKey: String? = null,
        private val writeSucceeds: Boolean = true,
    ) : GeminiEncryptedKeyStore {
        private var key: String? = initialKey
        var writeCount = 0
            private set
        var clearCalled = false
            private set

        override suspend fun readKey(): String? = key

        override suspend fun writeKey(apiKey: String): Boolean {
            writeCount += 1
            if (!writeSucceeds) return false
            key = apiKey
            return true
        }

        override suspend fun clearKey() {
            clearCalled = true
            key = null
        }
    }

    private class FakeOpenAiEncryptedKeyStore(
        initialKey: String? = null,
        private val writeSucceeds: Boolean = true,
    ) : OpenAiEncryptedKeyStore {
        private var key: String? = initialKey
        var clearCalled = false
            private set

        override suspend fun readKey(): String? = key

        override suspend fun writeKey(apiKey: String): Boolean {
            if (!writeSucceeds) return false
            key = apiKey
            return true
        }

        override suspend fun clearKey() {
            clearCalled = true
            key = null
        }
    }
}
