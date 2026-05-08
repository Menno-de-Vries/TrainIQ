package com.trainiq.domain.usecase

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ClearAppDataUseCaseTest {
    @Test
    fun clearAppDataOrchestratesRuntimeSecretsPreferencesAndDiagnostics() {
        val source = File("src/main/java/com/trainiq/domain/usecase/UseCases.kt").readText()
        val body = source.substringAfter("class ClearAppDataUseCase").substringBefore("private fun ProgressionSuggestion.toInitialDraft")

        assertTrue(body.contains("runtimeStore.clearAll()"))
        assertTrue(body.contains("aiUsageGate.clearEncryptedApiKey()"))
        assertTrue(body.contains("preferencesRepository.clearLocalPrivateData()"))
        assertTrue(body.contains("performanceSessionStore.clearAll()"))
    }
}
