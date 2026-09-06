package com.trainiq.features.nutrition

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
