package com.trainiq.ai.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenAiVerificationRecorderTest {
    @Test
    fun recorderPersistsSuccessAndFailureThenClearsOnKeyChange() = runTest {
        val stored = MutableStateFlow<OpenAiVerificationSnapshot?>(null)
        var now = 100L
        val recorder = OpenAiVerificationRecorder(
            snapshots = stored,
            save = { stored.value = it },
            clear = { stored.value = null },
            contractFingerprint = { "contract-v1" },
            nowMillis = { now },
        )

        recorder.recordSuccess(AiFeature.GOAL_ADVICE)
        assertEquals(OpenAiVerificationOutcome.VERIFIED, recorder.current()?.outcome)
        assertEquals(100L, recorder.current()?.lastVerifiedAtMillis)

        now = 200L
        recorder.recordFailure(
            AiProviderRequestException(
                provider = AiProvider.OPENAI,
                feature = AiFeature.WEEKLY_REPORT,
                category = AiFailureCategory.QUOTA_BILLING,
                httpStatus = 429,
                errorCode = "insufficient_quota",
            ),
        )
        assertEquals(OpenAiVerificationOutcome.FAILED, recorder.current()?.outcome)
        assertEquals(100L, recorder.current()?.lastVerifiedAtMillis)

        recorder.clear()
        assertNull(recorder.current())
    }

    @Test
    fun recorderIgnoresSnapshotFromDifferentRequestContract() = runTest {
        val stored = MutableStateFlow<OpenAiVerificationSnapshot?>(
            recordOpenAiVerificationSuccess(null, AiFeature.MEAL_SCAN, "old-contract", 50L),
        )
        val recorder = OpenAiVerificationRecorder(
            snapshots = stored,
            save = { stored.value = it },
            clear = { stored.value = null },
            contractFingerprint = { "new-contract" },
            nowMillis = { 100L },
        )

        assertNull(recorder.current())
    }
}
