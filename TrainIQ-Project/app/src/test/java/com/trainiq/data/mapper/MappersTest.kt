package com.trainiq.data.mapper

import app.cash.turbine.test
import com.trainiq.core.database.BodyMeasurementEntity
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.UserProfileEntity
import com.trainiq.data.datasource.CachedHeartRateRecord
import com.trainiq.data.datasource.CachedSleepSessionRecord
import com.trainiq.data.datasource.CachedStepRecord
import com.trainiq.data.datasource.HealthConnectCacheState
import com.trainiq.domain.model.BiologicalSex
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {

    @Test
    fun userProfileEntity_toDomain_mapsAllFields() {
        val entity = UserProfileEntity(
            id = 1L,
            name = "Menno",
            age = 34,
            sex = "MALE",
            height = 183.0,
            weight = 84.5,
            bodyFat = 14.2,
            activityLevel = "Active",
            goal = "Lean bulk",
            calorieTarget = 2900,
            proteinTarget = 180,
            carbsTarget = 320,
            fatTarget = 75,
            trainingFocus = "Upper body",
        )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.age, domain.age)
        assertEquals(BiologicalSex.MALE, domain.sex)
        assertEquals(entity.height, domain.height, 0.0)
        assertEquals(entity.weight, domain.weight, 0.0)
        assertEquals(entity.bodyFat, domain.bodyFat, 0.0)
        assertEquals(entity.activityLevel, domain.activityLevel)
        assertEquals(entity.goal, domain.goal)
        assertEquals(entity.calorieTarget, domain.calorieTarget)
        assertEquals(entity.proteinTarget, domain.proteinTarget)
        assertEquals(entity.carbsTarget, domain.carbsTarget)
        assertEquals(entity.fatTarget, domain.fatTarget)
        assertEquals(entity.trainingFocus, domain.trainingFocus)
    }

    @Test
    fun exerciseEntity_toDomain_mapsCoreFields() {
        val entity = ExerciseEntity(7L, "Bench Press", "Chest", "Barbell")

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.muscleGroup, domain.muscleGroup)
        assertEquals(entity.equipment, domain.equipment)
    }

    @Test
    fun bodyMeasurementEntity_toDomain_mapsMeasurement() {
        val entity = BodyMeasurementEntity(
            id = 3L,
            date = 1700000000000L,
            weight = 82.0,
            bodyFat = 13.5,
            muscleMass = 36.1,
        )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.date, domain.date)
        assertEquals(entity.weight, domain.weight, 0.0)
        assertEquals(entity.bodyFat, domain.bodyFat, 0.0)
        assertEquals(entity.muscleMass, domain.muscleMass, 0.0)
    }

    @Test
    fun healthConnectCacheState_toDomainMetrics_emitsWeightedMetricsViaFlow() = runTest {
        val cacheState = HealthConnectCacheState(
            stepRecords = listOf(
                CachedStepRecord("steps-1", 0L, 10L, 4000),
                CachedStepRecord("steps-2", 11L, 20L, 2500),
            ),
            heartRateRecords = listOf(
                CachedHeartRateRecord("hr-1", 0L, 10L, 60, 62, 10L, 2),
                CachedHeartRateRecord("hr-2", 11L, 20L, 80, 84, 20L, 4),
            ),
            sleepSessionRecords = listOf(
                CachedSleepSessionRecord("sleep-1", 0L, 10L, 420),
                CachedSleepSessionRecord("sleep-2", 11L, 20L, 95),
            ),
        )

        flowOf(cacheState)
            .map { it.toDomainMetrics() }
            .test {
                val metrics = awaitItem()
                assertEquals(6500, metrics.stepsToday)
                assertEquals(73, metrics.averageHeartRateBpm)
                assertEquals(84, metrics.latestHeartRateBpm)
                assertEquals(515L, metrics.sleepMinutes)
                assertEquals(2, metrics.sleepSessionCount)
                awaitComplete()
            }
    }
}
