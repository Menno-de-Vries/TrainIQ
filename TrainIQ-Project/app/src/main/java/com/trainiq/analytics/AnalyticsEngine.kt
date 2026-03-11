package com.trainiq.analytics

import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.WorkoutSetEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsEngine @Inject constructor() {
    fun trainingVolume(sets: List<WorkoutSetEntity>): Double = sets.sumOf { it.weight * it.reps }

    fun weeklyVolumePerMuscleGroup(sets: List<WorkoutSetEntity>, exercises: List<ExerciseEntity>): Map<String, Double> {
        val exerciseMap = exercises.associateBy { it.id }
        return sets.groupBy { exerciseMap[it.exerciseId]?.muscleGroup ?: "Other" }
            .mapValues { (_, groupedSets) -> trainingVolume(groupedSets) }
    }

    fun estimatedOneRepMax(weight: Double, reps: Int): Double = weight * (1 + reps / 30.0)

    fun fatigueIndex(weeklyVolume: Double, baselineVolume: Double): Double {
        if (baselineVolume == 0.0) return 1.0
        return weeklyVolume / baselineVolume
    }
}
