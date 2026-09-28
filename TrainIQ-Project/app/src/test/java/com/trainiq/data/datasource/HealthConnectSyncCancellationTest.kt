package com.trainiq.data.datasource

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectSyncCancellationTest {
    @Test
    fun connectedAndIncrementalSyncUseCancellationPreservingBoundaries() {
        val source = File("src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt").readText()
        val connectedStatus = source.substringAfter("private suspend fun fetchConnectedStatus(")
            .substringBefore("private fun grantedMetrics(")
        val incrementalSync = source.substringAfter("private suspend fun syncTrackedMetrics(")
            .substringBefore("private suspend fun aggregateStepsToday(")

        assertTrue(connectedStatus.contains("runHealthConnectSyncCatchingCancellation"))
        assertTrue(incrementalSync.contains("runHealthConnectSyncCatchingCancellation"))
    }

    @Test
    fun syncBoundaryRethrowsCoroutineCancellation() = runTest {
        val cancellation = CancellationException("worker stopped")
        val actual = try {
            runHealthConnectSyncCatchingCancellation<Int> { throw cancellation }
            throw AssertionError("Expected coroutine cancellation to propagate")
        } catch (exception: CancellationException) {
            exception
        }

        assertSame(cancellation, actual)
    }
}
