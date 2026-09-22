package com.trainiq.features.sleep

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainiq.core.sleep.SleepChannelId
import com.trainiq.core.sleep.SleepRoutineScheduler
import com.trainiq.core.ui.reloadableObservation
import com.trainiq.data.sleep.SleepRoutineRepository
import com.trainiq.domain.sleep.SleepCountdownMillis
import com.trainiq.domain.sleep.SleepRoutine
import com.trainiq.domain.sleep.SleepRepeatMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface SleepRoutineUiState {
    data object Loading : SleepRoutineUiState
    data class Error(val message: String) : SleepRoutineUiState
    data class Success(
        val routine: SleepRoutine,
        val status: String,
        val busy: Boolean = false,
        val message: String? = null,
        val notificationsAllowed: Boolean = false,
        val exactAllowed: Boolean = false,
        val soundEnabled: Boolean = true,
        val fullScreenAllowed: Boolean = false,
    ) : SleepRoutineUiState
}

@HiltViewModel
class SleepRoutineViewModel @Inject constructor(
    private val repository: SleepRoutineRepository,
    private val scheduler: SleepRoutineScheduler,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val refresh = MutableStateFlow(0)
    private val reloads = MutableStateFlow(0)
    private val capabilities = refresh.map {
        SleepAlarmCapabilities(scheduler.notificationsAllowed(), scheduler.exactAllowed(),
            scheduler.soundEnabled(), scheduler.fullScreenAllowed())
    }.distinctUntilChanged()
    private val ticks = flow {
        while (true) { emit(System.currentTimeMillis()); delay(1_000) }
    }
    val uiState: StateFlow<SleepRoutineUiState> = reloadableObservation(reloads) {
        combine(repository.routine, ticks, busy, message, capabilities) {
            state, now, saving, error, access ->
            SleepRoutineUiState.Success(state, sleepRoutineStatus(state, now), saving, error,
                access.notifications, access.exact, access.sound, access.fullScreen) as SleepRoutineUiState
        }
    }.map { result ->
        result?.getOrElse { SleepRoutineUiState.Error("Slaaproutine laden mislukt. Probeer opnieuw.") }
            ?: SleepRoutineUiState.Loading
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SleepRoutineUiState.Loading)

    fun configure(enabled: Boolean, minute: Int) = action { repository.configure(enabled, minute) }
    fun confirm() = action { repository.confirm() }
    fun refresh() {
        // A terminal observation cannot recover just by updating capability state.
        if (uiState.value is SleepRoutineUiState.Error) reloads.update { it + 1 }
        action { repository.reconcile(); refresh.value++ }
    }
    fun permissionResult() { refresh.value++ }
    private fun action(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        message.value = null
        viewModelScope.launch {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { message.value = "Bijwerken of plannen mislukt. Controleer je instellingen en probeer opnieuw." }
            finally { busy.value = false }
        }
    }
}

private data class SleepAlarmCapabilities(val notifications: Boolean, val exact: Boolean,
    val sound: Boolean, val fullScreen: Boolean)

internal fun sleepRoutineStatus(state: SleepRoutine, now: Long): String = when {
    !state.enabled -> "Slaapvoorbereiding staat uit."
    state.active && state.confirmedAt > 0 -> {
        val seconds = ((state.confirmedAt + SleepCountdownMillis - now + 999) / 1000).coerceIn(0, SleepCountdownMillis / 1000)
        if (seconds == 0L) "Slaaproutine afgehandeld. De volgende herinnering volgt op je ingestelde tijd."
        else "Bevestigd: nog ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')} om te gaan slapen."
    }
    state.active -> "Tijd om je klaar te maken om te slapen. Bevestig pas wanneer je binnen 2 minuten gaat slapen."
    state.nextAt > 0 -> "Volgende herinnering: " + Instant.ofEpochMilli(state.nextAt).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE d MMM HH:mm", Locale.forLanguageTag("nl-NL")))
    else -> "Sla je voorbereidingstijd op om de herinnering te plannen."
}

@Composable
fun SleepRoutineRoute(onBack: () -> Unit, viewModel: SleepRoutineViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.permissionResult()
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    SleepRoutineScreen(state, onBack, viewModel::configure, viewModel::confirm, viewModel::refresh,
        onNotifications = {
            if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, SleepChannelId))
        },
        onExactAlarms = {
            if (Build.VERSION.SDK_INT >= 31) context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                "package:${context.packageName}".toUri()))
        },
        onNotificationSettings = { context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, SleepChannelId)) },
        onFullScreen = {
            if (Build.VERSION.SDK_INT >= 34) context.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                "package:${context.packageName}".toUri()))
        },
    )
}

@Composable
fun SleepRoutineScreen(
    uiState: SleepRoutineUiState,
    onBack: () -> Unit,
    onConfigure: (Boolean, Int) -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onNotifications: () -> Unit,
    onExactAlarms: () -> Unit,
    onNotificationSettings: () -> Unit,
    onFullScreen: () -> Unit = {},
) {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onBack) { Text("Terug") }
                Text("Slaapvoorbereiding", style = MaterialTheme.typography.headlineMedium)
                when (val state = uiState) {
                    SleepRoutineUiState.Loading -> Text("Slaaproutine laden…")
                    is SleepRoutineUiState.Error -> {
                        Text(state.message)
                        OutlinedButton(onClick = onRetry) { Text("Opnieuw proberen") }
                    }
                    is SleepRoutineUiState.Success -> {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(state.status, style = MaterialTheme.typography.titleMedium)
                                if (state.routine.active && state.routine.confirmedAt == 0L) {
                                    Button(onClick = onConfirm, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                                        Text("Ik ga binnen 2 minuten slapen")
                                    }
                                }
                            }
                        }
                        Text("Kies je dagelijkse bedtijd. Zonder bevestiging volgt elke ${SleepRepeatMillis / 60_000} minuten een nieuw slaapalarm.")
                        Row(Modifier.fillMaxWidth().toggleable(state.routine.enabled, enabled = !state.busy,
                            role = Role.Switch, onValueChange = { onConfigure(it, state.routine.minuteOfDay) }).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Dagelijkse slaapvoorbereiding", Modifier.weight(1f))
                            Switch(state.routine.enabled, onCheckedChange = null, enabled = !state.busy)
                        }
                        OutlinedButton(onClick = {
                            TimePickerDialog(context, { _, hour, minute -> onConfigure(state.routine.enabled, hour * 60 + minute) },
                                state.routine.minuteOfDay / 60, state.routine.minuteOfDay % 60, true).show()
                        }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                            Text("Voorbereidingstijd: %02d:%02d".format(state.routine.minuteOfDay / 60, state.routine.minuteOfDay % 60))
                        }
                        if (state.busy) Text("Bijwerken…")
                        state.message?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text("Opnieuw proberen") }
                        }
                        if (state.routine.enabled && !state.notificationsAllowed) {
                            Text("Meldingen zijn geblokkeerd. De routine blijft ingesteld, maar kan je niet waarschuwen.")
                            Button(onClick = onNotifications) { Text("Meldingen toestaan") }
                        }
                        if (state.routine.enabled && !state.exactAllowed) {
                            Text("Exacte alarms zijn niet toegestaan. Het ingestelde tijdstip en de herhaling kunnen hierdoor worden vertraagd.")
                            OutlinedButton(onClick = onExactAlarms) { Text("Exacte alarms instellen") }
                        }
                        if (state.routine.enabled && !state.fullScreenAllowed) {
                            Text("Volledig scherm is niet toegestaan. Open de slaapbevestiging via de melding of via Coach → Slaap.")
                            OutlinedButton(onClick = onFullScreen) { Text("Alarmscherm toestaan") }
                        }
                        if (state.routine.enabled && !state.soundEnabled) Text("Het geluid van dit meldingskanaal staat uit of stil. Controleer de meldingsinstellingen.")
                        OutlinedButton(onClick = onNotificationSettings) { Text("Geluid en meldingen beheren") }
                        Text("Het alarm gebruikt het Android-alarmvolume en standaard alarmgeluid, tenzij je het kanaal zelf hebt aangepast. Je persoonlijke Klok-alarm kan een ander geluid hebben. Niet-storen en een stil alarmkanaal of alarmvolume kunnen geluid blokkeren. Wegvegen bevestigt de routine niet. Na geforceerd stoppen moet je TrainIQ opnieuw openen.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
