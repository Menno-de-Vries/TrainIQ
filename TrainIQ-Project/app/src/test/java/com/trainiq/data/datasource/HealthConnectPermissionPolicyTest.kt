package com.trainiq.data.datasource

import com.trainiq.core.datastore.HealthConnectSyncPreferences
import com.trainiq.data.mapper.toDomainMetrics
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

    @Test
    fun incrementalFailurePayloadPreservesCachedMetricsAndToken() {
        val cachedState = HealthConnectCacheState(aggregatedStepsToday = 1234L)
        val storedState = HealthConnectSyncPreferences(
            changesToken = "existing-token",
            cacheStateJson = "{}",
            lastSyncedAt = 987L,
        )

        val payload = cachedIncrementalFailurePayload(
            cachedState = cachedState,
            storedState = storedState,
            failureMessage = "Changes API tijdelijk niet beschikbaar.",
        )

        assertEquals(cachedState, payload.cacheState)
        assertEquals("existing-token", payload.nextChangesToken)
        assertEquals(987L, payload.lastSyncedAt)
        assertEquals(
            HealthMetricSyncState.FAILED,
            payload.metricStatuses.single { it.metric == HealthMetricType.STEPS }.state,
        )
        assertEquals(1234, payload.cacheState.toDomainMetrics().stepsToday)
    }

    @Test
    fun fullSyncWithoutChangesTokenReportsFailedMetricsAndDoesNotPretendSynced() {
        val payload = fullSyncTokenFailurePayload(
            cacheState = HealthConnectCacheState(aggregatedStepsToday = 4321L),
            failureMessage = "ChangesToken kon niet worden opgehaald.",
            lastSyncedAt = 654L,
        )

        assertEquals("", payload.nextChangesToken)
        assertEquals(654L, payload.lastSyncedAt)
        assertEquals(4321, payload.cacheState.toDomainMetrics().stepsToday)
        assertTrue(payload.metricStatuses.all { it.state == HealthMetricSyncState.FAILED })
    }
}
