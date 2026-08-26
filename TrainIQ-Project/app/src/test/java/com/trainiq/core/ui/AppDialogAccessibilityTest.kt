package com.trainiq.core.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDialogAccessibilityTest {
    @Test
    fun `app dialog exposes a stable accessibility pane title`() {
        assertEquals("Routine verwijderen?", appDialogPaneTitle("  Routine verwijderen?  "))
        assertEquals("TrainIQ dialoog", appDialogPaneTitle(" "))
    }

    @Test
    fun `shared app dialog keeps modal pane semantics on alert dialog`() {
        val source = testSourceFile("core/ui/AppDesign.kt").readText()
        val appDialog = source.substringAfter("fun AppDialog(").substringBefore("@OptIn(ExperimentalMaterial3Api::class)")

        assertTrue(appDialog.contains("AlertDialog("))
        assertTrue(appDialog.contains("modifier = Modifier.semantics"))
        assertTrue(appDialog.contains("paneTitle = appDialogPaneTitle(title)"))
        assertTrue(appDialog.contains("confirmButton = { PrimaryActionButton(onClick = onConfirm)"))
        assertTrue(appDialog.contains("dismissButton = { TextButton(onClick = onDismiss)"))
    }
}

private fun testSourceFile(relativePackagePath: String): File {
    val userDir = File(System.getProperty("user.dir"))
    return listOf(
        File(userDir, "src/main/java/com/trainiq/$relativePackagePath"),
        File(userDir, "app/src/main/java/com/trainiq/$relativePackagePath"),
    ).first(File::isFile)
}
