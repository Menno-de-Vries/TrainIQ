package com.trainiq.core.workout

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.trainiq.data.repository.RoomWorkoutRestTimerStore
import com.trainiq.domain.workout.*
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.*

@Module
@InstallIn(SingletonComponent::class)
object WorkoutRestTimerModule {
    @Provides @Singleton fun coordinator(store: RoomWorkoutRestTimerStore, delivery: WorkoutRestTimerScheduler,
        clock: AndroidWorkoutRestTimerClock) = WorkoutRestTimerCoordinator(store, delivery, clock)
}

@EntryPoint @InstallIn(SingletonComponent::class)
interface WorkoutRestTimerEntryPoint { fun restTimerCoordinator(): WorkoutRestTimerCoordinator }

class WorkoutRestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(8_000) {
                    val coordinator = EntryPointAccessors.fromApplication(context.applicationContext,
                        WorkoutRestTimerEntryPoint::class.java).restTimerCoordinator()
                    val generation = intent.getStringExtra(RestGenerationExtra)
                    if (intent.action == RestTimerAction && generation != null) {
                        coordinator.completeIfCurrent(WorkoutRestTimerIdentity(intent.getLongExtra(RestSessionExtra, -1), generation))
                    } else coordinator.reconcile()
                }
            } catch (error: CancellationException) { throw error } catch (_: Exception) {
                // Foreground reconciliation recovers persisted future timers; no repeating service.
            } finally { pending.finish() }
        }
    }
}
