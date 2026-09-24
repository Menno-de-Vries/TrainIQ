package com.trainiq.features.workout

import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.GeneratedDay
import com.trainiq.domain.model.GeneratedExercise
import com.trainiq.domain.model.GeneratedRoutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeneratedRoutineEditingTest {
    private val routine = GeneratedRoutine(
        routineName = "Plan",
        routineDescription = "Concept",
        days = listOf(GeneratedDay("Dag 1", exercises = listOf(
            GeneratedExercise("Squat", "Benen", "Halterstang", 3, "8-12", 90),
            GeneratedExercise("Bankdrukken", "Borst", "Halterstang", 3, "8-12", 90),
        ))),
    )

    @Test fun replacementKeepsOtherExercisesAndStableLibraryId() {
        val updated = routine.withReplacedExercise(0, 0, Exercise(42, "Goblet squat", "Benen", "Dumbbells"))

        assertEquals(42L, updated.days.single().exercises.first().existingExerciseId)
        assertEquals("Bankdrukken", updated.days.single().exercises.last().exerciseName)
        assertEquals("Concept", updated.routineDescription)
    }

    @Test fun removeUpdatesDurationAndRejectsRemovingLastExercise() {
        val updated = routine.withRemovedExercise(0, 0)!!

        assertEquals(listOf("Bankdrukken"), updated.days.single().exercises.map { it.exerciseName })
        assertEquals(updated.days.single().estimatedDurationMinutes, updated.estimatedDurationMinutes)
        assertNull(updated.withRemovedExercise(0, 0))
    }
}
