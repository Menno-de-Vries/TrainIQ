package com.trainiq.data.datasource

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.Change
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.gson.Gson
import com.trainiq.core.datastore.HealthConnectSyncPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.data.mapper.toCachedHeartRateRecord
import com.trainiq.data.mapper.toCachedCaloriesBurnedRecord
import com.trainiq.data.mapper.toCachedSleepSessionRecord
import com.trainiq.data.mapper.toCachedStepRecord
import com.trainiq.data.mapper.toCachedWeightRecord
import com.trainiq.data.mapper.toDomainMetrics
import com.trainiq.domain.model.HealthConnectMetrics
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class HealthConnectDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
) {
    private val gson = Gson()

    private val readPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )

    private val trackedRecordTypes: Set<KClass<out androidx.health.connect.client.records.Record>> = setOf(
        StepsRecord::class,
        HeartRateRecord::class,
        SleepSessionRecord::class,
        TotalCaloriesBurnedRecord::class,
        WeightRecord::class,
    )

    fun permissions(): Set<String> = readPermissions

    fun providerInstallIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun settingsIntent(): Intent =
        Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    suspend fun getStatus(): HealthConnectStatus {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectStatus(
                state = HealthConnectState.UNSUPPORTED,
                message = "Health Connect is not supported on this device.",
            )

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus(
                state = HealthConnectState.PROVIDER_MISSING,
                message = "Install or update Health Connect before TrainIQ can read steps, heart rate, and sleep.",
            )

            HealthConnectClient.SDK_AVAILABLE -> fetchConnectedStatus()
            else -> HealthConnectStatus(
                state = HealthConnectState.ERROR,
                message = "Unable to determine Health Connect availability.",
            )
        }
    }

    private suspend fun fetchConnectedStatus(): HealthConnectStatus {
        return runCatching {
            val client = HealthConnectClient.getOrCreate(context)
            val grantedPermissions = client.permissionController.getGrantedPermissions()
            if (!grantedPermissions.containsAll(readPermissions)) {
                HealthConnectStatus(
                    state = HealthConnectState.PERMISSION_REQUIRED,
                    message = "Grant steps, heart rate, sleep, calories burned, and weight permissions to connect Health Connect.",
                )
            } else {
                val syncPayload = syncTrackedMetrics(client)
                preferencesRepository.saveHealthConnectSyncPreferences(
                    changesToken = syncPayload.nextChangesToken,
                    cacheStateJson = gson.toJson(syncPayload.cacheState),
                    lastSyncedAt = syncPayload.lastSyncedAt,
                )
                syncPayload.toStatus()
            }
        }.getOrElse { throwable ->
            HealthConnectStatus(
                state = HealthConnectState.ERROR,
                message = throwable.message ?: "Health Connect could not be read right now.",
            )
        }
    }

    private suspend fun syncTrackedMetrics(client: HealthConnectClient): SyncPayload {
        val storedState = preferencesRepository.getHealthConnectSyncPreferences()
        if (storedState.changesToken.isBlank() || storedState.cacheStateJson.isBlank()) {
            return performFullSync(client)
        }

        val cachedState = runCatching {
            gson.fromJson(storedState.cacheStateJson, HealthConnectCacheState::class.java) ?: HealthConnectCacheState()
        }.getOrElse { HealthConnectCacheState() }

        if (cachedState.isEmpty()) {
            return performFullSync(client)
        }

        return performIncrementalSync(client, storedState, cachedState)
    }

    private suspend fun aggregateStepsToday(client: HealthConnectClient): Long = runCatching {
        client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfToday(), Instant.now()),
            )
        )[StepsRecord.COUNT_TOTAL] ?: 0L
    }.getOrElse { 0L }

    private suspend fun performFullSync(client: HealthConnectClient): SyncPayload {
        val now = Instant.now()
        val cacheState = HealthConnectCacheState(
            aggregatedStepsToday = aggregateStepsToday(client),
            heartRateRecords = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfToday(), now),
                ),
            ).records.map(HeartRateRecord::toCachedHeartRateRecord),
            sleepSessionRecords = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfSleepWindow(), now),
                ),
            ).records.map(SleepSessionRecord::toCachedSleepSessionRecord),
            caloriesBurnedRecords = client.readRecords(
                ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfToday(), now),
                ),
            ).records.map(TotalCaloriesBurnedRecord::toCachedCaloriesBurnedRecord),
            weightRecords = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfSleepWindow(), now),
                ),
            ).records.map(WeightRecord::toCachedWeightRecord),
        ).prune(now)

        return SyncPayload(
            cacheState = cacheState,
            nextChangesToken = client.getChangesToken(ChangesTokenRequest(recordTypes = trackedRecordTypes)),
            lastSyncedAt = System.currentTimeMillis(),
        )
    }

    private suspend fun performIncrementalSync(
        client: HealthConnectClient,
        storedState: HealthConnectSyncPreferences,
        initialCacheState: HealthConnectCacheState,
    ): SyncPayload {
        var currentToken = storedState.changesToken
        var cacheState = initialCacheState
        var hasMore = true
        while (hasMore) {
            val changesResponse = client.getChanges(currentToken)
            if (changesResponse.changesTokenExpired) {
                preferencesRepository.clearHealthConnectSyncPreferences()
                return performFullSync(client)
            }
            currentToken = changesResponse.nextChangesToken
            cacheState = applyChanges(cacheState, changesResponse.changes)
            hasMore = changesResponse.hasMore
        }
        val normalizedCacheState = cacheState.prune(Instant.now())
        val hasNewUiData = normalizedCacheState != initialCacheState

        // Always re-aggregate: the call is a single cheap round-trip and guarantees
        // deduplication-correct results. We cannot safely reuse the cached value because
        // older DataStore entries have aggregatedStepsToday == 0 (pre-migration).
        val freshSteps = aggregateStepsToday(client)

        return SyncPayload(
            cacheState = normalizedCacheState.copy(aggregatedStepsToday = freshSteps),
            nextChangesToken = currentToken,
            lastSyncedAt = if (hasNewUiData) System.currentTimeMillis() else storedState.lastSyncedAt,
        )
    }

    private fun applyChanges(
        initialState: HealthConnectCacheState,
        changes: List<Change>,
    ): HealthConnectCacheState {
        var cacheState = initialState
        changes.forEach { change ->
            cacheState = when (change) {
                is UpsertionChange -> cacheState.upsert(change.record)
                is DeletionChange -> cacheState.removeRecord(change.recordId)
                else -> cacheState
            }
        }
        return cacheState
    }

    private fun HealthConnectCacheState.upsert(record: androidx.health.connect.client.records.Record): HealthConnectCacheState {
        val now = Instant.now()
        return when (record) {
            is StepsRecord -> {
                val mapped = record.toCachedStepRecord()
                copy(
                    stepRecords = stepRecords.filterNot { it.recordId == mapped.recordId } +
                        listOfNotNull(mapped.takeIf { it.endTimeMillis >= startOfToday().toEpochMilli() }),
                ).prune(now)
            }

            is HeartRateRecord -> {
                val mapped = record.toCachedHeartRateRecord()
                copy(
                    heartRateRecords = heartRateRecords.filterNot { it.recordId == mapped.recordId } +
                        listOfNotNull(mapped.takeIf { it.endTimeMillis >= startOfToday().toEpochMilli() }),
                ).prune(now)
            }

            is SleepSessionRecord -> {
                val mapped = record.toCachedSleepSessionRecord()
                copy(
                    sleepSessionRecords = sleepSessionRecords.filterNot { it.recordId == mapped.recordId } +
                        listOfNotNull(mapped.takeIf { it.endTimeMillis >= startOfSleepWindow().toEpochMilli() }),
                ).prune(now)
            }

            is TotalCaloriesBurnedRecord -> {
                val mapped = record.toCachedCaloriesBurnedRecord()
                copy(
                    caloriesBurnedRecords = caloriesBurnedRecords.filterNot { it.recordId == mapped.recordId } +
                        listOfNotNull(mapped.takeIf { it.endTimeMillis >= startOfToday().toEpochMilli() }),
                ).prune(now)
            }

            is WeightRecord -> {
                val mapped = record.toCachedWeightRecord()
                copy(
                    weightRecords = weightRecords.filterNot { it.recordId == mapped.recordId } +
                        listOfNotNull(mapped.takeIf { it.timeMillis >= startOfSleepWindow().toEpochMilli() }),
                ).prune(now)
            }

            else -> this
        }
    }

    private fun HealthConnectCacheState.removeRecord(recordId: String): HealthConnectCacheState = copy(
        stepRecords = stepRecords.filterNot { it.recordId == recordId },
        heartRateRecords = heartRateRecords.filterNot { it.recordId == recordId },
        sleepSessionRecords = sleepSessionRecords.filterNot { it.recordId == recordId },
        caloriesBurnedRecords = caloriesBurnedRecords.filterNot { it.recordId == recordId },
        weightRecords = weightRecords.filterNot { it.recordId == recordId },
    )

    private fun HealthConnectCacheState.prune(now: Instant): HealthConnectCacheState {
        val todayStartMillis = startOfToday().toEpochMilli()
        val sleepWindowStartMillis = startOfSleepWindow().toEpochMilli()
        val nowMillis = now.toEpochMilli()
        return copy(
            stepRecords = stepRecords.filter { it.endTimeMillis in todayStartMillis..nowMillis },
            heartRateRecords = heartRateRecords.filter { it.endTimeMillis in todayStartMillis..nowMillis },
            sleepSessionRecords = sleepSessionRecords.filter { it.endTimeMillis in sleepWindowStartMillis..nowMillis },
            caloriesBurnedRecords = caloriesBurnedRecords.filter { it.endTimeMillis in todayStartMillis..nowMillis },
            weightRecords = weightRecords.filter { it.timeMillis in sleepWindowStartMillis..nowMillis },
        )
    }

    private fun SyncPayload.toStatus(): HealthConnectStatus {
        val metrics = cacheState.toDomainMetrics()
        val state = if (metrics.hasAnyData()) HealthConnectState.CONNECTED else HealthConnectState.NO_DATA
        return HealthConnectStatus(
            state = state,
            metrics = metrics,
            message = buildMessage(metrics, state),
            lastSyncedAt = lastSyncedAt,
        )
    }

    private fun buildMessage(metrics: HealthConnectMetrics, state: HealthConnectState): String {
        if (state == HealthConnectState.NO_DATA) {
            return "Health Connect is connected, but no recent step, heart rate, sleep, calorie, or weight data is available yet."
        }
        val parts = buildList {
            add("${metrics.stepsToday} steps")
            metrics.averageHeartRateBpm?.let { add("avg HR $it bpm") }
            if (metrics.sleepSessionCount > 0) {
                add("${metrics.sleepMinutes} min sleep")
            }
            metrics.caloriesBurnedToday?.let { add("${it.toInt()} kcal burned") }
            metrics.latestWeightKg?.let { add("${"%.1f".format(it)} kg latest weight") }
        }
        return "Health Connect synced ${parts.joinToString(", ")}."
    }

    private fun HealthConnectMetrics.hasAnyData(): Boolean =
        stepsToday > 0 || averageHeartRateBpm != null || sleepSessionCount > 0 || caloriesBurnedToday != null || latestWeightKg != null

    /**
     * Fetches today's step count directly from the Health Connect aggregate API.
     * Lightweight — only checks SDK status, permissions, and runs one aggregate query.
     * Returns 0 when HC is unavailable or permissions are not granted.
     */
    suspend fun getTodayStepsLive(): Int {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return 0
        return runCatching {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            if (!granted.contains(HealthPermission.getReadPermission(StepsRecord::class))) return@runCatching 0
            aggregateStepsToday(client).toInt()
        }.getOrElse { 0 }
    }

    /**
     * Reads today's step count from the DataStore cache written by the last full/incremental sync.
     * Does not touch the HealthConnectClient — safe to call at repository init time.
     */
    suspend fun getStepsFromPersistedCache(): Int {
        val storedState = preferencesRepository.getHealthConnectSyncPreferences()
        if (storedState.cacheStateJson.isBlank()) return 0
        return runCatching {
            val cacheState = gson.fromJson(storedState.cacheStateJson, HealthConnectCacheState::class.java)
                ?: return@runCatching 0
            cacheState.prune(Instant.now()).toDomainMetrics().stepsToday
        }.getOrElse { 0 }
    }

    private fun startOfToday(): Instant =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()

    private fun startOfSleepWindow(): Instant =
        LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
}

internal data class CachedStepRecord(
    val recordId: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val count: Int,
)

internal data class CachedHeartRateRecord(
    val recordId: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val averageBeatsPerMinute: Int?,
    val latestBeatsPerMinute: Int?,
    val latestSampleTimeMillis: Long?,
    val sampleCount: Int,
)

internal data class CachedSleepSessionRecord(
    val recordId: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMinutes: Long,
)

internal data class CachedCaloriesBurnedRecord(
    val recordId: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val kcal: Double,
)

internal data class CachedWeightRecord(
    val recordId: String,
    val timeMillis: Long,
    val weightKg: Double,
)

internal data class HealthConnectCacheState(
    /** Authoritative step count from the Health Connect aggregate API (deduplication-aware). */
    val aggregatedStepsToday: Long = 0L,
    /** Kept for backward-compatible JSON deserialization of older caches. Not used for step counting. */
    val stepRecords: List<CachedStepRecord> = emptyList(),
    val heartRateRecords: List<CachedHeartRateRecord> = emptyList(),
    val sleepSessionRecords: List<CachedSleepSessionRecord> = emptyList(),
    val caloriesBurnedRecords: List<CachedCaloriesBurnedRecord> = emptyList(),
    val weightRecords: List<CachedWeightRecord> = emptyList(),
) {
    fun isEmpty(): Boolean =
        aggregatedStepsToday == 0L &&
            heartRateRecords.isEmpty() &&
            sleepSessionRecords.isEmpty() &&
            caloriesBurnedRecords.isEmpty() &&
            weightRecords.isEmpty()
}

private data class SyncPayload(
    val cacheState: HealthConnectCacheState,
    val nextChangesToken: String,
    val lastSyncedAt: Long,
)
