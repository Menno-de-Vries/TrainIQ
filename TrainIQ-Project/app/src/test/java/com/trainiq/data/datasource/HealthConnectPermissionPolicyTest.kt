package com.trainiq.data.datasource

import com.trainiq.domain.model.HealthMetricSyncState
import com.trainiq.domain.model.HealthMetricType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectPermissionPolicyTest {
    @Test
    fun canReadStepsWhenOnlyStepPermissionIsGranted() {
        assertTrue(
            hasHealthConnectPermission(
                grantedPermissions = setOf("steps"),
                requiredPermission = "steps",
            ),
        )
    }

    @Test
    fun doesNotRequireUnrelatedPermissionsForStepRefresh() {
        assertFalse(
            hasHealthConnectPermission(
                grantedPermissions = setOf("heart_rate", "sleep"),
                requiredPermission = "steps",
            ),
        )
    }

    @Test
    fun partialMetricPermissionsKeepGrantedMetricsAvailableAndDenyMissingMetrics() {
        val statuses = buildHealthMetricPermissionStatuses(
            grantedPermissions = setOf("steps", "sleep"),
            requiredPermissionsByMetric = mapOf(
                HealthMetricType.STEPS to "steps",
                HealthMetricType.HEART_RATE to "heart_rate",
                HealthMetricType.SLEEP to "sleep",
            ),
            lastSyncedAt = 123L,
        )

        assertEquals(HealthMetricSyncState.STALE, statuses.single { it.metric == HealthMetricType.STEPS }.state)
        assertEquals(HealthMetricSyncState.DENIED, statuses.single { it.metric == HealthMetricType.HEART_RATE }.state)
        assertEquals(HealthMetricSyncState.STALE, statuses.single { it.metric == HealthMetricType.SLEEP }.state)
    }

    @Test
    fun missingPermissionsDoNotReportGrantedMetricsAsDenied() {
        val statuses = buildHealthMetricPermissionStatuses(
            grantedPermissions = setOf("steps"),
            requiredPermissionsByMetric = mapOf(
                HealthMetricType.STEPS to "steps",
                HealthMetricType.ACTIVE_CALORIES to "calories",
            ),
            lastSyncedAt = null,
        )

        assertEquals(HealthMetricSyncState.SYNCING, statuses.single { it.metric == HealthMetricType.STEPS }.state)
        assertEquals(HealthMetricSyncState.DENIED, statuses.single { it.metric == HealthMetricType.ACTIVE_CALORIES }.state)
    }

    @Test
    fun successfulMetricReadsRemainSyncedWhenUnrelatedMetricFails() {
        val statuses = buildHealthMetricSyncStatuses(
            metrics = HealthMetricType.entries,
            failedMetrics = mapOf(
                HealthMetricType.HEART_RATE to "Hartslag kan nu niet worden gelezen.",
            ),
            lastSyncedAt = 456L,
        )

        assertEquals(HealthMetricSyncState.SYNCED, statuses.single { it.metric == HealthMetricType.STEPS }.state)
        assertEquals(HealthMetricSyncState.FAILED, statuses.single { it.metric == HealthMetricType.HEART_RATE }.state)
        assertEquals(HealthMetricSyncState.SYNCED, statuses.single { it.metric == HealthMetricType.SLEEP }.state)
        assertEquals("Hartslag kan nu niet worden gelezen.", statuses.single { it.metric == HealthMetricType.HEART_RATE }.message)
    }

    @Test
    fun failedMetricReadsDoNotReceiveLastSyncedTimestamp() {
        val statuses = buildHealthMetricSyncStatuses(
            metrics = listOf(HealthMetricType.STEPS, HealthMetricType.WORKOUTS),
            failedMetrics = mapOf(HealthMetricType.WORKOUTS to "Workouts lezen is mislukt."),
            lastSyncedAt = 789L,
        )

        assertEquals(789L, statuses.single { it.metric == HealthMetricType.STEPS }.lastSyncedAt)
        assertEquals(null, statuses.single { it.metric == HealthMetricType.WORKOUTS }.lastSyncedAt)
    }
}
