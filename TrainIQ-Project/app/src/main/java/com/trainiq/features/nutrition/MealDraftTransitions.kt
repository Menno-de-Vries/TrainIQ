package com.trainiq.features.nutrition

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import com.trainiq.domain.model.MealType

/** Explicit destinations distinguish adding to a new registration from editing an existing row. */
internal sealed interface MealDraftDestination : java.io.Serializable {
    data class New(val type: MealType) : MealDraftDestination
    data class Edit(val id: Long) : MealDraftDestination
    data class Reuse(val id: Long) : MealDraftDestination
}

internal fun requiresMealDraftReplacement(
    destination: MealDraftDestination,
    currentType: MealType,
    editingId: Long?,
    saveId: Long?,
    hasItems: Boolean,
    name: String,
    notes: String,
    dateOverride: String?,
): Boolean {
    val occupied = editingId != null || saveId != null || hasItems || name.isNotBlank() ||
        notes.isNotBlank() || dateOverride != null
    if (!occupied) return false
    return when (destination) {
        is MealDraftDestination.New -> editingId != null || currentType != destination.type
        is MealDraftDestination.Edit -> editingId != destination.id
        is MealDraftDestination.Reuse -> true
    }
}

internal fun mealDraftLoggedAt(
    date: String,
    originalTimestamp: Long?,
    now: Instant,
    zone: ZoneId,
): Long? = runCatching {
    val selected = LocalDate.parse(date)
    val original = originalTimestamp?.let { Instant.ofEpochMilli(it).atZone(zone) }
    when {
        original != null && selected == original.toLocalDate() -> original.toInstant()
        original != null -> ZonedDateTime.ofLocal(selected.atTime(original.toLocalTime()), zone, original.offset).toInstant()
        selected == now.atZone(zone).toLocalDate() -> now
        else -> selected.atTime(LocalTime.NOON).atZone(zone).toInstant()
    }.toEpochMilli()
}.getOrNull()
