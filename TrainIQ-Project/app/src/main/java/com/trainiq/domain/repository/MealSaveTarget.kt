package com.trainiq.domain.repository

/** Creation reservations survive retry/recreation; an edit must refer to an existing meal. */
sealed interface MealSaveTarget {
    data class Create(val reservedId: Long? = null) : MealSaveTarget {
        init { require(reservedId == null || reservedId > 0L) }
    }
    data class Edit(val id: Long) : MealSaveTarget {
        init { require(id > 0L) }
    }
}

internal fun mealSaveTargetForExistingId(id: Long?): MealSaveTarget =
    id?.takeIf { it > 0L }?.let(MealSaveTarget::Edit) ?: MealSaveTarget.Create()

class MissingMealEditException : IllegalArgumentException("Deze maaltijd bestaat niet meer. Maak een nieuwe maaltijd aan.")
class MissingRecipeEditException : IllegalArgumentException("Dit recept bestaat niet meer. Maak een nieuw recept aan.")
