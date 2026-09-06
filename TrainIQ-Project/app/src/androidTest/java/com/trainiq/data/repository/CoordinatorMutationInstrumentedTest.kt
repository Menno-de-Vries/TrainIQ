package com.trainiq.data.repository

import androidx.test.core.app.ApplicationProvider
import com.trainiq.core.testing.TrainIqDebugDatabaseEntryPoint
import com.trainiq.domain.model.*
import com.trainiq.domain.repository.*
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class CoordinatorMutationInstrumentedTest {
    private val entry = EntryPointAccessors.fromApplication(
        ApplicationProvider.getApplicationContext(), TrainIqDebugDatabaseEntryPoint::class.java)
    private val coordinator = entry.dataCoordinator()
    private val dao = entry.trainIqDatabase().dao()

    @Test fun homeAndNextWorkoutSkipAnEmptyFirstDay() = runBlocking {
        val name = "Home proof ${System.nanoTime()}"
        coordinator.createRoutine(name, "")
        val routine = dao.getRoutinesSnapshot().single { it.name == name }
        coordinator.addWorkoutDay(routine.id, "Empty")
        coordinator.addWorkoutDay(routine.id, "Ready")
        val ready = dao.observeWorkoutDaysSnapshot(routine.id).single { it.name == "Ready" }
        coordinator.addExerciseToDay(ready.id, "Home proof press", "Chest", "Barbell", 3, "8", 90, 20.0, 7.0)
        coordinator.setActiveRoutine(routine.id)
        withTimeout(10_000) { coordinator.observeWorkoutOverview().first {
            it.activeRoutine?.id == routine.id && it.activeRoutine.days.any { day -> day.id == ready.id && day.exercises.isNotEmpty() }
        } }
        assertEquals(ready.id, coordinator.getNextWorkoutDay()?.id)
        val home = withTimeout(10_000) { coordinator.observeDashboard().first { it.nextWorkout != null } }
        assertEquals(ready.id, home.nextWorkout?.id)
    }

    @Test fun immediateMeasurementsRemainDistinct() = runBlocking {
        // Await the real initial stream before exercising consecutive mutations.
        withTimeout(10_000) { coordinator.observeProgressOverview().first() }
        val before = dao.readMeasurementsForExport().size
        repeat(12) { coordinator.addMeasurement(70.0 + it, 20.0, 30.0) }
        val measurements = dao.readMeasurementsForExport()
        assertEquals(before + 12, measurements.size)
        assertEquals((0..11).map { 70.0 + it }, measurements.takeLast(12).map { it.weight })
    }

    @Test fun immediateFoodEditIsUsedByMealSnapshot() = runBlocking {
        val name = "Immediate nutrient proof ${System.nanoTime()}"
        val food = coordinator.saveFoodItem(null, name, null, 100.0, 10.0, 20.0, 3.0, 100.0, FoodSourceType.MANUAL)
        withTimeout(10_000) { coordinator.observeNutritionOverview().first { overview -> overview.foods.any { it.id == food.id } } }
        coordinator.saveFoodItem(food.id, name, null, 234.0, 10.0, 20.0, 3.0, 100.0, FoodSourceType.MANUAL)
        val mealId = coordinator.saveMeal(null, MealType.LUNCH, name, null,
            listOf(MealEntryRequest(MealEntryType.FOOD, food.id, 100.0)))
        assertEquals(234.0, dao.readMealItemsForExport().single { it.mealId == mealId }.calories, 0.0)
    }
}
