package com.trainiq.data.repository

import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.data.mapper.parseSetType
import com.trainiq.domain.model.ExerciseEffortSummary
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.WorkoutAnalysisSnapshot
import com.trainiq.domain.model.WorkoutEffortSummary
import com.trainiq.domain.model.buildWorkoutAnalysisSnapshot

/** Repository metadata adapter: planned but unperformed exercises never become training facts. */
internal data class WorkoutDebriefAnalysis(
    val snapshot: WorkoutAnalysisSnapshot,
    val exerciseEffort: List<ExerciseEffortSummary>,
    val distribution: String,
    val topExercises: String,
)

internal fun buildWorkoutDebriefAnalysis(
    sets: List<LoggedSet>,
    exercises: List<ExerciseEntity>,
): WorkoutDebriefAnalysis {
    val snapshot = buildWorkoutAnalysisSnapshot(sets)
    val metadata = exercises.associateBy { it.id }
    val names = exercises.associate { it.id to it.name }
    return WorkoutDebriefAnalysis(
        snapshot = snapshot,
        exerciseEffort = WorkoutEffortSummary.exercises(snapshot.completedSets, names),
        distribution = snapshot.completedSets.map { it.exerciseId }.distinct()
            .groupingBy { metadata[it]?.muscleGroup?.takeIf(String::isNotBlank) ?: "Onbekend" }
            .eachCount().entries.joinToString { "${it.key} ${it.value}" },
        topExercises = snapshot.completedSets.sortedByDescending { it.weight * it.reps }.take(3)
            .joinToString { set ->
                workoutDebriefTopSetText(
                    names[set.exerciseId] ?: "Oefening ${set.exerciseId}",
                    formatSummaryWeight(set.weight), set.reps,
                )
            }.ifBlank { workoutDebriefEmptyTopSetsText() },
    )
}

internal fun WorkoutSetEntity.toAnalysisSet() = LoggedSet(
    id = id, exerciseId = exerciseId, weight = weight, reps = reps, rpe = rpe,
    repsInReserve = repsInReserve, completed = completed, setType = parseSetType(setType),
    restSeconds = restSeconds, orderIndex = orderIndex, performedExerciseId = performedExerciseId, loggedAt = loggedAt,
)
