package com.trainiq.core.database

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutCompletionObservationInstrumentedTest {
    private lateinit var database: TrainIqDatabase

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, TrainIqDatabase::class.java,
        ).build()
    }

    @After fun tearDown() = database.close()

    @Test fun observesSavedDebriefDeletionAndReplacementWithoutResavingWorkout() = runBlocking {
        val dao = database.dao()
        val session = WorkoutSessionEntity(id = 42L, date = 1_000L, duration = 60L,
            debriefSummary = "Lokaal", debriefGenerationId = "first")
        dao.importWorkoutSessions(listOf(session))
        val events = Channel<WorkoutCompletionSnapshot?>(Channel.UNLIMITED)
        val collection = launch { dao.observeWorkoutCompletionSnapshot(42L).collect { events.send(it) } }
        try {
            assertEquals("Lokaal", withTimeout(5_000) { events.receive() }?.session?.debriefSummary)
            dao.importWorkoutSessions(listOf(session.copy(debriefSummary = "Late AI", debriefSource = "GEMINI_2_5_FLASH")))
            assertEquals("Late AI", withTimeout(5_000) { events.receive() }?.session?.debriefSummary)
            assertEquals(1, dao.getWorkoutSessions().size)
            dao.deleteWorkoutSessionById(42L)
            assertNull(withTimeout(5_000) { events.receive() })
            dao.importWorkoutSessions(listOf(session.copy(debriefSummary = "Vervanging", debriefGenerationId = "second")))
            val replacement = withTimeout(5_000) { events.receive() }
            assertEquals("second", replacement?.session?.debriefGenerationId)
            assertEquals("LOCAL_FALLBACK", replacement?.session?.debriefSource)
        } finally {
            collection.cancel()
        }
    }

    @Test fun transactionContainsOnlySelectedSessionAndObservesRelatedSetAndMetadataChanges() = runBlocking {
        val dao = database.dao()
        val exercise = ExerciseEntity(3L, "Squat", "Benen", "Barbell")
        val set = WorkoutSetEntity(id = 8L, sessionId = 42L, exerciseId = 3L,
            weight = 60.0, reps = 8, rpe = 0.0, performedExerciseId = 6L)
        database.withTransaction {
            dao.insertRoutines(listOf(WorkoutRoutineEntity(1L, "Routine", "", true)))
            dao.insertWorkoutDays(listOf(WorkoutDayEntity(2L, 1L, "Dag", 0)))
            dao.insertExercises(listOf(exercise))
            dao.insertWorkoutExercises(listOf(WorkoutExerciseEntity(4L, 2L, 3L, 3, "8", 60)))
            dao.importWorkoutSessions(listOf(
                WorkoutSessionEntity(id = 42L, date = 1_000L, duration = 60L, workoutDayId = 2L),
                WorkoutSessionEntity(id = 99L, date = 2_000L, duration = 60L),
            ))
            dao.insertPerformedExercises(listOf(PerformedExerciseEntity(6L, 42L, 3L)))
            dao.insertWorkoutSets(listOf(set))
        }
        val events = Channel<WorkoutCompletionSnapshot?>(Channel.UNLIMITED)
        val collection = launch { dao.observeWorkoutCompletionSnapshot(42L).collect { events.send(it) } }
        try {
            val first = withTimeout(5_000) { events.receive() }!!
            assertEquals(42L, first.session.id)
            assertEquals(listOf(8L), first.sets.map { it.set.id })
            assertEquals(listOf(6L), first.performedExercises.map { it.id })
            assertEquals("Routine", first.days.single().routines.single().name)
            assertEquals(4L, first.days.single().workoutExercises.single().id)
            database.withTransaction {
                dao.importWorkoutSets(listOf(set.copy(reps = 10)))
                dao.insertExercises(listOf(exercise.copy(name = "Front squat")))
            }
            val changed = withTimeout(5_000) { events.receive() }!!
            assertEquals(10, changed.sets.single().set.reps)
            assertEquals("Front squat", changed.sets.single().exercises.single().name)
            dao.deleteWorkoutSetsForSession(42L)
            assertTrue(withTimeout(5_000) { events.receive() }!!.sets.isEmpty())
        } finally {
            collection.cancel()
        }
    }
}
