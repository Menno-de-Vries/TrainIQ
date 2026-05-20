package com.trainiq.features.nutrition

import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealAnalysisSource
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.NutritionFacts
import android.content.pm.PackageManager
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraScannerStateTest {
    @Test
    fun cameraPermissionRequest_isUserInitiatedRatherThanAutomaticOnEntry() {
        assertEquals(false, shouldAutoRequestCameraPermissionOnEntry())
    }

    @Test
    fun cameraPermissionGateCopy_explainsInitialAndDeniedStates() {
        assertEquals("Geef cameratoegang om de scanner te gebruiken.", scannerPermissionGateMessage(permissionDenied = false))

        val deniedMessage = scannerPermissionGateMessage(permissionDenied = true)
        assertTrue(deniedMessage.contains("maaltijden of barcodes"))
        assertTrue(deniedMessage.contains("app-instellingen"))
        assertEquals("Toegang geven", scannerPermissionGrantLabel())
        assertEquals("Instellingen openen", scannerPermissionSettingsLabel())
        assertEquals("Terug", scannerPermissionBackLabel())
    }

    @Test
    fun scannerActionLabels_matchTheActualAction() {
        assertEquals("Sluiten", scannerErrorPrimaryActionLabel(ScannerSheetErrorAction.Dismiss))
        assertEquals("Opnieuw scannen", scannerErrorPrimaryActionLabel(ScannerSheetErrorAction.ScanAgain))
    }

    @Test
    fun scannerSheetCopy_exposesStateAndActionLabelsForAccessibility() {
        assertEquals("Scannen...", scannerProcessingTitle())
        assertTrue(scannerProcessingMessage().contains("Gemini Flash"))
        assertEquals("Scan voltooid", scannerCompletedTitle())
        assertEquals("Producten controleren", scannerReviewProductsLabel())
        assertEquals("Opnieuw scannen", scannerScanAgainLabel())
        assertEquals("Geen producten gevonden", scannerEmptyTitle())
        assertEquals("Handmatig toevoegen", scannerManualAddLabel())
        assertEquals("Opnieuw proberen", scannerRetryLabel())
    }

    @Test
    fun nutritionFieldsExposeLabelsForCompactFontAccessibility() {
        val source = File("src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt").readText()
        val numberBody = source.substringAfter("private fun NutritionNumberField(")
            .substringBefore("private fun NutritionTextField(")
        val fieldBody = source.substringAfter("private fun NutritionTextField(")
            .substringBefore("private fun EmptyStateCard(")

        assertTrue(numberBody.contains(".semantics(mergeDescendants = true) { contentDescription = label }"))
        assertTrue(fieldBody.contains(".semantics(mergeDescendants = true) { contentDescription = label }"))
        assertTrue(source.contains("label = \"Productnaam\""))
        assertTrue(source.contains("label = \"Barcode (optioneel)\""))
        assertTrue(source.contains("label = \"kcal / 100g\""))
        assertTrue(source.contains("label = \"Eiwit / 100g\""))
    }

    @Test
    fun cameraPermissionState_usesExistingGrantedPermission() {
        assertEquals(true, isCameraPermissionGranted(PackageManager.PERMISSION_GRANTED))
        assertEquals(false, isCameraPermissionGranted(PackageManager.PERMISSION_DENIED))
    }

    @Test
    fun cameraFallbackState_showsFallbackWhenPermissionGrantedButNoCamera() {
        assertEquals(true, shouldShowCameraFallback(hasPermission = true, hasCameraFeature = false, cameraError = null))
        assertEquals(false, shouldShowCameraFallback(hasPermission = false, hasCameraFeature = false, cameraError = null))
        assertEquals(false, shouldShowCameraFallback(hasPermission = true, hasCameraFeature = true, cameraError = null))
    }

    @Test
    fun cameraFallbackState_showsFallbackWhenCameraBindFails() {
        assertEquals(
            true,
            shouldShowCameraFallback(
                hasPermission = true,
                hasCameraFeature = true,
                cameraError = scannerCameraBindFailureMessage(ScannerMode.AI_MEAL),
            ),
        )
    }

    @Test
    fun scannerCameraUnavailableMessage_pointsToManualMealFallback() {
        val message = scannerCameraUnavailableMessage(ScannerMode.AI_MEAL)

        assertTrue(message.contains("Camera niet beschikbaar"))
        assertTrue(message.contains("handmatig"))
    }

    @Test
    fun scannerManualFallbackLabel_matchesScannerMode() {
        assertEquals("Handmatig toevoegen", scannerManualFallbackLabel(ScannerMode.AI_MEAL))
        assertEquals("Code handmatig invoeren", scannerManualFallbackLabel(ScannerMode.BARCODE))
        assertEquals("Handmatig invoeren", scannerManualFallbackLabel(ScannerMode.AI_SCALE))
        assertEquals("Foto importeren", scalePhotoImportLabel())
    }

    @Test
    fun cameraRestorableState_preservesPermissionDeniedAndCameraError() {
        val saved = saveCameraScannerRestorableState(
            CameraScannerRestorableState(
                permissionDenied = true,
                cameraError = "Camera kan nu niet starten.",
            ),
        )

        val restored = restoreCameraScannerRestorableState(saved)

        assertEquals(true, restored.permissionDenied)
        assertEquals("Camera kan nu niet starten.", restored.cameraError)
    }

    @Test
    fun cameraRestorableState_defaultsMissingValuesAfterRecreation() {
        val restored = restoreCameraScannerRestorableState(emptyList())

        assertEquals(false, restored.permissionDenied)
        assertEquals(null, restored.cameraError)
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
