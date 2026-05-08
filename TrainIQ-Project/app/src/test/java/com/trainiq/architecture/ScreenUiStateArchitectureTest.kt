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
            .substringBefore("    fun observeExerciseHistory")
        val publicStateFlows = Regex("""(?m)^\s*val\s+\w+:\s*StateFlow<""")
            .findAll(viewModelBody)
            .map { it.value.trim() }
            .toList()

        assertEquals(listOf("val uiState: StateFlow<"), publicStateFlows.map { it.substringBefore("<") + "<" })
        assertTrue(viewModelBody.contains("ScreenUiState<WorkoutUiContent>"))
        assertFalse(viewModelBody.contains("\n    val activeWorkoutUiState: StateFlow<"))
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
