package com.trainiq.features.nutrition

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NutritionSubmitDispatchInstrumentedTest {
    @Test fun immediateWritesKeepAdmissionClosedUntilTheNextMainThreadTask() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        var pending = false
        var writes = 0
        var successes = 0
        var failures = 0
        var failNext = false
        fun submit() {
            if (pending) return
            pending = true
            scope.launchNutritionSubmit {
                performNutritionWrite(
                    save = {
                        writes++
                        if (failNext) error("synthetic immediate write failure")
                    },
                    onSaved = { successes++ },
                    onFailure = { failures++ },
                    onFinished = { pending = false },
                )
            }
        }
        try {
            instrumentation.runOnMainSync {
                submit()
                submit()
                assertEquals("Writes must be queued even when they never suspend", 0, writes)
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertEquals(1, writes)
                assertEquals(1, successes)
                assertFalse(pending)
                failNext = true
                submit()
                submit()
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertEquals(2, writes)
                assertEquals(1, failures)
                assertFalse(pending)
                failNext = false
                submit()
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertEquals(3, writes)
                assertEquals(2, successes)
                assertFalse(pending)
            }
        } finally {
            scope.cancel()
        }
    }
}
