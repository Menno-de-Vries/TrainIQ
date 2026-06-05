package com.trainiq.data.datasource

import com.trainiq.core.datastore.HealthConnectSyncPreferences
import com.trainiq.data.mapper.toDomainMetrics
import com.trainiq.domain.model.HealthMetricSyncState
import com.trainiq.domain.model.HealthMetricType
import java.time.LocalDateTime
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
            metrics = HealthConnectSyncMetricTypes,
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

    @Test
    fun metricChangesTokensRoundTripByMetricName() {
        val encoded = encodeMetricChangesTokens(
            mapOf(
                HealthMetricType.STEPS to "steps-token",
                HealthMetricType.HEART_RATE to "heart-rate-token",
                HealthMetricType.SLEEP to "sleep-token",
            ),
        )

        assertEquals(
            mapOf(
                HealthMetricType.STEPS to "steps-token",
                HealthMetricType.HEART_RATE to "heart-rate-token",
                HealthMetricType.SLEEP to "sleep-token",
            ),
            decodeMetricChangesTokens(encoded),
        )
    }

    @Test
    fun blankMetricChangesTokensFallBackToLegacyTokenForExistingInstalls() {
        val storedState = HealthConnectSyncPreferences(
            changesToken = "legacy-all-metrics-token",
            changesTokensJson = "",
            cacheStateJson = "{}",
            lastSyncedAt = 321L,
        )

        val tokens = storedState.resolvedMetricChangesTokens(HealthConnectSyncMetricTypes)

        assertTrue(HealthConnectSyncMetricTypes.all { tokens[it] == "legacy-all-metrics-token" })
        assertFalse(tokens.containsKey(HealthMetricType.WEIGHT))
    }

    @Test
    fun syncPayloadKeepsPerMetricTokensSeparateFromCompatibilityToken() {
        val payload = SyncPayload(
            cacheState = HealthConnectCacheState(aggregatedStepsToday = 777L),
            nextChangesTokens = mapOf(
                HealthMetricType.STEPS to "steps-token",
                HealthMetricType.WORKOUTS to "workouts-token",
            ),
            lastSyncedAt = 555L,
        )

        assertEquals("steps-token", payload.nextChangesTokens[HealthMetricType.STEPS])
        assertEquals("workouts-token", payload.nextChangesTokens[HealthMetricType.WORKOUTS])
        assertEquals("steps-token", payload.nextChangesToken)
    }

    @Test
    fun cacheStateDropsMetricsAfterPermissionRevocation() {
        val cacheState = HealthConnectCacheState(
            aggregatedStepsToday = 777L,
            stepRecords = listOf(CachedStepRecord("steps", 1L, 2L, 100)),
            heartRateRecords = listOf(CachedHeartRateRecord("hr", 1L, 2L, 60, 64, 2L, 3)),
            sleepSessionRecords = listOf(CachedSleepSessionRecord("sleep", 1L, 2L, 480)),
            caloriesBurnedRecords = listOf(CachedCaloriesBurnedRecord("cal", 1L, 2L, 300.0)),
            weightRecords = listOf(CachedWeightRecord("weight", 1L, 80.0)),
            exerciseSessionRecords = listOf(CachedExerciseSessionRecord("workout", 1L, 2L, 45, "Squat")),
        )

        val redacted = cacheState.onlyMetrics(setOf(HealthMetricType.STEPS, HealthMetricType.SLEEP))

        assertEquals(777L, redacted.aggregatedStepsToday)
        assertEquals(1, redacted.stepRecords.size)
        assertEquals(1, redacted.sleepSessionRecords.size)
        assertTrue(redacted.heartRateRecords.isEmpty())
        assertTrue(redacted.caloriesBurnedRecords.isEmpty())
        assertTrue(redacted.weightRecords.isEmpty())
        assertTrue(redacted.exerciseSessionRecords.isEmpty())
    }

    @Test
    fun revokedMetricTokensAreNotRetainedWhenReEncodingGrantedMetricTokens() {
        val retainedTokens = mapOf(
            HealthMetricType.STEPS to "steps-token",
            HealthMetricType.HEART_RATE to "revoked-heart-token",
            HealthMetricType.SLEEP to "sleep-token",
        ).filterKeys { it in setOf(HealthMetricType.STEPS, HealthMetricType.SLEEP) }

        val decoded = decodeMetricChangesTokens(encodeMetricChangesTokens(retainedTokens))

        assertEquals("steps-token", decoded[HealthMetricType.STEPS])
        assertEquals("sleep-token", decoded[HealthMetricType.SLEEP])
        assertFalse(decoded.containsKey(HealthMetricType.HEART_RATE))
    }

    @Test
    fun domainMetricsPreferAggregateStepsOverRawRecordSum() {
        val metrics = HealthConnectCacheState(
            aggregatedStepsToday = 12820L,
            stepRecords = listOf(
                CachedStepRecord("raw-1", 1L, 2L, 12384),
            ),
        ).toDomainMetrics()

        assertEquals(12820, metrics.stepsToday)
    }

    @Test
    fun todayStepAggregateRangeUsesLocalDateTimeDayBoundaries() {
        val now = LocalDateTime.of(2026, 6, 4, 18, 45, 30)
        val range = healthConnectTodayLocalDateTimeRange(now)

        assertEquals(LocalDateTime.of(2026, 6, 4, 0, 0), range.start)
        assertEquals(now, range.end)
    }

    @Test
    fun stepAggregateDoesNotFilterToSingleDataOrigin() {
        val source = java.io.File("src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt").readText()
        val aggregateBody = source.substringAfter("private suspend fun aggregateStepsToday")
            .substringBefore("private suspend fun performFullSync")

        assertTrue(aggregateBody.contains("StepsRecord.COUNT_TOTAL"))
        assertTrue(aggregateBody.contains("healthConnectTodayLocalDateTimeRange"))
        assertFalse(aggregateBody.contains("DataOrigin"))
        assertFalse(aggregateBody.contains("dataOriginFilter"))
    }
}
