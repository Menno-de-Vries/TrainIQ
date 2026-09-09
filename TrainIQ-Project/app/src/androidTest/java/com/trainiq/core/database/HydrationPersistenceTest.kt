package com.trainiq.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HydrationPersistenceTest {
    @Test fun retryEditDateDeleteAndManualEntriesRemainIndependent() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TrainIqDatabase::class.java).build()
        try {
            val dao = db.dao()
            val meal = MealEntity(id = 1, date = 1000, name = "Drink", calories = 10, protein = 0, carbs = 0, fat = 0)
            val item = MealItemEntity(id = 1, mealId = 1, itemType = "SNAPSHOT", referenceId = 0, name = "Drink", gramsUsed = 100.0,
                calories = 10.0, protein = 0.0, carbs = 0.0, fat = 0.0, hydrationMl = 250.0)
            repeat(2) { dao.saveMeal(meal, listOf(item)) }
            assertEquals(250.0, dao.observeHydration().first().single().volumeMl, 0.0)
            dao.saveMeal(meal.copy(date = 2000), listOf(item.copy(hydrationMl = 150.0, servingCount = 2)))
            val edited = dao.observeHydration().first().single()
            assertEquals(300.0, edited.volumeMl, 0.0); assertEquals(2000L, edited.timestamp)
            dao.saveHydration(HydrationEntity("manual", 2000, 200.0))
            dao.saveHydration(HydrationEntity("manual", 2000, 200.0))
            assertEquals(2, dao.observeHydration().first().size)
            dao.saveMeal(meal, listOf(item.copy(hydrationMl = 0.0)))
            assertEquals("manual", dao.observeHydration().first().single().id)
            dao.saveMeal(meal, listOf(item)); dao.deleteMealWithItems(1)
            assertEquals(200.0, dao.observeHydration().first().single().volumeMl, 0.0)
            dao.deleteHydration("manual"); assertTrue(dao.observeHydration().first().isEmpty())
        } finally { db.close() }
    }
}
