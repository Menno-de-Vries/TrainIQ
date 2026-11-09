package com.trainiq.features.workout

import com.trainiq.domain.workout.ScheduledWorkoutRestTimer
import com.trainiq.domain.workout.WorkoutRestTimerIdentity
import com.trainiq.domain.model.ActiveWorkoutSession
import com.trainiq.domain.model.ActiveWorkoutSetEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RestTimerSameValueRefreshTest {
    private fun session() = ActiveWorkoutSession(sessionId = 7, dayId = 8, startedAt = 1, updatedAt = 1,
        loggedSets = emptyList(), drafts = emptyMap(), collapsedExerciseIds = emptySet(),
        restTimerEndsAt = null, restTimerTotalSeconds = 0)
    private fun adjusted(previous: RestTimerValue, generation: String = "adjustment") = RestTimerValue(90_000, 90,
        ScheduledWorkoutRestTimer(WorkoutRestTimerIdentity(previous.sessionId, generation), 8, 90_000, 80_000, "boot", 90),
        previous.sessionId, previous.sessionStartedAt)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun mergeKeepsFreshNonTimerFieldsAndOptimisticTimer() = runTest {
        val original = session()
        val attempts = mutableListOf<RestTimerValue>()
        val writer = RestTimerWrites(backgroundScope, persist = { _, next -> attempts += next },
            publish = {}, onFailure = { throw it })
        writer.mergeSession(original)
        val next = adjusted(original.timerValue())
        writer.submit(original.timerValue(), next)
        val merged = writer.mergeSession(original.copy(notes = "new notes", collapsedExerciseIds = setOf(3), updatedAt = 5))
        assertEquals("new notes", merged.notes)
        assertEquals(setOf(3L), merged.collapsedExerciseIds)
        assertEquals(next.metadata, merged.timerValue().metadata)
        runCurrent()
        assertEquals(listOf(next), attempts)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun zeroRestSetAndEveryRawIdentityReplacementInvalidateOldQueuedTap() = runTest {
        val original = session()
        val newSet = ActiveWorkoutSetEntry(1, 3, 80.0, 5, 8.0, loggedAt = 2)
        val replacements = listOf(
            original.copy(loggedSets = listOf(newSet)),
            original.copy(sessionId = 9), original.copy(startedAt = 2),
            original.copy(restTimerHandled = true),
            original.copy(restTimerEndsAt = 50_000),
            original.copy(restTimerElapsedDeadline = 45_000),
            original.copy(restTimerBootId = "other"),
            original.copy(restTimerGeneration = "external"),
        )
        for (replacement in replacements) {
            var writes = 0
            val writer = RestTimerWrites(backgroundScope, persist = { _, _ -> writes++ }, publish = {}, onFailure = { throw it })
            writer.mergeSession(original)
            writer.submit(original.timerValue(), adjusted(original.timerValue()))
            val merged = writer.mergeSession(replacement)
            runCurrent()
            assertEquals("Replacement must fence the old tap: $replacement", 0, writes)
            assertEquals(replacement.timerValue().metadata, merged.timerValue().metadata)
        }
        var writes = 0
        val writer = RestTimerWrites(backgroundScope, persist = { _, _ -> writes++ }, publish = {}, onFailure = { throw it })
        val withSet = original.copy(loggedSets = listOf(newSet))
        writer.mergeSession(withSet)
        writer.submit(withSet.timerValue(), adjusted(withSet.timerValue()))
        writer.mergeSession(withSet.copy(loggedSets = listOf(newSet.copy(reps = 6))))
        runCurrent()
        assertEquals("A correction with the same set ID and null timer is a real replacement", 0, writes)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun mergeRecognizesOwnAcknowledgementAndFailureRestoresRawConfirmedMetadata() = runTest {
        val original = session().copy(restTimerHandled = true, restTimerBootId = "raw-boot")
        var visible = original.timerValue()
        var fail = true
        var errors = 0
        val writer = RestTimerWrites(backgroundScope, persist = { _, _ -> if (fail) error("synthetic") },
            publish = { visible = it }, onFailure = { errors++ })
        writer.mergeSession(original)
        writer.submit(original.timerValue(), adjusted(original.timerValue()))
        runCurrent()
        assertEquals(1, errors)
        assertEquals(original.timerValue().metadata, visible.metadata)
        fail = false
        val first = adjusted(original.timerValue(), "first")
        val second = adjusted(original.timerValue(), "second")
        writer.submit(original.timerValue(), first)
        writer.submit(first, second)
        val ack = original.copy(restTimerEndsAt = first.endsAt, restTimerTotalSeconds = first.seconds,
            restTimerGeneration = first.timer!!.identity.generation, restTimerElapsedDeadline = first.timer.elapsedDeadline,
            restTimerBootId = first.timer.bootId, restTimerHandled = false)
        assertEquals(second.metadata, writer.mergeSession(ack).timerValue().metadata)
        runCurrent()
        assertEquals(second, visible)
        assertEquals(1, errors)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun inFlightRefreshAndOwnAcknowledgementKeepTheNextQueuedTap() = runTest {
        val initial = RestTimerValue(60_000, 60, ScheduledWorkoutRestTimer(
            WorkoutRestTimerIdentity(7, "original"), 8, 60_000, 50_000, "boot", 60), 7, 1)
        val first = initial.copy(endsAt = 90_000, seconds = 90, timer = initial.timer!!.copy(
            identity = WorkoutRestTimerIdentity(7, "first"), wallDeadline = 90_000, totalSeconds = 90))
        val second = first.copy(endsAt = 120_000, seconds = 120, timer = first.timer!!.copy(
            identity = WorkoutRestTimerIdentity(7, "second"), wallDeadline = 120_000, totalSeconds = 120))
        val gate = CompletableDeferred<Unit>()
        val attempts = mutableListOf<Pair<RestTimerValue, RestTimerValue>>()
        var visible = initial
        val writer = RestTimerWrites(backgroundScope, persist = { previous, next ->
            attempts += previous to next
            if (next == first) gate.await()
        }, publish = { visible = it }, onFailure = { throw it })
        writer.submit(initial, first)
        runCurrent()
        writer.submit(first, second)
        writer.supersede(initial.copy())
        writer.supersede(first.copy()) // our first write's DB response, before its continuation resumes
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf(initial to first, first to second), attempts)
        assertEquals(second, visible)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun unchangedConfirmedSessionRefreshDoesNotDiscardQueuedAdjustment() = runTest {
        val timer = ScheduledWorkoutRestTimer(WorkoutRestTimerIdentity(7, "generation"), 8, 60_000, 50_000, "boot", 60)
        val original = RestTimerValue(60_000, 60, timer, 7, 1)
        val adjusted = original.copy(endsAt = 90_000, seconds = 90,
            timer = timer.copy(identity = timer.identity.copy(generation = "adjustment"),
                wallDeadline = 90_000, elapsedDeadline = 80_000, totalSeconds = 90))
        val writes = mutableListOf<RestTimerValue>()
        var displayed = original
        val writer = RestTimerWrites(backgroundScope, persist = { _, next -> writes += next },
            publish = { displayed = it }, onFailure = { throw it })
        writer.submit(original, adjusted)
        // The session refresh reports exactly the same confirmed timer, not a new timer generation.
        writer.supersede(original.copy())
        runCurrent()
        assertEquals("An unchanged refresh must not discard a queued deliberate adjustment", listOf(adjusted), writes)
        assertEquals(adjusted, displayed)
    }
}
