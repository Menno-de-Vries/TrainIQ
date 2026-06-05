package com.trainiq.core.reminders

import org.junit.Assert.assertFalse
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
}
