package com.trainiq.domain.usecase

import org.junit.Assert.assertTrue
import org.junit.Test

class AppDataImportLimitTest {
    @Test
    fun previewRejectsOversizedJsonBeforeParsing() {
        val oversized = "x".repeat(MaxTrainIqImportJsonChars + 1)

        val error = runCatching {
            PreviewAppDataImportUseCase().invoke(oversized)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("te groot"))
    }

    @Test
    fun previewRejectsExcessiveImportRowCount() {
        val exercises = (1..MaxTrainIqImportRows + 1).joinToString(separator = ",") { id ->
            """{"id":$id,"name":"Exercise $id","muscleGroup":"Full body","equipment":"None"}"""
        }
        val json = """{"exercises":[$exercises]}"""

        val error = runCatching {
            PreviewAppDataImportUseCase().invoke(json)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("te veel rijen"))
    }
}
