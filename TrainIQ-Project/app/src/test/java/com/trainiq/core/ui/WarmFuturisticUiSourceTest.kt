package com.trainiq.core.ui

import java.io.File
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
}

private fun testSourceFile(relativePackagePath: String): File {
    val userDir = File(System.getProperty("user.dir"))
    return listOf(
        File(userDir, "src/main/java/com/trainiq/$relativePackagePath"),
        File(userDir, "app/src/main/java/com/trainiq/$relativePackagePath"),
    ).first(File::isFile)
}
