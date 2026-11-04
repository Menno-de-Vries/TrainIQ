package com.trainiq.domain.workout

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkoutRestTimerDeliveryTest {
    private val now = WorkoutRestTimerNow(100_000, 10_000, "a")
    private val timer = ScheduledWorkoutRestTimer(WorkoutRestTimerIdentity(1, "one"), 2, 100_000, 10_000, "a", 30)
    private class Store(var timer: ScheduledWorkoutRestTimer?) : WorkoutRestTimerStore {
        override suspend fun read() = timer
        override suspend fun claim(identity: WorkoutRestTimerIdentity, now: WorkoutRestTimerNow): RestTimerCompletion {
            val current = timer?.takeIf { it.identity == identity } ?: return RestTimerCompletion.OBSOLETE
            return current.completion(now).also {
                if (it == RestTimerCompletion.COMPLETED || it == RestTimerCompletion.EXPIRED) timer = current.copy(handled = true)
            }
        }
    }
    private class Delivery : WorkoutRestTimerDelivery {
        val effects = mutableListOf<String>()
        var failSchedule = false
        var gate: CompletableDeferred<Unit>? = null
        override fun schedule(timer: ScheduledWorkoutRestTimer) {
            if (failSchedule) error("OS scheduling unavailable")
            effects += "schedule:${timer.identity.generation}"
        }
        override fun cancel(identity: WorkoutRestTimerIdentity) { effects += "cancel:${identity.generation}" }
        override suspend fun show(timer: ScheduledWorkoutRestTimer, silent: Boolean): Boolean {
            gate?.await()
            effects += "show:${timer.identity.generation}:$silent"
            return true
        }
    }
    private fun coordinator(store: Store, delivery: Delivery) = WorkoutRestTimerCoordinator(store, delivery,
        object : WorkoutRestTimerClock { override fun now() = now })

    @Test fun concurrentCallbacksDeliverOnceAndSuccessorCannotBeOvertaken() = runTest {
        val store = Store(timer); val delivery = Delivery(); val coordinator = coordinator(store, delivery)
        delivery.gate = CompletableDeferred()
        val first = async { coordinator.completeIfCurrent(timer.identity) }
        runCurrent()
        val duplicate = async { coordinator.completeIfCurrent(timer.identity) }
        val next = async { coordinator.mutate { store.timer = timer.copy(identity = WorkoutRestTimerIdentity(1, "two"), elapsedDeadline = 30_000) } }
        runCurrent()
        delivery.gate!!.complete(Unit)
        assertEquals(RestTimerCompletion.COMPLETED, first.await())
        assertEquals(RestTimerCompletion.HANDLED, duplicate.await())
        next.await()
        assertEquals(listOf("cancel:one", "show:one:false", "cancel:one", "schedule:two"), delivery.effects)
    }

    @Test fun failedStorageDoesNotCancelAndMissingPlanningRecoversAtNextReconcile() = runTest {
        val store = Store(timer.copy(elapsedDeadline = 30_000)); val delivery = Delivery(); val coordinator = coordinator(store, delivery)
        try { coordinator.mutate { error("storage rollback") }; fail() } catch (_: IllegalStateException) { }
        assertTrue(delivery.effects.isEmpty())
        delivery.failSchedule = true
        coordinator.mutate { store.timer = store.timer!!.copy(identity = WorkoutRestTimerIdentity(1, "two")) }
        assertEquals("two", store.timer!!.identity.generation)
        delivery.failSchedule = false
        coordinator.reconcile()
        assertEquals("schedule:two", delivery.effects.last())
        coordinator.mutate { store.timer = null }
        assertEquals("cancel:two", delivery.effects.last())
        assertEquals(RestTimerCompletion.OBSOLETE, coordinator.completeIfCurrent(timer.identity))
    }

    @Test fun lateRecoveryIsSilentAndRebootDoesNotDeliverAnyCue() = runTest {
        val store = Store(timer.copy(elapsedDeadline = -20_001)); val delivery = Delivery(); val coordinator = coordinator(store, delivery)
        coordinator.reconcile()
        assertEquals(listOf("cancel:one", "show:one:true"), delivery.effects)
        store.timer = timer.copy(bootId = "old", identity = WorkoutRestTimerIdentity(1, "old"))
        coordinator.reconcile()
        assertEquals("cancel:old", delivery.effects.last())
        assertEquals(1, delivery.effects.count { it.startsWith("show:") })
    }
}
