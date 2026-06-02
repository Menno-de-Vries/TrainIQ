package com.trainiq.core.theme

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeDynamicColorTest {
    @Test
    fun dynamicColorIsEnabledOnlyOnAndroid12AndNewerWhenRequested() {
        assertFalse(shouldUseDynamicColor(dynamicColor = true, sdkInt = 30))
        assertFalse(shouldUseDynamicColor(dynamicColor = false, sdkInt = 31))
        assertTrue(shouldUseDynamicColor(dynamicColor = true, sdkInt = 31))
    }

    @Test
    fun trainIqThemeUsesAndroid12DynamicColorGateAndBothLightDarkSchemes() {
        val source = testSourceFile("core/theme/Theme.kt").readText()
        val themeBody = source.substringAfter("fun TrainIqTheme(").substringBefore("internal fun shouldUseDynamicColor")

        assertTrue(themeBody.contains("shouldUseDynamicColor(dynamicColor, Build.VERSION.SDK_INT)"))
        assertTrue(themeBody.contains("dynamicTrainIqColorScheme(darkTheme = true, context = context)"))
        assertTrue(themeBody.contains("dynamicTrainIqColorScheme(darkTheme = false, context = context)"))
        assertTrue(source.contains("dynamicDarkColorScheme(context)"))
        assertTrue(source.contains("dynamicLightColorScheme(context)"))
    }

    @Test
    fun fallbackThemeUsesCalmCoachColorTokensWithoutDisablingDynamicColor() {
        val source = testSourceFile("core/theme/Theme.kt").readText()

        assertTrue(source.contains("calmCoachLightTrainIqColors()"))
        assertTrue(source.contains("calmCoachDarkTrainIqColors()"))
        assertTrue(source.contains("Color(0xFF0F766E)"))
        assertTrue(source.contains("Color(0xFF5EDCC2)"))
        assertTrue(source.contains("shouldUseDynamicColor(dynamicColor, Build.VERSION.SDK_INT)"))
    }
}

private fun testSourceFile(relativePackagePath: String): File {
    val userDir = File(System.getProperty("user.dir").orEmpty())
    return listOf(
        File(userDir, "src/main/java/com/trainiq/$relativePackagePath"),
        File(userDir, "app/src/main/java/com/trainiq/$relativePackagePath"),
    ).first(File::isFile)
}
