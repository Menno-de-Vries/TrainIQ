package com.trainiq.features.nutrition

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Keep the caller's pending guard closed for the whole input task, including when
// storage completes without suspending. Main.immediate can finish inline.
internal fun CoroutineScope.launchNutritionSubmit(block: suspend CoroutineScope.() -> Unit) =
    launch(Dispatchers.Main, block = block)

internal suspend fun <T> performNutritionWrite(
    save: suspend () -> T,
    onSaved: (T) -> Unit,
    onFailure: (Throwable) -> Unit,
    onFinished: () -> Unit,
) {
    try {
        onSaved(save())
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        onFailure(error)
    } finally {
        onFinished()
    }
}
