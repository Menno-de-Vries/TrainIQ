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

        val key = migration.currentKey(legacyKey = "legacy-key")

        assertEquals("legacy-key", key)
        assertEquals("legacy-key", store.readKey())
        assertEquals(1, store.writeCount)
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
    fun migrationFailureDoesNotDeleteOldKey() = runTest {
        val store = FakeGeminiEncryptedKeyStore(writeSucceeds = false)
        val migration = GeminiKeyMigration(store)

        val key = migration.currentKey(legacyKey = "legacy-key")

        assertEquals("legacy-key", key)
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
}
