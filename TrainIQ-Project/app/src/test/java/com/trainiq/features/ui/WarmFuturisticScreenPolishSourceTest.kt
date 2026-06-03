package com.trainiq.features.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WarmFuturisticScreenPolishSourceTest {
    private val root = File(System.getProperty("user.dir"), "src/main/java/com/trainiq/features")

    @Test
    fun ownedScreens_keepTaskTwoWithinWarmMaterialComponents() {
        val sources = ownedScreenSources()

        assertTrue(sources["home"]!!.contains("MaterialTheme.trainIqColors.amber"))
        assertTrue(sources["nutrition"]!!.contains("MaterialTheme.trainIqColors.amber"))
        assertTrue(sources["progress"]!!.contains("MaterialTheme.trainIqColors.amber"))
        assertTrue(sources["coach"]!!.contains("MaterialTheme.trainIqColors.amber"))
        assertTrue(sources["settings"]!!.contains("SectionCard("))
        assertFalse(sources["settings"]!!.contains("Voortgang openen"))
    }

    @Test
    fun ownedScreens_useWrappingActionRowsForLongDutchLabels() {
        val combined = ownedScreenSources().values.joinToString("\n")

        assertTrue(combined.contains("FlowRow("))
        assertTrue(combined.contains("modifier = Modifier.fillMaxWidth()"))
        assertFalse(
            "Long action rows should avoid fixed two-column buttons that clip at large font scale.",
            combined.contains("modifier = Modifier.weight(1f)) { Text(\"Wijzigingen opslaan\")"),
        )
    }

    @Test
    fun homeMetricsUseSharedWarmMetricCards() {
        val home = ownedScreenSources()["home"]!!

        assertTrue(home.contains("CompactMetricCard("))
        assertFalse(home.contains("com.trainiq.core.util.MetricCard"))
    }

    private fun ownedScreenSources(): Map<String, String> = mapOf(
        "home" to File(root, "home/HomeScreen.kt").readText(),
        "nutrition" to File(root, "nutrition/NutritionScreen.kt").readText(),
        "progress" to File(root, "progress/ProgressScreen.kt").readText(),
        "coach" to File(root, "coach/CoachScreen.kt").readText(),
        "settings" to File(root, "settings/SettingsSection.kt").readText(),
    )
}
