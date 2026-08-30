package com.trainiq.core.workout

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.trainiq.ai.services.AiFeatureThrottledException
import com.trainiq.ai.services.AiProviderUnavailableException
import com.trainiq.ai.services.AiFailureCategory
import com.trainiq.ai.services.AiProviderRequestException
import com.trainiq.ai.services.AiRateLimitException
import com.trainiq.ai.services.AiTimeoutException
import com.trainiq.domain.repository.WorkoutDebriefRefreshOutcome
import com.trainiq.domain.repository.WorkoutDebriefScheduler
import com.trainiq.domain.usecase.RefreshWorkoutDebriefUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

@Singleton
class WorkManagerWorkoutDebriefScheduler @Inject constructor(
    private val workManager: WorkManager,
) : WorkoutDebriefScheduler {
    override fun enqueue(sessionId: Long) {
        if (sessionId <= 0L) return
        val request = OneTimeWorkRequestBuilder<WorkoutDebriefWorker>()
            .setInputData(Data.Builder().putLong(WorkoutDebriefSessionIdKey, sessionId).build())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkoutDebriefBackoff.toMinutes(),
                TimeUnit.MINUTES,
            )
            .build()
        workManager.enqueueUniqueWork(
            workoutDebriefWorkName(sessionId),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}

class WorkoutDebriefWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val sessionId = inputData.getLong(WorkoutDebriefSessionIdKey, 0L)
        if (sessionId <= 0L) return Result.failure()
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkoutDebriefWorkerEntryPoint::class.java,
        )
        return try {
            when (entryPoint.refreshWorkoutDebriefUseCase()(sessionId)) {
                WorkoutDebriefRefreshOutcome.UPDATED,
                WorkoutDebriefRefreshOutcome.ALREADY_ENRICHED,
                WorkoutDebriefRefreshOutcome.SESSION_MISSING,
                -> Result.success()
                WorkoutDebriefRefreshOutcome.INVALID_RESULT -> Result.failure()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            if (!shouldRetryWorkoutDebriefFailure(throwable)) return Result.failure()
            if (runAttemptCount >= WorkoutDebriefMaxAttempts - 1) return Result.failure()
            Result.retry()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkoutDebriefWorkerEntryPoint {
    fun refreshWorkoutDebriefUseCase(): RefreshWorkoutDebriefUseCase
}

internal const val WorkoutDebriefMaxAttempts = 3
internal const val WorkoutDebriefSessionIdKey = "session_id"
internal val WorkoutDebriefBackoff: Duration = Duration.ofMinutes(15)
internal fun workoutDebriefWorkName(sessionId: Long) = "workout_debrief_$sessionId"

internal fun shouldRetryWorkoutDebriefFailure(throwable: Throwable): Boolean = when (throwable) {
    is AiProviderRequestException -> throwable.category in setOf(
        AiFailureCategory.TEMPORARY_RATE_LIMIT,
        AiFailureCategory.TIMEOUT,
        AiFailureCategory.NETWORK,
        AiFailureCategory.SERVICE_FAILURE,
    )
    is AiRateLimitException,
    is AiFeatureThrottledException,
    is AiTimeoutException,
    is IOException,
    -> true
    is AiProviderUnavailableException -> throwable.failures.isNotEmpty()
    is HttpException -> throwable.code() in listOf(408, 409, 425, 429, 500, 502, 503, 504)
    else -> false
}
