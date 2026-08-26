package com.trainiq.ai.services

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertTrue
import org.junit.Test

class AiUsageGateSourceTest {
    @Test
    fun geminiKeyWriteAndClearPathsRemoveLegacyPlaintextKey() {
        val source = Files.readString(
            Paths.get("src/main/java/com/trainiq/ai/services/AiUsageGate.kt"),
        )

        val saveApiKeyBody = source.functionBody("saveApiKey")
        val clearEncryptedBody = source.functionBody("clearEncryptedApiKey")
        val clearAllBody = source.functionBody("clearAllAiKeys")

        assertTrue(saveApiKeyBody.contains("preferencesRepository.clearGeminiApiKey()"))
        assertTrue(saveApiKeyBody.contains("if (saved)"))
        assertTrue(clearEncryptedBody.contains("preferencesRepository.clearGeminiApiKey()"))
        assertTrue(clearAllBody.contains("preferencesRepository.clearGeminiApiKey()"))
    }

    private fun String.functionBody(functionName: String): String {
        val start = indexOf("fun $functionName")
        require(start >= 0) { "Missing function $functionName" }
        val bodyStart = indexOf('{', start)
        require(bodyStart >= 0) { "Missing body for $functionName" }
        var depth = 0
        for (index in bodyStart until length) {
            when (this[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return substring(bodyStart, index + 1)
                }
            }
        }
        error("Unterminated body for $functionName")
    }
}
