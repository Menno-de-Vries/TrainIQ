package com.trainiq.features.home

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class HomeDashboardRefreshTest {
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
}
