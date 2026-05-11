package com.trainiq.data.repository

import com.trainiq.core.database.ExerciseEntity
import com.trainiq.data.local.TrainIqStorageState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseLibrarySeeder @Inject constructor(
    private val runtimeStore: RoomTrainIqRuntimeStore,
) {
    suspend fun ensureSeeded() {
        val additions = missingCanonicalExercises(runtimeStore.state.value)
        if (additions.isEmpty()) return
        runtimeStore.seedExerciseLibrary(additions)
    }
}

internal fun shouldSkipExerciseLibrarySeed(state: TrainIqStorageState): Boolean =
    state.exercises.size >= 50

internal fun mergeCanonicalExerciseLibrary(state: TrainIqStorageState): TrainIqStorageState {
    val additions = missingCanonicalExercises(state)
    return if (additions.isEmpty()) state else state.copy(exercises = state.exercises + additions)
}

internal fun missingCanonicalExercises(state: TrainIqStorageState): List<ExerciseEntity> {
    if (shouldSkipExerciseLibrarySeed(state)) return emptyList()
    val existingNames = state.exercises.map { it.name.lowercase() }.toSet()
    val nextId = (state.exercises.maxOfOrNull { it.id } ?: 0L) + 1L
    return canonicalExerciseLibrary()
        .filter { it.name.lowercase() !in existingNames }
        .mapIndexed { index, exercise -> exercise.copy(id = nextId + index) }
}

private fun canonicalExerciseLibrary(): List<ExerciseEntity> = listOf(
    ExerciseEntity(1, "Bench Press", "Chest", "Barbell"),
    ExerciseEntity(2, "Incline Bench Press", "Chest", "Barbell"),
    ExerciseEntity(3, "Dumbbell Fly", "Chest", "Dumbbell"),
    ExerciseEntity(4, "Cable Fly", "Chest", "Cable"),
    ExerciseEntity(5, "Dips", "Chest", "Bodyweight"),
    ExerciseEntity(6, "Deadlift", "Back", "Barbell"),
    ExerciseEntity(7, "Barbell Row", "Back", "Barbell"),
    ExerciseEntity(8, "Dumbbell Row", "Back", "Dumbbell"),
    ExerciseEntity(9, "Lat Pulldown", "Back", "Cable"),
    ExerciseEntity(10, "Cable Row", "Back", "Cable"),
    ExerciseEntity(11, "Pull-up", "Back", "Bodyweight"),
    ExerciseEntity(12, "Face Pull", "Back", "Cable"),
    ExerciseEntity(13, "T-Bar Row", "Back", "Barbell"),
    ExerciseEntity(14, "Overhead Press", "Shoulders", "Barbell"),
    ExerciseEntity(15, "Dumbbell Press", "Shoulders", "Dumbbell"),
    ExerciseEntity(16, "Lateral Raise", "Shoulders", "Dumbbell"),
    ExerciseEntity(17, "Rear Delt Fly", "Shoulders", "Dumbbell"),
    ExerciseEntity(18, "Arnold Press", "Shoulders", "Dumbbell"),
    ExerciseEntity(19, "Squat", "Legs", "Barbell"),
    ExerciseEntity(20, "Romanian Deadlift", "Hamstrings", "Barbell"),
    ExerciseEntity(21, "Leg Press", "Legs", "Machine"),
    ExerciseEntity(22, "Leg Curl", "Hamstrings", "Machine"),
    ExerciseEntity(23, "Leg Extension", "Legs", "Machine"),
    ExerciseEntity(24, "Hip Thrust", "Glutes", "Barbell"),
    ExerciseEntity(25, "Split Squat", "Legs", "Dumbbell"),
    ExerciseEntity(26, "Walking Lunge", "Legs", "Dumbbell"),
    ExerciseEntity(27, "Calf Raise", "Calves", "Machine"),
    ExerciseEntity(28, "Barbell Curl", "Biceps", "Barbell"),
    ExerciseEntity(29, "Dumbbell Curl", "Biceps", "Dumbbell"),
    ExerciseEntity(30, "Hammer Curl", "Biceps", "Dumbbell"),
    ExerciseEntity(31, "Preacher Curl", "Biceps", "Barbell"),
    ExerciseEntity(32, "Cable Curl", "Biceps", "Cable"),
    ExerciseEntity(33, "Tricep Pushdown", "Triceps", "Cable"),
    ExerciseEntity(34, "Skull Crusher", "Triceps", "Barbell"),
    ExerciseEntity(35, "Close-Grip Bench", "Triceps", "Barbell"),
    ExerciseEntity(36, "Overhead Tricep Ext", "Triceps", "Dumbbell"),
    ExerciseEntity(37, "Tricep Kickback", "Triceps", "Dumbbell"),
    ExerciseEntity(38, "Plank", "Core", "Bodyweight"),
    ExerciseEntity(39, "Hanging Leg Raise", "Core", "Bodyweight"),
    ExerciseEntity(40, "Ab Wheel Rollout", "Core", "Bodyweight"),
    ExerciseEntity(41, "Cable Crunch", "Core", "Cable"),
    ExerciseEntity(42, "Russian Twist", "Core", "Bodyweight"),
    ExerciseEntity(43, "Chest Press", "Chest", "Machine"),
    ExerciseEntity(44, "Seated Row", "Back", "Machine"),
    ExerciseEntity(45, "Hack Squat", "Legs", "Machine"),
    ExerciseEntity(46, "Bulgarian Split Squat", "Legs", "Dumbbell"),
    ExerciseEntity(47, "Seated Calf Raise", "Calves", "Machine"),
    ExerciseEntity(48, "EZ-Bar Curl", "Biceps", "Barbell"),
    ExerciseEntity(49, "Rope Overhead Extension", "Triceps", "Cable"),
    ExerciseEntity(50, "Pallof Press", "Core", "Cable"),
    ExerciseEntity(51, "Back Extension", "Back", "Machine"),
    ExerciseEntity(52, "Good Morning", "Hamstrings", "Barbell"),
)
