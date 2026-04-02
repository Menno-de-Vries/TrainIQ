package com.trainiq.data.repository

import com.trainiq.domain.model.ReadinessLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainIqRepositoryProgressionTest {
    @Test
    fun resolveReadiness_whenLowRpeAndCompletedRecentSessions_returnsIncrease() {
        assertEquals(ReadinessLevel.INCREASE, resolveReadiness(lastSessionAvgRpe = 6.5f, completedRecentSessions = true))
    }

    @Test
    fun resolveReadiness_whenRpeIsVeryHigh_returnsDeload() {
        assertEquals(ReadinessLevel.DELOAD, resolveReadiness(lastSessionAvgRpe = 9.3f, completedRecentSessions = true))
    }

    @Test
    fun resolveReadiness_whenConditionsAreMixed_returnsMaintain() {
        assertEquals(ReadinessLevel.MAINTAIN, resolveReadiness(lastSessionAvgRpe = 7.5f, completedRecentSessions = true))
        assertEquals(ReadinessLevel.MAINTAIN, resolveReadiness(lastSessionAvgRpe = 6.4f, completedRecentSessions = false))
    }
}
