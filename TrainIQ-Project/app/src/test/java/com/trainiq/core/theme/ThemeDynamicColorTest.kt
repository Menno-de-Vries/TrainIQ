package com.trainiq.core.theme

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
}
