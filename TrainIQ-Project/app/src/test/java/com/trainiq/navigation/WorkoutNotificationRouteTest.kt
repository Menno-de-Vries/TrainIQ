package com.trainiq.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutNotificationRouteTest {
    @Test fun existingDayOnlyRouteKeepsStartDefaultsAndNotificationRetainsIdentityOnRestore() {
        val old = Json.decodeFromString<ActiveWorkout>("""{"dayId":7}""")
        assertEquals(7L, old.dayId)
        assertNull(old.restSessionId)
        assertNull(old.restGeneration)
        assertNull(old.restOpenRequest())
        val notification = ActiveWorkout(7, 12, "generation")
        assertEquals(notification, Json.decodeFromString<ActiveWorkout>(Json.encodeToString(notification)))
        // An incomplete notification route still uses resume-only and cannot become an explicit start.
        assertEquals("", ActiveWorkout(7, 12).restOpenRequest()?.generation)
        assertEquals(-1L, ActiveWorkout(7, restGeneration = "old").restOpenRequest()?.sessionId)
    }
}
