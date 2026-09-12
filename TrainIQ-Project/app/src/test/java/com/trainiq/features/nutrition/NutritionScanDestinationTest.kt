package com.trainiq.features.nutrition

import com.trainiq.domain.model.MealType
import com.trainiq.navigation.CameraScanner
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionScanDestinationTest {
    @Test fun typedScannerArgumentsRestoreEachOriginAndMealCategory() {
        NutritionScanDestination.entries.forEach { destination ->
            ScannerMode.entries.forEach { mode ->
                val route = CameraScanner("Context", mode, destination, MealType.SNACK)
                assertEquals(route, Json.decodeFromString<CameraScanner>(Json.encodeToString(route)))
            }
        }
    }
}
