package com.trainiq.features.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.data.repository.HydrationRepository
import com.trainiq.domain.model.HydrationRecord
import com.trainiq.domain.model.explicitVolumeMl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.util.UUID

sealed interface HydrationUiState {
    data object Loading : HydrationUiState
    data class Success(val records: List<HydrationRecord>, val totalMl: Double, val saving: Boolean, val message: String?, val earlierRecords: List<HydrationRecord> = emptyList()) : HydrationUiState
    data object Error : HydrationUiState
}

@HiltViewModel
class HydrationViewModel @Inject constructor(private val repository: HydrationRepository) : ViewModel() {
    private val reload = MutableStateFlow(0)
    private val writing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val day = flow {
        while (true) { emit(LocalDate.now()); kotlinx.coroutines.delay(60_000) }
    }.distinctUntilChanged()
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HydrationUiState> = reload.flatMapLatest {
        combine(repository.observe(), writing, message, day) { records, busy, feedback, today ->
            val entries = records.filter { Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() == today }
            HydrationUiState.Success(entries, entries.sumOf { it.volumeMl }, busy, feedback, records.filterNot { it in entries }) as HydrationUiState
        }.catch { emit(HydrationUiState.Error) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HydrationUiState.Loading)
    fun retry() { reload.value++ }
    fun save(id: String, timestamp: Long, amount: String, saved: () -> Unit) = write {
        repository.save(id, timestamp, amount); message.value = "Vocht opgeslagen."; saved()
    }
    fun delete(id: String) = write { repository.delete(id); message.value = "Vocht verwijderd." }
    private fun write(action: suspend () -> Unit) {
        if (writing.value) return
        writing.value = true
        viewModelScope.launch {
            try { action() }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { message.value = "Opslaan mislukt. Probeer opnieuw." }
            finally { writing.value = false }
        }
    }
}

@Composable
fun HydrationRoute(viewModel: HydrationViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HydrationCard(state, viewModel::retry, viewModel::save, viewModel::delete)
}

@Composable
fun HydrationCard(
    state: HydrationUiState,
    retry: () -> Unit,
    save: (String, Long, String, () -> Unit) -> Unit,
    delete: (String) -> Unit,
) {
    var entryId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var timestamp by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var showEarlier by rememberSaveable { mutableStateOf(false) }
    com.trainiq.core.ui.AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Vocht vandaag", style = MaterialTheme.typography.titleMedium)
            when (state) {
                HydrationUiState.Loading -> ShimmerCardPlaceholder(lineCount = 2)
                HydrationUiState.Error -> {
                    Text("Vochtgegevens konden niet worden geladen.")
                    Button(onClick = retry) { Text("Opnieuw proberen") }
                }
                is HydrationUiState.Success -> {
                    Text("${state.totalMl.toLong()} ml", style = MaterialTheme.typography.headlineSmall)
                    if (state.records.isEmpty()) Text("Nog geen vocht geregistreerd vandaag.")
                    OutlinedTextField(amount, { amount = it }, label = { Text("Hoeveelheid (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, enabled = !state.saving, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        if (!editing && !submitted) timestamp = System.currentTimeMillis()
                        submitted = true
                        save(entryId, timestamp, amount) {
                        amount = ""; editing = false; submitted = false; entryId = UUID.randomUUID().toString()
                    } }, enabled = !state.saving && explicitVolumeMl(amount, "ml") != null, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.saving) "Opslaan…" else if (editing) "Wijziging opslaan" else "Vocht toevoegen")
                    }
                    if (editing) TextButton(onClick = {
                        editing = false; submitted = false; amount = ""; entryId = UUID.randomUUID().toString()
                    }, enabled = !state.saving) { Text("Annuleren") }
                    state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    if (state.earlierRecords.isNotEmpty()) TextButton(onClick = { showEarlier = !showEarlier }) {
                        Text(if (showEarlier) "Eerdere vochtinvoer verbergen" else "Eerdere vochtinvoer bekijken")
                    }
                    (state.records + if (showEarlier) state.earlierRecords else emptyList()).forEach { record ->
                        if (record in state.earlierRecords) Text(
                            Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text("${record.label}: ${record.volumeMl.toLong()} ml")
                        if (record.mealId == null) {
                            Row {
                                TextButton(onClick = { entryId = record.id; timestamp = record.timestamp; amount = record.volumeMl.toString(); editing = true }, enabled = !state.saving) { Text("Corrigeren") }
                                TextButton(onClick = { delete(record.id) }, enabled = !state.saving) { Text("Verwijderen") }
                            }
                        } else Text("Pas deze bijdrage aan via de maaltijd hieronder.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
