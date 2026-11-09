package com.trainiq.data.repository

import com.trainiq.core.database.*
import com.trainiq.data.local.ActiveWorkoutSessionStorage
import com.trainiq.data.local.TrainIqStorageState
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.SetType
import org.junit.Assert.*
import org.junit.Test

class WorkoutFinishWriteTest {
    private val current = TrainIqStorageState(
        days = listOf(WorkoutDayEntity(7, 1, "Day", 0)),
        exercises = listOf(ExerciseEntity(3, "Press", "Chest", "Barbell")),
        performedExercises = listOf(PerformedExerciseEntity(21, 12, 3, 4, 0)),
        activeWorkoutSession = ActiveWorkoutSessionStorage(sessionId = 12, dayId = 7, startedAt = 1, notes = "Fresh note"),
    )
    @Test fun sameSnapshotProducesSetContentsEffortAndLocalDebrief() {
        val sets = listOf(
            LoggedSet(exerciseId = 3, weight = 80.0, reps = 5, rpe = 8.0, performedExerciseId = 21, sourceWorkoutExerciseId = 4, loggedAt = 100),
            LoggedSet(exerciseId = 3, weight = 90.0, reps = 6, rpe = 7.0, performedExerciseId = 21, sourceWorkoutExerciseId = 4,
                setType = SetType.BACK_OFF, restSeconds = 30, orderIndex = 1, loggedAt = 200),
            LoggedSet(exerciseId = 3, weight = 999.0, reps = 99, rpe = 0.0, completed = false),
        )
        val write = buildWorkoutFinishWrite(current, 7, 60, sets, 12, 1, 60_001)
        assertEquals("Fresh note", write.session!!.notes)
        assertEquals(1L, write.session.routineId)
        assertEquals(60L, write.session.duration)
        assertEquals(sets.map { it.weight }, write.sets.map { it.weight })
        assertEquals(listOf(100L, 200L), write.sets.take(2).map { it.loggedAt })
        assertEquals(30, write.sets[1].restSeconds)
        assertEquals("BACK_OFF", write.sets[1].setType)
        assertTrue(write.result.debrief.summary.contains("940"))
        assertTrue(write.result.debrief.summary.contains("2 sets"))
        assertTrue(write.result.debrief.summary.contains("7,5"))
        assertEquals(write.result.debrief.summary, write.session.debriefSummary)
        assertEquals(write.result.generationId, write.session.debriefGenerationId)
    }
    @Test fun emptyOrIncompleteOnlyReturnsNoWriteAndExplicitLegacyInputsRemainAuthoritative() {
        for (sets in listOf(emptyList(), listOf(LoggedSet(exerciseId = 3, weight = 80.0, reps = 5, rpe = 8.0, completed = false)))) {
            val empty = buildWorkoutFinishWrite(current, 7, 60, sets, 12, 1, 60_001)
            assertNull(empty.session)
            assertEquals(0L, empty.result.sessionId)
        }
        val legacy = buildWorkoutFinishWrite(current, 7, 75, listOf(LoggedSet(exerciseId = 3, weight = 20.0, reps = 5, rpe = 7.0)), null, null, 100_000)
        assertEquals(75L, legacy.session!!.duration)
        assertEquals(25_000L, legacy.session.startedAt)
        assertEquals("", legacy.session.notes)
        assertTrue(legacy.result.debrief.summary.contains("100"))
    }
}
