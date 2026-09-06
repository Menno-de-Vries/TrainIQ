package com.trainiq.architecture

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenUiStateArchitectureTest {
    private val root = File(System.getProperty("user.dir"), "src/main/java/com/trainiq")

    @Test
    fun workoutViewModelExposesOnlyOnePublicStateFlowNamedUiState() {
        val source = File(root, "features/workout/WorkoutScreen.kt").readText()
        val viewModelBody = source.substringAfter("class WorkoutViewModel @Inject constructor(")
            .substringBefore("fun WorkoutRoute(")
        val publicStateFlows = Regex("""(?m)^\s*val\s+\w+:\s*StateFlow<""")
            .findAll(viewModelBody)
            .map { it.value.trim() }
            .toList()

        assertEquals(listOf("val uiState: StateFlow<"), publicStateFlows.map { it.substringBefore("<") + "<" })
        assertTrue(viewModelBody.contains("ScreenUiState<WorkoutUiContent>"))
        assertFalse(viewModelBody.contains("\n    val activeWorkoutUiState: StateFlow<"))
    }

    @Test
    fun activeWorkoutUiStateKeepsPerSecondClockTicksOutOfBroadScreenState() {
        val source = File(root, "features/workout/WorkoutScreen.kt").readText()
        val stateBody = source.substringAfter("private val activeWorkoutUiState: StateFlow<ActiveWorkoutUiState>")
            .substringBefore("    val uiState: StateFlow<ScreenUiState<WorkoutUiContent>>")

        assertFalse(stateBody.contains(".combine(_elapsedSeconds)"))
        assertFalse(stateBody.contains(".combine(_restTimerSeconds)"))
        assertFalse(stateBody.contains(".combine(_restTimerTotalSeconds)"))
        assertTrue(stateBody.contains(".combine(_exerciseRestOverrides)"))
        assertTrue(stateBody.contains("state.copy(exerciseRestOverrides = exerciseRestOverrides)"))
    }

    @Test
    fun cameraViewModelExposesOnlyOnePublicStateFlowNamedUiState() {
        val source = File(root, "features/nutrition/CameraScannerScreen.kt").readText()
        val viewModelBody = source.substringAfter("class CameraScannerViewModel @Inject constructor(")
            .substringBefore("    fun setContextHint")
        val publicStateFlows = Regex("""(?m)^\s*val\s+\w+:\s*StateFlow<""")
            .findAll(viewModelBody)
            .map { it.value.trim() }
            .toList()

        assertEquals(listOf("val uiState: StateFlow<"), publicStateFlows.map { it.substringBefore("<") + "<" })
        assertTrue(viewModelBody.contains("ScreenUiState<CameraUiContent>"))
    }
}
