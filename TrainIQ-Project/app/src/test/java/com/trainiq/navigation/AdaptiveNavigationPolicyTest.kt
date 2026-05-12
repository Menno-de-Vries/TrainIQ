package com.trainiq.navigation

import com.trainiq.features.settings.settingsOpenProgressActionLabel
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
    fun compactBottomNavigationUsesOverflowPolicy() {
        val visibleRoutes = compactBottomNavigationRouteClasses()

        assertTrue(visibleRoutes.size <= 5)
        assertTrue(Settings::class in visibleRoutes)
        assertFalse(Progress::class in visibleRoutes)
        assertEquals(Settings::class, visibleRoutes.last())
        assertEquals("Meer", bottomNavigationLabel("Instellingen"))
    }

    @Test
    fun compactSwipeNavigationMatchesVisibleBottomNavigation() {
        assertTrue(compactSwipeNavigationRouteClasses() == compactBottomNavigationRouteClasses())
        assertFalse(Progress::class in compactSwipeNavigationRouteClasses())
    }

    @Test
    fun compactOverflowSettingsMakesProgressDiscoverable() {
        assertEquals("Meer", settingsOverflowSectionTitle())
        assertTrue(settingsOverflowSectionBody().contains("Voortgang"))
        assertTrue(settingsOpenProgressActionLabel().contains("Voortgang openen"))
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
