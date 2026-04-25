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
            targetWeightKg = 100.0,
            targetRpe = 8.5,
            setType = "TOP_SET",
            supersetGroupId = 99L,
            orderIndex = 1,
        )
        val exercise = Exercise(4L, "Bench Press", "Chest", "Barbell")

        val plan = entity.toDomain(exercise)

        assertEquals(SetType.NORMAL, plan.setType)
        assertEquals(99L, plan.supersetGroupId)
        assertEquals(100.0, plan.targetWeightKg, 0.0)
        assertEquals(8.5, plan.targetRpe, 0.0)
        assertEquals(4, plan.sets.size)
        assertEquals(SetType.NORMAL, plan.sets.first().setType)
        assertEquals(8, plan.sets.first().targetReps)
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

        assertEquals(SetType.NORMAL, plan.setType)
        assertNull(plan.supersetGroupId)
    }

    @Test
    fun parseSetType_withLegacyAliases_mapsToCanonicalTypes() {
        assertEquals(SetType.NORMAL, parseSetType("WORKING"))
        assertEquals(SetType.NORMAL, parseSetType("TOP_SET"))
        assertEquals(SetType.WARM_UP, parseSetType("WARMUP"))
        assertEquals(SetType.BACK_OFF, parseSetType("BACKOFF"))
        assertEquals(SetType.DROP_SET, parseSetType("DROP"))
        assertEquals(SetType.FAILURE, parseSetType("FAILURE"))
    }
}
