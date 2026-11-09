package com.trainiq.features.nutrition

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.HydrationRecord
import com.trainiq.data.repository.HydrationRepository
import com.trainiq.testing.resetTrainIqAndroidTestDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class HydrationUiTest {
    @OptIn(ExperimentalTestApi::class)
    @Test fun compactDisclosureRestoresTypedDraftAndKeepsHistoryReachable() = runComposeUiTest {
        val restoration = StateRestorationTester(this)
        val record = HydrationRecord("record", 1L, 200.0, null, "Handmatig vocht")
        val earlier = HydrationRecord("earlier", 2L, 300.0, null, "Eerder vocht")
        restoration.setContent {
            TrainIqTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    HydrationCard(HydrationUiState.Success(listOf(record), 200.0, false, null, earlierRecords = listOf(earlier)), {}, { _, _, _, _ -> }, {})
                }
            }
        }
        onNodeWithText("+250 ml").assertExists()
        onNodeWithText("Hoeveelheid (ml)").assertDoesNotExist()
        onNodeWithText("Andere hoeveelheid").performClick()
        onNodeWithText("Hoeveelheid (ml)").performTextInput("175")
        onNodeWithText("Andere hoeveelheid verbergen").performScrollTo().performClick()
        onNodeWithText("Hoeveelheid (ml)").assertDoesNotExist()
        restoration.emulateSaveAndRestore()
        onNodeWithText("Hoeveelheid (ml)").assertTextContains("175")
        onNodeWithText("Registraties (1)").performScrollTo().performClick()
        onNodeWithText("Handmatig vocht: 200 ml").assertExists()
        onNodeWithText("Eerdere vochtinvoer bekijken").performScrollTo().performClick()
        onNodeWithText("Eerder vocht: 300 ml").assertExists()
    }
    @OptIn(ExperimentalTestApi::class)
    @Test fun realViewModelGuardsRapidWritesAndStoresOneRecord() = runComposeUiTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = resetTrainIqAndroidTestDatabase(context)
        val vm = HydrationViewModel(HydrationRepository(db))
        var callbacks = 0
        setContent { val state by vm.uiState.collectAsState(); HydrationCard(state, vm::retry, vm::save, vm::delete) }
        waitUntil(timeoutMillis = 15_000) { vm.uiState.value is HydrationUiState.Success }
        runOnIdle {
            vm.save("drink-one", 1L, "250") { callbacks++ }
            vm.save("drink-two", 2L, "500") { callbacks++ }
        }
        waitUntil(timeoutMillis = 15_000) { runBlocking { db.dao().readHydrationForExport().isNotEmpty() } }
        waitUntil(timeoutMillis = 15_000) { callbacks == 1 }
        val records = runBlocking { db.dao().readHydrationForExport() }
        assertEquals(1, records.size)
        assertEquals("drink-one", records.single().id)
    }
    @OptIn(ExperimentalTestApi::class)
    @Test fun delayedSuccessAfterRestorationClearsDraftAndNextDrinkGetsNewIdentity() = runComposeUiTest {
        val restoration = StateRestorationTester(this)
        var busy by mutableStateOf(false)
        var completedId by mutableStateOf<String?>(null)
        val ids = mutableListOf<String>()
        restoration.setContent {
            TrainIqTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    HydrationCard(HydrationUiState.Success(emptyList(), 0.0, busy, null, completedSaveId = completedId, completedSaveToken = completedId?.let { "completion" }), {},
                        { id, _, _, _ -> ids += id; busy = true }, {})
                }
            }
        }
        onNodeWithText("+250 ml").performScrollTo().performClick()
        restoration.emulateSaveAndRestore()
        runOnIdle { completedId = ids.single(); busy = false }
        onNodeWithText("+500 ml").performScrollTo().assertIsEnabled().performClick()
        runOnIdle { assertEquals(2, ids.size); assertNotEquals(ids[0], ids[1]) }
    }
    @OptIn(ExperimentalTestApi::class)
    @Test fun quickDrinkUsesOneStableIdentityForRetryAndNewIdentityAfterSuccess() = runComposeUiTest {
        var busy by mutableStateOf(false)
        var message by mutableStateOf<String?>(null)
        var callback: (() -> Unit)? = null
        val calls = mutableListOf<Triple<String, Long, String>>()
        setContent {
            TrainIqTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    HydrationCard(HydrationUiState.Success(emptyList(), 0.0, busy, message), {},
                        { id, time, amount, saved -> calls += Triple(id, time, amount); busy = true; callback = saved }, {})
                }
            }
        }
        onNodeWithText("+250 ml").performScrollTo().performClick()
        onNodeWithText("+250 ml").assertIsNotEnabled()
        runOnIdle { assertEquals(1, calls.size); assertEquals("250", calls.single().third); busy = false; message = "Opslaan mislukt. Probeer opnieuw." }
        onNodeWithText("Opslaan mislukt. Probeer opnieuw.").assertExists()
        onNodeWithText("Vocht toevoegen").performScrollTo().performClick()
        runOnIdle { assertEquals(2, calls.size); assertEquals(calls[0], calls[1]); callback?.invoke(); busy = false }
        onNodeWithText("+500 ml").performScrollTo().performClick()
        runOnIdle { assertEquals(3, calls.size); assertNotEquals(calls[0].first, calls[2].first); assertEquals("500", calls[2].third) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun quickDrinksDoNotOverwriteManualDraft() = runComposeUiTest {
        setContent { TrainIqTheme { HydrationCard(HydrationUiState.Success(emptyList(), 0.0, false, null), {}, { _, _, _, _ -> }, {}) } }
        onNodeWithText("Andere hoeveelheid").performClick()
        onNodeWithText("Hoeveelheid (ml)").performTextInput("175")
        onNodeWithText("+250 ml").assertIsNotEnabled()
        onNodeWithText("+500 ml").assertIsNotEnabled()
        onNodeWithText("Hoeveelheid (ml)").assertTextContains("175")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun oldCompletionForEditedRecordCannotClearNewPendingEdit() = runComposeUiTest {
        val record = HydrationRecord("record", 1L, 250.0, null, "Handmatig vocht")
        var busy by mutableStateOf(false)
        var calls = 0
        setContent {
            TrainIqTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    HydrationCard(HydrationUiState.Success(listOf(record), 250.0, busy, null,
                        completedSaveId = record.id, completedSaveToken = "old"), {},
                        { _, _, _, _ -> calls++; busy = true }, {})
                }
            }
        }
        onNodeWithText("Registraties (1)").performScrollTo().performClick()
        onNodeWithText("Corrigeren").performScrollTo().performClick()
        onNodeWithText("Hoeveelheid (ml)").performTextReplacement("300")
        onNodeWithText("Wijziging opslaan").performScrollTo().performClick()
        runOnIdle { assertEquals(1, calls); busy = false }
        onNodeWithText("Hoeveelheid (ml)").assertTextContains("300")
        onNodeWithText("Wijziging opslaan").assertIsEnabled()
    }
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
        onNodeWithText("Andere hoeveelheid").performScrollTo().performClick()
        onNodeWithText("Hoeveelheid (ml)").performTextInput("250")
        onNodeWithText("Vocht toevoegen").performScrollTo().performClick()
        onNodeWithText("250 ml").assertExists()
        val id = lastId
        onNodeWithText("Registraties (1)").performScrollTo().performClick()
        onNodeWithText("Corrigeren").performScrollTo().performClick()
        onNodeWithText("Hoeveelheid (ml)").performTextReplacement("200")
        onNodeWithText("Wijziging opslaan").performScrollTo().performClick()
        runOnIdle { assertEquals(id, lastId); assertEquals(1, records.size) }
        onNodeWithText("Verwijderen").performScrollTo().performClick()
        onNodeWithText("Nog geen vocht geregistreerd vandaag.").assertExists()
    }
}
