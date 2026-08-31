package com.trainiq.ai.services

import com.google.gson.Gson
import com.trainiq.BuildConfig
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class AiReadiness {
    DISABLED,
    NO_DECRYPTABLE_KEY,
    CONFIGURED,
}

enum class OpenAiVerificationOutcome {
    VERIFIED,
    FAILED,
}

data class OpenAiVerificationSnapshot(
    val outcome: OpenAiVerificationOutcome,
    val contractFingerprint: String,
    val feature: AiFeature,
    val checkedAtMillis: Long,
    val lastVerifiedAtMillis: Long? = null,
    val failureCategory: AiFailureCategory? = null,
    val httpStatus: Int? = null,
    val errorCode: String? = null,
    val errorType: String? = null,
    val requestId: String? = null,
)

internal fun AiPreferences.readiness(): AiReadiness = when {
    !enabled -> AiReadiness.DISABLED
    geminiApiKey.isBlank() && openAiApiKey.isBlank() -> AiReadiness.NO_DECRYPTABLE_KEY
    else -> AiReadiness.CONFIGURED
}

internal fun recordOpenAiVerificationSuccess(
    previous: OpenAiVerificationSnapshot?,
    feature: AiFeature,
    contractFingerprint: String,
    checkedAtMillis: Long,
): OpenAiVerificationSnapshot = OpenAiVerificationSnapshot(
    outcome = OpenAiVerificationOutcome.VERIFIED,
    contractFingerprint = contractFingerprint,
    feature = feature,
    checkedAtMillis = checkedAtMillis.coerceAtLeast(0L),
    lastVerifiedAtMillis = checkedAtMillis.coerceAtLeast(0L),
)

internal fun recordOpenAiVerificationFailure(
    previous: OpenAiVerificationSnapshot?,
    failure: AiProviderRequestException,
    contractFingerprint: String,
    checkedAtMillis: Long,
): OpenAiVerificationSnapshot = OpenAiVerificationSnapshot(
    outcome = OpenAiVerificationOutcome.FAILED,
    contractFingerprint = contractFingerprint,
    feature = failure.feature,
    checkedAtMillis = checkedAtMillis.coerceAtLeast(0L),
    lastVerifiedAtMillis = previous
        ?.currentForContract(contractFingerprint)
        ?.lastVerifiedAtMillis,
    failureCategory = failure.category,
    httpStatus = failure.httpStatus?.takeIf { it in 100..599 },
    errorCode = sanitizeVerificationMetadata(failure.errorCode),
    errorType = sanitizeVerificationMetadata(failure.errorType),
    requestId = sanitizeVerificationMetadata(failure.requestId),
)

internal fun OpenAiVerificationSnapshot.currentForContract(
    expectedFingerprint: String,
): OpenAiVerificationSnapshot? = takeIf { contractFingerprint == expectedFingerprint }

internal val OpenAiContractFingerprint: String by lazy {
    val contract = listOf(
        BuildConfig.OPENAI_BASE_URL,
        "v1/responses",
        OpenAiModelSelectionPolicy.fingerprint,
        "responses-json-schema-v1",
    ).joinToString("|")
    MessageDigest.getInstance("SHA-256")
        .digest(contract.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}

internal object OpenAiVerificationSnapshotCodec {
    private val gson = Gson()

    fun encode(snapshot: OpenAiVerificationSnapshot): String = gson.toJson(
        PersistedOpenAiVerificationSnapshot(
            outcome = snapshot.outcome.name,
            contractFingerprint = snapshot.contractFingerprint,
            feature = snapshot.feature.name,
            checkedAtMillis = snapshot.checkedAtMillis,
            lastVerifiedAtMillis = snapshot.lastVerifiedAtMillis,
            failureCategory = snapshot.failureCategory?.name,
            httpStatus = snapshot.httpStatus,
            errorCode = snapshot.errorCode,
            errorType = snapshot.errorType,
            requestId = snapshot.requestId,
        ),
    )

    fun decode(raw: String?): OpenAiVerificationSnapshot? {
        if (raw.isNullOrBlank() || raw.length > MaxVerificationSnapshotChars) return null
        val persisted = runCatching {
            gson.fromJson(raw, PersistedOpenAiVerificationSnapshot::class.java)
        }.getOrNull() ?: return null
        val outcome = persisted.outcome?.let { rawOutcome ->
            OpenAiVerificationOutcome.entries.firstOrNull { it.name == rawOutcome }
        } ?: return null
        val feature = persisted.feature?.let { rawFeature ->
            AiFeature.entries.firstOrNull { it.name == rawFeature }
        } ?: return null
        val failureCategory = persisted.failureCategory?.let { rawCategory ->
            AiFailureCategory.entries.firstOrNull { it.name == rawCategory }
        }
        val fingerprint = persisted.contractFingerprint
            ?.takeIf { it.isNotBlank() && it.length <= MaxVerificationMetadataChars }
            ?: return null
        val checkedAtMillis = persisted.checkedAtMillis?.takeIf { it >= 0L } ?: return null
        if (persisted.lastVerifiedAtMillis?.let { it < 0L } == true) return null
        return OpenAiVerificationSnapshot(
            outcome = outcome,
            contractFingerprint = fingerprint,
            feature = feature,
            checkedAtMillis = checkedAtMillis,
            lastVerifiedAtMillis = persisted.lastVerifiedAtMillis,
            failureCategory = failureCategory,
            httpStatus = persisted.httpStatus?.takeIf { it in 100..599 },
            errorCode = sanitizeVerificationMetadata(persisted.errorCode),
            errorType = sanitizeVerificationMetadata(persisted.errorType),
            requestId = sanitizeVerificationMetadata(persisted.requestId),
        )
    }

    private data class PersistedOpenAiVerificationSnapshot(
        val outcome: String? = null,
        val contractFingerprint: String? = null,
        val feature: String? = null,
        val checkedAtMillis: Long? = null,
        val lastVerifiedAtMillis: Long? = null,
        val failureCategory: String? = null,
        val httpStatus: Int? = null,
        val errorCode: String? = null,
        val errorType: String? = null,
        val requestId: String? = null,
    )
}

@Singleton
class OpenAiVerificationRecorder internal constructor(
    snapshots: Flow<OpenAiVerificationSnapshot?>,
    private val save: suspend (OpenAiVerificationSnapshot) -> Unit,
    clear: suspend () -> Unit,
    private val contractFingerprint: () -> String,
    private val nowMillis: () -> Long,
) {
    private val storedSnapshots = snapshots
    private val clearStored = clear

    @Inject
    constructor(repository: UserPreferencesRepository) : this(
        snapshots = repository.openAiVerificationSnapshot,
        save = repository::saveOpenAiVerificationSnapshot,
        clear = repository::clearOpenAiVerificationSnapshot,
        contractFingerprint = { OpenAiContractFingerprint },
        nowMillis = System::currentTimeMillis,
    )

    val snapshots: Flow<OpenAiVerificationSnapshot?> = storedSnapshots.map { snapshot ->
        snapshot?.currentForContract(contractFingerprint())
    }

    suspend fun current(): OpenAiVerificationSnapshot? = snapshots.first()

    suspend fun recordSuccess(feature: AiFeature) {
        save(
            recordOpenAiVerificationSuccess(
                previous = current(),
                feature = feature,
                contractFingerprint = contractFingerprint(),
                checkedAtMillis = nowMillis(),
            ),
        )
    }

    internal suspend fun recordFailure(failure: AiProviderRequestException) {
        save(
            recordOpenAiVerificationFailure(
                previous = current(),
                failure = failure,
                contractFingerprint = contractFingerprint(),
                checkedAtMillis = nowMillis(),
            ),
        )
    }

    suspend fun clear() {
        clearStored()
    }
}

private fun sanitizeVerificationMetadata(value: String?): String? =
    value?.trim()
        ?.take(MaxVerificationMetadataChars)
        ?.map { char -> if (char.isLetterOrDigit() || char in "-_.:") char else '_' }
        ?.joinToString("")
        ?.takeIf { it.isNotBlank() }

private const val MaxVerificationMetadataChars = 128
private const val MaxVerificationSnapshotChars = 4_096
