package com.trainiq.ai.services

import com.trainiq.domain.model.AiFallbackContext
import com.trainiq.domain.model.BodyMeasurementPhotoSource
import com.trainiq.domain.model.WeeklyReportSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AiSurfaceReadinessTest {
    @Test
    fun bodyPhotoDisabledReturnsTypedFallbackWithoutRemoteCall() = runTest {
        val generator = NeverCalledJsonGenerator()
        val service = BodyMeasurementPhotoService(
            aiJsonGenerator = generator,
            readinessProvider = { AiReadiness.DISABLED },
            imageBytesProvider = { error("Image decoding must not start while AI is disabled") },
        )

        val result = service.analyzeScaleImage("unused.jpg", "")

        assertEquals(BodyMeasurementPhotoSource.LOCAL_FALLBACK, result.source)
        assertEquals(AiFallbackContext.AI_DISABLED, result.fallbackContext)
        assertFalse(generator.called)
    }

    @Test
    fun weeklyReportMissingKeyReturnsTypedFallbackWithoutRemoteCall() = runTest {
        val generator = NeverCalledJsonGenerator()
        val service = WeeklyReportService(
            aiJsonGenerator = generator,
            readinessProvider = { AiReadiness.NO_DECRYPTABLE_KEY },
        )

        val result = service.generateWeeklyReport(volume = 5_000.0, weightTrend = 0.0, adherence = 80)

        assertEquals(WeeklyReportSource.LOCAL_FALLBACK, result.source)
        assertEquals(AiFallbackContext.NO_DECRYPTABLE_KEY, result.fallbackContext)
        assertFalse(generator.called)
    }

    @Test
    fun routineDisabledReturnsTypedFallbackWithoutRemoteCall() = runTest {
        val generator = NeverCalledJsonGenerator()
        val service = RoutineGeneratorService(
            aiJsonGenerator = generator,
            readinessProvider = { AiReadiness.DISABLED },
        )

        val result = service.generateRoutine(
            goal = "Sterker worden",
            targetFocus = "Full body",
            daysPerWeek = 3,
            equipment = "Barbell",
            experienceLevel = "intermediate",
            sessionDurationMinutes = 60,
            includeDeload = false,
        )

        assertEquals(GeneratedRoutineSource.LOCAL_FALLBACK, result.source)
        assertEquals(AiFallbackContext.AI_DISABLED, result.fallbackContext)
        assertFalse(generator.called)
    }

    private class NeverCalledJsonGenerator : AiJsonGenerator {
        var called = false

        override suspend fun generateJson(request: AiRouteRequest): AiRouteResult {
            called = true
            error("Remote boundary must not be called")
        }
    }
}
