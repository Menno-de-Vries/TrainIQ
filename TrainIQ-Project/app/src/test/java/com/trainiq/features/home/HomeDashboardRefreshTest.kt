package com.trainiq.features.home

import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.RoutineSet
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutExercisePlan
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class HomeDashboardRefreshTest {
    @Test
    fun homeViewModelDoesNotOwnPeriodicRefreshLoop() {
        val source = File("src/main/java/com/trainiq/features/home/HomeScreen.kt").readText()
        val viewModelBody = source.substringAfter("class HomeViewModel @Inject constructor(")
            .substringBefore("internal class HomeRefreshGate")

        assertFalse(
            "Home periodic refresh must be lifecycle-scoped from HomeRoute, not retained forever in HomeViewModel.",
            viewModelBody.contains("while (true)") &&
                viewModelBody.contains("refreshDashboardDataUseCase()"),
        )
    }

    @Test
    fun homeRefreshGate_whenRefreshIsInFlight_rejectsDuplicateStart() {
        val gate = HomeRefreshGate()

        assertTrue(gate.tryStart())
        assertFalse(gate.tryStart())
    }

    @Test
    fun homeRefreshGate_afterRefreshFinishes_allowsNextStart() {
        val gate = HomeRefreshGate()

        assertTrue(gate.tryStart())
        gate.finish()

        assertTrue(gate.tryStart())
    }

    @Test
    fun refreshDashboardDataSafely_whenRefreshSucceeds_returnsTrue() = runTest {
        val result = refreshDashboardDataSafely { }

        assertTrue(result)
    }

    @Test
    fun refreshDashboardDataSafely_whenRefreshFails_returnsFalse() = runTest {
        val result = refreshDashboardDataSafely { error("Health Connect failed") }

        assertFalse(result)
    }

    @Test
    fun homeRefreshMessageNamesSuccessfulAndFailedHealthConnectRefreshes() {
        assertEquals("Health Connect bijgewerkt.", homeHealthRefreshMessage(success = true))
        assertEquals("Health Connect kon niet worden ververst. Laatste bekende data blijft zichtbaar.", homeHealthRefreshMessage(success = false))
    }

    @Test
    fun refreshDashboardDataSafely_whenCancelled_rethrowsCancellation() = runTest {
        try {
            refreshDashboardDataSafely { throw CancellationException("cancelled") }
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
        }
    }

    @Test
    fun buildHomeRecoverySubtitle_whenHeartRateMissing_omitsHeartRateSegment() {
        val result = buildHomeRecoverySubtitle(
            stepsToday = 4000,
            averageHeartRateBpm = null,
            todaysWorkoutCalories = 320,
        )

        assertTrue(result == "Training 320 kcal")
    }

    @Test
    fun buildHomeRecoverySubtitle_whenHeartRateAvailable_includesHeartRateSegment() {
        val result = buildHomeRecoverySubtitle(
            stepsToday = 2800,
            averageHeartRateBpm = 64,
            todaysWorkoutCalories = 180,
        )

        assertTrue(result == "Gem. hartslag 64 bpm - Training 180 kcal")
    }

    @Test
    fun buildHomeRecoverySubtitle_whenStepsUnavailable_namesOfflineState() {
        val result = buildHomeRecoverySubtitle(
            stepsToday = null,
            averageHeartRateBpm = null,
            todaysWorkoutCalories = 180,
        )

        assertTrue(result == "Stappen offline - Training 180 kcal")
    }

    @Test
    fun energyBalanceBreakdownCopyExplainsEachCaloriesOutSource() {
        val source = File("src/main/java/com/trainiq/core/util/Formatters.kt").readText()

        assertTrue(source.contains("Waar komt calorieën uit vandaan?"))
        assertTrue(source.contains("BMR"))
        assertTrue(source.contains("TEF"))
        assertTrue(source.contains("Stappen"))
        assertTrue(source.contains("Training"))
        assertTrue(source.contains("energyOutBreakdownRows"))
    }

    @Test
    fun homeScreenShowsHealthConnectRefreshStateAndLastSyncCopy() {
        val source = File("src/main/java/com/trainiq/features/home/HomeScreen.kt").readText()

        assertTrue(source.contains("isRefreshingHealth"))
        assertTrue(source.contains("homeHealthRefreshMessage"))
        assertTrue(source.contains("Laatst gesynchroniseerd"))
        assertTrue(source.contains("Verversen"))
    }

    @Test
    fun nextWorkoutIntensityLabel_whenPlannedRpeExists_usesRoutineData() {
        val result = nextWorkoutIntensityLabel(
            workoutDay(
                targetRpe = 7.0,
                setRpeValues = listOf(8.0, 9.0),
            ),
        )

        assertTrue(result == "Doel RPE 8.5")
    }

    @Test
    fun nextWorkoutIntensityLabel_whenNoPlannedRpe_omitsLabel() {
        val result = nextWorkoutIntensityLabel(workoutDay(targetRpe = 0.0))

        assertTrue(result == null)
    }

    private fun workoutDay(
        targetRpe: Double,
        setRpeValues: List<Double> = emptyList(),
    ): WorkoutDay = WorkoutDay(
        id = 1L,
        routineId = 1L,
        name = "Push",
        orderIndex = 0,
        exercises = listOf(
            WorkoutExercisePlan(
                id = 10L,
                exercise = Exercise(id = 20L, name = "Bench press", muscleGroup = "Chest", equipment = "Barbell"),
                targetSets = 3,
                repRange = "8-10",
                restSeconds = 90,
                targetRpe = targetRpe,
                sets = setRpeValues.mapIndexed { index, rpe ->
                    RoutineSet(
                        id = index.toLong() + 1L,
                        workoutExerciseId = 10L,
                        orderIndex = index,
                        targetRpe = rpe,
                    )
                },
            ),
        ),
    )
}
