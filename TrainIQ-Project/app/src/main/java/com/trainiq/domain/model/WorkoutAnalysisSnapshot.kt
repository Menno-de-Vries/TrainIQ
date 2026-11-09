package com.trainiq.domain.model

/** Facts from performed sets only. Zero external kg does not mean zero effort. */
data class WorkoutAnalysisSnapshot(
    val completedSets: List<LoggedSet>,
    val comparisonSets: List<LoggedSet>,
    val completedSetCount: Int,
    val totalReps: Int,
    val totalVolumeKg: Double,
    val comparisonVolumeKg: Double,
    val comparisonSetCount: Int,
    /** Unweighted mean of valid reported set-RPE, across every completed set type. */
    val reportedMeanRpe: Double?,
    /** Coverage numerator; [completedSetCount] is the denominator. */
    val reportedRpeCount: Int,
)

fun buildWorkoutAnalysisSnapshot(sets: List<LoggedSet>): WorkoutAnalysisSnapshot {
    val completed = sets.filter { it.completed }
    // Only completed types determine whether the legacy comparison fallback is needed.
    val comparison = completed.filter { it.setType == SetType.NORMAL || it.setType == SetType.BACK_OFF }
        .ifEmpty { completed }
    return WorkoutAnalysisSnapshot(
        completedSets = completed,
        comparisonSets = comparison,
        completedSetCount = completed.size,
        totalReps = completed.sumOf { it.reps },
        totalVolumeKg = completed.sumOf { it.weight * it.reps },
        comparisonVolumeKg = comparison.sumOf { it.weight * it.reps },
        comparisonSetCount = comparison.size,
        reportedMeanRpe = WorkoutEffortSummary.averageRpe(completed),
        reportedRpeCount = completed.count { WorkoutEffortSummary.recordedRpe(it.rpe) != null },
    )
}
