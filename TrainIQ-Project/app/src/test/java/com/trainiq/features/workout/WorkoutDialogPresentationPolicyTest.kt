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
    fun generatedRoutinePreviewKeepsActionsOutsideWeightedScrollArea() {
        val source = File("src/main/java/com/trainiq/features/workout/RoutineDialogs.kt").readText()
        val body = source.substringAfter("fun GeneratedRoutinePreviewDialog").substringBefore("private fun GeneratedRoutineSource.label")
        val scrollArea = body.substringAfter(".weight(1f, fill = false)").substringBefore("Button(onClick = onSave")
        val actionArea = body.substringAfter("Button(onClick = onSave")

        assertTrue(body.contains(".fillMaxHeight(0.92f)"))
        assertTrue(scrollArea.contains(".verticalScroll(rememberScrollState())"))
        assertTrue(actionArea.contains("modifier = Modifier.fillMaxWidth()"))
        assertTrue(actionArea.contains("Text(if (isSaving) \"Opslaan...\" else \"Opslaan\")"))
        assertTrue(actionArea.contains("Text(\"Opnieuw proberen\")"))
        assertTrue(actionArea.contains("Text(\"Annuleren\")"))
    }

    @Test
    fun generatedRoutinePreviewUsesFullWidthWrappedActions() {
        val source = File("src/main/java/com/trainiq/features/workout/RoutineDialogs.kt").readText()
        val body = source.substringAfter("fun GeneratedRoutinePreviewDialog").substringBefore("private fun GeneratedRoutineSource.label")
        val actionArea = body.substringAfter("Button(onClick = onSave")

        assertFalse(actionArea.contains("Row("))
        assertFalse(actionArea.contains("Modifier.weight(1f)"))
        assertTrue(actionArea.contains("TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth())"))
        assertTrue(actionArea.contains("TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth())"))
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
