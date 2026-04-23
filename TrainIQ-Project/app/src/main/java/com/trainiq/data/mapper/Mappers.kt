package com.trainiq.data.mapper

import com.trainiq.core.database.BodyMeasurementEntity
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.UserProfileEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.data.datasource.CachedHeartRateRecord
import com.trainiq.data.datasource.CachedCaloriesBurnedRecord
import com.trainiq.data.datasource.CachedSleepSessionRecord
import com.trainiq.data.datasource.CachedStepRecord
import com.trainiq.data.datasource.CachedWeightRecord
import com.trainiq.data.datasource.HealthConnectCacheState
import com.trainiq.domain.model.BodyMeasurement
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.HealthConnectMetrics
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutExercisePlan
import com.trainiq.domain.model.WorkoutRoutine
import com.trainiq.domain.model.WorkoutSessionSummary
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import java.time.Duration
import kotlin.math.roundToInt

fun UserProfileEntity.toDomain() = UserProfile(
    id = id,
    name = name,
    age = age,
    sex = runCatching { BiologicalSex.valueOf(sex) }.getOrDefault(BiologicalSex.MALE),
    height = height,
    weight = weight,
    bodyFat = bodyFat,
    activityLevel = activityLevel,
    goal = goal,
    calorieTarget = calorieTarget,
    proteinTarget = proteinTarget,
    carbsTarget = carbsTarget,
    fatTarget = fatTarget,
    trainingFocus = trainingFocus,
)

fun ExerciseEntity.toDomain() = Exercise(id, name, muscleGroup, equipment)

fun BodyMeasurementEntity.toDomain() = BodyMeasurement(id, date, weight, bodyFat, muscleMass)

fun WorkoutSessionEntity.toDomain(totalVolume: Double) = WorkoutSessionSummary(id, date, duration, caloriesBurned, totalVolume)

fun WorkoutDayEntity.toDomain(exercises: List<WorkoutExercisePlan>) = WorkoutDay(id, routineId, name, orderIndex, exercises)

fun WorkoutRoutineEntity.toDomain(days: List<WorkoutDay>) = WorkoutRoutine(id, name, description, active, days)

fun WorkoutExerciseEntity.toDomain(exercise: Exercise) =
    WorkoutExercisePlan(
        id = id,
        exercise = exercise,
        targetSets = targetSets,
        repRange = repRange,
        restSeconds = restSeconds,
        setType = parseSetType(setType),
        supersetGroupId = supersetGroupId,
    )

fun parseSetType(value: String?): SetType =
    runCatching { SetType.valueOf(value ?: SetType.WORKING.name) }.getOrDefault(SetType.WORKING)

internal fun StepsRecord.toCachedStepRecord() = CachedStepRecord(
    recordId = metadata.id,
    startTimeMillis = startTime.toEpochMilli(),
    endTimeMillis = endTime.toEpochMilli(),
    count = count.toInt(),
)

internal fun HeartRateRecord.toCachedHeartRateRecord(): CachedHeartRateRecord {
    val latestSample = samples.maxByOrNull { it.time }
    val averageBpm = samples.takeIf { it.isNotEmpty() }
        ?.map { it.beatsPerMinute.toDouble() }
        ?.average()
        ?.roundToInt()
    return CachedHeartRateRecord(
        recordId = metadata.id,
        startTimeMillis = startTime.toEpochMilli(),
        endTimeMillis = endTime.toEpochMilli(),
        averageBeatsPerMinute = averageBpm,
        latestBeatsPerMinute = latestSample?.beatsPerMinute?.toInt(),
        latestSampleTimeMillis = latestSample?.time?.toEpochMilli(),
        sampleCount = samples.size,
    )
}

internal fun SleepSessionRecord.toCachedSleepSessionRecord() = CachedSleepSessionRecord(
    recordId = metadata.id,
    startTimeMillis = startTime.toEpochMilli(),
    endTimeMillis = endTime.toEpochMilli(),
    durationMinutes = Duration.between(startTime, endTime).toMinutes(),
)

internal fun TotalCaloriesBurnedRecord.toCachedCaloriesBurnedRecord() = CachedCaloriesBurnedRecord(
    recordId = metadata.id,
    startTimeMillis = startTime.toEpochMilli(),
    endTimeMillis = endTime.toEpochMilli(),
    kcal = energy.inKilocalories,
)

internal fun WeightRecord.toCachedWeightRecord() = CachedWeightRecord(
    recordId = metadata.id,
    timeMillis = time.toEpochMilli(),
    weightKg = weight.inKilograms,
)

internal fun HealthConnectCacheState.toDomainMetrics(): HealthConnectMetrics = HealthConnectMetrics(
    // Prefer the aggregate API result (deduplication-aware). Fall back to the manual sum only
    // for old DataStore caches that predate the aggregate field (aggregatedStepsToday == 0).
    stepsToday = if (aggregatedStepsToday > 0) aggregatedStepsToday.toInt()
                 else stepRecords.sumOf(CachedStepRecord::count),
    averageHeartRateBpm = heartRateRecords
        .takeIf { it.isNotEmpty() }
        ?.let { records ->
            val weightedSamples = records.sumOf { it.sampleCount.coerceAtLeast(1) }
            if (weightedSamples == 0) {
                null
            } else {
                records.sumOf { (it.averageBeatsPerMinute ?: 0) * it.sampleCount.coerceAtLeast(1) }
                    .toDouble()
                    .div(weightedSamples)
                    .roundToInt()
            }
        },
    latestHeartRateBpm = heartRateRecords
        .maxByOrNull { it.latestSampleTimeMillis ?: Long.MIN_VALUE }
        ?.latestBeatsPerMinute,
    sleepMinutes = sleepSessionRecords.sumOf(CachedSleepSessionRecord::durationMinutes),
    sleepSessionCount = sleepSessionRecords.size,
    caloriesBurnedToday = caloriesBurnedRecords.takeIf { it.isNotEmpty() }?.sumOf(CachedCaloriesBurnedRecord::kcal),
    latestWeightKg = weightRecords.maxByOrNull(CachedWeightRecord::timeMillis)?.weightKg,
)
