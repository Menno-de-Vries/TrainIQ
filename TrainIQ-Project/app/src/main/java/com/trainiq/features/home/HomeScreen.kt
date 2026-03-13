package com.trainiq.features.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.util.MetricCard
import com.trainiq.core.util.ProgressCard
import com.trainiq.domain.model.HealthConnectAvailability
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HomeDashboard
import com.trainiq.domain.usecase.GetHealthConnectStatusUseCase
import com.trainiq.domain.usecase.ObserveHomeDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHomeDashboardUseCase: ObserveHomeDashboardUseCase,
    private val getHealthConnectStatusUseCase: GetHealthConnectStatusUseCase,
) : ViewModel() {
    val dashboard: StateFlow<HomeDashboard?> = observeHomeDashboardUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _healthConnectStatus = MutableStateFlow(
        HealthConnectStatus(
            availability = HealthConnectAvailability.UNAVAILABLE,
            message = "Health Connect status wordt geladen.",
        ),
    )
    val healthConnectStatus: StateFlow<HealthConnectStatus> = _healthConnectStatus.asStateFlow()

    init {
        refreshHealthConnectStatus()
    }

    fun refreshHealthConnectStatus() {
        viewModelScope.launch {
            _healthConnectStatus.value = getHealthConnectStatusUseCase()
        }
    }
}

@Composable
fun HomeRoute(
    onStartWorkout: (Long) -> Unit,
    onOpenCoach: () -> Unit,
    onOpenTrain: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val healthConnectStatus by viewModel.healthConnectStatus.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.refreshHealthConnectStatus() }

    LaunchedEffect(Unit) {
        viewModel.refreshHealthConnectStatus()
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshHealthConnectStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HomeScreen(
        state = dashboard,
        healthConnectStatus = healthConnectStatus,
        onStartWorkout = onStartWorkout,
        onOpenCoach = onOpenCoach,
        onOpenTrain = onOpenTrain,
        onRequestHealthPermission = {
            permissionLauncher.launch(setOf(HealthPermission.getReadPermission(StepsRecord::class)))
        },
        onRefreshHealth = viewModel::refreshHealthConnectStatus,
    )
}

@Composable
fun HomeScreen(
    state: HomeDashboard?,
    healthConnectStatus: HealthConnectStatus,
    onStartWorkout: (Long) -> Unit,
    onOpenCoach: () -> Unit,
    onOpenTrain: () -> Unit,
    onRequestHealthPermission: () -> Unit,
    onRefreshHealth: () -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("TrainIQ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }

        if (state?.profile == null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Rond je setup af", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Voeg je profiel, doel en macrodoelen toe om je dashboard persoonlijk te maken.")
                        Button(onClick = onOpenCoach) { Text("Open Coach & Setup") }
                    }
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ProgressCard(
                        title = "Calorie progress",
                        progress = state.calorieProgress / state.calorieTarget.coerceAtLeast(1).toFloat(),
                        current = "${state.calorieProgress} kcal",
                        target = "${state.calorieTarget} kcal",
                        modifier = Modifier.weight(1f),
                    )
                    ProgressCard(
                        title = "Protein progress",
                        progress = state.proteinProgress / state.proteinTarget.coerceAtLeast(1).toFloat(),
                        current = "${state.proteinProgress} g",
                        target = "${state.proteinTarget} g",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Streak",
                    value = "${state?.streak ?: 0} days",
                    subtitle = if ((state?.streak ?: 0) > 0) "Opeenvolgende dagen met activiteit" else "Log een training of maaltijd om te starten",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Steps",
                    value = healthConnectStatus.stepsToday?.toString() ?: "Not connected",
                    subtitle = healthConnectStatus.message,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            HealthConnectCard(
                status = healthConnectStatus,
                onRequestPermission = onRequestHealthPermission,
                onOpenInstall = {
                    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"))
                    val intent = if (marketIntent.resolveActivity(context.packageManager) != null) marketIntent else webIntent
                    context.startActivity(intent)
                },
                onOpenSettings = {
                    val settingsIntent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                    if (settingsIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(settingsIntent)
                    } else {
                        onRefreshHealth()
                    }
                },
                onRefresh = onRefreshHealth,
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Next workout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (state?.nextWorkout == null) {
                        Text("Er is nog geen actieve routine of trainingsdag ingesteld.")
                        Button(onClick = onOpenTrain) { Text("Open Train") }
                    } else {
                        Text(state.nextWorkout.name)
                        Text(state.nextWorkout.exercises.joinToString { it.exercise.name }.ifBlank { "Voeg oefeningen toe aan deze dag." })
                        Button(onClick = { onStartWorkout(state.nextWorkout.id) }) { Text("Start workout") }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Coach insight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(state?.aiInsight ?: "Je dashboard wordt gevuld zodra je profiel, trainingen en voeding toevoegt.")
                }
            }
        }
    }
}

@Composable
private fun HealthConnectCard(
    status: HealthConnectStatus,
    onRequestPermission: () -> Unit,
    onOpenInstall: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Health Connect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(status.message)
            when (status.availability) {
                HealthConnectAvailability.NEEDS_INSTALL -> Button(onClick = onOpenInstall) { Text("Install or update") }
                HealthConnectAvailability.NEEDS_PERMISSION -> Button(onClick = onRequestPermission) { Text("Grant access") }
                HealthConnectAvailability.CONNECTED -> OutlinedButton(onClick = onOpenSettings) { Text("Open Health Connect") }
                HealthConnectAvailability.ERROR -> OutlinedButton(onClick = onRefresh) { Text("Retry") }
                HealthConnectAvailability.UNAVAILABLE -> Text("Deze telefoon ondersteunt Health Connect niet.")
            }
        }
    }
}
