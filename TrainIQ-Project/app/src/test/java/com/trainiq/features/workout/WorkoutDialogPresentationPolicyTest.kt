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

    @Test
    fun generatedRoutinePreviewUsesReadOnlyDutchMetadataInsteadOfNoOpChips() {
        val source = File("src/main/java/com/trainiq/features/workout/RoutineDialogs.kt").readText()
        val body = source.substringAfter("fun GeneratedRoutinePreviewDialog").substringBeforeLast("}")

        assertFalse(body.contains("AssistChip"))
        assertFalse(body.contains("Days per week"))
        assertFalse(body.contains("Available equipment"))
        assertFalse(body.contains("Experience level"))
        assertTrue(body.contains("GeneratedRoutineInfoPill"))
        assertTrue(body.contains("dagen"))
        assertTrue(body.contains("min/sessie"))
        assertTrue(body.contains("oefeningen"))
        assertTrue(body.contains("contentDescription = \"Bron:"))
    }
}
