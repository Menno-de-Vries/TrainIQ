package com.trainiq.features.nutrition

import com.trainiq.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class NutritionAiBatchTest {
    private fun row(id: String, grams: String = "150") = EditableAiItem("Corrected $id", grams, "150", "15", "20", "3", null, null, id)
    private fun food(item: ValidatedAiFood) = FoodItem(if (item.row.rowId == "A") 1L else 2L,
        item.row.name, caloriesPer100g = item.caloriesPer100g, proteinPer100g = item.proteinPer100g,
        carbsPer100g = item.carbsPer100g, fatPer100g = item.fatPer100g, sourceType = FoodSourceType.AI, createdAt = 1L, updatedAt = 1L)

    @Test fun partialRetryUsesWholeReviewTotalAndExplicitContextCorrectionDoesNotReplaySuccess() = runTest {
        val writes = mutableListOf<String>()
        var failB = true
        val batch = NutritionAiBatch(this) { input ->
            writes += input.row.rowId
            if (input.row.rowId == "B" && failB) error("synthetic failure")
            food(input)
        }
        batch.beginReview("scan")
        val request = AiFoodBatchRequest("scan", NutritionScanDestination.ProductLibrary, "products", "totaal 200 g", listOf(row("A", "100"), row("B", "100")))
        assertTrue(batch.submit(request))
        assertEquals(setOf("A"), batch.state.value.savedRowIds)
        val partial = batch.state.value
        assertEquals(mapOf("A" to 100.0), partial.successfulGramsByRow)
        failB = false
        val corrected = request.copy(items = listOf(row("B", "150")))
        assertFalse("A100 plus corrected B150 exceeds the same review's 200g budget", batch.submit(corrected))
        assertEquals(listOf("A", "B"), writes)
        assertEquals("150", corrected.items.single().grams)
        assertFalse(batch.state.value.busy)
        assertTrue(batch.state.value.message.orEmpty().contains("overschrijden"))
        assertTrue(batch.submit(corrected.copy(context = "totaal 250 g")))
        assertEquals(listOf("A", "B", "B"), writes)
        assertEquals(mapOf("A" to 100.0), partial.successfulGramsByRow)
        assertTrue(batch.acknowledgeCompletion(batch.state.value.completion!!.id))
        assertEquals(250.0, batch.state.value.confirmedGrams, 0.0)
        assertFalse(batch.submit(request.copy(context = "totaal 300 g")))
        assertTrue(batch.beginReview("new scan"))
        assertEquals(0.0, batch.state.value.confirmedGrams, 0.0)
        assertTrue(batch.submit(corrected.copy(reviewId = "new scan", context = "totaal 150 g")))
        assertEquals(listOf("A", "B", "B", "B"), writes)
    }

    @Test fun totalToleranceIncludesPriorSuccessAtExactOnePercentBoundary() = runTest {
        val writes = mutableListOf<String>()
        val batch = NutritionAiBatch(this) { writes += it.row.rowId; food(it) }
        val first = AiFoodBatchRequest("scan", NutritionScanDestination.ProductLibrary, "products", "totaal 200 g", listOf(row("A", "100")))
        batch.beginReview("scan")
        batch.submit(first)
        batch.acknowledgeCompletion(batch.state.value.completion!!.id)
        assertTrue(batch.submit(first.copy(items = listOf(row("B", "102")))))
        batch.acknowledgeCompletion(batch.state.value.completion!!.id)
        batch.beginReview("new scan")
        batch.submit(first.copy(reviewId = "new scan"))
        batch.acknowledgeCompletion(batch.state.value.completion!!.id)
        assertFalse(batch.submit(first.copy(reviewId = "new scan", items = listOf(row("B", "102.001")))))
        assertEquals(listOf("A", "B", "A"), writes)
        assertFalse(batch.state.value.busy)
    }

    @Test fun rejectedRecipeLinkKeepsStoredBudgetButRejectedMealTransferReleasesIt() = runTest {
        val writes = mutableListOf<String>()
        val batch = NutritionAiBatch(this) { writes += it.row.rowId; food(it) }
        val request = AiFoodBatchRequest("scan", NutritionScanDestination.RecipeDraft, "recipe", "totaal 200 g", listOf(row("A", "100")))
        batch.beginReview("scan")
        batch.submit(request)
        assertTrue(batch.rejectIntegration(batch.state.value.integrations.single().id))
        assertEquals(100.0, batch.state.value.confirmedGrams, 0.0)
        assertFalse(batch.submit(request.copy(items = listOf(row("B", "150")))))
        assertEquals(listOf("A"), writes)
        assertFalse(batch.state.value.busy)
        batch.beginReview("meal scan")
        val meal = request.copy(reviewId = "meal scan", target = NutritionScanDestination.MealDraft,
            targetToken = "meal", context = "totaal 100 g")
        batch.submit(meal)
        batch.rejectIntegration(batch.state.value.integrations.single().id)
        assertEquals(0.0, batch.state.value.confirmedGrams, 0.0)
        assertTrue(batch.submit(meal.copy(items = listOf(row("B", "100")))))
        val integration = batch.state.value.integrations.single()
        assertTrue(batch.acknowledgeIntegration(integration.id, meal.targetToken))
        batch.acknowledgeCompletion(batch.state.value.completion!!.id)
        assertEquals(100.0, batch.state.value.confirmedGrams, 0.0)
        assertFalse(batch.submit(meal))
        assertEquals(listOf("A"), writes)
    }

    @Test fun cancellationAfterConfirmedSuccessKeepsItsBudgetForRetry() = runTest {
        val writes = mutableListOf<String>()
        val batch = NutritionAiBatch(this) { input ->
            writes += input.row.rowId
            if (input.row.rowId == "B") throw CancellationException("synthetic cancellation")
            food(input)
        }
        batch.beginReview("scan")
        val request = AiFoodBatchRequest("scan", NutritionScanDestination.ProductLibrary, "products", "totaal 200 g", listOf(row("A", "100"), row("B", "100")))
        batch.submit(request)
        assertEquals(setOf("A"), batch.state.value.savedRowIds)
        assertFalse(batch.state.value.busy)
        assertFalse(batch.submit(request.copy(items = listOf(row("B", "150")))))
        assertEquals(listOf("A", "B"), writes)
    }

    @Test fun partialFailureRetainsCorrectedRowsAndRetriesOnlyRemainingOnce() = runTest {
        val barrier = CompletableDeferred<Unit>()
        val writes = mutableListOf<ValidatedAiFood>()
        var failB = true
        val batch = NutritionAiBatch(this) { item ->
            writes += item
            if (item.row.rowId == "A") barrier.await()
            if (item.row.rowId == "B" && failB) error("private disk error")
            food(item)
        }
        batch.beginReview("scan")
        val a = row("A"); val b = row("B", "180")
        val request = AiFoodBatchRequest("scan", NutritionScanDestination.ProductLibrary, "products", "", listOf(a, b))
        assertTrue(batch.submit(request))
        assertFalse(batch.submit(request))
        assertFalse(batch.beginReview("new scan"))
        barrier.complete(Unit); testScheduler.runCurrent()
        assertEquals(setOf("A"), batch.state.value.savedRowIds)
        assertFalse(batch.state.value.busy)
        assertTrue(batch.state.value.message.orEmpty().contains("1 mislukt"))
        assertFalse(batch.state.value.message.orEmpty().contains("private"))
        assertFalse(batch.submit(request))
        failB = false
        assertTrue(batch.submit(request.copy(items = listOf(b))))
        assertEquals(listOf("A", "B", "B"), writes.map { it.row.rowId })
        assertEquals("180", writes.last().row.grams)
        assertEquals(150.0 / 180.0 * 100.0, writes.last().caloriesPer100g, 0.0001)
        val completion = batch.state.value.completion!!
        assertTrue(batch.acknowledgeCompletion(completion.id))
        assertFalse(batch.state.value.busy)
    }

    @Test fun invalidInputWritesNothingAndCancellationReleasesGuard() = runTest {
        var calls = 0
        val batch = NutritionAiBatch(this) { calls++; throw CancellationException("fixture") }
        batch.beginReview("scan")
        val request = AiFoodBatchRequest("scan", NutritionScanDestination.ProductLibrary, "products", "", listOf(row("A", "0")))
        assertFalse(batch.submit(request)); assertEquals(0, calls); assertFalse(batch.state.value.busy)
        assertTrue(batch.submit(request.copy(items = listOf(row("A")))))
        assertFalse(batch.state.value.busy)
        assertTrue(batch.state.value.savedRowIds.isEmpty())
    }

    @Test fun recipeSuccessRequiresMatchingSingleIntegrationAndMismatchHasSafeRecovery() = runTest {
        var calls = 0
        val batch = NutritionAiBatch(this) { calls++; food(it) }
        batch.beginReview("scan")
        val request = AiFoodBatchRequest("scan", NutritionScanDestination.RecipeDraft, "original recipe", "", listOf(row("A")))
        batch.submit(request)
        val integration = batch.state.value.integrations.single()
        assertFalse(batch.acknowledgeIntegration(integration.id, "different recipe"))
        assertTrue(batch.rejectIntegration(integration.id))
        assertFalse(batch.state.value.busy)
        assertFalse(batch.submit(request))
        assertEquals(1, calls)
        assertTrue(batch.state.value.message.orEmpty().contains("handmatig"))
        assertTrue(batch.beginReview("next"))
        batch.submit(request.copy(reviewId = "next", items = listOf(row("B"))))
        val next = batch.state.value.integrations.single()
        assertTrue(batch.acknowledgeIntegration(next.id, request.targetToken))
        assertFalse(batch.acknowledgeIntegration(next.id, request.targetToken))
        assertEquals(next, batch.state.value.integrations.single())
        assertTrue(batch.acknowledgeCompletion(batch.state.value.completion!!.id))
        assertFalse(batch.state.value.busy)
    }

    @Test fun oversizedAndDuplicateRowsCannotStartOrLeavePending() = runTest {
        var writes = 0
        val batch = NutritionAiBatch(this) { writes++; food(it) }
        batch.beginReview("scan")
        val request = AiFoodBatchRequest("scan", NutritionScanDestination.ProductLibrary, "products", "", List(21) { row(it.toString()) })
        assertFalse(batch.submit(request))
        assertFalse(batch.submit(request.copy(items = listOf(row("A"), row("A")))))
        assertEquals(0, writes)
        assertFalse(batch.state.value.busy)
    }

    @Test fun mealTransferUsesSnapshotsWithoutProductWriteAndPreservesCorrectedTotals() = runTest {
        val batch = NutritionAiBatch(this) { error("Meal review must not create products") }
        batch.beginReview("scan")
        batch.submit(AiFoodBatchRequest("scan", NutritionScanDestination.MealDraft, "meal:7", "", listOf(row("A", "180"))))
        val integration = batch.state.value.integrations.single()
        assertEquals(180.0, integration.snapshot!!.gramsUsed, 0.0)
        assertEquals(150.0, integration.snapshot.snapshot!!.calories, 0.0)
        assertEquals("meal:7", integration.targetToken)
        assertTrue(batch.rejectIntegration(integration.id))
        assertFalse(batch.state.value.busy)
        assertTrue(batch.state.value.savedRowIds.isEmpty())
        assertFalse(batch.state.value.message.orEmpty().contains("product is opgeslagen"))
        assertTrue(batch.submit(AiFoodBatchRequest("scan", NutritionScanDestination.MealDraft, "meal:8", "", listOf(row("A", "180")))))
        assertEquals("meal:8", batch.state.value.integrations.single().targetToken)
    }
}
