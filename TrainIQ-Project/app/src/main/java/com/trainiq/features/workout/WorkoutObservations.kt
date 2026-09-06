package com.trainiq.features.workout

import com.trainiq.core.datastore.WorkoutFeedbackPreferences
import com.trainiq.core.ui.ScreenUiState
import com.trainiq.core.ui.reloadableObservation
import com.trainiq.domain.model.WorkoutOverview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class WorkoutObservations(
    scope: CoroutineScope,
    observeOverview: () -> Flow<WorkoutOverview>,
    observePreferences: () -> Flow<WorkoutFeedbackPreferences>,
) {
    private val reloads = MutableStateFlow(0)
    val overview = reloadableObservation(reloads, observeOverview)
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)
    val preferences = reloadableObservation(reloads, observePreferences)
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)
    val error = combine(overview, preferences) { current, feedback ->
        when {
            current?.isFailure == true -> ScreenUiState.Error("Training niet beschikbaar", "Trainingsgegevens konden niet worden geladen.")
            feedback?.isFailure == true -> ScreenUiState.Error("Training niet beschikbaar", "Trainingsvoorkeuren konden niet worden geladen.")
            else -> null
        }
    }

    fun retry() { reloads.update { it + 1 } }
}
