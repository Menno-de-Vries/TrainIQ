package com.trainiq.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trainiq.core.database.*
import com.trainiq.domain.workout.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WorkoutRestTimerRoomTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun database() = Room.inMemoryDatabaseBuilder(context, TrainIqDatabase::class.java).build()
    private suspend fun seed(database: TrainIqDatabase) {
        database.dao().insertRoutines(listOf(WorkoutRoutineEntity(1, "Rest fixture", "", true)))
        database.dao().insertWorkoutDays(listOf(WorkoutDayEntity(2, 1, "Day", 0)))
        database.dao().insertActiveWorkoutSessions(listOf(ActiveWorkoutSessionEntity(sessionId = 7, dayId = 2,
            routineId = 1, startedAt = 1, updatedAt = 1, notes = "Keep notes", restTimerEndsAt = 50_000,
            restTimerTotalSeconds = 30, restTimerGeneration = "one", restTimerElapsedDeadline = 10_000, restTimerBootId = "a")))
    }

    @Test fun transactionalClaimRejectsOldEarlyAndDuplicateCallbacksWithoutLosingWorkout() = runBlocking {
        val database = database()
        try {
            seed(database)
            val store = RoomWorkoutRestTimerStore(database)
            val now = WorkoutRestTimerNow(50_000, 10_000, "a")
            val identity = WorkoutRestTimerIdentity(7, "one")
            assertEquals(RestTimerCompletion.OBSOLETE, store.claim(identity.copy(generation = "old"), now))
            assertEquals(RestTimerCompletion.EARLY, store.claim(identity, now.copy(elapsedMillis = 9_999)))
            val first = async { store.claim(identity, now) }
            val second = async { store.claim(identity, now) }
            assertEquals(setOf(RestTimerCompletion.COMPLETED, RestTimerCompletion.HANDLED), setOf(first.await(), second.await()))
            val saved = database.dao().readActiveWorkoutSessionsForExport().single()
            assertEquals("Keep notes", saved.notes)
            assertEquals("one", saved.restTimerGeneration)
            assertTrue(saved.restTimerHandled)
            assertNull(saved.restTimerEndsAt)
        } finally { database.close() }
    }

    @Test fun newStoreRestoresSameBootButRebootAndLegacyTimersExpireQuietly() = runBlocking {
        val database = database()
        try {
            seed(database)
            val restored = RoomWorkoutRestTimerStore(database).read()!!
            assertEquals(1, restored.remainingSeconds(WorkoutRestTimerNow(-1, 9_001, "a")))
            val store = RoomWorkoutRestTimerStore(database)
            assertEquals(RestTimerCompletion.EXPIRED, store.claim(restored.identity, WorkoutRestTimerNow(0, 1, "b")))
            database.dao().updateActiveWorkoutRestTimer(7, 999_999, 120, 2)
            val legacy = store.read()!!
            assertEquals(RestTimerCompletion.EXPIRED, store.claim(legacy.identity, WorkoutRestTimerNow(0, 1, "b")))
            assertNull(store.read())
        } finally { database.close() }
    }

    @Test fun discardedSessionCannotBeClaimedOrRecreatedByLateCompletion() = runBlocking {
        val database = database()
        try {
            seed(database)
            database.dao().discardActiveWorkoutSession(7)
            val store = RoomWorkoutRestTimerStore(database)
            assertEquals(RestTimerCompletion.OBSOLETE, store.claim(WorkoutRestTimerIdentity(7, "one"), WorkoutRestTimerNow(0, 10_000, "a")))
            assertNull(store.read())
        } finally { database.close() }
    }
}
