package com.trainiq.core.ui

sealed interface ScreenUiState<out T> {
    data object Loading : ScreenUiState<Nothing>
    data class Success<T>(val content: T, val message: UiMessage? = null) : ScreenUiState<T>
    data class Error(
        val title: String,
        val message: String,
        val canRetry: Boolean = true,
    ) : ScreenUiState<Nothing>
}

data class UiMessage(
    val text: String,
)
