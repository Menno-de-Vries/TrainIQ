package com.trainiq.navigation

import com.trainiq.features.settings.settingsOverflowSectionBody
import com.trainiq.features.settings.settingsOverflowSectionTitle
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
        assertEquals(Coach::class, compactSelectedNavigationRouteClass(Progress::class))
        assertEquals(Home::class, compactSelectedNavigationRouteClass(Home::class))
    }

    @Test
    fun topLevelSwipeStopsAtEndsAndMovesOneVisibleDestination() {
        assertEquals(null, topLevelSwipeTargetIndex(0, -1, 5))
        assertEquals(1, topLevelSwipeTargetIndex(0, 1, 5))
        assertEquals(3, topLevelSwipeTargetIndex(4, -1, 5))
        assertEquals(null, topLevelSwipeTargetIndex(4, 1, 5))
        assertEquals(null, topLevelSwipeTargetIndex(-1, 1, 5))
    }

    @Test
    fun settingsContainsOnlyConfigurationCopy() {
        assertEquals("Meer", settingsOverflowSectionTitle())
        assertFalse(settingsOverflowSectionBody().contains("Voortgang"))
    }

    @Test
    fun mediumAndExpandedWidthsUseNavigationRail() {
        assertTrue(shouldUseNavigationRail(TrainIqWindowWidthClass.Medium))
        assertTrue(shouldUseNavigationRail(TrainIqWindowWidthClass.Expanded))
    }

    @Test
    fun onlyShortLandscapeWindowsCondenseNavigationWhilePortraitKeepsLabels() {
        assertTrue(shouldUseCompactShortBottomBar(TrainIqWindowWidthClass.Compact, screenHeightDp = 480))
        assertFalse(shouldUseCompactShortBottomBar(TrainIqWindowWidthClass.Compact, screenHeightDp = 481))
        assertFalse(shouldUseCompactShortBottomBar(TrainIqWindowWidthClass.Compact, screenHeightDp = 640))
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
