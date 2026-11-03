package com.trainiq.domain.model

import org.junit.Assert.*
import org.junit.Test

class WorkoutAnalysisSnapshotTest {
    @Test fun completedFactsKeepTotalAndComparisonVolumeSeparateFromReportedEffort() {
        val sets = listOf(set(20.0, 10, 3.0, SetType.WARM_UP), set(60.0, 8, 8.0), set(60.0, 8, 0.0))
        val result = buildWorkoutAnalysisSnapshot(sets)
        assertEquals(sets, result.completedSets)
        assertEquals(3, result.completedSetCount)
        assertEquals(26, result.totalReps)
        assertEquals(1160.0, result.totalVolumeKg, 0.0)
        assertEquals(960.0, result.comparisonVolumeKg, 0.0)
        assertEquals(2, result.comparisonSetCount)
        assertEquals(5.5, result.reportedMeanRpe!!, 0.0)
        assertEquals(2, result.reportedRpeCount)
        val unknown = buildWorkoutAnalysisSnapshot(sets.map { it.copy(rpe = 0.0) })
        assertEquals(1160.0, unknown.totalVolumeKg, 0.0)
        assertEquals(960.0, unknown.comparisonVolumeKg, 0.0)
        assertNull(unknown.reportedMeanRpe)
        assertEquals(0, unknown.reportedRpeCount)
    }

    @Test fun onlyCompletedTypesDetermineFallbackAndAllCompletedTypesKeepTheirDetails() {
        val drop = set(40.0, 6, 6.0, SetType.DROP_SET).copy(orderIndex = 2, repsInReserve = 3)
        val failure = set(30.0, 5, 9.0, SetType.FAILURE)
        val result = buildWorkoutAnalysisSnapshot(listOf(drop, failure, set(999.0, 10, 10.0).copy(completed = false)))
        assertEquals(listOf(drop, failure), result.completedSets)
        assertEquals(2, result.comparisonSetCount)
        assertEquals(390.0, result.comparisonVolumeKg, 0.0)
        assertEquals(7.5, result.reportedMeanRpe!!, 0.0)
        assertEquals(2, result.reportedRpeCount)
        val details = WorkoutEffortSummary.exercises(result.completedSets, emptyMap()).single()
        assertEquals("Oefening 1", details.exerciseName)
        assertEquals(CompletedSetEffort(2, 40.0, 6, 6.0, "DROP_SET", 3), details.sets.first())
    }

    @Test fun zeroExternalWeightDoesNotEraseRepsOrReportedEffort() {
        val result = buildWorkoutAnalysisSnapshot(listOf(set(0.0, 12, 8.0), set(0.0, 10, Double.NaN)))
        assertEquals(2, result.completedSetCount)
        assertEquals(22, result.totalReps)
        assertEquals(0.0, result.totalVolumeKg, 0.0)
        assertEquals(8.0, result.reportedMeanRpe!!, 0.0)
        assertEquals(1, result.reportedRpeCount)
    }

    @Test fun emptyOrIncompleteOnlySessionHasNoFactsOrEffort() {
        listOf(emptyList(), listOf(set(60.0, 8, 10.0).copy(completed = false))).forEach { sets ->
            val result = buildWorkoutAnalysisSnapshot(sets)
            assertTrue(result.completedSets.isEmpty())
            assertEquals(0, result.completedSetCount)
            assertEquals(0, result.totalReps)
            assertEquals(0.0, result.totalVolumeKg, 0.0)
            assertEquals(0.0, result.comparisonVolumeKg, 0.0)
            assertEquals(0, result.comparisonSetCount)
            assertEquals(0, result.reportedRpeCount)
            assertNull(result.reportedMeanRpe)
        }
    }

    private fun set(weight: Double, reps: Int, rpe: Double, type: SetType = SetType.NORMAL) =
        LoggedSet(exerciseId = 1L, weight = weight, reps = reps, rpe = rpe, setType = type)
}
