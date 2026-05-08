package com.trainiq.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Header
import retrofit2.http.Query

class GeminiApiContractTest {
    @Test
    fun generateContentUsesHeaderAuthInsteadOfQueryAuth() {
        val method = GeminiApi::class.java.methods.single { it.name == "generateContent" }
        val parameterAnnotations = method.parameterAnnotations.flatten()

        assertFalse(parameterAnnotations.any { it is Query && it.value == "key" })
        assertTrue(parameterAnnotations.any { it is Header && it.value == "x-goog-api-key" })
    }
}
