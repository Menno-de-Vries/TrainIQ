package com.trainiq.core.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPolicyTest {
    @Test
    fun mealReminder_whenDisabled_doesNotSend() {
        assertFalse(
            shouldSendMealReminder(
                remindersEnabled = false,
                nowMillis = 10_000L,
                latestMealAtMillis = null,
                lastReminderAtMillis = 0L,
            ),
        )
    }

    @Test
    fun mealReminder_whenRecentMealExists_doesNotSend() {
        val now = MealReminderInterval.toMillis()

        assertFalse(
            shouldSendMealReminder(
                remindersEnabled = true,
                nowMillis = now,
                latestMealAtMillis = now - 30_000L,
                lastReminderAtMillis = 0L,
            ),
        )
    }

    @Test
    fun mealReminder_whenNoRecentMealAndCooldownExpired_sends() {
        val now = MealReminderInterval.toMillis() * 2

        assertTrue(
            shouldSendMealReminder(
                remindersEnabled = true,
                nowMillis = now,
                latestMealAtMillis = now - MealReminderInterval.toMillis() - 1L,
                lastReminderAtMillis = now - MealReminderInterval.toMillis() - 1L,
            ),
        )
    }

    @Test
    fun workoutReminder_whenWorkoutIsInsideTwoDayGap_doesNotSend() {
        val now = WorkoutReminderGap.toMillis()

        assertFalse(
            shouldSendWorkoutReminder(
                remindersEnabled = true,
                nowMillis = now,
                latestWorkoutAtMillis = now - 60_000L,
                lastReminderAtMillis = 0L,
            ),
        )
    }

    @Test
    fun workoutReminder_whenTwoDayGapAndDailyCooldownExpired_sends() {
        val now = WorkoutReminderGap.toMillis() + WorkoutReminderCooldown.toMillis()

        assertTrue(
            shouldSendWorkoutReminder(
                remindersEnabled = true,
                nowMillis = now,
                latestWorkoutAtMillis = now - WorkoutReminderGap.toMillis() - 1L,
                lastReminderAtMillis = now - WorkoutReminderCooldown.toMillis() - 1L,
            ),
        )
    }

    @Test
    fun reminderContent_rotatesThroughVariedSubtleEmojiMealMessages() {
        val contents = (0 until 10)
            .map { index -> mealReminderContent(index * MealReminderInterval.toMillis()).body }

        assertTrue(contents.toSet().size >= 8)
        assertTrue(contents.any { it.contains("🥗") || it.contains("🍽️") || it.contains("💧") })
        contents.forEach { body ->
            assertTrue("Reminder body should stay concise: $body", body.length <= 110)
            assertFalse(body.contains("!!!"))
        }
    }

    @Test
    fun reminderContent_rotatesThroughVariedSubtleEmojiWorkoutMessages() {
        val contents = (0 until 10)
            .map { index -> workoutReminderContent(index * WorkoutReminderCooldown.toMillis()).body }

        assertTrue(contents.toSet().size >= 8)
        assertTrue(contents.any { it.contains("💪") || it.contains("⚡") || it.contains("🏋️") })
        contents.forEach { body ->
            assertTrue("Reminder body should stay concise: $body", body.length <= 110)
            assertFalse(body.contains("!!!"))
        }
    }

    @Test
    fun reminderContent_keepsTitlesStableForNotificationGrouping() {
        assertEquals("Voeding bijwerken", mealReminderContent(0L).title)
        assertEquals("Krachttraining reminder", workoutReminderContent(0L).title)
    }
}
