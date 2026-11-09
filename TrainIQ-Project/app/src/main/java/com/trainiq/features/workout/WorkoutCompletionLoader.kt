package com.trainiq.features.workout

import com.trainiq.domain.model.WorkoutCompletionSummary
import com.trainiq.domain.model.WorkoutCompletionUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class WorkoutCompletionLoader(
    private val scope: CoroutineScope,
    private val observe: (Long) -> Flow<WorkoutCompletionSummary?>,
) {
    private val state = MutableStateFlow<WorkoutCompletionUiState>(WorkoutCompletionUiState.Loading)
    val uiState = state.asStateFlow()
    private var job: Job? = null
    private var revision = 0L
    private var sessionId: Long? = null

    init {
        scope.launch {
            // collectAsStateWithLifecycle removes the subscriber when the route stops or leaves composition.
            state.subscriptionCount.map { it > 0 }.distinctUntilChanged().collect { observed ->
                if (observed) {
                    sessionId?.let(::startObservation)
                } else {
                    revision++
                    job?.cancel()
                    job = null
                }
            }
        }
    }

    fun load(sessionId: Long) {
        this.sessionId = sessionId
        revision++
        job?.cancel()
        state.value = WorkoutCompletionUiState.Loading
        if (state.subscriptionCount.value > 0) startObservation(sessionId)
    }

    private fun startObservation(sessionId: Long) {
        val token = ++revision
        job?.cancel()
        job = scope.launch {
            try {
                observe(sessionId).collect { summary ->
                    currentCoroutineContext().ensureActive()
                    if (token == revision) {
                        state.value = if (summary == null) {
                            WorkoutCompletionUiState.Error("Deze afgeronde training kon niet worden geladen.")
                        } else {
                            WorkoutCompletionUiState.Success(summary)
                        }
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
