package com.trainiq.navigation

import org.junit.Assert.assertFalse
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
    }

    @Test
    fun compactSwipeNavigationMatchesVisibleBottomNavigation() {
        assertTrue(compactSwipeNavigationRouteClasses() == compactBottomNavigationRouteClasses())
        assertFalse(Progress::class in compactSwipeNavigationRouteClasses())
    }

    @Test
    fun mediumAndExpandedWidthsUseNavigationRail() {
        assertTrue(shouldUseNavigationRail(TrainIqWindowWidthClass.Medium))
        assertTrue(shouldUseNavigationRail(TrainIqWindowWidthClass.Expanded))
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
