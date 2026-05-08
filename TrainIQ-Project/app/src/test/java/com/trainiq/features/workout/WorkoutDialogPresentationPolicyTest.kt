package com.trainiq.features.workout

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutDialogPresentationPolicyTest {
    @Test
    fun generatedRoutinePreviewUsesAdaptiveSheetInsteadOfDenseAlertDialog() {
        val source = File("src/main/java/com/trainiq/features/workout/RoutineDialogs.kt").readText()
        val body = source.substringAfter("fun GeneratedRoutinePreviewDialog").substringBefore("private fun GeneratedRoutineSource.label")

        assertTrue(body.contains("ModalBottomSheet"))
        assertFalse(body.contains("AlertDialog"))
        assertTrue(body.contains("verticalScroll"))
    }
}
