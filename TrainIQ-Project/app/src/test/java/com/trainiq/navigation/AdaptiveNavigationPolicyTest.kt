package com.trainiq.navigation

import com.trainiq.features.settings.settingsOverflowSectionBody
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
        assertEquals(null, topLevelSwipeTargetIndex(2, 2, 5))
        assertEquals(null, topLevelSwipeTargetIndex(2, 0, 5))
    }

    @Test
    fun swipeIsAvailableOnlyOnCompactUnobstructedTopLevelContent() {
        fun enabled(
            rail: Boolean = false,
            index: Int = 2,
            ime: Boolean = false,
            detail: Boolean = false,
            tour: Boolean = false,
        ) = shouldEnableTopLevelSwipe(rail, index, ime, detail, tour)

        assertTrue(enabled())
        assertFalse(enabled(rail = true))
        assertFalse(enabled(index = -1))
        assertFalse(enabled(ime = true))
        assertFalse(enabled(detail = true))
        assertFalse(enabled(tour = true))
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

    @Test
    fun routeContentAppliesWidthPolicyInsideRemainingAreaAfterNavigationRail() {
        val expandedScreenWidthDp = 1440f
        val navigationRailWidthDp = 92f
        val remainingContentWidthDp = expandedScreenWidthDp - navigationRailWidthDp
        val routeWidthDp = adaptiveRouteContentWidthDp(
            TrainIqWindowWidthClass.Expanded,
            availableWidthDp = remainingContentWidthDp,
        )

        assertEquals(1120f, routeWidthDp)
        assertEquals(114f, (remainingContentWidthDp - routeWidthDp) / 2f)
        assertEquals(700f, adaptiveRouteContentWidthDp(TrainIqWindowWidthClass.Expanded, 700f))
        assertEquals(840f, adaptiveRouteContentWidthDp(TrainIqWindowWidthClass.Medium, 900f))
        assertEquals(360f, adaptiveRouteContentWidthDp(TrainIqWindowWidthClass.Compact, 360f))
    }

    @Test
    fun routeViewportFillsParentBeforeCenteringItsCappedContent() {
        val source = File("src/main/java/com/trainiq/navigation/TrainIqNav.kt").readText()
        val routeHost = source.substringAfter("private fun TrainIqNavHost(").substringBefore("composable<Onboarding>")
        val viewport = source.substringAfter("internal fun AdaptiveRouteViewport(")
            .substringBefore("internal fun bottomNavigationLabel")

        assertTrue(routeHost.contains("AdaptiveRouteViewport("))
        assertTrue(viewport.contains("modifier = modifier.fillMaxSize()"))
        assertTrue(viewport.contains("contentAlignment = Alignment.TopCenter"))
        assertTrue(viewport.contains("adaptiveRouteContentWidthDp(widthClass, maxWidth.value)"))
    }
}
