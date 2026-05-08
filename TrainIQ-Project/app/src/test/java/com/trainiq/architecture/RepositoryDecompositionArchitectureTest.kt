package com.trainiq.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryDecompositionArchitectureTest {
    private val root = File("src/main/java/com/trainiq")

    @Test
    fun repositoryModuleBindsFocusedRepositoryImplementations() {
        val appModule = File(root, "core/di/AppModule.kt").readText()

        listOf(
            "RoomHomeRepository",
            "RoomWorkoutRepository",
            "RoomNutritionRepository",
            "RoomProgressRepository",
            "RoomCoachRepository",
        ).forEach { implementation ->
            assertTrue("RepositoryModule must bind $implementation", appModule.contains(implementation))
        }
        assertFalse(
            "RepositoryModule must not bind every domain repository interface to TrainIqRepository",
            appModule.contains("bindHomeRepository(repository: TrainIqRepository)") ||
                appModule.contains("bindWorkoutRepository(repository: TrainIqRepository)") ||
                appModule.contains("bindNutritionRepository(repository: TrainIqRepository)") ||
                appModule.contains("bindProgressRepository(repository: TrainIqRepository)") ||
                appModule.contains("bindCoachRepository(repository: TrainIqRepository)"),
        )
    }

    @Test
    fun legacyCoordinatorDoesNotImplementDomainRepositoryInterfaces() {
        val source = File(root, "data/repository/TrainIqRepository.kt").readText()
        val classHeader = source.substringAfter("class TrainIqDataCoordinator @Inject constructor")
            .substringBefore("{")

        assertFalse(classHeader.contains("HomeRepository"))
        assertFalse(classHeader.contains("WorkoutRepository"))
        assertFalse(classHeader.contains("NutritionRepository"))
        assertFalse(classHeader.contains("ProgressRepository"))
        assertFalse(classHeader.contains("CoachRepository"))
    }

    @Test
    fun coordinatorDelegatesProgressionAndActiveWorkoutMutationsToFocusedUnits() {
        val coordinator = File(root, "data/repository/TrainIqRepository.kt").readText()
        val progression = File(root, "data/repository/WorkoutProgressionSuggestionCalculator.kt").readText()
        val activeMutations = File(root, "data/repository/ActiveWorkoutSessionMutations.kt").readText()

        assertTrue(coordinator.contains("progressionSuggestionCalculator.calculate("))
        assertTrue(coordinator.contains("ActiveWorkoutSessionMutations.startOrResume("))
        assertTrue(coordinator.contains("ActiveWorkoutSessionMutations.logSet("))
        assertTrue(progression.contains("class WorkoutProgressionSuggestionCalculator"))
        assertTrue(activeMutations.contains("internal object ActiveWorkoutSessionMutations"))
        assertFalse(coordinator.contains("plateauDetected && effectiveRir"))
        assertFalse(coordinator.contains("Rond je actieve training af of verwijder die voordat je een andere training start."))
    }
}
