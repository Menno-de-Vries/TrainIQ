package com.trainiq.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.trainiq.core.database.*
import com.trainiq.core.testing.TrainIqDebugDatabaseEntryPoint
import com.trainiq.core.workout.WorkoutRestTimerEntryPoint
import com.trainiq.domain.model.ActiveWorkoutSetDraft
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.SetType
import com.trainiq.testing.resetTrainIqAndroidTestDatabase
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

/** Real coordinator and Room; the existing timer mutex provides a controlled write barrier. */
class WorkoutFinishPendingSetRaceInstrumentedTest {
    @Test fun waitingFinishRejectsReplacedIdentityAndStartTimeWithoutTouchingReplacement() = runBlocking {
        for (replacementId in listOf(12L, 13L)) {
            val (db, coordinator, timers, _, now) = fixture()
            val dao = db.dao()
            val replacementSet = dao.readActiveWorkoutSetsForExport().single().copy(sessionId = replacementId, weight = 55.0)
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val barrier = async { timers.mutate { entered.complete(Unit); release.await() } }
            try {
                entered.await()
                val finish = async(start = CoroutineStart.UNDISPATCHED) { runCatching { coordinator.finishActiveWorkout(7L) } }
                assertFalse(finish.isCompleted)
                dao.discardActiveWorkoutSession(12L)
                dao.insertWorkoutSession(WorkoutSessionEntity(replacementId, now, 0, routineId = 1L,
                    workoutDayId = 7L, startedAt = now, endedAt = 0, status = "DRAFT", completed = false))
                val replacement = ActiveWorkoutSessionEntity(replacementId, 7L, 1L, now, now,
                    notes = "Replacement stays", restTimerHandled = true)
                dao.insertActiveWorkoutSessions(listOf(replacement))
                dao.insertPerformedExercises(listOf(PerformedExerciseEntity(21L, replacementId, 3L, 4L, 0)))
                dao.insertActiveWorkoutSets(listOf(replacementSet))
                release.complete(Unit)
                assertTrue(withTimeout(10_000) { finish.await() }.isFailure)
                assertEquals(listOf(replacement), dao.readActiveWorkoutSessionsForExport())
                assertEquals(listOf(replacementSet), dao.readActiveWorkoutSetsForExport())
                assertTrue(dao.readWorkoutSetsForExport().isEmpty())
                assertTrue(dao.readWorkoutSessionsForExport().none { it.completed })
            } finally { release.complete(Unit); barrier.await() }
        }
    }

    @Test fun removedSessionCannotBeRecreatedByWaitingFinish() = runBlocking {
        val (db, coordinator, timers) = fixture()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val barrier = async { timers.mutate { entered.complete(Unit); release.await() } }
        try {
            entered.await()
            val finish = async(start = CoroutineStart.UNDISPATCHED) { runCatching { coordinator.finishActiveWorkout(7L) } }
            db.dao().discardActiveWorkoutSession(12L)
            release.complete(Unit)
            assertTrue(withTimeout(10_000) { finish.await() }.isFailure)
            assertTrue(db.dao().readWorkoutSessionsForExport().isEmpty())
            assertTrue(db.dao().readActiveWorkoutSessionsForExport().isEmpty())
        } finally { release.complete(Unit); barrier.await() }
    }

    @Test fun failedFinalInsertRollsBackSessionAndRetainsActiveSetsAndTimer() = runBlocking {
        val (db, coordinator) = fixture()
        val dao = db.dao()
        val now = com.trainiq.core.workout.AndroidWorkoutRestTimerClock(
            ApplicationProvider.getApplicationContext<Context>()).now()
        dao.insertActiveWorkoutSessions(listOf(dao.readActiveWorkoutSessionsForExport().single().copy(
            restTimerGeneration = "rollback-timer", restTimerEndsAt = now.wallMillis + 120_000,
            restTimerElapsedDeadline = now.elapsedMillis + 120_000, restTimerBootId = now.bootId,
            restTimerTotalSeconds = 120)))
        val active = dao.readActiveWorkoutSessionsForExport()
        val sets = dao.readActiveWorkoutSetsForExport()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_finish BEFORE INSERT ON workout_sets BEGIN SELECT RAISE(ABORT, 'synthetic finish failure'); END")
        try {
            assertTrue(runCatching { coordinator.finishActiveWorkout(7L) }.isFailure)
            assertEquals(active, dao.readActiveWorkoutSessionsForExport())
            assertEquals(sets, dao.readActiveWorkoutSetsForExport())
            assertEquals("DRAFT", dao.readWorkoutSessionsForExport().single().status)
            assertTrue(dao.readWorkoutSetsForExport().isEmpty())
            assertEquals(21L, dao.readPerformedExercisesForExport().single().id)
        } finally { db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_finish") }
    }

    @Test fun explicitLegacyFinishKeepsItsInputsAndDoesNotCleanUpActiveWorkout() = runBlocking {
        val (db, coordinator) = fixture()
        val active = db.dao().readActiveWorkoutSessionsForExport()
        val input = listOf(LoggedSet(exerciseId = 3L, weight = 20.0, reps = 5, rpe = 7.0))
        val result = coordinator.finishWorkout(7L, 75L, input)
        assertTrue(result.summary.contains("100"))
        assertEquals(active, db.dao().readActiveWorkoutSessionsForExport())
        assertEquals(1, db.dao().readActiveWorkoutSetsForExport().size)
        val stored = db.dao().readWorkoutSessionsForExport().single { it.completed }
        assertEquals(75L, stored.duration)
        assertEquals(20.0, db.dao().readWorkoutSetsForExport().single().weight, 0.0)
        db.dao().discardActiveWorkoutSession(12L)
        coordinator.finishWorkout(7L, 90L, input)
        assertEquals(2, db.dao().readWorkoutSessionsForExport().count { it.completed })
    }

    @Test fun incompleteOnlyActiveWorkoutIsNotClearedOrCompleted() = runBlocking {
        val (db, coordinator) = fixture()
        val set = db.dao().readActiveWorkoutSetsForExport().single()
        db.dao().insertActiveWorkoutSets(listOf(set.copy(completed = false)))
        val result = coordinator.finishActiveWorkout(7L)
        assertEquals(0L, result.sessionId)
        assertEquals("Geen sets gelogd.", result.debrief.summary)
        assertEquals(listOf(set.copy(completed = false)), db.dao().readActiveWorkoutSetsForExport())
        assertFalse(db.dao().readWorkoutSessionsForExport().single().completed)
    }

    @Test fun pendingSetCommittedBeforeFinishMustRemainInCompletedWorkout() = runBlocking {
        val (db, coordinator, timers, store, now, initial) = fixture()
        val dao = db.dao()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val barrier = async {
            timers.mutate { entered.complete(Unit); release.await() }
        }
        try {
            withTimeout(10_000) { entered.await() }
            // Undispatched starts reach the held mutex in order; finish reads the one-set state first.
            val log = async(start = CoroutineStart.UNDISPATCHED) {
                store.logActiveWorkoutSet(initial, 7L,
                    LoggedSet(exerciseId = 3L, weight = 90.0, reps = 6, rpe = 8.0,
                        performedExerciseId = 21L, sourceWorkoutExerciseId = 4L, setType = SetType.NORMAL),
                    ActiveWorkoutSetDraft(weight = "90", reps = "6", rpe = "8"), 0, now + 1)
            }
            assertFalse("Second write must be waiting at the barrier", log.isCompleted)
            val finish = async(start = CoroutineStart.UNDISPATCHED) { coordinator.finishActiveWorkout(7L) }
            assertFalse("Finish must also wait after snapshotting the earlier state", finish.isCompleted)
            assertEquals(1, store.state.value.activeWorkoutSession!!.loggedSets.size)
            release.complete(Unit)
            val logged = withTimeout(10_000) { log.await() }
            assertEquals("Second set must commit before finish", 2, logged.active.loggedSets.size)
            val result = withTimeout(10_000) { finish.await() }
            assertEquals(12L, result.sessionId)
            val completed = dao.readWorkoutSetsForExport().filter { it.sessionId == 12L }
            val remaining = dao.readActiveWorkoutSetsForExport().filter { it.sessionId == 12L }
            assertEquals("A committed second set must survive finishing; completed=${completed.size}, active=${remaining.size}", 2, completed.size)
            assertEquals(listOf(80.0 to 5, 90.0 to 6), completed.sortedBy { it.orderIndex }.map { it.weight to it.reps })
            assertEquals(listOf(8.0, 8.0), completed.map { it.rpe })
            assertTrue(completed.all { it.performedExerciseId == 21L && it.setType == "NORMAL" && it.completed })
            assertEquals(listOf(0, 1), completed.sortedBy { it.orderIndex }.map { it.orderIndex })
            assertEquals(listOf(now, now + 1), completed.sortedBy { it.orderIndex }.map { it.loggedAt })
            assertTrue(completed.all { it.restSeconds == 0 })
            assertEquals(4L, dao.readPerformedExercisesForExport().single().sourceWorkoutExerciseId)
            assertTrue(remaining.isEmpty())
            val session = dao.readWorkoutSessionsForExport().single { it.id == 12L }
            assertEquals(result.debrief.summary, session.debriefSummary)
            assertTrue(result.debrief.summary.contains("940"))
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
            assertTrue(runCatching { coordinator.finishActiveWorkout(7L) }.isFailure)
            assertEquals(2, dao.readWorkoutSetsForExport().size)
        } finally {
            release.complete(Unit)
            barrier.await()
        }
    }
    private data class Fixture(val db: TrainIqDatabase, val coordinator: TrainIqDataCoordinator,
        val timers: com.trainiq.domain.workout.WorkoutRestTimerCoordinator,
        val store: RoomTrainIqRuntimeStore, val now: Long,
        val initial: com.trainiq.data.local.TrainIqStorageState)

    private suspend fun fixture(): Fixture {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = resetTrainIqAndroidTestDatabase(context)
        val dao = db.dao()
        val now = System.currentTimeMillis()
        dao.insertRoutines(listOf(WorkoutRoutineEntity(1L, "Synthetic routine", "", true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(7L, 1L, "Day", 0)))
        dao.insertExercises(listOf(ExerciseEntity(3L, "Synthetic press", "Chest", "Barbell")))
        dao.insertWorkoutExercises(listOf(WorkoutExerciseEntity(4L, 7L, 3L, 3, "5", 0)))
        dao.insertWorkoutSession(WorkoutSessionEntity(id = 12L, date = now - 60_000, duration = 60,
            routineId = 1L, workoutDayId = 7L, startedAt = now - 60_000, endedAt = 0,
            status = "DRAFT", completed = false))
        dao.insertPerformedExercises(listOf(PerformedExerciseEntity(21L, 12L, 3L, 4L, 0)))
        dao.insertActiveWorkoutSessions(listOf(ActiveWorkoutSessionEntity(12L, 7L, 1L, now - 60_000, now)))
        dao.insertActiveWorkoutSets(listOf(ActiveWorkoutSetEntity(sessionId = 12L, id = 1L, exerciseId = 3L,
            performedExerciseId = 21L, sourceWorkoutExerciseId = 4L, weight = 80.0, reps = 5, rpe = 8.0,
            setType = "NORMAL", restSeconds = 0, orderIndex = 0, completed = true, loggedAt = now)))

        val coordinator = EntryPointAccessors.fromApplication(context, TrainIqDebugDatabaseEntryPoint::class.java).dataCoordinator()
        val timers = EntryPointAccessors.fromApplication(context, WorkoutRestTimerEntryPoint::class.java).restTimerCoordinator()
        val storeField = TrainIqDataCoordinator::class.java.getDeclaredField("runtimeStore").apply { isAccessible = true }
        val store = storeField.get(coordinator) as RoomTrainIqRuntimeStore
        val timersField = RoomTrainIqRuntimeStore::class.java.getDeclaredField("restTimers").apply { isAccessible = true }
        assertSame("Barrier must use the production store's actual mutex", timers, timersField.get(store))
        val initial = withTimeout(10_000) { store.state.first {
            it.activeWorkoutSession?.loggedSets?.size == 1 && it.activeWorkoutSession.startedAt == now - 60_000
        } }
        return Fixture(db, coordinator, timers, store, now, initial)
    }
}
