package com.trainiq.features.workout

import com.trainiq.domain.model.WorkoutCompletionSummary
import com.trainiq.domain.model.WorkoutCompletionUiState
import com.trainiq.domain.model.WorkoutDebriefSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class WorkoutCompletionLoader(
    private val scope: CoroutineScope,
    private val read: suspend (Long) -> WorkoutCompletionSummary?,
) {
    private val state = MutableStateFlow<WorkoutCompletionUiState>(WorkoutCompletionUiState.Loading)
    val uiState = state.asStateFlow()
    private var job: Job? = null
    private var revision = 0L

    fun load(sessionId: Long) {
        val token = ++revision
        job?.cancel()
        state.value = WorkoutCompletionUiState.Loading
        job = scope.launch {
            try {
                val summary = read(sessionId)
                currentCoroutineContext().ensureActive()
                if (token != revision) return@launch
                if (summary == null) {
                    state.value = WorkoutCompletionUiState.Error("Deze afgeronde training kon niet worden geladen.")
                    return@launch
                }
                state.value = WorkoutCompletionUiState.Success(summary)
                if (summary.debrief.source != WorkoutDebriefSource.LOCAL_FALLBACK) return@launch
                repeat(6) {
                    delay(2_000L)
                    val refreshed = read(sessionId)
                    currentCoroutineContext().ensureActive()
                    if (token != revision) return@launch
                    if (refreshed != null) {
                        state.value = WorkoutCompletionUiState.Success(refreshed)
                        if (refreshed.debrief.source != WorkoutDebriefSource.LOCAL_FALLBACK) return@launch
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                if (token == revision && state.value !is WorkoutCompletionUiState.Success) {
                    state.value = WorkoutCompletionUiState.Error("Samenvatting laden mislukt. Probeer opnieuw.")
                }
            }
        }
    }
}
