package com.trainiq.core.ui

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReloadableObservationTest {
    @Test
    fun failureThenReloadResubscribesAndEmitsSuccess() = runTest {
        val reloads = MutableStateFlow(0)
        var subscriptions = 0

        reloadableObservation(reloads) {
            flow {
                subscriptions += 1
                if (subscriptions == 1) error("first load failed")
                emit("loaded")
            }
        }.test {
            assertEquals(null, awaitItem())
            assertTrue(awaitItem()?.isFailure == true)

            reloads.value += 1

            assertEquals(null, awaitItem())
            assertEquals("loaded", awaitItem()?.getOrNull())
            assertEquals(2, subscriptions)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun combinedReloadStaysLoadingUntilEveryUpstreamEmits() = runTest {
        val reloads = MutableStateFlow(0)
        val sources = List(7) { MutableSharedFlow<Int>(extraBufferCapacity = 1) }

        reloadableObservation(reloads) { combine(sources) { it.sum() } }.test {
            assertEquals(null, awaitItem())
            sources.take(6).forEachIndexed { index, source -> source.tryEmit(index + 1) }
            expectNoEvents()

            sources.last().tryEmit(7)

            assertEquals(28, awaitItem()?.getOrNull())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
