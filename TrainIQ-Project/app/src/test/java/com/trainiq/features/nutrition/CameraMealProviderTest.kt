package com.trainiq.features.nutrition

import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.ui.ScreenUiState
import com.trainiq.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import androidx.lifecycle.viewModelScope
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraMealProviderTest {
    @Test fun actualCameraVmPassesCapturedProviderAndContextToAnalyzer() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val calls = mutableListOf<Triple<String, String, FoodProviderMode>>()
            val result = MealAnalysisResult(listOf(MealScanItem("Bowl", 100.0, NutritionFacts(100.0, 10.0, 10.0, 2.0))))
            val model = CameraScannerViewModel(MutableStateFlow(AiPreferences(true, "synthetic")),
                { path, context, _, mode -> calls += Triple(path, context, mode); result },
                { _, _ -> error("Meal must not call scale analyzer") }, {})
            val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
            runCurrent()
            model.setContextHint("180 g brood")
            model.analyze("synthetic-path", ScannerMode.AI_MEAL, FoodProviderMode.FATSECRET)
            runCurrent()
            assertEquals(listOf(Triple("synthetic-path", "180 g brood", FoodProviderMode.FATSECRET)), calls)
            assertEquals(result, model.completedMealResult())
            model.resetToPreview()
            model.analyze("default-path")
            runCurrent()
            assertEquals(FoodProviderMode.AUTOMATIC, calls.last().third)
            model.resetToPreview()
            collection.cancel()
            model.viewModelScope.cancel()
            runCurrent()
        } finally { Dispatchers.resetMain() }
    }
}
