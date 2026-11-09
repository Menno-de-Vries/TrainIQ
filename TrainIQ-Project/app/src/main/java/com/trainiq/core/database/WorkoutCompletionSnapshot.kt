package com.trainiq.core.database

import androidx.room.Embedded
import androidx.room.Relation

/** Only the completed session and its display data, read together by Room in one transaction. */
data class WorkoutCompletionSnapshot(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId", entity = WorkoutSetEntity::class)
    val sets: List<WorkoutCompletionSetSnapshot>,
    @Relation(parentColumn = "id", entityColumn = "session_id")
    val performedExercises: List<PerformedExerciseEntity>,
    @Relation(parentColumn = "routine_id", entityColumn = "id")
    val routines: List<WorkoutRoutineEntity>,
    @Relation(parentColumn = "workout_day_id", entityColumn = "id", entity = WorkoutDayEntity::class)
    val days: List<WorkoutCompletionDaySnapshot>,
)

data class WorkoutCompletionSetSnapshot(
    @Embedded val set: WorkoutSetEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercises: List<ExerciseEntity>,
)

data class WorkoutCompletionDaySnapshot(
    @Embedded val day: WorkoutDayEntity,
    @Relation(parentColumn = "routineId", entityColumn = "id")
    val routines: List<WorkoutRoutineEntity>,
    @Relation(parentColumn = "id", entityColumn = "dayId")
    val workoutExercises: List<WorkoutExerciseEntity>,
)
