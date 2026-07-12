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
    fun homeMomentumUsesSingleWarmCardWithEqualHeightMetricTiles() {
        val home = ownedScreenSources()["home"]!!
        val momentumBlock = home.substringAfter("private fun HomeMomentumCard(")
            .substringBefore("internal fun homeStreakValue")

        assertTrue(momentumBlock.contains("HomeMomentumMetricRow("))
        assertTrue(momentumBlock.contains("height(IntrinsicSize.Max)"))
        assertTrue(momentumBlock.contains("fillMaxHeight()"))
        assertFalse(momentumBlock.contains("CompactMetricCard("))
        assertFalse(home.contains("com.trainiq.core.util.MetricCard"))
    }

    @Test
    fun homeUsesFiniteVerticalScrollForItsFixedFullWidthCardSet() {
        val home = ownedScreenSources()["home"]!!

        assertTrue(home.contains("verticalScroll(rememberScrollState())"))
        assertFalse(home.contains("LazyVerticalGrid("))
    }

    @Test
    fun homeFiniteScrollKeepsInsetsAndContentPaddingInsideItsScrollViewport() {
        val home = ownedScreenSources()["home"]!!
        val successBody = home.substringAfter("is HomeUiState.Success ->")
            .substringBefore("internal fun buildHomeRecoverySubtitle")
        val homeScrollModifier = successBody.substringAfter("Column(")
            .substringBefore("verticalArrangement =")

        val scrollIndex = homeScrollModifier.indexOf("verticalScroll(rememberScrollState())")
        assertTrue(scrollIndex >= 0)
        assertTrue(scrollIndex < homeScrollModifier.indexOf("navigationBarsPadding()"))
        assertTrue(scrollIndex < homeScrollModifier.indexOf("imePadding()"))
        assertTrue(scrollIndex < homeScrollModifier.indexOf(".padding("))
    }

    private fun ownedScreenSources(): Map<String, String> = mapOf(
        "home" to File(root, "home/HomeScreen.kt").readText(),
        "nutrition" to File(root, "nutrition/NutritionScreen.kt").readText(),
        "progress" to File(root, "progress/ProgressScreen.kt").readText(),
        "coach" to File(root, "coach/CoachScreen.kt").readText(),
        "settings" to File(root, "settings/SettingsSection.kt").readText(),
    )
}
