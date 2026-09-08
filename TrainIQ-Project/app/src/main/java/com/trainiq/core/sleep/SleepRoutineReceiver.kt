package com.trainiq.core.sleep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trainiq.data.sleep.SleepRoutineRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SleepRoutineEntryPoint {
    fun sleepRoutineRepository(): SleepRoutineRepository
}

class SleepRoutineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository(context).reconcile(timeChanged = intent.action == Intent.ACTION_TIME_CHANGED ||
                    intent.action == Intent.ACTION_TIMEZONE_CHANGED)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // The durable recovery worker retries storage/scheduling failures.
            } finally {
                pending.finish()
            }
        }
    }
}

class SleepRoutineWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        repository(applicationContext).reconcile()
        Result.success()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        Result.retry()
    }
}

private fun repository(context: Context) = EntryPointAccessors.fromApplication(
    context.applicationContext, SleepRoutineEntryPoint::class.java).sleepRoutineRepository()
