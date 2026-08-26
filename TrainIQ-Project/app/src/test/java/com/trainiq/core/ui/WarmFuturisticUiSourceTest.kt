package com.trainiq.core.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WarmFuturisticUiSourceTest {
    @Test
    fun `theme contains warm futuristic moodboard colors`() {
        val theme = testSourceFile("core/theme/Theme.kt").readText()

        assertTrue(theme.contains("0xFF0A0D18"))
        assertTrue(theme.contains("0xFF202334"))
        assertTrue(theme.contains("0xFFFFB25C"))
        assertTrue(theme.contains("0xFFFF7662"))
        assertTrue(theme.contains("0xFF5BE8B2"))
        assertTrue(theme.contains("0xFF6FABFF"))
    }

    @Test
    fun `shared app design exposes warm glass and wrapping action helpers`() {
        val appDesign = testSourceFile("core/ui/AppDesign.kt").readText()

        assertTrue(appDesign.contains("fun WarmGlassCard("))
        assertTrue(appDesign.contains("fun AppPill("))
        assertTrue(appDesign.contains("fun CompactMetricCard("))
        assertTrue(appDesign.contains("fun WrappingActionRow("))
        assertTrue(appDesign.contains("FlowRow("))
        assertTrue(appDesign.contains("softWrap = true"))
    }

    @Test
    fun `wrapping action row gives each action its own safe modifier`() {
        val appDesign = testSourceFile("core/ui/AppDesign.kt").readText()
        val actionRowBody = appDesign.substringAfter("fun ActionButtonRow(")
            .substringBefore("@Composable\nfun WrappingActionRow(")

        assertTrue(actionRowBody.contains("content: @Composable (Modifier) -> Unit"))
        assertTrue(actionRowBody.contains("val actionModifier ="))
        assertTrue(actionRowBody.contains("defaultMinSize(minHeight = 48.dp)"))
        assertTrue(actionRowBody.contains("content(actionModifier)"))
        assertFalse(
            "Actions should be separate FlowRow children, not all rendered inside one shared Box.",
            actionRowBody.contains("Box(\n            modifier = if (layout == ActionButtonLayout.Stacked)"),
        )
    }

    @Test
    fun `app card gradient is drawn on the full card bounds without unbounded sizing`() {
        val appDesign = testSourceFile("core/ui/AppDesign.kt").readText()
        val appCardBody = appDesign.substringAfter("fun AppCard(").substringBefore("@Composable\nfun WarmGlassCard(")

        assertTrue(appCardBody.contains(".background(cardBrush, shape)"))
        assertTrue(appCardBody.contains("containerColor = Color.Transparent"))
        assertTrue(appCardBody.contains("Brush.linearGradient"))
        assertFalse(appCardBody.contains(".matchParentSize()"))
        assertFalse(appCardBody.contains(".fillMaxSize()"))
    }
}

private fun testSourceFile(relativePackagePath: String): File {
    val userDir = File(System.getProperty("user.dir") ?: ".")
    return listOf(
        File(userDir, "src/main/java/com/trainiq/$relativePackagePath"),
        File(userDir, "app/src/main/java/com/trainiq/$relativePackagePath"),
    ).first { it.isFile }
}
