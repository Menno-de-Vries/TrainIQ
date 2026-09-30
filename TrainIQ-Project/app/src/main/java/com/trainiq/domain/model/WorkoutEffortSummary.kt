package com.trainiq.domain.model

/** Recorded effort only: zero is the persisted unknown sentinel, never an effort estimate. */
object WorkoutEffortSummary {
    fun recordedRpe(rpe: Double): Double? = rpe.takeIf { it.isFinite() && it in 1.0..10.0 }

    fun averageRpe(sets: List<LoggedSet>): Double? = sets
        .filter { it.completed }
        .mapNotNull { recordedRpe(it.rpe) }
        .average().takeIf { it.isFinite() }

    /** Reuses the existing unweighted arithmetic mean; missing sets do not dilute reported RPE. */
    fun exercises(sets: List<LoggedSet>, names: Map<Long, String>): List<ExerciseEffortSummary> = sets
        .filter { it.completed }
        .groupBy { it.exerciseId }
        .map { (exerciseId, completedSets) ->
            ExerciseEffortSummary(
                exerciseId = exerciseId,
                exerciseName = names[exerciseId] ?: "Oefening $exerciseId",
                averageRpe = averageRpe(completedSets),
                sets = completedSets.map { set ->
                    CompletedSetEffort(
                        set.orderIndex, set.weight, set.reps, recordedRpe(set.rpe), set.setType.name,
                        set.repsInReserve?.takeIf { it in 0..10 },
                    )
                },
            )
        }
}

data class ExerciseEffortSummary(
    val exerciseId: Long,
    val exerciseName: String,
    val averageRpe: Double?,
    val sets: List<CompletedSetEffort>,
)

data class CompletedSetEffort(
    val orderIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val setType: String,
    val repsInReserve: Int?,
)
