package com.trainiq.navigation

import com.trainiq.features.settings.settingsOverflowSectionBody
import com.trainiq.features.settings.settingsOpenProgressActionLabel
import com.trainiq.features.settings.settingsOverflowSectionTitle
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveNavigationPolicyTest {
    @Test
    fun compactWidthKeepsBottomNavigation() {
        assertFalse(shouldUseNavigationRail(TrainIqWindowWidthClass.Compact))
    }

    @Test
    fun compactBottomNavigationUsesFivePrimaryDestinationsAndKeepsProgressInRail() {
        val visibleRoutes = compactBottomNavigationRouteClasses()

        assertEquals(listOf(Home::class, Train::class, Nutrition::class, Coach::class, Settings::class), visibleRoutes)
        assertFalse(Progress::class in visibleRoutes)
        assertEquals(
            listOf(Home::class, Train::class, Nutrition::class, Progress::class, Coach::class, Settings::class),
            navigationRailRouteClasses(),
        )
        assertEquals("Meer", bottomNavigationLabel("Instellingen"))
        assertEquals(Settings::class, compactSelectedNavigationRouteClass(Progress::class))
        assertEquals(Home::class, compactSelectedNavigationRouteClass(Home::class))
    }

    @Test
    fun topLevelNavigationDoesNotAttachScreenWideSwipeGesture() {
        val source = File("src/main/java/com/trainiq/navigation/TrainIqNav.kt").readText()

        assertFalse(source.contains(".topLevelTabSwipeNavigation("))
        assertFalse(source.contains("private fun Modifier.topLevelTabSwipeNavigation("))
    }

    @Test
    fun compactOverflowSettingsMakesProgressDirectlyDiscoverable() {
        assertEquals("Meer", settingsOverflowSectionTitle())
        assertTrue(settingsOverflowSectionBody().contains("Voortgang"))
        assertEquals("Voortgang openen", settingsOpenProgressActionLabel())
    }

    @Test
    fun mediumAndExpandedWidthsUseNavigationRail() {
        assertTrue(shouldUseNavigationRail(TrainIqWindowWidthClass.Medium))
        assertTrue(shouldUseNavigationRail(TrainIqWindowWidthClass.Expanded))
    }

    @Test
    fun compactShortScreensUseCondensedBottomNavigation() {
        assertTrue(shouldUseCompactShortBottomBar(TrainIqWindowWidthClass.Compact, screenHeightDp = 640))
        assertFalse(shouldUseCompactShortBottomBar(TrainIqWindowWidthClass.Compact, screenHeightDp = 641))
        assertFalse(shouldUseCompactShortBottomBar(TrainIqWindowWidthClass.Medium, screenHeightDp = 640))
    }

    @Test
    fun dashboardGridAddsColumnsOnWiderScreens() {
        assertTrue(adaptiveDashboardGridColumns(TrainIqWindowWidthClass.Compact) == 2)
        assertTrue(adaptiveDashboardGridColumns(TrainIqWindowWidthClass.Medium) == 3)
        assertTrue(adaptiveDashboardGridColumns(TrainIqWindowWidthClass.Expanded) == 4)
    }

    @Test
    fun contentWidthIsConstrainedOnExpandedScreens() {
        assertTrue(adaptiveContentMaxWidthDp(TrainIqWindowWidthClass.Compact) == Int.MAX_VALUE)
        assertTrue(adaptiveContentMaxWidthDp(TrainIqWindowWidthClass.Medium) == 840)
        assertTrue(adaptiveContentMaxWidthDp(TrainIqWindowWidthClass.Expanded) == 1120)
    }
}
