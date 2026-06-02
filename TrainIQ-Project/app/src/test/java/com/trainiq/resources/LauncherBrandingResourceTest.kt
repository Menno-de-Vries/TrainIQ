package com.trainiq.resources

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherBrandingResourceTest {
    private val projectDir = File(System.getProperty("user.dir").orEmpty())

    @Test
    fun appThemeDefinesExplicitSplashBranding() {
        val themes = File(projectDir, "src/main/res/values/themes.xml").readText()
        val nightThemes = File(projectDir, "src/main/res/values-night/themes.xml").readText()

        assertTrue(themes.contains("windowSplashScreenBackground"))
        assertTrue(themes.contains("windowSplashScreenAnimatedIcon"))
        assertTrue(themes.contains("postSplashScreenTheme"))
        assertTrue(nightThemes.contains("windowSplashScreenBackground"))
        assertTrue(nightThemes.contains("windowSplashScreenAnimatedIcon"))
    }

    @Test
    fun adaptiveIconsUseSharedCalmCoachLauncherLayers() {
        val adaptiveIcon = File(projectDir, "src/main/res/mipmap-anydpi-v26/ic_launcher.xml").readText()
        val roundAdaptiveIcon = File(projectDir, "src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml").readText()
        val foreground = File(projectDir, "src/main/res/drawable/ic_launcher_foreground.xml").readText()
        val background = File(projectDir, "src/main/res/drawable/ic_launcher_background.xml").readText()

        assertTrue(adaptiveIcon.contains("@drawable/ic_launcher_background"))
        assertTrue(adaptiveIcon.contains("@drawable/ic_launcher_foreground"))
        assertTrue(roundAdaptiveIcon.contains("@drawable/ic_launcher_background"))
        assertTrue(roundAdaptiveIcon.contains("@drawable/ic_launcher_foreground"))
        assertTrue(foreground.contains("android:viewportWidth=\"108\""))
        assertTrue(foreground.contains("M54"))
        assertTrue(background.contains("#E8FFF4"))
    }
}
