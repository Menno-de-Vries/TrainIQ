package com.trainiq.data.local

import android.content.Context
import com.google.gson.Gson
import com.trainiq.core.database.BodyMeasurementEntity
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.MealEntity
import com.trainiq.core.database.UserProfileEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class TrainIqLocalStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val gson = Gson()
    private val mutex = Mutex()
    private val storageFile = context.filesDir.resolve("trainiq-state.json")
    private val _state = MutableStateFlow(loadState())

    val state: StateFlow<TrainIqStorageState> = _state.asStateFlow()

    suspend fun update(transform: (TrainIqStorageState) -> TrainIqStorageState) {
        mutex.withLock {
            val updated = transform(_state.value)
            val tempFile = storageFile.resolveSibling("${storageFile.name}.tmp")
            tempFile.writeText(gson.toJson(updated))
            if (storageFile.exists()) {
                storageFile.delete()
            }
            tempFile.renameTo(storageFile)
            _state.value = updated
        }
    }

    suspend fun clearAll() {
        mutex.withLock {
            if (storageFile.exists()) {
                storageFile.delete()
            }
            _state.value = TrainIqStorageState()
        }
    }

    suspend fun clearProfile() {
        update { it.copy(profile = null) }
    }

    private fun loadState(): TrainIqStorageState {
        if (!storageFile.exists()) return TrainIqStorageState()
        return runCatching {
            gson.fromJson(storageFile.readText(), TrainIqStorageState::class.java) ?: TrainIqStorageState()
        }.getOrElse { TrainIqStorageState() }
    }
}

data class TrainIqStorageState(
    val profile: UserProfileEntity? = null,
    val routines: List<WorkoutRoutineEntity> = emptyList(),
    val days: List<WorkoutDayEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val workoutExercises: List<WorkoutExerciseEntity> = emptyList(),
    val meals: List<MealEntity> = emptyList(),
    val measurements: List<BodyMeasurementEntity> = emptyList(),
    val sessions: List<WorkoutSessionEntity> = emptyList(),
    val workoutSets: List<WorkoutSetEntity> = emptyList(),
)
