package com.trainiq.features.nutrition

import com.trainiq.domain.repository.UnavailableMealItemException
import com.trainiq.domain.repository.InvalidMealItemException
import com.trainiq.domain.repository.MissingMealEditException
import com.trainiq.domain.repository.MissingRecipeEditException
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
        } catch (_: MissingMealEditException) {
            message("Deze maaltijd is verwijderd. Je concept blijft behouden. Stop met bewerken en voeg een nieuwe maaltijd toe.")
            return
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

internal fun recipeSaveFailureMessage(error: Throwable): String =
    if (error is MissingRecipeEditException) {
        "Dit recept is verwijderd. Je concept blijft behouden. Stop met bewerken en maak een nieuw recept aan."
    } else {
        "Recept opslaan mislukt. Controleer je invoer en probeer opnieuw."
    }
