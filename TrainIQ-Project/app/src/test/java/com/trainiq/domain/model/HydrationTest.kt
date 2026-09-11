package com.trainiq.domain.model

import org.junit.Assert.*
import org.junit.Test

class HydrationTest {
    @Test fun explicitVolumesConvertWithoutAssumingDensity() {
        assertEquals(250.0, explicitVolumeMl("25", "cl")!!, 0.0)
        assertEquals(1500.0, explicitVolumeMl("1,5", "l")!!, 0.0)
        assertEquals(300.0, explicitVolumeMl("300", "ml")!!, 0.0)
        assertNull(explicitVolumeMl("300", "g"))
        assertNull(explicitVolumeMl("NaN", "ml"))
        assertNull(explicitVolumeMl("-1", "ml"))
        assertNull(explicitVolumeMl("0", "ml"))
    }
}
