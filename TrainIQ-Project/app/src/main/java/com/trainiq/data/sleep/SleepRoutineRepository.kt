package com.trainiq.data.sleep

import com.trainiq.core.database.TrainIqDao
import com.trainiq.core.database.toDomain
import com.trainiq.core.database.toEntity
import com.trainiq.core.sleep.SleepRoutineScheduler
import com.trainiq.domain.sleep.SleepRoutine
import com.trainiq.domain.sleep.SleepRepeatMillis
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class SleepRoutineRepository @Inject constructor(
    private val dao: TrainIqDao,
    private val scheduler: SleepRoutineScheduler,
) {
    private val mutex = Mutex()
    val routine = dao.observeSleepRoutine().map { it?.toDomain() ?: SleepRoutine() }

    suspend fun configure(enabled: Boolean, minute: Int) = mutex.withLock {
        val state = read().configure(enabled, minute, System.currentTimeMillis(), ZoneId.systemDefault())
        dao.saveSleepRoutine(state.toEntity())
        scheduler.cancelNotification()
        scheduler.schedule(state)
    }

    suspend fun confirm() = mutex.withLock {
        val state = read().confirm(System.currentTimeMillis())
        dao.saveSleepRoutine(state.toEntity())
        scheduler.cancelNotification()
        scheduler.schedule(state)
        if (state.active && state.confirmedAt > 0) scheduler.showCountdown(state)
    }

    suspend fun clear() = mutex.withLock {
        dao.saveSleepRoutine(SleepRoutine().toEntity())
        scheduler.schedule(SleepRoutine())
        scheduler.cancelNotification()
    }

    suspend fun reconcile(timeChanged: Boolean = false) = mutex.withLock {
        val now = System.currentTimeMillis()
        var state = read()
        val alreadyActive = state.active
        if (timeChanged && state.enabled && !state.active) {
            state = state.configure(true, state.minuteOfDay, now, ZoneId.systemDefault())
        }
        state = state.advance(now, ZoneId.systemDefault())
        if (state.active && state.confirmedAt == 0L && state.nextAt <= now) {
            scheduler.showReminder(escalated = alreadyActive)
            state = state.copy(nextAt = now + SleepRepeatMillis)
        } else if (!state.active) {
            scheduler.cancelNotification()
        }
        dao.saveSleepRoutine(state.toEntity())
        scheduler.schedule(state)
    }

    private suspend fun read() = dao.getSleepRoutine()?.toDomain() ?: SleepRoutine()
}
