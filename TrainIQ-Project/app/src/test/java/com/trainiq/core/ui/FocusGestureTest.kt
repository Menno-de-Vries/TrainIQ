package com.trainiq.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusGestureTest {
    @Test
    fun `small stationary tap can request text input focus`() {
        val touchSlop = 18f
        val tapSlop = tapOnlyFocusSlop(touchSlop)

        assertTrue(shouldRequestTextInputFocusFromGesture(maxDistanceFromStart = 0.5f, totalDistance = 0.5f, tapSlop = tapSlop))
    }

    @Test
    fun `normal finger tap jitter can request text input focus`() {
        val touchSlop = 18f
        val tapSlop = tapOnlyFocusSlop(touchSlop)

        assertTrue(shouldRequestTextInputFocusFromGesture(maxDistanceFromStart = 6f, totalDistance = 6f, tapSlop = tapSlop))
    }

    @Test
    fun `text input tap is passed through so filled fields can place cursor`() {
        val touchSlop = 18f
        val tapSlop = tapOnlyFocusSlop(touchSlop)

        assertFalse(shouldSuppressTextInputGesture(maxDistanceFromStart = 0.5f, totalDistance = 0.5f, tapSlop = tapSlop))
        assertFalse(shouldSuppressTextInputGesture(maxDistanceFromStart = 6f, totalDistance = 6f, tapSlop = tapSlop))
    }

    @Test
    fun `text input drag is suppressed so overscroll does not focus input`() {
        val touchSlop = 18f
        val tapSlop = tapOnlyFocusSlop(touchSlop)

        assertTrue(shouldSuppressTextInputGesture(maxDistanceFromStart = 12f, totalDistance = 12f, tapSlop = tapSlop))
    }

    @Test
    fun `focused input requests visibility again after ime settles`() {
        val delays = focusedInputBringIntoViewDelaysMillis(
            firstRequestDelayMillis = 80L,
            settledImeDelayMillis = 320L,
        )

        assertEquals(listOf(80L, 320L), delays)
    }

    @Test
    fun `short overscroll movement is not treated as text input tap`() {
        val touchSlop = 18f
        val tapSlop = tapOnlyFocusSlop(touchSlop)

        assertFalse(shouldRequestTextInputFocusFromGesture(maxDistanceFromStart = 12f, totalDistance = 12f, tapSlop = tapSlop))
    }

    @Test
    fun `drag movement beyond tap slop does not request text input focus`() {
        val touchSlop = 18f
        val tapSlop = tapOnlyFocusSlop(touchSlop)

        assertFalse(shouldRequestTextInputFocusFromGesture(maxDistanceFromStart = 10f, totalDistance = 10f, tapSlop = tapSlop))
    }
}
