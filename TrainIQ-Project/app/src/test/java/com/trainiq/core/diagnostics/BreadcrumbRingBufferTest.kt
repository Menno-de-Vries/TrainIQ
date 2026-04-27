package com.trainiq.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class BreadcrumbRingBufferTest {
    @Test
    fun snapshot_afterMoreThanMaxEntries_keepsOnlyLatestTwentyInOrder() {
        val buffer = BreadcrumbRingBuffer()

        (1..25).forEach { index ->
            buffer.add(message = "event-$index", timestampMillis = index.toLong())
        }

        val snapshot = buffer.snapshot()

        assertEquals(20, snapshot.size)
        assertEquals("event-6", snapshot.first().message)
        assertEquals("event-25", snapshot.last().message)
    }
}
