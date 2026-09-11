package com.trainiq.features.nutrition

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.HydrationRecord
import org.junit.Assert.*
import org.junit.Test

class HydrationUiTest {
    @OptIn(ExperimentalTestApi::class)
    @Test fun manualEntryIsCorrectableAndRemovableWithoutCreatingFood() = runComposeUiTest {
        var records by mutableStateOf(emptyList<HydrationRecord>())
        var lastId: String? = null
        setContent {
            TrainIqTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    HydrationCard(HydrationUiState.Success(records, records.sumOf { it.volumeMl }, false, null), {},
                        { id, time, amount, saved ->
                            lastId = id
                            records = listOf(HydrationRecord(id, time, amount.toDouble(), null, "Handmatig vocht"))
                            saved()
                        }, { records = records.filterNot { row -> row.id == it } })
                }
            }
        }
        onNodeWithText("Nog geen vocht geregistreerd vandaag.").assertExists()
        onNodeWithText("Hoeveelheid (ml)").performTextInput("250")
        onNodeWithText("Vocht toevoegen").performScrollTo().performClick()
        onNodeWithText("250 ml").assertExists()
        val id = lastId
        onNodeWithText("Corrigeren").performScrollTo().performClick()
        onNodeWithText("Hoeveelheid (ml)").performTextReplacement("200")
        onNodeWithText("Wijziging opslaan").performScrollTo().performClick()
        runOnIdle { assertEquals(id, lastId); assertEquals(1, records.size) }
        onNodeWithText("Verwijderen").performScrollTo().performClick()
        onNodeWithText("Nog geen vocht geregistreerd vandaag.").assertExists()
    }
}
