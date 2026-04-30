package com.trainiq.data.repository

import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.PerformedExerciseEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.domain.model.WorkoutDebrief
import com.trainiq.domain.model.WorkoutDebriefSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutCompletionSummaryTest {
    @Test
    fun buildWorkoutCompletionSummary_includesSavedSessionSetsAndSourceLabel() {
        val session = WorkoutSessionEntity(
            id = 42L,
            date = 10_000L,
            duration = 2_700L,
            routineId = 1L,
            workoutDayId = 7L,
            startedAt = 10_000L,
            endedAt = 12_700L,
            status = "COMPLETED",
            completed = true,
            debriefSummary = "Sterke sessie met solide werksets.",
            debriefRecommendation = "Verhoog waar technisch stabiel.",
            debriefSource = WorkoutDebriefSource.LOCAL_FALLBACK.name,
        )
        val summary = buildWorkoutCompletionSummary(
            session = session,
            routines = listOf(WorkoutRoutineEntity(1L, "Upper Body", "", true)),
            days = listOf(WorkoutDayEntity(7L, 1L, "Push", 0)),
            exercises = listOf(
                ExerciseEntity(3L, "Bench Press", "Chest", "Barbell"),
                ExerciseEntity(4L, "Cable Row", "Back", "Cable"),
            ),
            workoutExercises = listOf(
                WorkoutExerciseEntity(30L, 7L, 3L, 2, "6-8", 120, orderIndex = 0),
                WorkoutExerciseEntity(40L, 7L, 4L, 2, "8-10", 90, orderIndex = 1),
            ),
            performedExercises = listOf(
                PerformedExerciseEntity(300L, 42L, 3L, 30L, 0),
                PerformedExerciseEntity(400L, 42L, 4L, 40L, 1),
            ),
            sets = listOf(
                WorkoutSetEntity(1L, 42L, 3L, 80.0, 8, 8.0, performedExerciseId = 300L, orderIndex = 0),
                WorkoutSetEntity(2L, 42L, 4L, 70.0, 10, 7.0, performedExerciseId = 400L, orderIndex = 1),
            ),
            fallbackDebrief = WorkoutDebrief(
                summary = "Fallback",
                progressionFeedback = "",
                recommendation = "",
                nextSessionFocus = "",
                recoveryScore = 75,
                intensitySignal = "MAINTAIN",
            ),
        )

        assertEquals(42L, summary.sessionId)
        assertEquals("Upper Body - Push", summary.workoutName)
        assertEquals(2, summary.exercisesCompleted)
        assertEquals(2, summary.setsLogged)
        assertEquals(1_340.0, summary.totalVolume, 0.0)
        assertEquals(WorkoutDebriefSource.LOCAL_FALLBACK, summary.debrief.source)
        assertTrue(summary.sourceLabel.contains("trainingsdata"))
        assertEquals(listOf("Bench Press", "Cable Row"), summary.exercises.map { it.name })
        assertEquals("80 kg x 8", summary.strongestSetLabel)
    }
}
