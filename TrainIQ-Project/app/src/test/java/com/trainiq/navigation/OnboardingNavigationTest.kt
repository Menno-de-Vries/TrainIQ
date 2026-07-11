package com.trainiq.navigation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingNavigationTest {
    @Test
    fun navigationDefinesTypeSafeOnboardingRouteAndUsesItForIncompleteFirstRun() {
        val source = File("src/main/java/com/trainiq/navigation/TrainIqNav.kt").readText()

        assertTrue(source.contains("@Serializable\ndata object Onboarding"))
        assertTrue(source.contains("onboardingPreferences"))
        assertTrue(source.contains("startDestination = if (onboardingPreferences.completed) Home else Onboarding"))
        assertTrue(source.contains("composable<Onboarding>"))
        assertTrue(source.contains("OnboardingRoute"))
    }

    @Test
    fun onboardingRouteHidesTopLevelNavigationChrome() {
        val source = File("src/main/java/com/trainiq/navigation/TrainIqNav.kt").readText()

        assertTrue(source.contains("hasRoute(Onboarding::class)"))
        assertTrue(source.contains("!isOnboardingDestination"))
    }

    @Test
    fun guidedTourCoversTopLevelTabsInOrderAndUsesTypeSafeRoutes() {
        assertTrue(
            guidedTourTopLevelRouteClasses() == listOf(
                Home::class,
                Train::class,
                Nutrition::class,
                Progress::class,
                Coach::class,
                Settings::class,
            ),
        )
    }

    @Test
    fun navigationHostsGuidedTourOverlayWithClearActions() {
        val source = File("src/main/java/com/trainiq/navigation/TrainIqNav.kt").readText()

        assertTrue(source.contains("GuidedTourOverlay"))
        assertTrue(source.contains("shouldShowGuidedTour(onboardingPreferences)"))
        assertTrue(source.contains("markGuidedTourCompleted"))
        assertTrue(source.contains("markGuidedTourSkipped"))
        assertTrue(source.contains("Later afronden"))
        assertTrue(source.contains("Tour afronden"))
    }

    @Test
    fun guidedTourUsesCompactActionsAndClearsBottomNavigation() {
        val source = File("src/main/java/com/trainiq/navigation/TrainIqNav.kt").readText()
        val overlay = source.substringAfter("private fun GuidedTourOverlay").substringBefore("private fun Modifier.topLevelTabSwipeNavigation")

        assertFalse(overlay.contains("heightIn(min = 176.dp)"))
        assertTrue(overlay.contains("onClick = onSkip"))
        assertTrue(overlay.contains("heightIn(min = 48.dp)"))
        assertTrue(overlay.contains("maxLines = 1"))
        assertTrue(guidedTourBottomPaddingDp(useNavigationRail = true, useCompactShortBottomBar = false) == 20)
        assertTrue(guidedTourBottomPaddingDp(useNavigationRail = false, useCompactShortBottomBar = true) == 58)
        assertTrue(guidedTourBottomPaddingDp(useNavigationRail = false, useCompactShortBottomBar = false) == 70)
    }

    @Test
    fun guidedTourCopyKeepsRoutinesInTrainingAndNotCoach() {
        val source = File("src/main/java/com/trainiq/navigation/TrainIqNav.kt").readText()
        val trainingStep = source.substringAfter("title = \"Training\"").substringBefore("GuidedTourStep(")
        val coachStep = source.substringAfter("title = \"Coach\"").substringBefore("GuidedTourStep(")

        assertTrue(trainingStep.contains("routine", ignoreCase = true))
        assertTrue(coachStep.contains("calorie", ignoreCase = true))
        assertTrue(coachStep.contains("macro", ignoreCase = true))
        assertFalse(coachStep.contains("routine", ignoreCase = true))
    }

    @Test
    fun guidedTourCopyDefinesConcreteFirstActionsForEveryTopLevelTab() {
        val source = File("src/main/java/com/trainiq/navigation/TrainIqNav.kt").readText()
        val tourBody = source.substringAfter("private fun guidedTourSteps").substringBefore("fun adaptiveDashboardGridColumns")

        listOf(
            "Check je dagstatus",
            "Maak of start je eerste routine",
            "Leg je eerste maaltijd",
            "Voeg een lichaamsmeting",
            "Vul je profiel en calorie doel",
            "Controleer Health Connect",
        ).forEach { action ->
            assertTrue("Expected guided-tour action: $action", tourBody.contains(action))
        }
    }
}
