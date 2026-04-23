package com.trainiq.data.mapper

import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutMapperTest {
    @Test
    fun workoutExerciseEntityToDomain_withSetTypeAndSuperset_mapsNewPlanningFields() {
        val entity = WorkoutExerciseEntity(
            id = 10L,
            dayId = 2L,
            exerciseId = 4L,
            targetSets = 4,
            repRange = "6-8",
            restSeconds = 120,
            setType = "TOP_SET",
            supersetGroupId = 99L,
            orderIndex = 1,
        )
        val exercise = Exercise(4L, "Bench Press", "Chest", "Barbell")

        val plan = entity.toDomain(exercise)

        assertEquals(SetType.TOP_SET, plan.setType)
        assertEquals(99L, plan.supersetGroupId)
    }

    @Test
    fun workoutExerciseEntityToDomain_withUnknownSetType_defaultsToWorking() {
        val entity = WorkoutExerciseEntity(
            id = 10L,
            dayId = 2L,
            exerciseId = 4L,
            targetSets = 4,
            repRange = "6-8",
            restSeconds = 120,
            setType = "UNKNOWN",
        )
        val exercise = Exercise(4L, "Bench Press", "Chest", "Barbell")

        val plan = entity.toDomain(exercise)

        assertEquals(SetType.WORKING, plan.setType)
        assertNull(plan.supersetGroupId)
    }
}
