package com.trainiq.features.nutrition

import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealAnalysisSource
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.NutritionFacts
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraScannerStateTest {
    @Test
    fun cameraPermissionRequest_isUserInitiatedRatherThanAutomaticOnEntry() {
        assertEquals(false, shouldAutoRequestCameraPermissionOnEntry())
    }

    @Test
    fun scannerActionLabels_matchTheActualAction() {
        assertEquals("Sluiten", scannerErrorPrimaryActionLabel(ScannerSheetErrorAction.Dismiss))
        assertEquals("Opnieuw scannen", scannerErrorPrimaryActionLabel(ScannerSheetErrorAction.ScanAgain))
    }

    @Test
    fun cameraPermissionState_usesExistingGrantedPermission() {
        assertEquals(true, isCameraPermissionGranted(PackageManager.PERMISSION_GRANTED))
        assertEquals(false, isCameraPermissionGranted(PackageManager.PERMISSION_DENIED))
    }

    @Test
    fun completedScanCopy_usesDutchProductLabels() {
        assertEquals("1 product gevonden. Suggestie: Ochtend.", scannerCompletedMessage(1, MealType.BREAKFAST))
        assertEquals("2 producten gevonden.", scannerCompletedMessage(2, null))
    }

    @Test
    fun classifyMealScanResult_withItems_returnsCompletedState() {
        val result = MealAnalysisResult(
            items = listOf(
                MealScanItem(
                    name = "Kwark",
                    estimatedGrams = 250.0,
                    nutrition = NutritionFacts(calories = 150.0, protein = 25.0, carbs = 10.0, fat = 1.0),
                ),
            ),
            suggestedMealType = MealType.BREAKFAST,
        )

        val state = classifyMealScanResultForScanner(result, contextHint = "ontbijt")

        assertTrue(state is CameraScannerUiState.Completed)
        state as CameraScannerUiState.Completed
        assertEquals(MealType.BREAKFAST, state.suggestedMealType)
        assertEquals(1, state.itemCount)
    }

    @Test
    fun classifyMealScanResult_withoutItems_returnsEmptyStateWithRetryMessage() {
        val result = MealAnalysisResult(
            items = emptyList(),
            suggestedMealType = MealType.LUNCH,
            notes = "AI-maaltijdanalyse is nu niet beschikbaar.",
        )

        val state = classifyMealScanResultForScanner(result, contextHint = "lunch")

        assertTrue(state is CameraScannerUiState.Empty)
        state as CameraScannerUiState.Empty
        assertEquals("lunch", state.contextHint)
        assertTrue(state.message.contains("Geen producten gevonden"))
        assertTrue(state.message.contains("handmatig"))
    }

    @Test
    fun classifyMealScanResult_withLocalFallback_returnsFallbackState() {
        val result = MealAnalysisResult(
            items = emptyList(),
            notes = "AI-maaltijdanalyse is nu niet beschikbaar.",
            source = MealAnalysisSource.LOCAL_FALLBACK,
        )

        val state = classifyMealScanResultForScanner(result, contextHint = "avond")

        assertTrue(state is CameraScannerUiState.LocalFallback)
        state as CameraScannerUiState.LocalFallback
        assertEquals("avond", state.contextHint)
        assertTrue(state.message.contains("AI-maaltijdanalyse"))
    }
}
