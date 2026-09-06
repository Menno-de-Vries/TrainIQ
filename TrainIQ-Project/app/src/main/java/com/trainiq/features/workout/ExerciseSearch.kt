package com.trainiq.features.workout

import com.trainiq.domain.model.Exercise

internal fun exerciseSearchEmptyText(): String = "Geen passende oefeningen gevonden. Pas je zoekopdracht aan."

internal fun filterPickerExercises(exercises: List<Exercise>, query: String): List<Exercise> {
    val terms = exerciseSearchTerms(query)
    if (terms.isEmpty()) return exercises
    return exercises.filter { matchesExerciseSearch(it, terms) }
}

internal fun exerciseSearchTerms(query: String): List<String> =
    query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

internal fun matchesExerciseSearch(exercise: Exercise, terms: List<String>, extra: String = ""): Boolean {
    val searchable = "${exercise.name} ${exercise.muscleGroup} ${exercise.equipment} $extra " +
        exerciseHistorySubtitleText(exercise.muscleGroup, exercise.equipment)
    return terms.all { searchable.contains(it, ignoreCase = true) }
}
