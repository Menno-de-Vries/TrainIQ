package com.trainiq.core.health

import com.trainiq.domain.model.HealthConnectMetrics
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HealthConnectStepDiagnostic
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthConnectUserCopyTest {
    @Test
    fun displayedSamsungSelectionUsesSamsungHealthLabel() {
        val status = HealthConnectStatus(
            state = HealthConnectState.CONNECTED,
            metrics = HealthConnectMetrics(stepsToday = 84),
            message = "Verbonden",
            stepDiagnostic = HealthConnectStepDiagnostic(
                aggregateStepsToday = 8,
                samsungHealthStepsToday = 84,
                samsungHealthAggregateStepsToday = 8,
                samsungRawStepRecordSumToday = 84,
                queriedAt = 123L,
            ),
        )

        assertEquals("Samsung Health", healthConnectStepSourceLabel(status))
    }

    @Test
    fun generalAggregateUsesHealthConnectLabel() {
        val status = HealthConnectStatus(
            state = HealthConnectState.CONNECTED,
            metrics = HealthConnectMetrics(stepsToday = 84),
            message = "Verbonden",
        )

        assertEquals("Health Connect", healthConnectStepSourceLabel(status))
    }
}
