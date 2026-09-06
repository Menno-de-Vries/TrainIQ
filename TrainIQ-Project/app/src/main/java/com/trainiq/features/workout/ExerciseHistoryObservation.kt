package com.trainiq.features.workout

import com.trainiq.core.ui.ScreenUiState
import com.trainiq.core.ui.reloadableObservation
import com.trainiq.domain.model.ExerciseHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

internal class ExerciseHistoryObservation(scope: CoroutineScope, observe: () -> Flow<ExerciseHistory>) {
    private val reloads = MutableStateFlow(0)
    val uiState = reloadableObservation(reloads, observe).map { result ->
        when {
            result == null -> ScreenUiState.Loading
            result.isFailure -> ScreenUiState.Error("Geschiedenis niet beschikbaar", "Oefengeschiedenis kon niet worden geladen.")
            else -> ScreenUiState.Success(result.getOrThrow())
        }
    }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), ScreenUiState.Loading)
    fun retry() { reloads.update { it + 1 } }
}
