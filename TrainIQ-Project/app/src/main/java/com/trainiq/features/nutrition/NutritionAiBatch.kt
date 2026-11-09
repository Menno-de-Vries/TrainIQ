package com.trainiq.features.nutrition

import com.trainiq.domain.model.FoodItem
import com.trainiq.domain.repository.MealEntryRequest
import com.trainiq.domain.repository.MealEntrySnapshot
import com.trainiq.domain.repository.MealEntryType
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiFoodBatchRequest(
    val reviewId: String,
    val target: NutritionScanDestination,
    val targetToken: String,
    val context: String,
    val items: List<EditableAiItem>,
)

data class AiFoodIntegration(
    val id: String,
    val target: NutritionScanDestination,
    val targetToken: String,
    val grams: Double,
    val food: FoodItem? = null,
    val snapshot: MealEntryRequest? = null,
)

data class AiFoodBatchCompletion(val id: String, val target: NutritionScanDestination)

data class AiFoodBatchState(
    val saving: Boolean = false,
    val savedRowIds: Set<String> = emptySet(),
    val integrations: List<AiFoodIntegration> = emptyList(),
    val pendingIntegrationIds: Set<String> = emptySet(),
    val completion: AiFoodBatchCompletion? = null,
    val message: String? = null,
) {
    val busy: Boolean get() = saving || pendingIntegrationIds.isNotEmpty() || completion != null
}

internal data class ValidatedAiFood(
    val row: EditableAiItem,
    val grams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
) {
    fun snapshot() = MealEntryRequest(
        itemType = MealEntryType.SNAPSHOT,
        referenceId = 0L,
        gramsUsed = grams,
        notes = row.notes,
        snapshot = MealEntrySnapshot(row.name.trim(), caloriesPer100g * grams / 100.0,
            proteinPer100g * grams / 100.0, carbsPer100g * grams / 100.0, fatPer100g * grams / 100.0),
    )
}

internal fun validatedAiFood(item: EditableAiItem): ValidatedAiFood? {
    if (validateEditableAiItem(item).hasErrors) return null
    val grams = item.grams.toNutritionNumberOrNull(100_000.0)?.takeIf { it > 0.0 } ?: return null
    fun per100(value: String) = value.toNutritionNumberOrNull(100_000.0)?.times(100.0)?.div(grams)
    val calories = per100(item.calories)?.takeIf { it in 0.0..5000.0 } ?: return null
    val protein = per100(item.protein)?.takeIf { it in 0.0..1000.0 } ?: return null
    val carbs = per100(item.carbs)?.takeIf { it in 0.0..1000.0 } ?: return null
    val fat = per100(item.fat)?.takeIf { it in 0.0..1000.0 } ?: return null
    return ValidatedAiFood(item, grams, calories, protein, carbs, fat)
}

/** Retained by the real Nutrition entry VM; no composable callback survives a disk suspension. */
internal class NutritionAiBatch(
    private val scope: CoroutineScope,
    private val save: suspend (ValidatedAiFood) -> FoodItem,
) {
    private val _state = MutableStateFlow(AiFoodBatchState())
    val state = _state.asStateFlow()
    private var reviewId: String? = null

    fun beginReview(id: String): Boolean {
        if (_state.value.busy) return false
        if (id != reviewId) {
            reviewId = id
            _state.value = AiFoodBatchState()
        }
        return true
    }

    fun submit(request: AiFoodBatchRequest): Boolean {
        if (_state.value.busy || request.reviewId != reviewId || request.items.isEmpty() || request.items.size > 20) return false
        val rows = request.items.toList()
        if (rows.map { it.rowId }.distinct().size != rows.size || rows.any { it.rowId in _state.value.savedRowIds }) return false
        val validated = rows.map { validatedAiFood(it) ?: run {
            _state.value = _state.value.copy(message = "Controleer alle porties en voedingswaarden voordat je toevoegt.")
            return false
        } }
        if (aiItemsExceedDeclaredMealTotal(request.context, rows)) {
            _state.value = _state.value.copy(message = "De hoeveelheden overschrijden je opgegeven totaal. Controleer de porties.")
            return false
        }
        _state.value = _state.value.copy(saving = true, message = null)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            var succeeded = 0
            var failed = 0
            try {
                currentCoroutineContext().ensureActive()
                for (item in validated) {
                    try {
                        val food = if (request.target == NutritionScanDestination.MealDraft) null else save(item)
                        // Record a confirmed write immediately, before another suspension/cancellation point.
                        val integration = when (request.target) {
                            NutritionScanDestination.ProductLibrary -> null
                            NutritionScanDestination.RecipeDraft -> AiFoodIntegration(item.row.rowId, request.target,
                                request.targetToken, item.grams, food = food)
                            NutritionScanDestination.MealDraft -> AiFoodIntegration(item.row.rowId, request.target,
                                request.targetToken, item.grams, snapshot = item.snapshot())
                        }
                        _state.value = _state.value.copy(
                            savedRowIds = _state.value.savedRowIds + item.row.rowId,
                            integrations = _state.value.integrations + listOfNotNull(integration),
                            pendingIntegrationIds = _state.value.pendingIntegrationIds + listOfNotNull(integration?.id),
                        )
                        succeeded++
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        failed++
                    }
                }
                _state.value = _state.value.copy(
                    completion = if (failed == 0) AiFoodBatchCompletion(UUID.randomUUID().toString(), request.target) else null,
                    message = if (failed == 0) "$succeeded AI-items verwerkt." else "$succeeded AI-items verwerkt, $failed mislukt. Controleer de overgebleven items en probeer opnieuw.",
                )
            } catch (cancelled: CancellationException) {
                _state.value = _state.value.copy(message = "Opslaan onderbroken. Controleer de overgebleven items voordat je opnieuw probeert.")
                throw cancelled
            } finally {
                _state.value = _state.value.copy(saving = false)
            }
        }
        return true
    }

    fun acknowledgeIntegration(id: String, targetToken: String): Boolean {
        val current = _state.value.integrations.firstOrNull { it.id == id && it.targetToken == targetToken } ?: return false
        if (id !in _state.value.pendingIntegrationIds) return false
        _state.value = _state.value.copy(pendingIntegrationIds = _state.value.pendingIntegrationIds - current.id)
        return true
    }

    fun acknowledgeCompletion(id: String): Boolean {
        if (_state.value.pendingIntegrationIds.isNotEmpty() || _state.value.completion?.id != id) return false
        _state.value = _state.value.copy(completion = null)
        return true
    }

    fun rejectIntegration(id: String): Boolean {
        val integration = _state.value.integrations.firstOrNull { it.id == id } ?: return false
        _state.value = _state.value.copy(integrations = _state.value.integrations - integration,
            pendingIntegrationIds = _state.value.pendingIntegrationIds - integration.id, completion = null,
            savedRowIds = if (integration.target == NutritionScanDestination.MealDraft) _state.value.savedRowIds - id else _state.value.savedRowIds,
            message = if (integration.target == NutritionScanDestination.MealDraft)
                "Deze AI-items zijn niet aan het gewijzigde maaltijdconcept toegevoegd. Kies het bedoelde concept en probeer opnieuw."
                else "Het product is opgeslagen, maar niet aan het gewijzigde concept toegevoegd. Kies het opgeslagen product handmatig.")
        return true
    }
}
