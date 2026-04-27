package com.trainiq.features.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.health.HealthConnectRefreshOnResume
import com.trainiq.core.health.rememberHealthConnectPermissionRequester
import com.trainiq.core.theme.spacing
import com.trainiq.core.ui.PermissionManagerCard
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.core.ui.AppCard
import com.trainiq.core.ui.AppChip
import com.trainiq.core.ui.PrimaryActionButton
import com.trainiq.core.ui.SecondaryActionButton
import com.trainiq.core.ui.clearFocusOnScrollOrDrag
import com.trainiq.core.theme.trainIqColors
import com.trainiq.core.util.EnergyBalanceCard
import com.trainiq.core.util.MacroBreakdownCard
import com.trainiq.core.util.MetricCard
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HomeDashboard
import com.trainiq.domain.model.buildEnergyBalance
import com.trainiq.domain.usecase.GetHealthConnectStatusUseCase
import com.trainiq.domain.usecase.ObserveHomeDashboardUseCase
import com.trainiq.domain.usecase.RefreshDashboardDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val dashboard: HomeDashboard,
        val healthConnectStatus: HealthConnectStatus,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHomeDashboardUseCase: ObserveHomeDashboardUseCase,
    private val getHealthConnectStatusUseCase: GetHealthConnectStatusUseCase,
    private val refreshDashboardDataUseCase: RefreshDashboardDataUseCase,
) : ViewModel() {
    private val healthConnectRefreshGate = HomeRefreshGate()

    private val dashboard = observeHomeDashboardUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val healthConnectStatus = MutableStateFlow(
        HealthConnectStatus(
            state = HealthConnectState.ERROR,
            message = "Health Connect-status laden.",
        ),
    )

    val uiState: StateFlow<HomeUiState> = combine(dashboard, healthConnectStatus) { home, health ->
        when {
            home == null -> HomeUiState.Loading
            else -> HomeUiState.Success(
                home.copy(
                    steps = health.stepsToday,
                    energyBalance = home.profile?.let { profile ->
                        buildEnergyBalance(
                            profile = profile,
                            caloriesIn = home.calorieProgress.toDouble(),
                            steps = health.stepsToday ?: 0,
                            workoutCalories = home.todaysWorkoutCalories,
                        )
                    },
                ),
                health,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    init {
        viewModelScope.launch {
            delay(2_000L)
            refreshHealthConnectStatus()
        }
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                refreshDashboardDataSafely { refreshDashboardDataUseCase() }
            }
        }
    }

    fun refreshHealthConnectStatus() {
        if (!healthConnectRefreshGate.tryStart()) return
        viewModelScope.launch {
            try {
                healthConnectStatus.value = runCatching { getHealthConnectStatusUseCase() }.getOrElse {
                    HealthConnectStatus(
                        state = HealthConnectState.ERROR,
                        message = "Health Connect kan nu niet worden ververst.",
                    )
                }
            } finally {
                healthConnectRefreshGate.finish()
            }
        }
    }
}

internal class HomeRefreshGate {
    private var inFlight = false

    @Synchronized
    fun tryStart(): Boolean {
        if (inFlight) return false
        inFlight = true
        return true
    }

    @Synchronized
    fun finish() {
        inFlight = false
    }
}

internal suspend fun refreshDashboardDataSafely(refreshDashboardData: suspend () -> Unit): Boolean {
    return try {
        refreshDashboardData()
        true
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        false
    }
}

@Composable
fun HomeRoute(
    onStartWorkout: (Long) -> Unit,
    onOpenCoach: () -> Unit,
    onOpenTrain: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val requestHealthPermission = rememberHealthConnectPermissionRequester(viewModel::refreshHealthConnectStatus)

    HealthConnectRefreshOnResume(
        onRefresh = viewModel::refreshHealthConnectStatus,
        refreshOnFirstResume = false,
    )

    HomeScreen(
        uiState = uiState,
        onStartWorkout = onStartWorkout,
        onOpenCoach = onOpenCoach,
        onOpenTrain = onOpenTrain,
        onOpenSettings = onOpenSettings,
        onRequestHealthPermission = requestHealthPermission,
        onRefreshHealth = viewModel::refreshHealthConnectStatus,
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onStartWorkout: (Long) -> Unit,
    onOpenCoach: () -> Unit,
    onOpenTrain: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestHealthPermission: () -> Unit,
    onRefreshHealth: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    when (uiState) {
            HomeUiState.Loading -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .clearFocusOnScrollOrDrag()
                        .navigationBarsPadding()
                        .imePadding(),
                    contentPadding = PaddingValues(
                        start = MaterialTheme.spacing.medium,
                        top = MaterialTheme.spacing.medium,
                        end = MaterialTheme.spacing.medium,
                        bottom = 132.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                ) {
                    item(span = { GridItemSpan(2) }) {
                        ScreenHeader(title = "TrainIQ", subtitle = "Vandaag in een slimme cockpit", actionIcon = Icons.Default.Settings, actionContentDescription = "Instellingen openen", onActionClick = onOpenSettings)
                    }
                    items(4) { ShimmerCardPlaceholder(lineCount = 4, modifier = Modifier.height(170.dp)) }
                }
            }

            is HomeUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(MaterialTheme.spacing.medium)) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(MaterialTheme.spacing.large),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        ) {
                            Text("Home niet beschikbaar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text(uiState.message, style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = onRefreshHealth) { Text("Opnieuw proberen") }
                        }
                    }
                }
            }

            is HomeUiState.Success -> {
                val dashboard = uiState.dashboard
                val healthConnectStatus = uiState.healthConnectStatus
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .clearFocusOnScrollOrDrag()
                        .navigationBarsPadding()
                        .imePadding(),
                    contentPadding = PaddingValues(
                        start = MaterialTheme.spacing.medium,
                        top = MaterialTheme.spacing.medium,
                        end = MaterialTheme.spacing.medium,
                        bottom = 132.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                ) {
                    item(span = { GridItemSpan(2) }) {
                        ScreenHeader(title = "TrainIQ", subtitle = "Vandaag in een slimme cockpit", actionIcon = Icons.Default.Settings, actionContentDescription = "Instellingen openen", onActionClick = onOpenSettings)
                    }
                    if (dashboard.profile == null) {
                        item(span = { GridItemSpan(2) }) {
                            DiscoveryCard(onOpenCoach = onOpenCoach)
                        }
                        item(span = { GridItemSpan(2) }) {
                            SetupChecklistCard(
                                hasRoutine = dashboard.nextWorkout != null,
                                hasLoggedFood = dashboard.calorieProgress > 0,
                                healthConnectStatus = healthConnectStatus,
                                onOpenCoach = onOpenCoach,
                                onOpenTrain = onOpenTrain,
                                onRequestHealthPermission = onRequestHealthPermission,
                            )
                        }
                    } else {
                        item(span = { GridItemSpan(2) }) {
                            EnergyBalanceCard(
                                energyBalance = dashboard.energyBalance,
                                calorieTarget = dashboard.calorieTarget,
                                modifier = Modifier,
                            )
                        }
                        item(span = { GridItemSpan(2) }) {
                            MacroBreakdownCard(
                            protein = dashboard.proteinProgress,
                            proteinTarget = dashboard.proteinTarget,
                            carbs = dashboard.carbsProgress,
                            carbsTarget = dashboard.carbsTarget,
                            fat = dashboard.fatProgress,
                            fatTarget = dashboard.fatTarget,
                                modifier = Modifier,
                            )
                        }
                        item {
                            MetricCard(
                                title = "Reeks",
                                value = "${dashboard.streak} dagen",
                                subtitle = if (dashboard.streak > 0) "Je ritme staat stevig" else "Log een training of maaltijd om momentum op te bouwen",
                                modifier = Modifier.height(170.dp),
                            )
                        }
                        item {
                            MetricCard(
                                title = "Herstel",
                                value = when (healthConnectStatus.state) {
                                    HealthConnectState.CONNECTED, HealthConnectState.NO_DATA -> "${healthConnectStatus.stepsToday ?: 0}"
                                    else -> "Offline"
                                },
                                subtitle = buildHomeRecoverySubtitle(
                                    averageHeartRateBpm = healthConnectStatus.averageHeartRateBpm,
                                    todaysWorkoutCalories = dashboard.todaysWorkoutCalories,
                                ),
                                modifier = Modifier.height(170.dp),
                            )
                        }
                        item(span = { GridItemSpan(2) }) {
                            PermissionManagerCard(
                                status = healthConnectStatus,
                                onRequestPermission = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onRequestHealthPermission()
                                },
                                onOpenInstall = {
                                    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
                                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"))
                                    if (!context.startActivityIfResolvable(marketIntent)) {
                                        context.startActivityIfResolvable(webIntent)
                                    }
                                },
                                onOpenSettings = {
                                    val settingsIntent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                                    if (!context.startActivityIfResolvable(settingsIntent)) {
                                        onRefreshHealth()
                                    }
                                },
                                onRefresh = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onRefreshHealth()
                                },
                            )
                        }
                        item(span = { GridItemSpan(2) }) {
                            NextWorkoutCard(
                                dashboard = dashboard,
                                onOpenTrain = onOpenTrain,
                                onStartWorkout = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onStartWorkout(it)
                                },
                            )
                        }
                        item(span = { GridItemSpan(2) }) {
                            CoachInsightCard(
                                insight = dashboard.aiInsight,
                                onOpenCoach = onOpenCoach,
                            )
                        }
                    }
                }
            }
        }
}

internal fun buildHomeRecoverySubtitle(
    averageHeartRateBpm: Int?,
    todaysWorkoutCalories: Int,
): String = buildString {
    append("Stappen")
    averageHeartRateBpm?.let { append(" - Gem. hartslag $it") }
    append(" - Training $todaysWorkoutCalories kcal")
}

@Composable
private fun DiscoveryCard(onOpenCoach: () -> Unit) {
    AppCard(accent = MaterialTheme.colorScheme.tertiary) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ),
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text("Ontdekmodus", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(
                "Welkom bij je slimme coach. Vul je profiel eenmalig in en TrainIQ stemt herstel, voeding en training beter op jou af.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.trainIqColors.mutedText,
            )
            PrimaryActionButton(onClick = onOpenCoach) { Text("Instellen starten") }
        }
    }
}

@Composable
private fun SetupChecklistCard(
    hasRoutine: Boolean,
    hasLoggedFood: Boolean,
    healthConnectStatus: HealthConnectStatus,
    onOpenCoach: () -> Unit,
    onOpenTrain: () -> Unit,
    onRequestHealthPermission: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text("Eerst instellen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(
                "TrainIQ toont je dashboard zodra de basis klopt. Zo voorkom je lege macrodoelen en misleidende energiebalans.",
                color = MaterialTheme.trainIqColors.mutedText,
            )
            SetupChecklistRow("Profiel invullen", done = false)
            SetupChecklistRow("Routine maken of starten", done = hasRoutine)
            SetupChecklistRow("Eerste maaltijd loggen", done = hasLoggedFood)
            SetupChecklistRow(
                "Health Connect optioneel koppelen",
                done = healthConnectStatus.state == HealthConnectState.CONNECTED || healthConnectStatus.state == HealthConnectState.NO_DATA,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                PrimaryActionButton(onClick = onOpenCoach, modifier = Modifier.weight(1f)) { Text("Profiel invullen") }
                SecondaryActionButton(onClick = onOpenTrain, modifier = Modifier.weight(1f)) { Text("Routine maken") }
            }
            if (healthConnectStatus.state == HealthConnectState.PERMISSION_REQUIRED) {
                SecondaryActionButton(onClick = onRequestHealthPermission, modifier = Modifier.fillMaxWidth()) {
                    Text("Health Connect koppelen")
                }
            }
        }
    }
}

@Composable
private fun SetupChecklistRow(label: String, done: Boolean) {
    Text(
        "${if (done) "Klaar" else "Nog te doen"} - $label",
        style = MaterialTheme.typography.bodyMedium,
        color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.trainIqColors.mutedText,
    )
}

@Composable
private fun NextWorkoutCard(
    dashboard: HomeDashboard,
    onOpenTrain: () -> Unit,
    onStartWorkout: (Long) -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text("Volgende training", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            if (dashboard.nextWorkout == null) {
                Text("Er staat nog geen actieve trainingsdag klaar. Ga naar Train om je eerste sessie in te stellen.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
                SecondaryActionButton(onClick = onOpenTrain) { Text("Train openen") }
            } else {
                val exerciseSummary = remember(dashboard.nextWorkout.exercises) {
                    dashboard.nextWorkout.exercises.joinToString { it.exercise.name }
                        .ifBlank { "Voeg oefeningen toe aan deze dag." }
                }
                Text(dashboard.nextWorkout.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
                Text(
                    exerciseSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.trainIqColors.mutedText,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppChip(label = "Training starten")
                    AppChip(label = "RPE 8")
                }
                PrimaryActionButton(onClick = { onStartWorkout(dashboard.nextWorkout.id) }) { Text("Training starten") }
            }
        }
    }
}

@Composable
private fun CoachInsightCard(
    insight: String,
    onOpenCoach: () -> Unit,
) {
    AppCard(accent = MaterialTheme.colorScheme.primary) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.03f),
                        ),
                    ),
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text("AI-inzicht", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(insight, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
            SecondaryActionButton(onClick = onOpenCoach) { Text("Coach openen") }
        }
    }
}

@Composable
private fun WelcomeConnectCard(onRequestHealthPermission: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text("Welkom & verbinden", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(
                "Verbind Health Connect zodat TrainIQ beweging, herstel en slaap kan meenemen zonder extra handmatig werk.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.trainIqColors.mutedText,
            )
            PrimaryActionButton(onClick = onRequestHealthPermission) { Text("Health verbinden") }
        }
    }
}

private fun Context.startActivityIfResolvable(intent: Intent): Boolean {
    if (intent.resolveActivity(packageManager) == null) return false
    return runCatching {
        startActivity(intent)
    }.isSuccess
}
