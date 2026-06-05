package com.trainiq.core.reminders

import java.time.Duration

enum class ReminderType {
    MEAL,
    WORKOUT,
}

data class ReminderContent(
    val type: ReminderType,
    val title: String,
    val body: String,
)

internal val MealReminderInterval: Duration = Duration.ofHours(4)
internal val WorkoutReminderGap: Duration = Duration.ofDays(2)
internal val WorkoutReminderCooldown: Duration = Duration.ofDays(1)

internal fun shouldSendMealReminder(
    remindersEnabled: Boolean,
    nowMillis: Long,
    latestMealAtMillis: Long?,
    lastReminderAtMillis: Long,
): Boolean {
    if (!remindersEnabled) return false
    val intervalMillis = MealReminderInterval.toMillis()
    if (latestMealAtMillis != null && nowMillis - latestMealAtMillis < intervalMillis) return false
    if (lastReminderAtMillis > 0L && nowMillis - lastReminderAtMillis < intervalMillis) return false
    return true
}

internal fun shouldSendWorkoutReminder(
    remindersEnabled: Boolean,
    nowMillis: Long,
    latestWorkoutAtMillis: Long?,
    lastReminderAtMillis: Long,
): Boolean {
    if (!remindersEnabled) return false
    val gapMillis = WorkoutReminderGap.toMillis()
    if (latestWorkoutAtMillis != null && nowMillis - latestWorkoutAtMillis < gapMillis) return false
    if (lastReminderAtMillis > 0L && nowMillis - lastReminderAtMillis < WorkoutReminderCooldown.toMillis()) return false
    return true
}

internal fun mealReminderContent(nowMillis: Long): ReminderContent {
    val options = listOf(
        "Even checken: iets gegeten of gedronken? Log het kort, dan blijft je plan scherp.",
        "Kleine food-log nu, betere coaching straks. Zet je maaltijd of snack erin.",
        "Als je iets hebt gepakt: registreer het even. Je hoeft het niet perfect te maken.",
    )
    return ReminderContent(
        type = ReminderType.MEAL,
        title = "Voeding bijwerken",
        body = options[((nowMillis / MealReminderInterval.toMillis()).toInt()).floorMod(options.size)],
    )
}

internal fun workoutReminderContent(nowMillis: Long): ReminderContent {
    val options = listOf(
        "Kom op, pak je krachttraining weer op. Je kan dit.",
        "Twee dagen geen krachttraining gezien. Plan een korte sessie en bouw rustig door.",
        "Je bent lekker bezig. Tijd om je volgende sterke sessie neer te zetten.",
    )
    return ReminderContent(
        type = ReminderType.WORKOUT,
        title = "Krachttraining reminder",
        body = options[((nowMillis / WorkoutReminderCooldown.toMillis()).toInt()).floorMod(options.size)],
    )
}

private fun Int.floorMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor
