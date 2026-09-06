package com.trainiq.features.settings

import com.trainiq.domain.usecase.AppDataImportPreview
import com.trainiq.domain.usecase.AppDataImportResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Keeps the confirmed document paired with its preview throughout a replacement. */
internal class SettingsImportController(
    private val scope: CoroutineScope,
    private val previewDocument: (String) -> AppDataImportPreview,
    private val importDocument: suspend (String) -> AppDataImportResult,
    private val message: (String) -> Unit,
    private val onImported: () -> Unit,
    private val worker: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _preview = MutableStateFlow<AppDataImportPreview?>(null)
    val preview = _preview.asStateFlow()
    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()
    private var pendingJson: String? = null
    private var previewJob: Job? = null
    private var revision = 0L

    fun preview(json: String) {
        if (_isImporting.value) return
        dismiss()
        val request = revision
        previewJob = scope.launch {
            try {
                val result = withContext(worker) { previewDocument(json) }
                if (request != revision) return@launch
                pendingJson = json
                _preview.value = result
                message("Importbestand gecontroleerd. Bevestig om lokale data te vervangen.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (request == revision) message(failure.message ?: "Importbestand kon niet worden gelezen.")
            }
        }
    }

    fun confirm() {
        if (_isImporting.value) return
        val json = pendingJson ?: run {
            message("Kies eerst een geldig importbestand.")
            return
        }
        _isImporting.value = true
        scope.launch {
            try {
                val result = withContext(worker) { importDocument(json) }
                pendingJson = null
                _preview.value = null
                message("TrainIQ-data geimporteerd: ${result.importedRowCount} rijen hersteld.")
                onImported()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                message(failure.message ?: "Data importeren mislukt. Probeer opnieuw.")
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun dismiss() {
        if (_isImporting.value) return
        revision++
        previewJob?.cancel()
        previewJob = null
        pendingJson = null
        _preview.value = null
    }
}
