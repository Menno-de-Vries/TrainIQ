package com.trainiq.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AndroidKeystoreGeminiKeyStore @Inject constructor(
    @ApplicationContext context: Context,
) : GeminiEncryptedKeyStore {
    private val storage = context.getSharedPreferences(EncryptedGeminiPrefsName, Context.MODE_PRIVATE)

    override suspend fun readKey(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val iv = storage.getString(IvKey, null)?.decodeBase64() ?: return@runCatching null
            val ciphertext = storage.getString(CiphertextKey, null)?.decodeBase64() ?: return@runCatching null
            val cipher = Cipher.getInstance(Transformation)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GcmTagBits, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8).takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    override suspend fun writeKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val cipher = Cipher.getInstance(Transformation)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val ciphertext = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
            storage.edit()
                .putString(IvKey, cipher.iv.encodeBase64())
                .putString(CiphertextKey, ciphertext.encodeBase64())
                .commit()
        }.getOrDefault(false)
    }

    override suspend fun clearKey() = withContext(Dispatchers.IO) {
        storage.edit()
            .remove(IvKey)
            .remove(CiphertextKey)
            .commit()
        Unit
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
        (keyStore.getKey(KeyAlias, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KeyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private fun String.decodeBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
    private fun ByteArray.encodeBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
}

private const val AndroidKeyStore = "AndroidKeyStore"
private const val KeyAlias = "trainiq_gemini_api_key_v1"
private const val Transformation = "AES/GCM/NoPadding"
private const val GcmTagBits = 128
private const val EncryptedGeminiPrefsName = "trainiq_encrypted_gemini"
private const val IvKey = "gemini_api_key_iv"
private const val CiphertextKey = "gemini_api_key_ciphertext"
