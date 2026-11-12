package com.trainiq.core.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.junit.Rule
import org.junit.Test

class PermissionManagerCardInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun failedCheckIsShownAsAFormattedCheckRatherThanASuccessfulSync() {
        val checkedAt = 1_700_000_000_000L
        val time = Instant.ofEpochMilli(checkedAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
        compose.setContent {
            TrainIqTheme {
                PermissionManagerCard(
                    status = HealthConnectStatus(HealthConnectState.NO_DATA, message = "Controle mislukt", lastSyncedAt = checkedAt),
                    onRequestPermission = {}, onOpenInstall = {}, onOpenSettings = {}, onRefresh = {},
                )
            }
        }

        compose.onNodeWithText("Laatst gecontroleerd om $time").assertIsDisplayed()
        compose.onNodeWithText("Laatst gesynchroniseerd", substring = true).assertDoesNotExist()
        compose.onNodeWithText(checkedAt.toString(), substring = true).assertDoesNotExist()
    }

    @Test fun missingCheckTimeShowsNoCheckOrSyncClaim() {
        compose.setContent {
            TrainIqTheme {
                PermissionManagerCard(
                    status = HealthConnectStatus(HealthConnectState.PERMISSION_REQUIRED, message = "Geen toegang"),
                    onRequestPermission = {}, onOpenInstall = {}, onOpenSettings = {}, onRefresh = {},
                )
            }
        }

        compose.onNodeWithText("Laatst gecontroleerd", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Laatst gesynchroniseerd", substring = true).assertDoesNotExist()
    }
}
