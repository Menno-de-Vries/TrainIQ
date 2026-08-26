package com.trainiq.features.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactSectionTabsSourceTest {
    @Test
    fun sectionTabsUseSharedCompactComponent() {
        val workout = File("src/main/java/com/trainiq/features/workout/WorkoutScreen.kt").readText()
        val progress = File("src/main/java/com/trainiq/features/progress/ProgressScreen.kt").readText()
        val coach = File("src/main/java/com/trainiq/features/coach/CoachScreen.kt").readText()

        assertTrue(workout.contains("CompactSectionTabs("))
        assertTrue(progress.contains("CompactSectionTabs("))
        assertTrue(coach.contains("CompactSectionTabs("))
    }

    @Test
    fun compactSectionTabsProtectLongLabels() {
        val source = File("src/main/java/com/trainiq/core/ui/AppDesign.kt").readText()

        assertTrue(source.contains("maxLines = 1"))
        assertTrue(source.contains("TextOverflow.Ellipsis"))
        assertTrue(source.contains("TextAlign.Center"))
        assertTrue(source.contains("Alignment.CenterHorizontally"))
    }
}
