package com.trainiq.navigation

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingNavigationTest {
    @Test
    fun navigationDefinesTypeSafeOnboardingRouteAndUsesItForIncompleteFirstRun() {
        val source = File("src/main/java/com/trainiq/navigation/TrainIqNav.kt").readText()

        assertTrue(source.contains("@Serializable\ndata object Onboarding"))
        assertTrue(source.contains("onboardingCompleted"))
        assertTrue(source.contains("startDestination = if (onboardingCompleted) Home else Onboarding"))
        assertTrue(source.contains("composable<Onboarding>"))
        assertTrue(source.contains("OnboardingRoute"))
    }

    @Test
    fun onboardingRouteHidesTopLevelNavigationChrome() {
        val source = File("src/main/java/com/trainiq/navigation/TrainIqNav.kt").readText()

        assertTrue(source.contains("hasRoute(Onboarding::class)"))
        assertTrue(source.contains("!isOnboardingDestination"))
    }
}
