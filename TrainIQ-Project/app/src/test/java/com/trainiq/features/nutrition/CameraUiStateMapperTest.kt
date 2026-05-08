package com.trainiq.features.nutrition

import com.trainiq.core.ui.ScreenUiState
import com.trainiq.domain.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraUiStateMapperTest {
    @Test
    fun cameraScreenUiState_wrapsScannerStateAsSuccessContent() {
        val state = cameraScreenUiState(
            CameraScannerUiState.Completed(
                suggestedMealType = MealType.DINNER,
                itemCount = 2,
            ),
        )

        assertTrue(state is ScreenUiState.Success)
        val content = (state as ScreenUiState.Success).content
        assertEquals(CameraScannerUiState.Completed(MealType.DINNER, 2), content.scannerState)
    }
}
