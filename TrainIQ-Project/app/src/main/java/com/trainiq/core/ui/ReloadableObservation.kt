package com.trainiq.core.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> reloadableObservation(
    reloads: Flow<Int>,
    observe: () -> Flow<T>,
): Flow<Result<T>?> = reloads.flatMapLatest {
    observe()
        .map<T, Result<T>?> { Result.success(it) }
        .onStart { emit(null) }
        .catch { emit(Result.failure(it)) }
}
