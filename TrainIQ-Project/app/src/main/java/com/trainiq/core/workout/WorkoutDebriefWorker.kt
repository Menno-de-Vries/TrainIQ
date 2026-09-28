package com.trainiq.core.workout

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.await
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
    override fun enqueue(sessionId: Long, generationId: String) {
        if (sessionId <= 0L || generationId.isBlank()) return
        val request = OneTimeWorkRequestBuilder<WorkoutDebriefWorker>()
            .setInputData(
                Data.Builder()
                    .putLong(WorkoutDebriefSessionIdKey, sessionId)
                    .putString(WorkoutDebriefGenerationIdKey, generationId)
                    .build(),
            )
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
            workoutDebriefWorkName(sessionId, generationId),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override suspend fun cancel(sessionId: Long, generationId: String) {
        if (sessionId <= 0L || generationId.isBlank()) return
        workManager.cancelUniqueWork(workoutDebriefWorkName(sessionId, generationId)).await()
    }
}

class WorkoutDebriefWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val sessionId = inputData.getLong(WorkoutDebriefSessionIdKey, 0L)
        val generationId = workoutDebriefGenerationIdForWorker(inputData.getString(WorkoutDebriefGenerationIdKey))
        if (sessionId <= 0L) return Result.failure()
        // Legacy work has only a recyclable session ID, so it cannot safely identify
        // the original session after an upgrade. Its deterministic local debrief remains.
        if (shouldSkipUnversionedWorkoutDebriefWork(generationId)) return Result.success()
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkoutDebriefWorkerEntryPoint::class.java,
        )
        return try {
            when (entryPoint.refreshWorkoutDebriefUseCase()(sessionId, generationId)) {
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

internal fun workoutDebriefGenerationIdForWorker(generationId: String?): String? =
    generationId?.takeIf(String::isNotBlank)

internal fun shouldSkipUnversionedWorkoutDebriefWork(generationId: String?): Boolean =
    generationId.isNullOrBlank()

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkoutDebriefWorkerEntryPoint {
    fun refreshWorkoutDebriefUseCase(): RefreshWorkoutDebriefUseCase
}

internal const val WorkoutDebriefMaxAttempts = 3
internal const val WorkoutDebriefSessionIdKey = "session_id"
internal const val WorkoutDebriefGenerationIdKey = "generation_id"
internal val WorkoutDebriefBackoff: Duration = Duration.ofMinutes(15)
internal fun workoutDebriefWorkName(sessionId: Long, generationId: String) =
    "workout_debrief_${sessionId}_$generationId"

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
