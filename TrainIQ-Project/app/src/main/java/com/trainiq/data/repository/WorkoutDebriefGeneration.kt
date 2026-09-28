package com.trainiq.data.repository

internal fun workoutDebriefGenerationMatches(currentGeneration: String, expectedGeneration: String?): Boolean =
    currentGeneration.isNotBlank() && !expectedGeneration.isNullOrBlank() && currentGeneration == expectedGeneration

internal suspend fun <T> runWorkoutDebriefForCurrentGeneration(
    currentGeneration: String,
    expectedGeneration: String?,
    isGenerationCurrent: suspend () -> Boolean = { true },
    generate: suspend () -> T,
): T? = if (
    workoutDebriefGenerationMatches(currentGeneration, expectedGeneration) && isGenerationCurrent()
) {
    generate()
} else {
    null
}

internal suspend fun cancelWorkoutDebriefBeforeSessionDelete(
    sessionId: Long,
    generationId: String?,
    cancel: suspend (Long, String) -> Unit,
    delete: suspend (Long) -> Unit,
) {
    generationId?.takeIf(String::isNotBlank)?.let { cancel(sessionId, it) }
    delete(sessionId)
}
