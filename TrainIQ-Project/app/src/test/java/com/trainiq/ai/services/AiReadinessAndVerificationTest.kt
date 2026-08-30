package com.trainiq.ai.services

import com.trainiq.core.datastore.AiPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiReadinessAndVerificationTest {
    @Test
    fun readinessDistinguishesDisabledMissingKeyAndConfiguredProviders() {
        val disabled = AiPreferences(enabled = false, apiKey = "", openAiApiKey = "openai-key")
        val missingKey = AiPreferences(enabled = true, apiKey = "", geminiApiKey = "", openAiApiKey = "")
        val configured = AiPreferences(enabled = true, apiKey = "", geminiApiKey = "", openAiApiKey = "openai-key")

        assertEquals(AiReadiness.DISABLED, disabled.readiness())
        assertEquals(AiReadiness.NO_DECRYPTABLE_KEY, missingKey.readiness())
        assertEquals(AiReadiness.CONFIGURED, configured.readiness())
    }

    @Test
    fun verificationFailurePreservesHistoricalSuccessUntilKeyOrContractChanges() {
        val verified = recordOpenAiVerificationSuccess(
            previous = null,
            feature = AiFeature.GOAL_ADVICE,
            contractFingerprint = "contract-v1",
            checkedAtMillis = 100L,
        )
        val failed = recordOpenAiVerificationFailure(
            previous = verified,
            failure = AiProviderRequestException(
                provider = AiProvider.OPENAI,
                feature = AiFeature.WEEKLY_REPORT,
                category = AiFailureCategory.MODEL_ACCESS,
                httpStatus = 403,
                errorCode = "model_not_found",
                errorType = "invalid_request_error",
                requestId = "req_safe_123",
                cause = IllegalStateException("synthetic-secret prompt-private image-private"),
            ),
            contractFingerprint = "contract-v1",
            checkedAtMillis = 200L,
        )

        assertEquals(OpenAiVerificationOutcome.FAILED, failed.outcome)
        assertEquals(100L, failed.lastVerifiedAtMillis)
        assertEquals(AiFailureCategory.MODEL_ACCESS, failed.failureCategory)
        assertEquals("req_safe_123", failed.requestId)
        assertEquals(failed, failed.currentForContract("contract-v1"))
        assertNull(failed.currentForContract("contract-v2"))
    }

    @Test
    fun verificationSnapshotCodecRoundTripsOnlySafeAllowlistedMetadata() {
        val snapshot = recordOpenAiVerificationFailure(
            previous = null,
            failure = AiProviderRequestException(
                provider = AiProvider.OPENAI,
                feature = AiFeature.MEAL_SCAN,
                category = AiFailureCategory.ENDPOINT_PERMISSION,
                httpStatus = 403,
                errorCode = "missing_scope",
                errorType = "permission_error",
                requestId = "req_safe_456",
                cause = IllegalArgumentException("synthetic-secret prompt-private image-private"),
            ),
            contractFingerprint = "contract-v1",
            checkedAtMillis = 300L,
        )

        val encoded = OpenAiVerificationSnapshotCodec.encode(snapshot)
        val decoded = OpenAiVerificationSnapshotCodec.decode(encoded)

        assertEquals(snapshot, decoded)
        assertTrue(encoded.contains("missing_scope"))
        assertFalse(encoded.contains("synthetic-secret"))
        assertFalse(encoded.contains("prompt-private"))
        assertFalse(encoded.contains("image-private"))
        assertNull(OpenAiVerificationSnapshotCodec.decode("not-json"))
        assertNull(OpenAiVerificationSnapshotCodec.decode("{\"outcome\":\"FAILED\"}"))
    }
}
