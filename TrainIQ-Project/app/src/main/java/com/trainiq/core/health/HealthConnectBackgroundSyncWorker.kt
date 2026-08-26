package com.trainiq.core.health

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.trainiq.data.datasource.HealthConnectDataSource
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HealthMetricSyncState
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class HealthConnectBackgroundSyncScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val healthConnectDataSource: HealthConnectDataSource,
) {
    suspend fun scheduleIfBackgroundReadAvailable(): Boolean {
        if (!healthConnectDataSource.canReadInBackground()) {
            workManager.cancelUniqueWork(HealthConnectBackgroundSyncWorkName)
            return false
        }
        val request = PeriodicWorkRequestBuilder<HealthConnectBackgroundSyncWorker>(
            HealthConnectBackgroundSyncInterval,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                HealthConnectBackgroundBackoff.toMinutes(),
                TimeUnit.MINUTES,
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            HealthConnectBackgroundSyncWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        return true
    }
}

class HealthConnectBackgroundSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            HealthConnectWorkerEntryPoint::class.java,
        )
        return try {
            val status = entryPoint.healthConnectDataSource().getStatus()
            if (shouldRetryHealthConnectBackgroundSync(status)) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            if (shouldRetryHealthConnectBackgroundSyncFailure(throwable)) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HealthConnectWorkerEntryPoint {
    fun healthConnectDataSource(): HealthConnectDataSource
}

internal const val HealthConnectBackgroundSyncWorkName = "health_connect_background_sync"
internal val HealthConnectBackgroundSyncInterval: Duration = Duration.ofHours(6)
internal val HealthConnectBackgroundBackoff: Duration = Duration.ofMinutes(30)

internal fun shouldRetryHealthConnectBackgroundSync(status: HealthConnectStatus): Boolean =
    status.state == HealthConnectState.ERROR ||
        status.metricStatuses.any { it.state == HealthMetricSyncState.FAILED }

internal fun shouldRetryHealthConnectBackgroundSyncFailure(throwable: Throwable): Boolean =
    when (throwable) {
        is SecurityException,
        is IllegalArgumentException,
        is UnsupportedOperationException,
        -> false
        else -> true
    }
