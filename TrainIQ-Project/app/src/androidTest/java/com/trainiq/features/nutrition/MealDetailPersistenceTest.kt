package com.trainiq.features.nutrition

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.trainiq.MainActivity
import com.trainiq.core.datastore.OnboardingPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.testing.TrainIqDebugDatabaseEntryPoint
import com.trainiq.domain.model.*
import com.trainiq.testing.resetTrainIqAndroidTestDatabase
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Real app navigation, editors, coordinator and Room; all food is synthetic and local. */
class MealDetailPersistenceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val entry get() = EntryPointAccessors.fromApplication(context, TrainIqDebugDatabaseEntryPoint::class.java)
    private val dao get() = entry.trainIqDatabase().dao()

    @Before fun seed() = runBlocking {
        resetTrainIqAndroidTestDatabase(context)
        UserPreferencesRepository(context).saveOnboardingPreferences(OnboardingPreferences(completed = true, guidedTourCompleted = true))
        val coordinator = entry.dataCoordinator()
        val food = coordinator.saveFoodItem(null, "Test ingredient", null, 100.0, 10.0, 20.0, 3.0, 80.0, FoodSourceType.MANUAL)
        coordinator.saveRecipe(null, "Test bereid recept", null, 400.0, listOf(food.id to 200.0))
        withTimeout(10_000) { coordinator.observeNutritionOverview().first { it.recipes.isNotEmpty() } }
        Unit
    }

    @Test fun cookedWeightIsSavedAndEditCancelAndBackNeverMutateMeal() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openRecipes()
            compose.onNodeWithContentDescription("Gram voor Test bereid recept").performScrollTo().assertTextContains("400")
            capture("recipe-default")
            compose.onNodeWithText("Aan maaltijd toevoegen").performScrollTo().performClick()
            compose.onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { dao.readMealItemsForExport().size == 1 } }
            val original = runBlocking { dao.readMealItemsForExport().single() }
            assertEquals(400.0, original.gramsUsed, 0.0)
            assertEquals(200.0, original.calories, 0.0)
            scenario.recreate()
            openMealCategory()
            compose.onNodeWithText("Test bereid recept").performScrollTo().performClick()
            compose.onNodeWithText("Hoeveelheid wijzigen").performClick()
            compose.onNodeWithContentDescription("Gram per portie").performScrollTo().performTextReplacement("100")
            compose.onNodeWithText("Annuleren").performScrollTo().performClick()
            assertEquals(original, runBlocking { dao.readMealItemsForExport().single() })
            compose.onNodeWithText("Test bereid recept").performScrollTo().performClick()
            compose.onNodeWithText("Hoeveelheid wijzigen").performClick()
            compose.onNodeWithContentDescription("Gram per portie").performScrollTo().performTextReplacement("250")
            // Dismiss the IME first, then invoke the activity's navigation back action.
            androidx.test.espresso.Espresso.pressBack()
            compose.waitForIdle()
            assertEquals(original, runBlocking { dao.readMealItemsForExport().single() })
            androidx.test.espresso.Espresso.pressBack()
            compose.waitForIdle()
            assertEquals(original, runBlocking { dao.readMealItemsForExport().single() })
        }
    }

    @Test fun explicitRecipeWeightUpdatesTotalsAndDeletionReturnsToEmptyCategory() {
        ActivityScenario.launch(MainActivity::class.java).use {
            openRecipes()
            compose.onNodeWithContentDescription("Gram voor Test bereid recept").performScrollTo().performTextReplacement("100")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithText("Aan maaltijd toevoegen").performScrollTo().performClick()
            capture("recipe-added")
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Maaltijd opslaan").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { dao.readMealItemsForExport().size == 1 } }
            assertEquals(100.0, runBlocking { dao.readMealItemsForExport().single().gramsUsed }, 0.0)
            assertEquals(50.0, runBlocking { dao.readMealItemsForExport().single().calories }, 0.0)
            openMealCategory()
            compose.onNodeWithText("Test bereid recept").performScrollTo().performClick()
            compose.onNodeWithText("Hoeveelheid wijzigen").performClick()
            compose.onNodeWithContentDescription("Gram per portie").performScrollTo().performTextReplacement("200")
            compose.onNodeWithText("Wijzigingen opslaan").performScrollTo().performClick()
            compose.waitUntil(10_000) { runBlocking { dao.readMealItemsForExport().single().gramsUsed == 200.0 } }
            assertEquals(100.0, runBlocking { dao.readMealItemsForExport().single().calories }, 0.0)
            compose.onNodeWithText("100 kcal").performScrollTo().assertExists()
            capture("meal-detail")
            compose.onNodeWithText("Terug").performClick()
            compose.onNodeWithText("Middag").performScrollTo().assertTextContains("100 kcal")
            capture("home-meals")
            compose.onNodeWithText("Middag").performClick()
            compose.onNodeWithText("Test bereid recept").performScrollTo().performClick()
            compose.onNodeWithText("Maaltijd verwijderen").performClick()
            compose.onNodeWithText("Verwijderen", ignoreCase = false).performClick()
            compose.waitUntil(10_000) { runBlocking { dao.readMealItemsForExport().isEmpty() } }
            compose.onNodeWithText(mealSectionEmptyText(MealType.LUNCH)).assertExists()
            compose.onNodeWithText("Terug").performClick()
            compose.onNodeWithText("Test bereid recept").assertDoesNotExist()
        }
    }

    private fun capture(name: String) {
        val command = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/trainiq-$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(command).use { it.readBytes() }
    }

    private fun openRecipes() {
        val navigation = (hasContentDescription("Voeding") or hasText("Voeding")) and hasClickAction()
        compose.waitUntil(30_000) { compose.onAllNodes(navigation).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(navigation).performClick()
        compose.onNodeWithContentDescription("Voeding secties openen").performClick()
        compose.onNodeWithText("Recepten").performClick()
    }

    private fun openMealCategory() {
        val home = (hasContentDescription("Start") or hasText("Start")) and hasClickAction()
        compose.waitUntil(30_000) { compose.onAllNodes(home).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(home).performClick()
        compose.onNodeWithText("Test bereid recept").assertDoesNotExist()
        val nutrition = (hasContentDescription("Voeding") or hasText("Voeding")) and hasClickAction()
        compose.onNode(nutrition).performClick()
        compose.onNodeWithContentDescription("Voeding secties openen").performClick()
        compose.onNodeWithText("Vandaag").performClick()
        compose.onNodeWithText("Middag").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Test bereid recept").fetchSemanticsNodes().isNotEmpty() }
    }
}
