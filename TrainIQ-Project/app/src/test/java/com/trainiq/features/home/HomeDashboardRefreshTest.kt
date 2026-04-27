package com.trainiq.features.home

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class HomeDashboardRefreshTest {
    @Test
    fun homeRefreshGate_whenRefreshIsInFlight_rejectsDuplicateStart() {
        val gate = HomeRefreshGate()

        assertTrue(gate.tryStart())
        assertFalse(gate.tryStart())
    }

    @Test
    fun homeRefreshGate_afterRefreshFinishes_allowsNextStart() {
        val gate = HomeRefreshGate()

        assertTrue(gate.tryStart())
        gate.finish()

        assertTrue(gate.tryStart())
    }

    @Test
    fun refreshDashboardDataSafely_whenRefreshSucceeds_returnsTrue() = runTest {
        val result = refreshDashboardDataSafely { }

        assertTrue(result)
    }

    @Test
    fun refreshDashboardDataSafely_whenRefreshFails_returnsFalse() = runTest {
        val result = refreshDashboardDataSafely { error("Health Connect failed") }

        assertFalse(result)
    }

    @Test
    fun refreshDashboardDataSafely_whenCancelled_rethrowsCancellation() = runTest {
        try {
            refreshDashboardDataSafely { throw CancellationException("cancelled") }
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
        }
    }

    @Test
    fun buildHomeRecoverySubtitle_whenHeartRateMissing_omitsHeartRateSegment() {
        val result = buildHomeRecoverySubtitle(
            averageHeartRateBpm = null,
            todaysWorkoutCalories = 320,
        )

        assertTrue(result == "Stappen - Training 320 kcal")
    }

    @Test
    fun buildHomeRecoverySubtitle_whenHeartRateAvailable_includesHeartRateSegment() {
        val result = buildHomeRecoverySubtitle(
            averageHeartRateBpm = 64,
            todaysWorkoutCalories = 180,
        )

        assertTrue(result == "Stappen - Gem. hartslag 64 - Training 180 kcal")
    }
}
