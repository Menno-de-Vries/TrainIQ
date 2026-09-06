package com.trainiq.features.nutrition

import com.trainiq.domain.repository.UnavailableMealItemException
import com.trainiq.domain.repository.InvalidMealItemException
import kotlinx.coroutines.CancellationException

internal suspend fun performMealSave(
    save: suspend () -> Unit,
    onSaved: () -> Unit,
    message: (String) -> Unit,
    onFinished: () -> Unit,
) {
    try {
        try {
            save()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: UnavailableMealItemException) {
            message("Deze maaltijd bevat een verwijderd product of recept. Verwijder het item uit je concept en probeer opnieuw.")
            return
        } catch (_: InvalidMealItemException) {
            message("Deze maaltijd bevat een onvolledig item. Controleer de producten, recepten en hoeveelheden in je concept.")
            return
        } catch (_: Exception) {
            message("Maaltijd opslaan mislukt. Je concept blijft behouden. Probeer opnieuw.")
            return
        }
        message("Maaltijd opgeslagen.")
        onSaved()
    } finally {
        onFinished()
    }
}
