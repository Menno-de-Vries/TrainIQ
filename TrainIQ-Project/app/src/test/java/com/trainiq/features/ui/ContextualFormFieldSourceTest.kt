package com.trainiq.features.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextualFormFieldSourceTest {
    @Test
    fun sharedContextualFormFieldDefinesScreenContexts() {
        val source = File("src/main/java/com/trainiq/core/ui/AppDesign.kt").readText()

        assertTrue(source.contains("fun TrainIqFormField("))
        assertTrue(source.contains("enum class TrainIqFormFieldContext"))
        assertTrue(source.contains("Goal"))
        assertTrue(source.contains("Progress"))
        assertTrue(source.contains("Nutrition"))
        assertTrue(source.contains("Settings"))
    }

    @Test
    fun targetedScreensUseContextualFieldsWithoutCopyingActiveWorkoutInput() {
        val root = File("src/main/java/com/trainiq/features")
        val coach = File(root, "coach/CoachScreen.kt").readText()
        val progress = File(root, "progress/ProgressScreen.kt").readText()
        val nutrition = File(root, "nutrition/NutritionScreen.kt").readText()
        val settings = File(root, "settings/SettingsSection.kt").readText()

        assertTrue(coach.contains("TrainIqFormFieldContext.Goal"))
        assertTrue(progress.contains("TrainIqFormFieldContext.Progress"))
        assertTrue(nutrition.contains("TrainIqFormFieldContext.Nutrition"))
        assertTrue(settings.contains("TrainIqFormFieldContext.Settings"))
        listOf(coach, progress, nutrition, settings).forEach { source ->
            assertFalse(source.contains("ActiveSetInputMetricValue("))
            assertFalse(source.contains("defaultMinSize(minWidth = 52.dp, minHeight = 52.dp)"))
        }
    }
}
