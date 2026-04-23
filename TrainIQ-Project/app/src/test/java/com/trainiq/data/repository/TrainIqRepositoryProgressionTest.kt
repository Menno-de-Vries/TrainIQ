package com.trainiq.data.repository

import com.trainiq.domain.model.ReadinessLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainIqRepositoryProgressionTest {
    @Test
    fun resolveReadiness_whenLowRpeAndCompletedRecentSessions_returnsIncrease() {
        assertEquals(ReadinessLevel.INCREASE, resolveReadiness(lastSessionAvgRpe = 6.5f, recentAverageRir = 2.0f, completedRecentSessions = true))
    }

    @Test
    fun resolveReadiness_whenRpeIsVeryHigh_returnsDeload() {
        assertEquals(ReadinessLevel.DELOAD, resolveReadiness(lastSessionAvgRpe = 9.3f, recentAverageRir = 1.0f, completedRecentSessions = true))
    }

    @Test
    fun resolveReadiness_whenConditionsAreMixed_returnsMaintain() {
        assertEquals(ReadinessLevel.MAINTAIN, resolveReadiness(lastSessionAvgRpe = 7.5f, recentAverageRir = 1.4f, completedRecentSessions = true))
        assertEquals(ReadinessLevel.MAINTAIN, resolveReadiness(lastSessionAvgRpe = 6.4f, recentAverageRir = 2.4f, completedRecentSessions = false))
    }

    @Test
    fun resolveReadiness_whenPlateauedWithRecoveryRoom_returnsPlateau() {
        assertEquals(
            ReadinessLevel.PLATEAU,
            resolveReadiness(
                lastSessionAvgRpe = 7.0f,
                recentAverageRir = 2.5f,
                completedRecentSessions = false,
                plateauDetected = true,
            ),
        )
    }
}
