package com.trainiq.domain.usecase

import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.HealthConnectMetrics
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HomeDashboard
import com.trainiq.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BuildHomeDashboardUseCaseTest {
    @Test
    fun mergeHealthStatusRebuildsStepsAndEnergyBalance() {
        val useCase = BuildHomeDashboardUseCase()
        val profile = UserProfile(
            id = 1L,
            name = "Menno",
            age = 34,
            sex = BiologicalSex.MALE,
            height = 180.0,
            weight = 82.0,
            bodyFat = 18.0,
            activityLevel = "Gemiddeld actief",
            goal = "Spieropbouw",
            calorieTarget = 2600,
            proteinTarget = 170,
            carbsTarget = 280,
            fatTarget = 80,
            trainingFocus = "Strength",
        )
        val dashboard = HomeDashboard(
            profile = profile,
            energyBalance = null,
            calorieTarget = 2600,
            calorieProgress = 1_900,
            proteinProgress = 125,
            proteinTarget = 170,
            carbsProgress = 200,
            carbsTarget = 280,
            fatProgress = 60,
            fatTarget = 80,
            todaysWorkoutCalories = 320,
            steps = null,
            nextWorkout = null,
            streak = 3,
            aiInsight = "Train vandaag rustig door.",
        )
        val status = HealthConnectStatus(
            state = HealthConnectState.CONNECTED,
            message = "Verbonden",
            metrics = HealthConnectMetrics(stepsToday = 8_500),
        )

        val merged = useCase.mergeHealthStatus(dashboard, status)

        assertEquals(8_500, merged.steps)
        assertNotNull(merged.energyBalance)
        assertEquals(1_900, merged.energyBalance?.caloriesIn?.toInt())
        assertEquals(320, merged.energyBalance?.workoutCalories)
    }
}
