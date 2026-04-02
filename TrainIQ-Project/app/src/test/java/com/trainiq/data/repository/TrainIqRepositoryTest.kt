package com.trainiq.data.repository

import com.trainiq.domain.model.ReadinessLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainIqRepositoryTest {
    @Test
    fun resolveReadiness_withLowRpeAndCompletedSessions_returnsIncrease() {
        assertEquals(
            ReadinessLevel.INCREASE,
            resolveReadiness(lastSessionAvgRpe = 6.9f, completedRecentSessions = true),
        )
    }

    @Test
    fun resolveReadiness_withHighRpe_returnsDeload() {
        assertEquals(
            ReadinessLevel.DELOAD,
            resolveReadiness(lastSessionAvgRpe = 9.1f, completedRecentSessions = true),
        )
    }

    @Test
    fun resolveReadiness_withBoundaryConditions_returnsMaintain() {
        assertEquals(
            ReadinessLevel.MAINTAIN,
            resolveReadiness(lastSessionAvgRpe = 7.0f, completedRecentSessions = true),
        )
        assertEquals(
            ReadinessLevel.MAINTAIN,
            resolveReadiness(lastSessionAvgRpe = 6.2f, completedRecentSessions = false),
        )
        assertEquals(
            ReadinessLevel.MAINTAIN,
            resolveReadiness(lastSessionAvgRpe = 9.0f, completedRecentSessions = true),
        )
    }
}
