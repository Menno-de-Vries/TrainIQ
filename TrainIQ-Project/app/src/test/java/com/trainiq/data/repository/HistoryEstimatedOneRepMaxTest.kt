package com.trainiq.data.repository

import com.trainiq.domain.model.ExerciseHistorySet
import com.trainiq.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryEstimatedOneRepMaxTest {
    private fun set(reps: Int, weight: Double = 100.0) = ExerciseHistorySet(
        orderIndex = 0, reps = reps, weightKg = weight, setType = SetType.NORMAL,
        restSeconds = 60, rpe = 8.0, repsInReserve = null, completed = true,
    )

    @Test fun historyUsesTheSameBrzyckiAndEpleyBoundaryAsTraining() {
        listOf(1 to 100.0, 5 to (100.0 / 0.8888), 10 to (100.0 / 0.7498), 11 to (100.0 * (1 + 11.0 / 30))).forEach { (reps, expected) ->
            assertEquals(expected, historyEstimatedOneRepMax(listOf(set(reps))), 0.0001)
        }
    }

    @Test fun emptyAndInvalidSetsCannotCreateARecord() {
        assertEquals(0.0, historyEstimatedOneRepMax(emptyList()), 0.0)
        assertEquals(0.0, historyEstimatedOneRepMax(listOf(set(0), set(5, 0.0))), 0.0)
        assertEquals(100.0 / 0.8888, historyEstimatedOneRepMax(listOf(set(0), set(5), set(1))), 0.0001)
    }
}
