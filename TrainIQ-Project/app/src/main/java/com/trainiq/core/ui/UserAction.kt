package com.trainiq.core.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal inline fun <T> runUserActionCatching(action: () -> T): Result<T> = try {
    Result.success(action())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}

/** UI writes may fail without terminating the app; cancellation remains owned by the caller. */
internal fun CoroutineScope.launchUserAction(
    onFailure: (Exception) -> Unit,
    action: suspend CoroutineScope.() -> Unit,
): Job = launch {
    try {
        action()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        onFailure(error)
    }
}

internal fun CoroutineScope.launchSingleSubmission(
    pending: MutableStateFlow<Boolean>,
    onFailure: (Exception) -> Unit,
    action: suspend () -> Unit,
): Boolean {
    if (pending.value) return false
    pending.value = true
    launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            currentCoroutineContext().ensureActive()
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            onFailure(error)
        } finally {
            pending.value = false
        }
    }
    return true
}
