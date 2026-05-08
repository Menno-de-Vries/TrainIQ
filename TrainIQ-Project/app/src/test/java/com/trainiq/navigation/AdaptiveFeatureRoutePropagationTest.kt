package com.trainiq.navigation

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveFeatureRoutePropagationTest {
    private val root = File(System.getProperty("user.dir"), "src/main/java/com/trainiq")

    @Test
    fun topLevelFeatureRoutesAcceptWindowWidthClass() {
        mapOf(
            "features/nutrition/NutritionScreen.kt" to "fun NutritionRoute(",
            "features/workout/WorkoutScreen.kt" to "fun WorkoutRoute(",
            "features/progress/ProgressScreen.kt" to "fun ProgressRoute(",
            "features/coach/CoachScreen.kt" to "fun CoachRoute(",
            "features/settings/SettingsSection.kt" to "fun SettingsRoute(",
        ).forEach { (path, signature) ->
            val source = File(root, path).readText()
            val routeSignature = source.substringAfter(signature).substringBefore(") {")

            assertTrue("$path must receive adaptive window info", routeSignature.contains("windowWidthClass: TrainIqWindowWidthClass"))
        }
    }

    @Test
    fun navHostPropagatesWindowWidthClassToEveryTopLevelFeature() {
        val nav = File(root, "navigation/TrainIqNav.kt").readText()

        listOf("WorkoutRoute", "NutritionRoute", "ProgressRoute", "CoachRoute", "SettingsRoute").forEach { route ->
            val call = nav.substringAfter("$route(").take(700)
            assertTrue("$route must receive windowWidthClass", call.contains("windowWidthClass = windowWidthClass"))
        }
    }
}
