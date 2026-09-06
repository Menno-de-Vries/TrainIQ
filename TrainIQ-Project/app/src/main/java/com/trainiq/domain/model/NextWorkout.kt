package com.trainiq.domain.model

internal fun WorkoutRoutine.nextStartableWorkoutDay(): WorkoutDay? =
    days.filter { it.exercises.isNotEmpty() }.minWithOrNull(compareBy<WorkoutDay> { it.orderIndex }.thenBy { it.id })
