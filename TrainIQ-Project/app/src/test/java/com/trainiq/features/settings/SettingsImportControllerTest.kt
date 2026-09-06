package com.trainiq.features.settings

import com.trainiq.domain.usecase.AppDataImportPreview
import com.trainiq.domain.usecase.AppDataImportResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsImportControllerTest {
    private fun preview(name: String) = AppDataImportPreview(name, 1, null, 1, 0, 0, 0, 1, 0)

    @Test
    fun ownerCancellationReleasesBusyStateWithoutReportingImportFailure() = runTest {
        val owner = CoroutineScope(coroutineContext + Job())
        val messages = mutableListOf<String>()
        val controller = SettingsImportController(owner, ::preview, { awaitCancellation() },
            messages::add, {}, StandardTestDispatcher(testScheduler))
        controller.preview("document")
        runCurrent()
        controller.confirm()
        runCurrent()
        assertTrue(controller.isImporting.value)
        owner.cancel()
        runCurrent()
        assertFalse(controller.isImporting.value)
        assertEquals(1, messages.size)
        assertEquals("document", controller.preview.value?.format)
    }

    @Test
    fun doubleConfirmationImportsOnlyConfirmedDocumentAndDismissCannotReplaceIt() = runTest {
        val completion = CompletableDeferred<Unit>()
        val imported = mutableListOf<String>()
        var refreshed = 0
        val controller = SettingsImportController(this, ::preview, {
            imported += it
            completion.await()
            AppDataImportResult(preview(it), 1)
        }, {}, { refreshed++ }, StandardTestDispatcher(testScheduler))
        controller.preview("first")
        runCurrent()
        controller.confirm()
        controller.confirm()
        assertTrue(controller.isImporting.value)
        controller.dismiss()
        controller.preview("other")
        runCurrent()
        assertEquals(listOf("first"), imported)
        assertEquals("first", controller.preview.value?.format)
        completion.complete(Unit)
        runCurrent()
        assertFalse(controller.isImporting.value)
        assertNull(controller.preview.value)
        assertEquals(1, refreshed)
    }

    @Test
    fun failedImportKeepsPreviewForRetry() = runTest {
        var attempts = 0
        val messages = mutableListOf<String>()
        val controller = SettingsImportController(this, ::preview, {
            if (++attempts == 1) error("storage unavailable")
            AppDataImportResult(preview(it), 1)
        }, messages::add, {}, StandardTestDispatcher(testScheduler))
        controller.preview("document")
        runCurrent()
        controller.confirm()
        runCurrent()
        assertFalse(controller.isImporting.value)
        assertEquals("document", controller.preview.value?.format)
        assertEquals("storage unavailable", messages.last())
        controller.confirm()
        runCurrent()
        assertEquals(2, attempts)
        assertNull(controller.preview.value)
    }

    @Test
    fun latestSelectionWinsAndDismissedPreviewCannotReappear() = runTest {
        val controller = SettingsImportController(this, ::preview,
            { error("must not import") }, {}, {}, StandardTestDispatcher(testScheduler))
        controller.preview("old")
        controller.preview("new")
        runCurrent()
        assertEquals("new", controller.preview.value?.format)
        controller.preview("dismissed")
        controller.dismiss()
        runCurrent()
        assertNull(controller.preview.value)
    }

    @Test
    fun invalidNewDocumentClearsPreviousConfirmationAndCanRecover() = runTest {
        val messages = mutableListOf<String>()
        val controller = SettingsImportController(this, {
            if (it == "invalid") error("invalid document") else preview(it)
        }, { error("must not import") }, messages::add, {}, StandardTestDispatcher(testScheduler))
        controller.preview("good")
        runCurrent()
        controller.preview("invalid")
        assertNull(controller.preview.value)
        runCurrent()
        assertEquals("invalid document", messages.last())
        controller.confirm()
        assertFalse(controller.isImporting.value)
        assertEquals("Kies eerst een geldig importbestand.", messages.last())
        controller.preview("recovered")
        runCurrent()
        assertEquals("recovered", controller.preview.value?.format)
    }
}
