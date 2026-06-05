package com.trainiq.core.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.trainiq.core.database.TrainIqDao
import com.trainiq.core.datastore.UserPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class TrainIqReminderScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val preferencesRepository: UserPreferencesRepository,
) {
    suspend fun syncScheduleWithPreferences() {
        if (preferencesRepository.getReminderPreferences().enabled) {
            schedule()
        } else {
            cancel()
        }
    }

    fun schedule() {
        val request = PeriodicWorkRequestBuilder<TrainIqReminderWorker>(ReminderCheckInterval)
            .build()
        workManager.enqueueUniquePeriodicWork(
            TrainIqReminderWorkName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(TrainIqReminderWorkName)
    }
}

class TrainIqReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            TrainIqReminderWorkerEntryPoint::class.java,
        )
        return try {
            val preferencesRepository = entryPoint.userPreferencesRepository()
            val preferences = preferencesRepository.getReminderPreferences()
            if (!preferences.enabled || !entryPoint.reminderNotifications().canPostNotifications()) {
                return Result.success()
            }

            val now = System.currentTimeMillis()
            val dao = entryPoint.trainIqDao()
            if (
                shouldSendMealReminder(
                    remindersEnabled = true,
                    nowMillis = now,
                    latestMealAtMillis = dao.latestMealDate(),
                    lastReminderAtMillis = preferences.lastMealReminderAt,
                )
            ) {
                entryPoint.reminderNotifications().show(mealReminderContent(now))
                preferencesRepository.markMealReminderShown(now)
            }
            if (
                shouldSendWorkoutReminder(
                    remindersEnabled = true,
                    nowMillis = now,
                    latestWorkoutAtMillis = dao.latestCompletedWorkoutDate(),
                    lastReminderAtMillis = preferences.lastWorkoutReminderAt,
                )
            ) {
                entryPoint.reminderNotifications().show(workoutReminderContent(now))
                preferencesRepository.markWorkoutReminderShown(now)
            }
            Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TrainIqReminderWorkerEntryPoint {
    fun trainIqDao(): TrainIqDao
    fun userPreferencesRepository(): UserPreferencesRepository
    fun reminderNotifications(): TrainIqReminderNotifications
}

internal const val TrainIqReminderWorkName = "trainiq_opt_in_reminders"
internal val ReminderCheckInterval: Duration = Duration.ofHours(4)
