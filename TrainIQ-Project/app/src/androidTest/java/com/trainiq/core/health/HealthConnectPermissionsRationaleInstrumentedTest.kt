package com.trainiq.core.health

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainiq.core.theme.TrainIqTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthConnectPermissionsRationaleInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun bottomPermissionActionsRemainReachableAndClickableAfterRationaleCopy() {
        var permissionClicks = 0
        var continueClicks = 0

        compose.setContent {
            TrainIqTheme {
                HealthConnectPermissionsRationaleContent(
                    statusMessage = "Stappen zijn gekoppeld. Nog niet gekoppeld: Slaap.",
                    onDismissStatus = {},
                    onRequestPermission = { permissionClicks += 1 },
                    onContinue = { continueClicks += 1 },
                )
            }
        }

        compose.onNodeWithText("Health Connect-toegang geven")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        compose.onNodeWithText("Doorgaan naar TrainIQ")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals(1, permissionClicks)
        assertEquals(1, continueClicks)
    }
}
