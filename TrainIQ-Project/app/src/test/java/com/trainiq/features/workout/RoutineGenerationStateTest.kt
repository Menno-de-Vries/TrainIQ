package com.trainiq.features.workout

import com.trainiq.ai.services.AiFailureCategory
import com.trainiq.ai.services.AiFeature
import com.trainiq.ai.services.AiProvider
import com.trainiq.ai.services.AiProviderRequestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RoutineGenerationStateTest {
    @Test fun failureRemainsVisibleUntilRetryOrDismissAndDoubleSubmissionIsIgnored() = runTest {
        val generation = RoutineGenerationState(this)
        val release = CompletableDeferred<Unit>()
        var calls = 0
        assertTrue(generation.submit { calls++; release.await(); throw AiProviderRequestException(AiProvider.GEMINI, AiFeature.ROUTINE_GENERATION, AiFailureCategory.INCOMPLETE_RESPONSE) })
        assertTrue(generation.state.value.isLoading)
        assertFalse(generation.submit { calls++ })
        assertEquals(1, calls)
        release.complete(Unit)
        testScheduler.runCurrent()
        assertFalse(generation.state.value.isLoading)
        assertNotNull(generation.state.value.error)
        val retry = CompletableDeferred<Unit>()
        assertTrue(generation.submit { calls++; retry.await() })
        assertNull(generation.state.value.error)
        retry.complete(Unit)
        testScheduler.runCurrent()
        assertEquals(RoutineGenerationUiState(), generation.state.value)
        generation.submit { throw IllegalStateException("private input") }
        assertNotNull(generation.state.value.error)
        assertFalse(generation.state.value.error.orEmpty().contains("private"))
        generation.dismissError()
        assertNull(generation.state.value.error)
        assertEquals(2, calls)
    }

    @Test fun cancellationIsNotAnErrorAndReleasesSubmissionGuard() = runTest {
        val generation = RoutineGenerationState(this)
        generation.submit { throw CancellationException("fixture") }
        assertEquals(RoutineGenerationUiState(), generation.state.value)
        assertTrue(generation.submit {})
    }
}
