package com.trainiq.data.repository

import com.trainiq.core.database.ExerciseEntity
import com.trainiq.data.local.TrainIqStorageState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseLibrarySeederTest {
    @Test
    fun mergeCanonicalExerciseLibrary_addsMissingExercisesAfterCurrentMaxId() {
        val state = TrainIqStorageState(
            exercises = listOf(ExerciseEntity(id = 100L, name = "Custom Lift", muscleGroup = "Core", equipment = "Cable")),
        )

        val updated = mergeCanonicalExerciseLibrary(state)

        assertTrue(updated.exercises.any { it.name == "Bench Press" })
        assertTrue(updated.exercises.any { it.name == "Good Morning" })
        assertEquals(101L, updated.exercises.first { it.name == "Bench Press" }.id)
    }

    @Test
    fun mergeCanonicalExerciseLibrary_doesNotDuplicateExistingExerciseNames() {
        val state = TrainIqStorageState(
            exercises = listOf(ExerciseEntity(id = 7L, name = "bench press", muscleGroup = "Chest", equipment = "Barbell")),
        )

        val updated = mergeCanonicalExerciseLibrary(state)

        assertEquals(1, updated.exercises.count { it.name.equals("Bench Press", ignoreCase = true) })
    }

    @Test
    fun shouldSkipExerciseLibrarySeed_preservesExistingLargeLibraries() {
        val state = TrainIqStorageState(
            exercises = (1L..50L).map { ExerciseEntity(id = it, name = "Custom $it", muscleGroup = "General", equipment = "Machine") },
        )

        assertTrue(shouldSkipExerciseLibrarySeed(state))
        assertEquals(state, mergeCanonicalExerciseLibrary(state))
    }
}
