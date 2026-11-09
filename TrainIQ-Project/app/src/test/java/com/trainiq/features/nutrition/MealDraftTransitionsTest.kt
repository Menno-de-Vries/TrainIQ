package com.trainiq.features.nutrition

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test
import com.trainiq.domain.model.MealType

class MealDraftTransitionsTest {
    private val zone = ZoneId.of("Europe/Amsterdam")
    private val now = Instant.parse("2026-11-09T17:43:12.321Z")
    private fun resolve(date: String, original: Long? = null) = mealDraftLoggedAt(date, original, now, zone)

    @Test fun newTodayUsesNowIncludingSubseconds() { assertEquals(now.toEpochMilli(), resolve("2026-11-09")) }
    @Test fun newPastDateUsesExplicitLocalNoon() {
        assertEquals(Instant.parse("2026-07-01T10:00:00Z").toEpochMilli(), resolve("2026-07-01"))
    }
    @Test fun newFutureDateUsesLocalNoon() {
        assertEquals(LocalTime.NOON, Instant.ofEpochMilli(resolve("2027-01-01")!!).atZone(zone).toLocalTime())
    }
    @Test fun sameEditDatePreservesExactOriginalTimestamp() {
        val original = Instant.parse("2026-07-01T07:32:51.456Z").toEpochMilli()
        assertEquals(original, resolve("2026-07-01", original))
    }
    @Test fun changedEditDateKeepsLocalTimeWithNewOffset() {
        val original = Instant.parse("2026-07-01T07:32:51.456Z").toEpochMilli()
        assertEquals(Instant.parse("2026-11-09T08:32:51.456Z").toEpochMilli(), resolve("2026-11-09", original))
    }
    @Test fun invalidDatesCannotProduceTimestamp() {
        listOf("", "abc", "2026-02-30", "2026-13-01", "09-11-2026").forEach { assertNull(resolve(it)) }
    }
    @Test fun sameDateDuringDstOverlapPreservesOriginalInstant() {
        val original = Instant.parse("2026-10-25T01:30:00Z").toEpochMilli()
        assertEquals(original, resolve("2026-10-25", original))
    }
    @Test fun changedDateIntoDstGapMovesForwardByGap() {
        val original = Instant.parse("2026-03-28T01:30:00Z").toEpochMilli()
        val result = Instant.ofEpochMilli(resolve("2026-03-29", original)!!).atZone(zone)
        assertEquals(LocalDate.parse("2026-03-29"), result.toLocalDate())
        assertEquals(LocalTime.of(3, 30), result.toLocalTime())
    }
    @Test fun todayDependsOnProvidedZoneRatherThanUtcDate() {
        val instant = Instant.parse("2026-11-09T23:30:00Z")
        assertEquals(instant.toEpochMilli(), mealDraftLoggedAt("2026-11-10", null, instant, zone))
    }

    private fun replaces(
        destination: MealDraftDestination = MealDraftDestination.New(MealType.DINNER),
        editingId: Long? = null, saveId: Long? = null, items: Boolean = false,
        name: String = "", notes: String = "", date: String? = null,
    ) = requiresMealDraftReplacement(destination, MealType.LUNCH, editingId, saveId, items, name, notes, date)

    @Test fun blankDraftStartsDirectlyButEveryMeaningfulFieldProtectsAnotherTarget() {
        assertFalse(replaces())
        assertFalse(replaces(name = "  ", notes = "  "))
        assertTrue(replaces(editingId = 1))
        assertTrue(replaces(saveId = 2))
        assertTrue(replaces(items = true))
        assertTrue(replaces(name = "meal"))
        assertTrue(replaces(notes = "note"))
        assertTrue(replaces(date = "invalid date still belongs to the draft"))
    }
    @Test fun sameNewTargetResumesButCannotSilentlyChangeAnEditIntoCreate() {
        val target = MealDraftDestination.New(MealType.LUNCH)
        assertFalse(replaces(target, items = true, saveId = 2))
        assertTrue(replaces(target, editingId = 1))
    }
    @Test fun sameEditResumesButAnotherEditAndReuseRequireReplacement() {
        assertFalse(replaces(MealDraftDestination.Edit(1), editingId = 1, items = true))
        assertTrue(replaces(MealDraftDestination.Edit(2), editingId = 1))
        assertTrue(replaces(MealDraftDestination.Edit(1), saveId = 1))
        assertTrue(replaces(MealDraftDestination.Reuse(1), editingId = 1))
        assertTrue(replaces(MealDraftDestination.Reuse(1), saveId = 1))
        assertFalse(replaces(MealDraftDestination.Reuse(1)))
    }
}
