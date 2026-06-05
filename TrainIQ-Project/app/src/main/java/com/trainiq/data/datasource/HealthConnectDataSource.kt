package com.trainiq.data.datasource

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.changes.Change
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trainiq.core.datastore.HealthConnectSyncPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.data.mapper.toCachedCaloriesBurnedRecord
import com.trainiq.data.mapper.toCachedExerciseSessionRecord
import com.trainiq.data.mapper.toCachedHeartRateRecord
import com.trainiq.data.mapper.toCachedSleepSessionRecord
import com.trainiq.data.mapper.toCachedStepRecord
import com.trainiq.data.mapper.toDomainMetrics
import com.trainiq.domain.model.HealthConnectMetrics
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStepDiagnostic
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HealthConnectStepDataFreshness
import com.trainiq.domain.model.HealthMetricStatus
import com.trainiq.domain.model.HealthMetricSyncState
import com.trainiq.domain.model.HealthMetricType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

internal val HealthConnectSyncMetricTypes = listOf(
    HealthMetricType.STEPS,
    HealthMetricType.HEART_RATE,
    HealthMetricType.SLEEP,
    HealthMetricType.ACTIVE_CALORIES,
    HealthMetricType.WORKOUTS,
)

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
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    private val trackedRecordTypesByMetric: Map<HealthMetricType, KClass<out androidx.health.connect.client.records.Record>> = mapOf(
        HealthMetricType.STEPS to StepsRecord::class,
        HealthMetricType.HEART_RATE to HeartRateRecord::class,
        HealthMetricType.SLEEP to SleepSessionRecord::class,
        HealthMetricType.ACTIVE_CALORIES to ActiveCaloriesBurnedRecord::class,
        HealthMetricType.WORKOUTS to ExerciseSessionRecord::class,
    )

    private val requiredPermissionsByMetric = mapOf(
        HealthMetricType.STEPS to HealthPermission.getReadPermission(StepsRecord::class),
        HealthMetricType.HEART_RATE to HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthMetricType.SLEEP to HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthMetricType.ACTIVE_CALORIES to HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthMetricType.WORKOUTS to HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    fun permissions(): Set<String> = readPermissions

    fun providerInstallIntent(): Intent =
        healthConnectProviderInstallIntent(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun settingsIntent(): Intent =
        Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    suspend fun canReadInBackground(): Boolean = withContext(Dispatchers.IO) {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return@withContext false
        runCatching {
            val client = HealthConnectClient.getOrCreate(context)
            val featureAvailable = client.features.getFeatureStatus(
                HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND,
            ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
            if (!featureAvailable) return@runCatching false
            HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND in client.permissionController.getGrantedPermissions()
        }.getOrDefault(false)
    }

    suspend fun getStatus(): HealthConnectStatus = withContext(Dispatchers.IO) {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_UNAVAILABLE -> {
                preferencesRepository.clearHealthConnectSyncPreferences()
                HealthConnectStatus(
                    state = HealthConnectState.UNSUPPORTED,
                    message = "Health Connect wordt niet ondersteund op dit apparaat.",
                    metricStatuses = unavailableMetricStatuses("Health Connect wordt niet ondersteund op dit apparaat."),
                    stepDataFreshness = HealthConnectStepDataFreshness.UNAVAILABLE,
                )
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                preferencesRepository.clearHealthConnectSyncPreferences()
                HealthConnectStatus(
                    state = HealthConnectState.PROVIDER_MISSING,
                    message = "Installeer of update Health Connect voordat TrainIQ stappen, hartslag, slaap, calorieën en workouts kan lezen.",
                    metricStatuses = unavailableMetricStatuses("Health Connect-provider ontbreekt of moet worden bijgewerkt."),
                    stepDataFreshness = HealthConnectStepDataFreshness.UNAVAILABLE,
                )
            }

            HealthConnectClient.SDK_AVAILABLE -> fetchConnectedStatus()
            else -> HealthConnectStatus(
                state = HealthConnectState.ERROR,
                message = "Health Connect-status kan nu niet worden bepaald.",
                metricStatuses = failedMetricStatuses("Health Connect-status kan nu niet worden bepaald."),
                stepDataFreshness = HealthConnectStepDataFreshness.ERROR,
            )
        }
    }

    private suspend fun fetchConnectedStatus(): HealthConnectStatus {
        return runCatching {
            val client = HealthConnectClient.getOrCreate(context)
            val grantedPermissions = client.permissionController.getGrantedPermissions()
            val grantedMetrics = grantedMetrics(grantedPermissions)
            if (grantedMetrics.isEmpty()) {
                preferencesRepository.clearHealthConnectSyncPreferences()
                HealthConnectStatus(
                    state = HealthConnectState.PERMISSION_REQUIRED,
                    message = if (grantedPermissions.isEmpty()) {
                        "Geen toegang tot Health Connect. Verbind opnieuw om stappen, hartslag, slaap, calorieën en workouts te lezen."
                    } else {
                        "Health Connect-toegang is gedeeltelijk. Sta ontbrekende metrics toe om alle inzichten te synchroniseren."
                    },
                    metricStatuses = buildHealthMetricPermissionStatuses(
                        grantedPermissions = grantedPermissions,
                        requiredPermissionsByMetric = requiredPermissionsByMetric,
                        lastSyncedAt = null,
                    ),
                    stepDataFreshness = HealthConnectStepDataFreshness.PERMISSION_MISSING,
                )
            } else {
                val syncPayload = syncTrackedMetrics(client, grantedMetrics)
                val storedState = preferencesRepository.getHealthConnectSyncPreferences()
                val mergedMetricTokens = storedState
                    .resolvedMetricChangesTokens(requiredPermissionsByMetric.keys)
                    .filterKeys { it in grantedMetrics }
                    .toMutableMap()
                    .apply {
                        syncPayload.nextChangesTokens.forEach { (metric, token) ->
                            if (token.isBlank()) remove(metric) else put(metric, token)
                        }
                    }
                preferencesRepository.saveHealthConnectSyncPreferences(
                    changesToken = mergedMetricTokens[HealthMetricType.STEPS].orEmpty(),
                    cacheStateJson = gson.toJson(syncPayload.cacheState),
                    lastSyncedAt = syncPayload.lastSyncedAt,
                    changesTokensJson = encodeMetricChangesTokens(mergedMetricTokens),
                )
                syncPayload.toStatus(
                    metricStatuses = syncPayload.metricStatuses.withDeniedMetrics(
                        grantedMetrics = grantedMetrics,
                        grantedPermissions = grantedPermissions,
                    ),
                    isPartialPermission = grantedMetrics.size < requiredPermissionsByMetric.size,
                )
            }
        }.getOrElse { throwable ->
            HealthConnectStatus(
                state = HealthConnectState.ERROR,
                message = throwable.message ?: "Health Connect kan nu niet worden gelezen.",
                metricStatuses = failedMetricStatuses("Health Connect kan nu niet worden gelezen."),
                stepDataFreshness = HealthConnectStepDataFreshness.ERROR,
            )
        }
    }

    private fun grantedMetrics(grantedPermissions: Set<String>): Set<HealthMetricType> =
        requiredPermissionsByMetric
            .filterValues { requiredPermission -> requiredPermission in grantedPermissions }
            .keys

    private suspend fun syncTrackedMetrics(
        client: HealthConnectClient,
        metricsToSync: Set<HealthMetricType>,
    ): SyncPayload {
        val storedState = preferencesRepository.getHealthConnectSyncPreferences()
        val metricTokens = storedState.resolvedMetricChangesTokens(metricsToSync)
        if (metricTokens.keys != metricsToSync || storedState.cacheStateJson.isBlank()) {
            return performFullSync(
                client = client,
                metricsToSync = metricsToSync,
                initialCacheState = readCacheStateFromStoredState(storedState).onlyMetrics(metricsToSync),
            )
        }

        val cachedState = readCacheStateFromStoredState(storedState).onlyMetrics(metricsToSync)

        if (cachedState.isEmpty()) {
            return performFullSync(client = client, metricsToSync = metricsToSync, initialCacheState = cachedState)
        }

        return runCatching {
            performIncrementalSync(client, storedState, metricTokens, cachedState)
        }.getOrElse { throwable ->
            cachedIncrementalFailurePayload(
                cachedState = cachedState,
                storedState = storedState,
                failureMessage = throwable.message ?: "Health Connect-incrementele sync is mislukt; vorige cache behouden.",
            )
        }
    }

    private suspend fun aggregateStepsToday(client: HealthConnectClient): Long = runCatching {
        val todayRange = healthConnectTodayLocalDateTimeRange(LocalDateTime.now())
        client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(todayRange.start, todayRange.end),
            )
        )[StepsRecord.COUNT_TOTAL] ?: 0L
    }.getOrElse { 0L }

    private suspend fun performFullSync(
        client: HealthConnectClient,
        metricsToSync: Set<HealthMetricType> = requiredPermissionsByMetric.keys,
        initialCacheState: HealthConnectCacheState = HealthConnectCacheState(),
    ): SyncPayload {
        val now = Instant.now()
        val syncQueriedAt = System.currentTimeMillis()
        val metricFailures = mutableMapOf<HealthMetricType, String>()
        val stepsToday = if (HealthMetricType.STEPS in metricsToSync) readMetricOrDefault(
            HealthMetricType.STEPS,
            metricFailures,
            initialCacheState.aggregatedStepsToday,
        ) {
            aggregateStepsToday(client)
        } else initialCacheState.aggregatedStepsToday
        val stepDiagnostic = if (HealthMetricType.STEPS in metricsToSync) {
            buildStepDiagnostic(client, stepsToday.toInt(), syncQueriedAt)
        } else {
            null
        }
        val heartRateRecords = if (HealthMetricType.HEART_RATE in metricsToSync) readMetricOrDefault(
            HealthMetricType.HEART_RATE,
            metricFailures,
            initialCacheState.heartRateRecords,
        ) {
            client.readAllRecords(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfToday(), now),
                mapper = HeartRateRecord::toCachedHeartRateRecord,
            )
        } else initialCacheState.heartRateRecords
        val sleepSessionRecords = if (HealthMetricType.SLEEP in metricsToSync) readMetricOrDefault(
            HealthMetricType.SLEEP,
            metricFailures,
            initialCacheState.sleepSessionRecords,
        ) {
            client.readAllRecords(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfSleepWindow(), now),
                mapper = SleepSessionRecord::toCachedSleepSessionRecord,
            )
        } else initialCacheState.sleepSessionRecords
        val caloriesBurnedRecords = if (HealthMetricType.ACTIVE_CALORIES in metricsToSync) readMetricOrDefault(
            HealthMetricType.ACTIVE_CALORIES,
            metricFailures,
            initialCacheState.caloriesBurnedRecords,
        ) {
            client.readAllRecords(
                recordType = ActiveCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfToday(), now),
                mapper = ActiveCaloriesBurnedRecord::toCachedCaloriesBurnedRecord,
            )
        } else initialCacheState.caloriesBurnedRecords
        val exerciseSessionRecords = if (HealthMetricType.WORKOUTS in metricsToSync) readMetricOrDefault(
            HealthMetricType.WORKOUTS,
            metricFailures,
            initialCacheState.exerciseSessionRecords,
        ) {
            client.readAllRecords(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfToday(), now),
                mapper = ExerciseSessionRecord::toCachedExerciseSessionRecord,
            )
        } else initialCacheState.exerciseSessionRecords
        val cacheState = HealthConnectCacheState(
            aggregatedStepsToday = stepsToday,
            heartRateRecords = heartRateRecords,
            sleepSessionRecords = sleepSessionRecords,
            caloriesBurnedRecords = caloriesBurnedRecords,
            weightRecords = emptyList(),
            exerciseSessionRecords = exerciseSessionRecords,
        ).prune(now)
        val nextChangesTokens = readChangesTokensByMetric(client, metricsToSync, metricFailures)
        val lastSyncedAt = System.currentTimeMillis()

        return SyncPayload(
            cacheState = cacheState,
            stepDiagnostic = stepDiagnostic,
            nextChangesTokens = nextChangesTokens,
            lastSyncedAt = lastSyncedAt,
            metricStatuses = buildHealthMetricSyncStatuses(
                metrics = metricsToSync,
                failedMetrics = metricFailures,
                lastSyncedAt = lastSyncedAt,
            ),
        )
    }

    private suspend fun readChangesTokensByMetric(
        client: HealthConnectClient,
        metricsToSync: Set<HealthMetricType>,
        failures: MutableMap<HealthMetricType, String>,
    ): Map<HealthMetricType, String> = trackedRecordTypesByMetric.filterKeys { it in metricsToSync }.mapNotNull { (metric, recordType) ->
        runCatching {
            metric to client.getChangesToken(ChangesTokenRequest(recordTypes = setOf(recordType)))
        }.getOrElse { throwable ->
            failures[metric] = throwable.message ?: "Health Connect ChangesToken kon niet worden opgehaald."
            null
        }
    }.toMap()

    private suspend fun <T> readMetricOrDefault(
        metric: HealthMetricType,
        failures: MutableMap<HealthMetricType, String>,
        default: T,
        block: suspend () -> T,
    ): T = runCatching { block() }.getOrElse { throwable ->
        failures[metric] = throwable.message ?: "Deze Health Connect-metric kan nu niet worden gelezen."
        default
    }

    private suspend fun <T : Record, R> HealthConnectClient.readAllRecords(
        recordType: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
        mapper: (T) -> R,
    ): List<R> {
        val records = mutableListOf<R>()
        var pageToken: String? = null
        do {
            val response = readRecords(
                ReadRecordsRequest(
                    recordType = recordType,
                    timeRangeFilter = timeRangeFilter,
                    pageToken = pageToken,
                    pageSize = HealthConnectReadPageSize,
                ),
            )
            records += response.records.map(mapper)
            pageToken = response.pageToken
        } while (!pageToken.isNullOrBlank())
        return records
    }

    private suspend fun performIncrementalSync(
        client: HealthConnectClient,
        storedState: HealthConnectSyncPreferences,
        metricTokens: Map<HealthMetricType, String>,
        initialCacheState: HealthConnectCacheState,
    ): SyncPayload {
        var cacheState = initialCacheState
        val nextMetricTokens = metricTokens.toMutableMap()
        val metricFailures = mutableMapOf<HealthMetricType, String>()
        metricTokens.forEach { (metric, token) ->
            var currentToken = token
            var hasMore = true
            val metricResult = runCatching {
                while (hasMore) {
                    val changesResponse = client.getChanges(currentToken)
                    if (changesResponse.changesTokenExpired) {
                        val refreshedMetric = performFullSync(
                            client = client,
                            metricsToSync = setOf(metric),
                            initialCacheState = cacheState,
                        )
                        cacheState = refreshedMetric.cacheState
                        refreshedMetric.nextChangesTokens[metric]?.let { nextMetricTokens[metric] = it }
                        refreshedMetric.metricStatuses
                            .firstOrNull { it.metric == metric && it.state == HealthMetricSyncState.FAILED }
                            ?.message
                            ?.let { metricFailures[metric] = it }
                        return@runCatching
                    }
                    currentToken = changesResponse.nextChangesToken
                    cacheState = applyChanges(cacheState, changesResponse.changes)
                    hasMore = changesResponse.hasMore
                }
            }
            metricResult.onFailure { throwable ->
                metricFailures[metric] = throwable.message ?: "Health Connect-incrementele sync is mislukt voor deze metric."
            }
            nextMetricTokens[metric] = currentToken
        }
        val normalizedCacheState = cacheState.prune(Instant.now())
        val hasNewUiData = normalizedCacheState != initialCacheState

        // Always re-aggregate: the call is a single cheap round-trip and guarantees
        // deduplication-correct results. We cannot safely reuse the cached value because
        // older DataStore entries have aggregatedStepsToday == 0 (pre-migration).
        val shouldRefreshSteps = HealthMetricType.STEPS in metricTokens.keys
        val freshSteps = if (shouldRefreshSteps) aggregateStepsToday(client) else initialCacheState.aggregatedStepsToday
        val stepDiagnostic = if (shouldRefreshSteps) {
            buildStepDiagnostic(client, freshSteps.toInt(), System.currentTimeMillis())
        } else {
            null
        }
        val hasFreshAggregateData = shouldRefreshSteps && freshSteps != initialCacheState.aggregatedStepsToday
        val lastSyncedAt = if (hasNewUiData || hasFreshAggregateData || metricFailures.isNotEmpty()) {
            System.currentTimeMillis()
        } else {
            storedState.lastSyncedAt
        }

        return SyncPayload(
            cacheState = normalizedCacheState.copy(aggregatedStepsToday = freshSteps),
            stepDiagnostic = stepDiagnostic,
            nextChangesTokens = nextMetricTokens,
            lastSyncedAt = lastSyncedAt,
            metricStatuses = buildHealthMetricSyncStatuses(
                metrics = metricTokens.keys,
                failedMetrics = metricFailures,
                lastSyncedAt = lastSyncedAt,
            ),
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

            is ActiveCaloriesBurnedRecord -> {
                val mapped = record.toCachedCaloriesBurnedRecord()
                copy(
                    caloriesBurnedRecords = caloriesBurnedRecords.filterNot { it.recordId == mapped.recordId } +
                        listOfNotNull(mapped.takeIf { it.endTimeMillis >= startOfToday().toEpochMilli() }),
                ).prune(now)
            }

            is ExerciseSessionRecord -> {
                val mapped = record.toCachedExerciseSessionRecord()
                copy(
                    exerciseSessionRecords = exerciseSessionRecords.filterNot { it.recordId == mapped.recordId } +
                        listOfNotNull(mapped.takeIf { it.endTimeMillis >= startOfToday().toEpochMilli() }),
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
        weightRecords = emptyList(),
        exerciseSessionRecords = exerciseSessionRecords.filterNot { it.recordId == recordId },
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
            weightRecords = emptyList(),
            exerciseSessionRecords = exerciseSessionRecords.filter { it.endTimeMillis in todayStartMillis..nowMillis },
        )
    }

    private fun SyncPayload.toStatus(
        metricStatuses: List<HealthMetricStatus> = this.metricStatuses,
        isPartialPermission: Boolean = false,
    ): HealthConnectStatus {
        val metrics = cacheState.toDomainMetrics()
        val state = if (metrics.hasAnyData()) HealthConnectState.CONNECTED else HealthConnectState.NO_DATA
        val freshness = stepDataFreshness(metrics, metricStatuses)
        return HealthConnectStatus(
            state = state,
            metrics = metrics,
            message = buildMessage(metrics, state, isPartialPermission),
            lastSyncedAt = lastSyncedAt,
            metricStatuses = metricStatuses,
            stepDataFreshness = freshness,
            stepDataUpdatedAt = lastSyncedAt.takeIf {
                freshness == HealthConnectStepDataFreshness.FRESH ||
                    freshness == HealthConnectStepDataFreshness.STALE_CACHE
            },
            stepDiagnostic = stepDiagnostic,
        )
    }

    private suspend fun buildStepDiagnostic(
        client: HealthConnectClient,
        aggregateStepsToday: Int,
        queriedAt: Long,
    ): HealthConnectStepDiagnostic {
        val todayRange = healthConnectTodayLocalDateTimeRange(LocalDateTime.now())
        val sourceSnapshot = readStepSourceSnapshotToday(client, todayRange)
        return HealthConnectStepDiagnostic(
            aggregateStepsToday = aggregateStepsToday,
            queriedAt = queriedAt,
            sourceLabels = sourceSnapshot.labels,
            latestSamsungSourceSeenAt = sourceSnapshot.latestSamsungSeenAt,
            dayStartLabel = todayRange.start.format(StepDiagnosticTimeFormatter),
            dayEndLabel = todayRange.end.format(StepDiagnosticTimeFormatter),
        )
    }

    private suspend fun readStepSourceSnapshotToday(
        client: HealthConnectClient,
        todayRange: HealthConnectLocalDateTimeRange,
    ): StepSourceSnapshot = runCatching {
        var latestSamsungSeenAt: Long? = null
        val labels = client.readAllRecords(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(todayRange.start, todayRange.end),
        ) { record ->
            val label = record.metadata.dataOrigin.packageName.toStepSourceLabel()
            if (label == "Samsung Health") {
                val endMillis = record.endTime.toEpochMilli()
                latestSamsungSeenAt = maxOf(latestSamsungSeenAt ?: endMillis, endMillis)
            }
            label
        }.distinct().sorted()
        StepSourceSnapshot(labels = labels, latestSamsungSeenAt = latestSamsungSeenAt)
    }.getOrDefault(StepSourceSnapshot())

    private suspend fun readStepSourceLabelsToday(client: HealthConnectClient): List<String> = runCatching {
        val todayRange = healthConnectTodayLocalDateTimeRange(LocalDateTime.now())
        client.readAllRecords(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(todayRange.start, todayRange.end),
        ) { record ->
            record.metadata.dataOrigin.packageName.toStepSourceLabel()
        }.distinct().sorted()
    }.getOrDefault(emptyList())

    private fun stepDataFreshness(
        metrics: HealthConnectMetrics,
        metricStatuses: List<HealthMetricStatus>,
    ): HealthConnectStepDataFreshness {
        val stepStatus = metricStatuses.firstOrNull { it.metric == HealthMetricType.STEPS }
            ?: return HealthConnectStepDataFreshness.PERMISSION_MISSING
        return when (stepStatus.state) {
            HealthMetricSyncState.SYNCED,
            HealthMetricSyncState.SYNCING,
            HealthMetricSyncState.STALE -> HealthConnectStepDataFreshness.FRESH
            HealthMetricSyncState.DENIED,
            HealthMetricSyncState.PARTIALLY_GRANTED -> HealthConnectStepDataFreshness.PERMISSION_MISSING
            HealthMetricSyncState.UNAVAILABLE -> HealthConnectStepDataFreshness.UNAVAILABLE
            HealthMetricSyncState.FAILED -> if (metrics.stepsToday > 0) {
                HealthConnectStepDataFreshness.STALE_CACHE
            } else {
                HealthConnectStepDataFreshness.ERROR
            }
        }
    }

    private fun List<HealthMetricStatus>.withDeniedMetrics(
        grantedMetrics: Set<HealthMetricType>,
        grantedPermissions: Set<String>,
    ): List<HealthMetricStatus> {
        val byMetric = associateBy { it.metric }.toMutableMap()
        buildHealthMetricPermissionStatuses(
            grantedPermissions = grantedPermissions,
            requiredPermissionsByMetric = requiredPermissionsByMetric.filterKeys { it !in grantedMetrics },
            lastSyncedAt = null,
        ).forEach { status -> byMetric[status.metric] = status }
        return HealthMetricType.entries.mapNotNull { byMetric[it] }
    }

    private fun readMetricsFromStoredState(storedState: HealthConnectSyncPreferences): HealthConnectMetrics? {
        if (storedState.cacheStateJson.isBlank()) return null
        return runCatching {
            val cacheState = readCacheStateFromStoredState(storedState)
            cacheState.prune(Instant.now()).toDomainMetrics()
        }.getOrNull()
    }

    private fun readCacheStateFromStoredState(storedState: HealthConnectSyncPreferences): HealthConnectCacheState =
        if (storedState.cacheStateJson.isBlank()) {
            HealthConnectCacheState()
        } else {
            runCatching {
                gson.fromJson(storedState.cacheStateJson, HealthConnectCacheState::class.java) ?: HealthConnectCacheState()
            }.getOrElse { HealthConnectCacheState() }
        }

    private fun buildMessage(metrics: HealthConnectMetrics, state: HealthConnectState, isPartialPermission: Boolean): String {
        if (state == HealthConnectState.NO_DATA) {
            if (isPartialPermission) {
                return "Health Connect is gedeeltelijk verbonden. Toegestane metrics zijn gesynchroniseerd, maar er is nog geen recente data."
            }
            return "Health Connect is verbonden, maar er is nog geen recente data voor stappen, hartslag, slaap, calorieën of workouts."
        }
        val parts = buildList {
            add("${metrics.stepsToday} stappen")
            metrics.averageHeartRateBpm?.let { add("gem. hartslag $it bpm") }
            if (metrics.sleepSessionCount > 0) {
                add("${metrics.sleepMinutes} min slaap")
            }
            metrics.caloriesBurnedToday?.let { add("${it.toInt()} kcal verbrand") }
            if (metrics.workoutSessionCountToday > 0) {
                add("${metrics.workoutMinutesToday} min training")
            }
        }
        if (isPartialPermission) {
            return "Health Connect gedeeltelijk gesynchroniseerd: ${parts.joinToString(", ")}."
        }
        return "Health Connect gesynchroniseerd: ${parts.joinToString(", ")}."
    }

    private fun HealthConnectMetrics.hasAnyData(): Boolean =
        stepsToday > 0 ||
            averageHeartRateBpm != null ||
            sleepSessionCount > 0 ||
            caloriesBurnedToday != null ||
            workoutSessionCountToday > 0

    /**
     * Fetches today's step count directly from the Health Connect aggregate API.
     * Lightweight — only checks SDK status, permissions, and runs one aggregate query.
     * Returns 0 when HC is unavailable or permissions are not granted.
     */
    suspend fun getTodayStepsLive(): Int = withContext(Dispatchers.IO) {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return@withContext 0
        runCatching {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            if (!hasHealthConnectPermission(
                    grantedPermissions = granted,
                    requiredPermission = HealthPermission.getReadPermission(StepsRecord::class),
                )
            ) {
                return@runCatching 0
            }
            aggregateStepsToday(client).toInt()
        }.getOrElse { 0 }
    }

    /**
     * Reads today's step count from the DataStore cache written by the last full/incremental sync.
     * Does not touch the HealthConnectClient — safe to call at repository init time.
     */
    suspend fun getStepsFromPersistedCache(): Int = withContext(Dispatchers.IO) {
        val storedState = preferencesRepository.getHealthConnectSyncPreferences()
        if (storedState.cacheStateJson.isBlank()) return@withContext 0
        runCatching {
            val cacheState = gson.fromJson(storedState.cacheStateJson, HealthConnectCacheState::class.java)
                ?: return@runCatching 0
            cacheState.prune(Instant.now()).toDomainMetrics().stepsToday
        }.getOrElse { 0 }
    }

    private fun startOfToday(): Instant =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()

    private fun startOfSleepWindow(): Instant =
        LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

    private companion object {
        const val HealthConnectReadPageSize = 100
        val StepDiagnosticTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}

private data class StepSourceSnapshot(
    val labels: List<String> = emptyList(),
    val latestSamsungSeenAt: Long? = null,
)

internal data class HealthConnectLocalDateTimeRange(
    val start: LocalDateTime,
    val end: LocalDateTime,
)

internal fun healthConnectTodayLocalDateTimeRange(now: LocalDateTime): HealthConnectLocalDateTimeRange =
    HealthConnectLocalDateTimeRange(
        start = now.toLocalDate().atStartOfDay(),
        end = now,
    )

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

internal data class CachedExerciseSessionRecord(
    val recordId: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMinutes: Long,
    val title: String?,
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
    val exerciseSessionRecords: List<CachedExerciseSessionRecord> = emptyList(),
) {
    fun isEmpty(): Boolean =
        aggregatedStepsToday == 0L &&
            heartRateRecords.isEmpty() &&
            sleepSessionRecords.isEmpty() &&
            caloriesBurnedRecords.isEmpty() &&
            weightRecords.isEmpty() &&
            exerciseSessionRecords.isEmpty()
}

internal fun HealthConnectCacheState.onlyMetrics(metrics: Set<HealthMetricType>): HealthConnectCacheState = copy(
    aggregatedStepsToday = if (HealthMetricType.STEPS in metrics) aggregatedStepsToday else 0L,
    stepRecords = if (HealthMetricType.STEPS in metrics) stepRecords else emptyList(),
    heartRateRecords = if (HealthMetricType.HEART_RATE in metrics) heartRateRecords else emptyList(),
    sleepSessionRecords = if (HealthMetricType.SLEEP in metrics) sleepSessionRecords else emptyList(),
    caloriesBurnedRecords = if (HealthMetricType.ACTIVE_CALORIES in metrics) caloriesBurnedRecords else emptyList(),
    weightRecords = emptyList(),
    exerciseSessionRecords = if (HealthMetricType.WORKOUTS in metrics) exerciseSessionRecords else emptyList(),
)

internal data class SyncPayload(
    val cacheState: HealthConnectCacheState,
    val lastSyncedAt: Long,
    val stepDiagnostic: HealthConnectStepDiagnostic? = null,
    val nextChangesTokens: Map<HealthMetricType, String> = emptyMap(),
    val nextChangesToken: String = nextChangesTokens[HealthMetricType.STEPS].orEmpty(),
    val metricStatuses: List<HealthMetricStatus> = buildHealthMetricSyncStatuses(
        metrics = HealthConnectSyncMetricTypes,
        failedMetrics = emptyMap(),
        lastSyncedAt = lastSyncedAt,
    ),
)

internal fun cachedIncrementalFailurePayload(
    cachedState: HealthConnectCacheState,
    storedState: HealthConnectSyncPreferences,
    failureMessage: String,
): SyncPayload = SyncPayload(
    cacheState = cachedState,
    lastSyncedAt = storedState.lastSyncedAt,
    stepDiagnostic = HealthConnectStepDiagnostic(
        aggregateStepsToday = cachedState.toDomainMetrics().stepsToday,
        queriedAt = storedState.lastSyncedAt,
    ),
    nextChangesTokens = storedState.resolvedMetricChangesTokens(HealthConnectSyncMetricTypes),
    nextChangesToken = storedState.changesToken,
    metricStatuses = buildHealthMetricSyncStatuses(
        metrics = HealthConnectSyncMetricTypes,
        failedMetrics = HealthConnectSyncMetricTypes.associateWith { failureMessage },
        lastSyncedAt = storedState.lastSyncedAt,
    ),
)

internal fun fullSyncTokenFailurePayload(
    cacheState: HealthConnectCacheState,
    failureMessage: String,
    lastSyncedAt: Long,
): SyncPayload = SyncPayload(
    cacheState = cacheState,
    lastSyncedAt = lastSyncedAt,
    stepDiagnostic = HealthConnectStepDiagnostic(
        aggregateStepsToday = cacheState.toDomainMetrics().stepsToday,
        queriedAt = lastSyncedAt,
    ),
    nextChangesTokens = emptyMap(),
    metricStatuses = buildHealthMetricSyncStatuses(
        metrics = HealthConnectSyncMetricTypes,
        failedMetrics = HealthConnectSyncMetricTypes.associateWith { failureMessage },
        lastSyncedAt = lastSyncedAt,
    ),
)

internal fun encodeMetricChangesTokens(tokens: Map<HealthMetricType, String>): String {
    val storageMap = tokens
        .filterValues { it.isNotBlank() }
        .mapKeys { (metric, _) -> metric.name }
    return if (storageMap.isEmpty()) "" else Gson().toJson(storageMap)
}

internal fun decodeMetricChangesTokens(tokensJson: String): Map<HealthMetricType, String> {
    if (tokensJson.isBlank()) return emptyMap()
    return runCatching {
        val type = object : TypeToken<Map<String, String>>() {}.type
        val storageMap = Gson().fromJson<Map<String, String>>(tokensJson, type).orEmpty()
        storageMap.mapNotNull { (metricName, token) ->
            val metric = runCatching { HealthMetricType.valueOf(metricName) }.getOrNull()
            metric?.takeIf { token.isNotBlank() }?.let { it to token }
        }.toMap()
    }.getOrDefault(emptyMap())
}

internal fun HealthConnectSyncPreferences.resolvedMetricChangesTokens(
    metrics: Iterable<HealthMetricType>,
): Map<HealthMetricType, String> {
    val requestedMetrics = metrics.toSet()
    val decodedTokens = decodeMetricChangesTokens(changesTokensJson)
        .filterKeys { it in requestedMetrics }
    if (decodedTokens.isNotEmpty()) return decodedTokens
    if (changesToken.isBlank()) return emptyMap()
    return requestedMetrics.associateWith { changesToken }
}

internal fun hasHealthConnectPermission(
    grantedPermissions: Set<String>,
    requiredPermission: String,
): Boolean = requiredPermission in grantedPermissions

internal fun String.toStepSourceLabel(): String = when {
    contains("samsung", ignoreCase = true) -> "Samsung Health"
    this == "android" || contains("healthconnect.phone", ignoreCase = true) -> "Jouw telefoon"
    isBlank() -> "Onbekende bron"
    else -> this
}

internal fun buildHealthMetricPermissionStatuses(
    grantedPermissions: Set<String>,
    requiredPermissionsByMetric: Map<HealthMetricType, String>,
    lastSyncedAt: Long?,
): List<HealthMetricStatus> = requiredPermissionsByMetric.map { (metric, requiredPermission) ->
    val granted = requiredPermission in grantedPermissions
    HealthMetricStatus(
        metric = metric,
        state = when {
            !granted -> HealthMetricSyncState.DENIED
            lastSyncedAt != null -> HealthMetricSyncState.STALE
            else -> HealthMetricSyncState.SYNCING
        },
        message = if (granted) null else "Toestemming ontbreekt voor deze Health Connect-metric.",
        lastSyncedAt = lastSyncedAt.takeIf { granted },
    )
}

internal fun buildHealthMetricSyncStatuses(
    metrics: Iterable<HealthMetricType>,
    failedMetrics: Map<HealthMetricType, String>,
    lastSyncedAt: Long,
): List<HealthMetricStatus> = metrics.map { metric ->
    val failureMessage = failedMetrics[metric]
    HealthMetricStatus(
        metric = metric,
        state = if (failureMessage == null) HealthMetricSyncState.SYNCED else HealthMetricSyncState.FAILED,
        message = failureMessage,
        lastSyncedAt = lastSyncedAt.takeIf { failureMessage == null },
    )
}

private fun unavailableMetricStatuses(message: String): List<HealthMetricStatus> =
    HealthConnectSyncMetricTypes.map { metric ->
        HealthMetricStatus(
            metric = metric,
            state = HealthMetricSyncState.UNAVAILABLE,
            message = message,
        )
    }

private fun failedMetricStatuses(message: String): List<HealthMetricStatus> =
    HealthConnectSyncMetricTypes.map { metric ->
        HealthMetricStatus(
            metric = metric,
            state = HealthMetricSyncState.FAILED,
            message = message,
        )
    }

internal fun healthConnectProviderInstallIntent(callerPackageName: String): Intent {
    val providerPackageName = "com.google.android.apps.healthdata"
    return Intent(Intent.ACTION_VIEW).apply {
        setPackage("com.android.vending")
        data = Uri.parse("market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding")
        putExtra("overlay", true)
        putExtra("callerId", callerPackageName)
    }
}
