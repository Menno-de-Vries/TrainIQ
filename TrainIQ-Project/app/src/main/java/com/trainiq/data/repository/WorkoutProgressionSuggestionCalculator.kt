package com.trainiq.data.repository

import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.data.mapper.parseSetType
import com.trainiq.domain.model.ProgressionSuggestion
import com.trainiq.domain.model.ReadinessLevel
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.StrengthCalculator
import com.trainiq.domain.model.WorkoutDay
import javax.inject.Inject

class WorkoutProgressionSuggestionCalculator @Inject constructor() {
    fun calculate(
        day: WorkoutDay?,
        sessions: List<WorkoutSessionEntity>,
        sets: List<WorkoutSetEntity>,
    ): List<ProgressionSuggestion> {
        day ?: return emptyList()
        val sessionsById = sessions
            .filter { it.completed && it.status == "COMPLETED" }
            .associateBy { it.id }
        val targetRepsByExerciseId = day.exercises.associate { it.exercise.id to parseTargetRepTarget(it.repRange) }
        return day.exercises.mapNotNull { plan ->
            val exerciseSessions = sets
                .filter { it.exerciseId == plan.exercise.id }
                .groupBy { it.sessionId }
                .mapNotNull { (sessionId, sessionSets) ->
                    val session = sessionsById[sessionId] ?: return@mapNotNull null
                    val progressionSets = sessionSets
                        .filter(::isProgressionSet)
                        .sortedByDescending { it.weight * it.reps }
                    if (progressionSets.isEmpty()) return@mapNotNull null
                    ExerciseSessionSnapshot(session.date, progressionSets)
                }
                .sortedByDescending { it.date }
                .take(3)
            val lastSession = exerciseSessions.firstOrNull() ?: return@mapNotNull null
            val lastSessionAvgRpe = lastSession.sets.map { it.rpe }.average().takeIf { !it.isNaN() }?.toFloat()
            val recentAverageRir = exerciseSessions
                .mapNotNull { session ->
                    session.sets
                        .mapNotNull { it.repsInReserve }
                        .average()
                        .takeIf { !it.isNaN() }
                        ?.toFloat()
                }
                .average()
                .takeIf { !it.isNaN() }
                ?.toFloat()
            val completedRecentSessions = exerciseSessions
                .take(2)
                .takeIf { it.size == 2 }
                ?.all { session ->
                    session.sets.isNotEmpty() && session.sets.all { it.reps >= (targetRepsByExerciseId[plan.exercise.id] ?: 0) }
                }
                ?: false
            val referenceWeight = lastSession.sets.maxOfOrNull { it.weight } ?: 0.0
            val readiness = resolveReadiness(
                lastSessionAvgRpe = lastSessionAvgRpe,
                recentAverageRir = recentAverageRir,
                completedRecentSessions = completedRecentSessions,
                plateauDetected = hasPlateaued(exerciseSessions),
            )
            val suggestedWeight = when (readiness) {
                ReadinessLevel.INCREASE -> referenceWeight + progressionLoadStep(
                    plan.exercise.name,
                    plan.exercise.muscleGroup,
                    plan.exercise.equipment,
                )
                ReadinessLevel.DELOAD -> referenceWeight * 0.9
                ReadinessLevel.PLATEAU -> referenceWeight
                ReadinessLevel.MAINTAIN -> referenceWeight
            }
            val lastLoggedSet = lastSession.sets.maxByOrNull { it.weight * it.reps }
            ProgressionSuggestion(
                exerciseId = plan.exercise.id,
                exerciseName = plan.exercise.name,
                suggestedWeightKg = suggestedWeight.coerceAtLeast(0.0),
                suggestedReps = plan.repRange,
                lastSessionAvgRpe = lastSessionAvgRpe,
                readinessSignal = readiness,
                lastLoggedWeightKg = lastLoggedSet?.weight,
                lastLoggedReps = lastLoggedSet?.reps?.toString(),
            )
        }
    }
}

private data class ExerciseSessionSnapshot(
    val date: Long,
    val sets: List<WorkoutSetEntity>,
)

private fun hasPlateaued(sessions: List<ExerciseSessionSnapshot>): Boolean {
    if (sessions.size < 3) return false
    val e1rms = sessions.take(3).map { session ->
        session.sets.maxOfOrNull { StrengthCalculator.estimateOneRepMax(it.weight, it.reps) } ?: 0.0
    }.filter { it > 0.0 }
    if (e1rms.size < 3) return false
    val min = e1rms.minOrNull() ?: return false
    val max = e1rms.maxOrNull() ?: return false
    return min > 0.0 && ((max - min) / min) < 0.01
}

internal fun resolveReadiness(
    lastSessionAvgRpe: Float?,
    recentAverageRir: Float?,
    completedRecentSessions: Boolean,
    plateauDetected: Boolean = false,
): ReadinessLevel {
    val effectiveRir = recentAverageRir
        ?: StrengthCalculator.estimateRepsInReserve(lastSessionAvgRpe?.toDouble() ?: 0.0)?.toFloat()
        ?: 0f
    return when {
        (lastSessionAvgRpe ?: 0f) > 9f -> ReadinessLevel.DELOAD
        recentAverageRir != null && recentAverageRir < 1f -> ReadinessLevel.DELOAD
        plateauDetected && effectiveRir >= 2f -> ReadinessLevel.PLATEAU
        effectiveRir >= 2f && completedRecentSessions -> ReadinessLevel.INCREASE
        else -> ReadinessLevel.MAINTAIN
    }
}

private fun progressionLoadStep(exerciseName: String, muscleGroup: String, equipment: String): Double {
    val compoundPattern = listOf("squat", "bench", "deadlift", "press", "row", "pull-up", "hip thrust")
    val normalized = "$exerciseName $muscleGroup $equipment".lowercase()
    return when {
        equipment.contains("dumbbell", ignoreCase = true) -> 1.0
        normalized.contains("raise") || normalized.contains("curl") || normalized.contains("extension") -> 1.0
        compoundPattern.any { normalized.contains(it) } -> 2.5
        else -> 1.25
    }
}

private fun WorkoutSetEntity.progressionSetType(): SetType = parseSetType(setType)

private fun isProgressionSet(set: WorkoutSetEntity): Boolean =
    set.completed && set.reps > 0 && set.weight >= 0.0 && set.progressionSetType().isProgressionType()

private fun SetType.isProgressionType(): Boolean = this == SetType.NORMAL || this == SetType.BACK_OFF

private fun parseTargetRepTarget(repRange: String): Int =
    repRange.substringAfter('-', repRange).trim().toIntOrNull()
        ?: repRange.filter(Char::isDigit).toIntOrNull()
        ?: 0
