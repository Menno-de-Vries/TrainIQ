package com.trainiq.features.home

import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.HealthConnectMetrics
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HealthConnectStepDiagnostic
import com.trainiq.domain.model.HealthConnectStepDataFreshness
import com.trainiq.domain.model.RoutineSet
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutExercisePlan
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun homeViewModelTriggersInitialHealthRefreshImmediatelyWithoutStartupDelay() {
        val source = File("src/main/java/com/trainiq/features/home/HomeScreen.kt").readText()
        val viewModelBody = source.substringAfter("class HomeViewModel @Inject constructor(")
            .substringBefore("internal data class HomeHealthRefreshUiState")

        assertTrue(viewModelBody.contains("refreshHealthConnectStatus()"))
        assertFalse(
            "Home must not wait 8 seconds before the first Health Connect aggregate refresh.",
            viewModelBody.contains("delay(8_000L)") || viewModelBody.contains("delay(8000L)"),
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
    fun homeMomentumCopy_formatsStreakAndConnectedSteps() {
        val status = HealthConnectStatus(
            state = HealthConnectState.CONNECTED,
            message = "Verbonden",
            metrics = HealthConnectMetrics(stepsToday = 7200),
            stepDataFreshness = HealthConnectStepDataFreshness.FRESH,
        )

        assertEquals("3 dagen", homeStreakValue(3))
        assertEquals("Ritme staat aan", homeStreakSubtitle(3))
        assertEquals("7200", homeStepsValue(status))
        assertTrue(homeMomentumEncouragement(3, status).contains("beweging"))
    }

    @Test
    fun homeMomentumCopy_formatsEmptyAndOfflineStates() {
        val status = HealthConnectStatus(
            state = HealthConnectState.PERMISSION_REQUIRED,
            message = "Toegang nodig",
        )

        assertEquals("0 dagen", homeStreakValue(0))
        assertEquals("Start vandaag", homeStreakSubtitle(0))
        assertEquals("Offline", homeStepsValue(status))
        assertTrue(homeMomentumEncouragement(0, status).contains("Begin lokaal"))
    }

    @Test
    fun homeMomentumCopy_whenNoDataAndNoStreak_usesStrongerStartEncouragement() {
        val status = HealthConnectStatus(
            state = HealthConnectState.NO_DATA,
            message = "Nog geen data",
        )

        val encouragement = homeMomentumEncouragement(0, status)

        assertTrue(encouragement.contains("Start klein"))
        assertTrue(encouragement.contains("coach direct scherper"))
    }

    @Test
    fun homeStepsValueShowsSuccessfulZeroInsteadOfMissingData() {
        val status = HealthConnectStatus(
            state = HealthConnectState.NO_DATA,
            message = "Verbonden zonder positieve waarden",
            metrics = HealthConnectMetrics(stepsToday = 0),
            stepDataFreshness = HealthConnectStepDataFreshness.FRESH,
        )

        assertEquals("0", homeStepsValue(status))
    }

    @Test
    fun homeStepsValueDoesNotPresentMissingOrFailedPermissionAsMeasuredZero() {
        val base = HealthConnectStatus(
            state = HealthConnectState.NO_DATA,
            message = "Geen bewezen stappenmeting",
            metrics = HealthConnectMetrics(stepsToday = 0),
        )

        listOf(HealthConnectState.NO_DATA, HealthConnectState.CONNECTED).forEach { state ->
            assertEquals(
                "Geen data",
                homeStepsValue(
                    base.copy(
                        state = state,
                        stepDataFreshness = HealthConnectStepDataFreshness.PERMISSION_MISSING,
                    ),
                ),
            )
            assertEquals(
                "Geen data",
                homeStepsValue(
                    base.copy(
                        state = state,
                        stepDataFreshness = HealthConnectStepDataFreshness.ERROR,
                    ),
                ),
            )
        }
    }

    @Test
    fun compactHomeHealthCopyShowsOnlyValueSourceAndUpdateTime() {
        val status = HealthConnectStatus(
            state = HealthConnectState.CONNECTED,
            message = "Verbonden",
            metrics = HealthConnectMetrics(stepsToday = 84),
            lastSyncedAt = 1_800_000L,
            stepDataFreshness = HealthConnectStepDataFreshness.FRESH,
            stepDataUpdatedAt = 1_800_000L,
            stepDiagnostic = HealthConnectStepDiagnostic(
                aggregateStepsToday = 8,
                samsungHealthStepsToday = 84,
                samsungHealthAggregateStepsToday = 8,
                samsungRawStepRecordSumToday = 84,
                queriedAt = 1_800_000L,
                sourceLabels = listOf("Jouw telefoon", "Samsung Health"),
                dayStartLabel = "00:00",
                dayEndLabel = "02:33",
            ),
        )

        val primary = homeHealthCompactSummary(status)

        assertEquals("84 stappen · Samsung Health", primary)
        assertEquals("Bijgewerkt om ${formatHomeLastSync(1_800_000L)}", homeHealthSyncSummary(status))
        assertFalse(primary.contains("raw", ignoreCase = true))
        assertFalse(primary.contains("aggregate", ignoreCase = true))
        assertFalse(primary.contains("Pariteit", ignoreCase = true))
    }

    @Test
    fun compactHomeHealthCopyNamesFreshStaleAndPermissionStates() {
        val fresh = HealthConnectStatus(
            state = HealthConnectState.CONNECTED,
            message = "Verbonden",
            metrics = HealthConnectMetrics(stepsToday = 84),
            lastSyncedAt = 1_800_000L,
            stepDataFreshness = HealthConnectStepDataFreshness.FRESH,
            stepDataUpdatedAt = 1_800_000L,
        )
        val stale = fresh.copy(stepDataFreshness = HealthConnectStepDataFreshness.STALE_CACHE)
        val missing = fresh.copy(stepDataFreshness = HealthConnectStepDataFreshness.PERMISSION_MISSING)

        assertEquals("84 stappen · Health Connect", homeHealthCompactSummary(fresh))
        assertEquals("Laatst bekend: 84 stappen · Health Connect", homeHealthCompactSummary(stale))
        assertEquals("Stappentoegang ontbreekt.", homeHealthCompactSummary(missing))
        assertEquals("Laatste update om ${formatHomeLastSync(1_800_000L)}", homeHealthSyncSummary(stale))
        assertNull(homeHealthSyncSummary(missing))
    }

    @Test
    fun homeUsesExactlyOneHealthConnectCardForSuccessOrActionState() {
        val connected = HealthConnectStatus(
            state = HealthConnectState.CONNECTED,
            message = "Verbonden",
            metrics = HealthConnectMetrics(stepsToday = 84),
            stepDataFreshness = HealthConnectStepDataFreshness.FRESH,
        )

        assertTrue(showCompactHomeHealthCard(connected))
        assertFalse(
            showCompactHomeHealthCard(
                connected.copy(stepDataFreshness = HealthConnectStepDataFreshness.PERMISSION_MISSING),
            ),
        )
        assertFalse(
            showCompactHomeHealthCard(
                connected.copy(state = HealthConnectState.PROVIDER_MISSING),
            ),
        )
    }

    @Test
    fun homeSourceUsesConditionalHealthConnectCards() {
        val source = File("src/main/java/com/trainiq/features/home/HomeScreen.kt").readText()
        val successBody = source.substringAfter("is HomeUiState.Success ->")
            .substringBefore("internal fun buildHomeRecoverySubtitle")

        assertTrue(successBody.contains("if (!showCompactHomeHealthCard(healthConnectStatus))"))
        assertTrue(successBody.contains("PermissionManagerCard("))
    }

    @Test
    fun homeShowsOneContextualFocusCardAfterConnectedHealthStatus() {
        val source = File("src/main/java/com/trainiq/features/home/HomeScreen.kt").readText()
        val successBody = source.substringAfter("is HomeUiState.Success ->")
            .substringBefore("internal fun buildHomeRecoverySubtitle")

        assertTrue(successBody.contains("if (!showCompactHomeHealthCard(healthConnectStatus))"))
        assertTrue(successBody.contains("if (dashboard.nextWorkout != null)"))
        assertTrue(successBody.contains("CoachInsightCard("))
    }

    @Test
    fun connectedHealthStatusRemainsRefreshableAsACompactHomeRow() {
        val source = File("src/main/java/com/trainiq/features/home/HomeScreen.kt").readText()
        val successBody = source.substringAfter("is HomeUiState.Success ->")
            .substringBefore("internal fun buildHomeRecoverySubtitle")

        assertTrue(successBody.contains("HomeHealthStatusRow("))
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
    fun energyBalanceHeroKeepsSourcesAccessibleBehindAnExpandableDetailsControl() {
        val source = File("src/main/java/com/trainiq/core/util/Formatters.kt").readText()

        assertTrue(source.contains("rememberSaveable"))
        assertTrue(source.contains("Bekijk verbranding"))
        assertTrue(source.contains("Verberg verbranding"))
        assertTrue(source.contains("AnimatedVisibility(visible = breakdownExpanded)"))
    }

    @Test
    fun homeScreenShowsCompactHealthConnectRefreshAndSyncCopy() {
        val source = File("src/main/java/com/trainiq/features/home/HomeScreen.kt").readText()

        assertTrue(source.contains("isRefreshingHealth"))
        assertTrue(source.contains("homeHealthRefreshMessage"))
        assertTrue(source.contains("homeHealthSyncSummary"))
        assertTrue(source.contains("Bijgewerkt om"))
        assertTrue(source.contains("Verversen"))
    }

    @Test
    fun repositoryDashboardRefreshUsesHealthConnectStatusAsSingleStepSource() {
        val source = File("src/main/java/com/trainiq/data/repository/TrainIqRepository.kt").readText()
        val refreshBody = source.substringAfter("suspend fun refreshDashboardData()")
            .substringBefore("fun observeWorkoutOverview()")

        assertTrue(refreshBody.contains("healthConnectDataSource.getStatus()"))
        assertTrue(refreshBody.contains("status.metrics?.stepsToday"))
        assertFalse(refreshBody.contains("getTodayStepsLive()"))
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
