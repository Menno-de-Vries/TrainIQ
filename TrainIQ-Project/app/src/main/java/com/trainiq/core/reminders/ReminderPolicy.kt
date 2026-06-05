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
        "🥗 Kleine log, scherpe coaching. Zet je maaltijd of snack erin.",
        "🍽️ Iets gegeten? Leg het kort vast, perfect hoeft niet.",
        "💧 Ook drankjes tellen mee. Check je dag even bij.",
        "✨ Eén minuut loggen, de rest van je dag blijft helder.",
        "📌 Snack of maaltijd gehad? Zet hem erin voordat je het vergeet.",
        "⚡ Korte food-check. Je macro’s blijven zo lekker scherp.",
        "🧭 Even bijwerken: wat zat er sinds je laatste log in?",
        "✅ Klaar in een tik: registreer wat je net hebt gepakt.",
        "🌿 Houd je plan rustig op koers met een snelle voedingslog.",
        "🔥 Klein momentje: voeding bijwerken en door.",
    )
    return ReminderContent(
        type = ReminderType.MEAL,
        title = "Voeding bijwerken",
        body = options[((nowMillis / MealReminderInterval.toMillis()).toInt()).floorMod(options.size)],
    )
}

internal fun workoutReminderContent(nowMillis: Long): ReminderContent {
    val options = listOf(
        "💪 Tijd voor je volgende sterke sessie. Kort mag ook.",
        "⚡ Twee dagen stil? Plan een korte training en bouw door.",
        "🏋️ Pak je routine weer op. Eén goede oefening telt al.",
        "🔥 Je hoeft niet maximaal te gaan. Start gewoon slim.",
        "✅ Krachtprikkel erin vandaag? Je momentum bedankt je straks.",
        "🧭 Even checken: welke sessie past vandaag het best?",
        "🌿 Rustig opbouwen is ook winnen. Zet je training klaar.",
        "📌 Plan je volgende setmoment voordat de dag volloopt.",
        "🚀 Kleine sessie, grote lijn. Houd je ritme levend.",
        "✨ Klaar voor progressie? Open TrainIQ en start beheerst.",
    )
    return ReminderContent(
        type = ReminderType.WORKOUT,
        title = "Krachttraining reminder",
        body = options[((nowMillis / WorkoutReminderCooldown.toMillis()).toInt()).floorMod(options.size)],
    )
}

private fun Int.floorMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor
