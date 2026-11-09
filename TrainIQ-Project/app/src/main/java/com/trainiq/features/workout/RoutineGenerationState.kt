package com.trainiq.features.workout

import com.trainiq.ai.services.toAiUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class RoutineGenerationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

/** One request at a time; failure belongs to the generator rather than the screen snackbar. */
internal class RoutineGenerationState(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(RoutineGenerationUiState())
    val state = _state.asStateFlow()

    fun submit(generate: suspend () -> Unit): Boolean {
        if (_state.value.isLoading) return false
        _state.value = RoutineGenerationUiState(isLoading = true)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                currentCoroutineContext().ensureActive()
                generate()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                _state.value = _state.value.copy(error = failure.toAiUserMessage("Routine genereren is mislukt."))
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
        return true
    }

    fun dismissError() {
        if (!_state.value.isLoading) _state.value = RoutineGenerationUiState()
    }
}
