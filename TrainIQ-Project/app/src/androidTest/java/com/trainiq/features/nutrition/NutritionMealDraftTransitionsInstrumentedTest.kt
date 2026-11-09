package com.trainiq.features.nutrition

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.test.core.app.ApplicationProvider
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.testing.TrainIqDebugDatabaseEntryPoint
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.*
import com.trainiq.domain.repository.MealEntryRequest
import com.trainiq.domain.repository.MealEntryType
import com.trainiq.domain.repository.MealSaveTarget
import com.trainiq.testing.resetTrainIqAndroidTestDatabase
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Exercises reachable NutritionScreen callbacks, not a separate transition simulation. */
@OptIn(ExperimentalTestApi::class)
class NutritionMealDraftTransitionsInstrumentedTest {
    @Test fun implicitDateIsResolvedAtSaveAfterDayChangeWithoutRecomposition() = runComposeUiTest {
        // This clock/date source deliberately is not Compose state: midnight does not trigger recomposition.
        var today = LocalDate.now().minusDays(1)
        val renderedDate = today.toString()
        var dateReads = 0
        val writes = mutableListOf<Write>()
        setContent { Fixture(currentDate = { dateReads++; today }) { writes += it } }
        section("Historie")
        onNodeWithText("Opnieuw gebruiken").performScrollTo().performClick()
        onNodeWithContentDescription("Datum (jjjj-mm-dd)").performScrollTo().assertTextContains(renderedDate)
        val save = onNodeWithText("Maaltijd opslaan").performScrollTo()
        waitForIdle()
        var readsBeforeMidnight = 0
        runOnIdle {
            readsBeforeMidnight = dateReads
            today = LocalDate.now()
        }
        // Querying semantics causes no input/scroll/state change. The old date must remain rendered until the click.
        onNodeWithContentDescription("Datum (jjjj-mm-dd)").assertTextContains(renderedDate)
        runOnIdle { assertEquals(readsBeforeMidnight, dateReads) }
        val beforeSave = Instant.now().toEpochMilli()
        save.performClick()
        runOnIdle {
            val loggedAt = requireNotNull(writes.single().items.single().loggedAt)
            assertTrue("Implicit date must use save-time now, not yesterday at noon", loggedAt >= beforeSave)
            assertTrue(loggedAt <= Instant.now().toEpochMilli())
            assertEquals(today, Instant.ofEpochMilli(loggedAt).atZone(ZoneId.systemDefault()).toLocalDate())
        }
    }

    @Test fun historyReuseWhileEditingRequiresChoiceAndCreatesDistinctIdentity() = runComposeUiTest {
        val writes = mutableListOf<Write>()
        setContent { Fixture { writes += it; it.saved() } }
        editOriginal()
        onNodeWithContentDescription("Maaltijdnaam").performScrollTo().performTextReplacement("Unsaved original")
        onNodeWithContentDescription("Datum (jjjj-mm-dd)").performScrollTo().performTextReplacement("2026-01-02")
        section("Historie")
        onNodeWithText("Opnieuw gebruiken").performScrollTo().performClick()
        onNodeWithText("Maaltijdconcept vervangen?").assertIsDisplayed()
        onNodeWithText("Concept behouden").performClick()
        onNodeWithContentDescription("Maaltijdnaam").performScrollTo().assertTextContains("Unsaved original")
        onNodeWithContentDescription("Datum (jjjj-mm-dd)").performScrollTo().assertTextContains("2026-01-02")
        onNodeWithText("Wijzigingen opslaan").performScrollTo().assertExists()
        section("Historie")
        onNodeWithText("Opnieuw gebruiken").performScrollTo().performClick()
        onNodeWithText("Vervangen en doorgaan").performClick()
        onNodeWithContentDescription("Maaltijdnaam").performScrollTo().assertTextContains("Original meal")
        onNodeWithContentDescription("Datum (jjjj-mm-dd)").performScrollTo().assertTextContains(LocalDate.now().toString())
        onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
        runOnIdle {
            assertEquals(1, writes.size)
            val created = writes.single().target as MealSaveTarget.Create
            assertNotEquals(original.id, created.reservedId)
            assertNotEquals(original.timestamp, writes.single().items.single().loggedAt)
            assertEquals("Original meal", original.name)
            assertEquals(80.0, writes.single().items.single().gramsUsed, 0.0)
        }
    }

    @Test fun pendingNewTargetSurvivesRestorationAndCancelPreservesEditFieldsAndItems() = runComposeUiTest {
        val restoration = StateRestorationTester(this)
        val writes = mutableListOf<Write>()
        restoration.setContent { Fixture { writes += it } }
        editOriginal()
        onNodeWithContentDescription("Maaltijdnaam").performScrollTo().performTextReplacement("Kept name")
        onNodeWithContentDescription("Notities").performScrollTo().performTextReplacement("Kept notes")
        onNodeWithContentDescription("Gram per portie").performScrollTo().performTextReplacement("125")
        section("Vandaag")
        onNodeWithContentDescription("Toevoegen aan Avond").performScrollTo().performClick()
        onNodeWithText("Maaltijdconcept vervangen?").assertExists()
        restoration.emulateSaveAndRestore()
        onNodeWithText("Maaltijdconcept vervangen?").assertExists()
        onNodeWithText("Concept behouden").performClick()
        onNodeWithContentDescription("Maaltijdnaam").performScrollTo().assertTextContains("Kept name")
        onNodeWithContentDescription("Notities").performScrollTo().assertTextContains("Kept notes")
        onNodeWithContentDescription("Gram per portie").performScrollTo().assertTextContains("125")
        onNodeWithText("Wijzigingen opslaan").performScrollTo().performClick()
        runOnIdle {
            assertEquals(MealSaveTarget.Edit(original.id), writes.single().target)
            assertEquals(original.timestamp, writes.single().items.single().loggedAt)
        }
        section("Vandaag")
        onNodeWithContentDescription("Toevoegen aan Avond").performScrollTo().performClick()
        onNodeWithText("Vervangen en doorgaan").performClick()
        onNodeWithText("Toevoegen aan Avond").assertExists()
        onNodeWithText("Huidige maaltijd controleren").assertDoesNotExist()
    }

    @Test fun failedEditAndSameEditReentryKeepIdentityTimestampAndDraft() = runComposeUiTest {
        val writes = mutableListOf<Write>()
        setContent { Fixture { writes += it /* failure: no success callback */ } }
        editOriginal()
        onNodeWithContentDescription("Maaltijdnaam").performScrollTo().performTextReplacement("Retry edit")
        onNodeWithText("Wijzigingen opslaan").performScrollTo().performClick()
        section("Vandaag")
        onNodeWithText("Original meal").performScrollTo().performClick()
        onNodeWithText("Hoeveelheid wijzigen").performClick()
        onNodeWithText("Maaltijdconcept vervangen?").assertDoesNotExist()
        onNodeWithContentDescription("Maaltijdnaam").performScrollTo().assertTextContains("Retry edit")
        onNodeWithContentDescription("Datum (jjjj-mm-dd)").performScrollTo().performTextReplacement("2026-02-30")
        onNodeWithText("Wijzigingen opslaan").performScrollTo().performClick()
        onNodeWithText("Vul een geldige datum in (jjjj-mm-dd).").performScrollTo().assertIsDisplayed()
        runOnIdle { assertEquals(1, writes.size) }
        onNodeWithContentDescription("Datum (jjjj-mm-dd)").performScrollTo().performTextReplacement(LocalDate.now().toString())
        onNodeWithText("Wijzigingen opslaan").performScrollTo().performClick()
        runOnIdle {
            assertEquals(2, writes.size)
            assertTrue(writes.all { it.target == MealSaveTarget.Edit(original.id) })
            assertTrue(writes.all { it.name == "Retry edit" && it.items.single().loggedAt == original.timestamp })
        }
    }

    @Test fun newReuseRetryAfterLostCallbackUsesSameNewIdentityAcrossRestoration() = runComposeUiTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = resetTrainIqAndroidTestDatabase(context)
        val coordinator = EntryPointAccessors.fromApplication(context, TrainIqDebugDatabaseEntryPoint::class.java).dataCoordinator()
        val foodId = runBlocking {
            val food = coordinator.saveFoodItem(null, "Synthetic food", null, 100.0, 10.0, 10.0, 2.0, 100.0, FoodSourceType.MANUAL)
            coordinator.saveMeal(MealSaveTarget.Create(original.id), original.mealType, original.name, original.notes,
                listOf(MealEntryRequest(MealEntryType.FOOD, food.id, 80.0, loggedAt = original.timestamp)))
            food.id
        }
        val originalRow = runBlocking { db.dao().readMealsForExport().single() }
        val originalItem = runBlocking { db.dao().readMealItemsForExport().single() }
        val writes = mutableListOf<Write>()
        val restoration = StateRestorationTester(this)
        restoration.setContent {
            Fixture {
                // Real coordinator -> Room, deliberately lose the success callback to simulate interrupted UI delivery.
                runBlocking {
                    coordinator.saveMeal(it.target, original.mealType, it.name, "",
                        it.items.map { request -> request.copy(referenceId = foodId) })
                }
                writes += it
            }
        }
        editOriginal()
        section("Historie")
        onNodeWithText("Opnieuw gebruiken").performScrollTo().performClick()
        onNodeWithText("Vervangen en doorgaan").performClick()
        onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
        restoration.emulateSaveAndRestore()
        onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
        runOnIdle {
            assertEquals(2, writes.size)
            assertEquals(writes.first().target, writes.last().target)
            assertTrue(writes.first().target is MealSaveTarget.Create)
            assertNotEquals(original.id, (writes.first().target as MealSaveTarget.Create).reservedId)
            val rows = runBlocking { db.dao().readMealsForExport() }
            val items = runBlocking { db.dao().readMealItemsForExport() }
            assertEquals(2, rows.size)
            assertEquals(originalRow, rows.single { it.id == original.id })
            assertEquals(originalItem, items.single { it.mealId == original.id })
            assertEquals(2, items.size)
            assertTrue(items.all { it.gramsUsed == 80.0 && it.calories == 80.0 })
        }
    }

    @Test fun lateSuccessCallbackCannotClearAnApprovedReplacementTarget() = runComposeUiTest {
        val writes = mutableListOf<Write>()
        setContent { Fixture { writes += it } }
        editOriginal()
        onNodeWithText("Wijzigingen opslaan").performScrollTo().performClick()
        section("Vandaag")
        onNodeWithContentDescription("Toevoegen aan Avond").performScrollTo().performClick()
        onNodeWithText("Vervangen en doorgaan").performClick()
        runOnIdle { writes.single().saved() }
        onNodeWithText("Toevoegen aan Avond").assertExists()
        onNodeWithText("Huidige maaltijd controleren").assertDoesNotExist()
    }

    private fun ComposeUiTest.section(name: String) {
        onNodeWithContentDescription("Voeding secties openen").performClick()
        onNodeWithText(name).performClick()
    }
    private fun ComposeUiTest.editOriginal() {
        onNodeWithText("Original meal").performScrollTo().performClick()
        onNodeWithText("Hoeveelheid wijzigen").performClick()
    }

    private data class Write(val target: MealSaveTarget, val name: String, val items: List<MealEntryRequest>, val saved: () -> Unit)

    @Composable private fun Fixture(currentDate: () -> LocalDate = { LocalDate.now() }, write: (Write) -> Unit) {
        TrainIqTheme(dynamicColor = false) {
            NutritionScreen(
                currentDate = currentDate,
                uiState = NutritionUiState.Success(overview, AiPreferences(false, "")),
                onSaveFood = { _, _, _, _, _, _, _, _, _, _, _ -> },
                onSaveRecipe = { _, _, _, _, _, _ -> },
                onSaveMeal = { target, _, name, _, items, saved -> write(Write(target, name, items, saved)) },
                onDeleteMeal = {}, onDeleteFood = {}, onDeleteRecipe = {},
                onTryStartAiBatchSave = { true }, onFinishAiBatchSave = {}, onSetScanResult = {},
                onSetMessage = {}, onDismissMessage = {}, onRetry = {}, onAiScanner = {}, onOpenBarcodeScanner = {},
            )
        }
    }

    private val original = LoggedMeal(
        101L, LocalDate.now().atTime(9, 37).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        MealType.LUNCH, "Original meal", "Original notes",
        listOf(LoggedMealItem(102L, 101L, LoggedMealItemType.FOOD, 103L, "Synthetic food", 80.0,
            nutritionSnapshot = NutritionFacts(80.0, 8.0, 8.0, 1.6))), NutritionFacts(80.0, 8.0, 8.0, 1.6),
    )
    private val overview = NutritionOverview(
        foods = listOf(FoodItem(103L, "Synthetic food", caloriesPer100g = 100.0, proteinPer100g = 10.0,
            carbsPer100g = 10.0, fatPer100g = 2.0, sourceType = FoodSourceType.MANUAL, createdAt = 1, updatedAt = 1)),
        recipes = emptyList(), meals = listOf(original), todaysNutrition = original.totalNutrition,
        todaysCalories = 80.0, todaysProtein = 8.0, todaysCarbs = 8.0, todaysFat = 1.6,
        todaysMeals = listOf(original), todaysMealsByType = mapOf(MealType.LUNCH to listOf(original)), todaysWorkoutCalories = 0,
    )
}
