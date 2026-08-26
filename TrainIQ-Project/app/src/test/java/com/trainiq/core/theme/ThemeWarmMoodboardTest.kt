package com.trainiq.core.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeWarmMoodboardTest {
    @Test
    fun darkTrainIqColors_matchWarmFuturisticMoodboardTokens() {
        val colors = warmMoodboardDarkTrainIqColors()

        assertEquals(Color(0xFF0A0D18), colors.appBackground)
        assertEquals(Color(0xFF202334), colors.card)
        assertEquals(Color(0xFFFFB25C), colors.amber)
        assertEquals(Color(0xFFFF7662), colors.peach)
        assertEquals(Color(0xFF5BE8B2), colors.mint)
        assertEquals(Color(0xFF6FABFF), colors.blue)
    }
}
