package com.trainiq.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainDetailModeChromeTest {
    @Test
    fun clearsTrainDetailModeWhenNavigatingToAnotherTopLevelDestination() {
        assertTrue(
            shouldClearTrainDetailMode(
                isTrainDestination = false,
                isTopLevelDestination = true,
            ),
        )
    }

    @Test
    fun keepsTrainDetailModeWhileExerciseHistoryIsOnTopOfTrainFlow() {
        assertFalse(
            shouldClearTrainDetailMode(
                isTrainDestination = false,
                isTopLevelDestination = false,
            ),
        )
    }

    @Test
    fun keepsTrainDetailModeForTrainDestinationUntilRoutineScreenUpdatesIt() {
        assertFalse(
            shouldClearTrainDetailMode(
                isTrainDestination = true,
                isTopLevelDestination = true,
            ),
        )
    }
}
