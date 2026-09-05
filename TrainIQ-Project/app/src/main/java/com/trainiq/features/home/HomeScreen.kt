package com.trainiq.features.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.health.HealthConnectRefreshOnResume
import com.trainiq.core.health.healthConnectStepSourceLabel
import com.trainiq.core.health.rememberHealthConnectPermissionRequester
import com.trainiq.core.theme.spacing
import com.trainiq.core.ui.PermissionManagerCard
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.AppCard
import com.trainiq.core.ui.AppChip
import com.trainiq.core.ui.PrimaryActionButton
import com.trainiq.core.ui.SecondaryActionButton
import com.trainiq.core.ui.clearFocusOnScrollOrDrag
import com.trainiq.core.ui.reloadableObservation
import com.trainiq.core.theme.trainIqColors
import com.trainiq.core.util.EnergyBalanceCard
import com.trainiq.core.util.MacroBreakdownCard
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HealthConnectStepDataFreshness
import com.trainiq.domain.model.HomeDashboard
import com.trainiq.domain.usecase.BuildHomeDashboardUseCase
import com.trainiq.domain.usecase.GetHealthConnectStatusUseCase
import com.trainiq.domain.usecase.ObserveHomeDashboardUseCase
import com.trainiq.domain.usecase.RefreshDashboardDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val dashboard: HomeDashboard,
        val healthConnectStatus: HealthConnectStatus,
        val isRefreshingHealth: Boolean = false,
        val refreshMessage: String? = null,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHomeDashboardUseCase: ObserveHomeDashboardUseCase,
    private val buildHomeDashboardUseCase: BuildHomeDashboardUseCase,
    private val getHealthConnectStatusUseCase: GetHealthConnectStatusUseCase,
    private val refreshDashboardDataUseCase: RefreshDashboardDataUseCase,
) : ViewModel() {
    private val healthConnectRefreshGate = HomeRefreshGate()
    private val healthRefreshUiState = MutableStateFlow(HomeHealthRefreshUiState())

    private val reloads = MutableStateFlow(0)
    private val dashboard = reloadableObservation(reloads) { observeHomeDashboardUseCase() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val healthConnectStatus = MutableStateFlow(
        HealthConnectStatus(
            state = HealthConnectState.ERROR,
            message = "Health Connect-status laden.",
        ),
    )

    val uiState: StateFlow<HomeUiState> = combine(dashboard, healthConnectStatus, healthRefreshUiState) { home, health, refresh ->
        when {
            home == null -> HomeUiState.Loading
            home.isFailure -> HomeUiState.Error("Dashboardgegevens konden niet worden geladen. Probeer opnieuw.")
            else -> HomeUiState.Success(
                buildHomeDashboardUseCase.mergeHealthStatus(home.getOrThrow(), health),
                health,
                isRefreshingHealth = refresh.isRefreshing,
                refreshMessage = refresh.message,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    init {
        viewModelScope.launch {
            refreshHealthConnectStatus()
        }
    }

    fun refreshDashboardAndHealthStatus() {
        if (dashboard.value?.isFailure == true) reloads.update { it + 1 }
        if (!healthConnectRefreshGate.tryStart()) return
        healthRefreshUiState.value = HomeHealthRefreshUiState(isRefreshing = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dashboardRefreshSucceeded = refreshDashboardDataSafely { refreshDashboardDataUseCase() }
                val statusResult = runCatching { getHealthConnectStatusUseCase() }
                statusResult.getOrNull()?.let { healthConnectStatus.value = it }
                healthRefreshUiState.value = HomeHealthRefreshUiState(
                    isRefreshing = false,
                    message = homeHealthRefreshMessage(dashboardRefreshSucceeded && statusResult.isSuccess),
                )
            } finally {
                healthConnectRefreshGate.finish()
            }
        }
    }

    fun refreshHealthConnectStatus() {
        if (!healthConnectRefreshGate.tryStart()) return
        viewModelScope.launch(Dispatchers.IO) {
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

internal data class HomeHealthRefreshUiState(
    val isRefreshing: Boolean = false,
    val message: String? = null,
)

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

internal fun homeHealthRefreshMessage(success: Boolean): String =
    if (success) {
        "Health Connect bijgewerkt."
    } else {
        "Health Connect kon niet worden ververst. Laatste bekende data blijft zichtbaar."
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

internal const val HomePeriodicRefreshIntervalMillis: Long = 60_000L

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
    val lifecycleOwner = LocalLifecycleOwner.current

    HealthConnectRefreshOnResume(
        onRefresh = viewModel::refreshHealthConnectStatus,
        refreshOnFirstResume = false,
    )

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(HomePeriodicRefreshIntervalMillis)
                viewModel.refreshDashboardAndHealthStatus()
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        onStartWorkout = onStartWorkout,
        onOpenCoach = onOpenCoach,
        onOpenTrain = onOpenTrain,
        onOpenSettings = onOpenSettings,
        onRequestHealthPermission = requestHealthPermission,
        onRefreshHealth = viewModel::refreshDashboardAndHealthStatus,
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clearFocusOnScrollOrDrag()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(
                            start = MaterialTheme.spacing.medium,
                            top = MaterialTheme.spacing.medium,
                            end = MaterialTheme.spacing.medium,
                            bottom = 132.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                ) {
                    ScreenHeader(title = "TrainIQ", subtitle = "Vandaag in een slimme cockpit", actionIcon = Icons.Default.Settings, actionContentDescription = "Instellingen openen", onActionClick = onOpenSettings)
                    repeat(4) { HomeStartupPlaceholder(modifier = Modifier.height(170.dp)) }
                }
            }

            is HomeUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(MaterialTheme.spacing.medium)) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(MaterialTheme.spacing.large),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        ) {
                            Text("Startscherm niet beschikbaar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text(uiState.message, style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton(onClick = onRefreshHealth) { Text("Opnieuw proberen") }
                        }
                    }
                }
            }

            is HomeUiState.Success -> {
                val dashboard = uiState.dashboard
                val healthConnectStatus = uiState.healthConnectStatus
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clearFocusOnScrollOrDrag()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(
                            start = MaterialTheme.spacing.medium,
                            top = MaterialTheme.spacing.medium,
                            end = MaterialTheme.spacing.medium,
                            bottom = 132.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                ) {
                    ScreenHeader(title = "TrainIQ", subtitle = "Vandaag in een slimme cockpit", actionIcon = Icons.Default.Settings, actionContentDescription = "Instellingen openen", onActionClick = onOpenSettings)
                    if (dashboard.profile == null) {
                        DiscoveryCard(onOpenCoach = onOpenCoach)
                        SetupChecklistCard(
                            hasRoutine = dashboard.nextWorkout != null,
                            hasLoggedFood = dashboard.calorieProgress > 0,
                            healthConnectStatus = healthConnectStatus,
                            onOpenCoach = onOpenCoach,
                            onOpenTrain = onOpenTrain,
                            onRequestHealthPermission = onRequestHealthPermission,
                        )
                    } else {
                        EnergyBalanceCard(
                            energyBalance = dashboard.energyBalance,
                            calorieTarget = dashboard.calorieTarget,
                            modifier = Modifier,
                        )
                        MacroBreakdownCard(
                            protein = dashboard.proteinProgress,
                            proteinTarget = dashboard.proteinTarget,
                            carbs = dashboard.carbsProgress,
                            carbsTarget = dashboard.carbsTarget,
                            fat = dashboard.fatProgress,
                            fatTarget = dashboard.fatTarget,
                            modifier = Modifier,
                        )
                        HomeMomentumCard(
                            streak = dashboard.streak,
                            healthStatus = healthConnectStatus,
                            todaysWorkoutCalories = dashboard.todaysWorkoutCalories,
                        )
                        if (showCompactHomeHealthCard(healthConnectStatus)) {
                            HomeHealthStatusRow(
                                status = healthConnectStatus,
                                isRefreshingHealth = uiState.isRefreshingHealth,
                                refreshMessage = uiState.refreshMessage,
                                onRefresh = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onRefreshHealth()
                                },
                            )
                        }
                        if (!showCompactHomeHealthCard(healthConnectStatus)) {
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
                        if (dashboard.nextWorkout != null) {
                            NextWorkoutCard(
                                dashboard = dashboard,
                                onOpenTrain = onOpenTrain,
                                onStartWorkout = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onStartWorkout(it)
                                },
                            )
                        } else {
                            CoachInsightCard(
                                insight = dashboard.coachInsight,
                                onOpenCoach = onOpenCoach,
                            )
                        }
                    }
                }
            }
        }
}

internal fun buildHomeRecoverySubtitle(
    stepsToday: Int?,
    averageHeartRateBpm: Int?,
    todaysWorkoutCalories: Int,
): String = buildString {
    if (stepsToday == null) {
        append("Stappen offline")
        append(" - ")
    }
    averageHeartRateBpm?.let {
        append("Gem. hartslag $it bpm")
        append(" - ")
    }
    append("Training $todaysWorkoutCalories kcal")
}

@Composable
private fun HomeMomentumCard(
    streak: Int,
    healthStatus: HealthConnectStatus,
    todaysWorkoutCalories: Int,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Momentum", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(
                    homeMomentumEncouragement(streak, healthStatus),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.trainIqColors.mutedText,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                HomeMomentumMetricRow(
                    modifier = Modifier.weight(1f),
                    title = "Reeks",
                    value = homeStreakValue(streak),
                    subtitle = homeStreakSubtitle(streak),
                    accent = MaterialTheme.trainIqColors.amber,
                )
                HomeMomentumMetricRow(
                    modifier = Modifier.weight(1f),
                    title = "Stappen",
                    value = homeStepsValue(healthStatus),
                    subtitle = buildHomeRecoverySubtitle(
                        stepsToday = healthStatus.stepsToday,
                        averageHeartRateBpm = healthStatus.averageHeartRateBpm,
                        todaysWorkoutCalories = todaysWorkoutCalories,
                    ),
                    accent = MaterialTheme.trainIqColors.mint,
                )
            }
        }
    }
}

@Composable
private fun HomeMomentumMetricRow(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .defaultMinSize(minHeight = 96.dp),
        shape = MaterialTheme.shapes.large,
        color = accent.copy(alpha = 0.10f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.trainIqColors.cardBorder.copy(alpha = 0.72f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.trainIqColors.mutedText,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

internal fun homeStreakValue(streak: Int): String = "$streak dagen"

internal fun homeStreakSubtitle(streak: Int): String =
    if (streak > 0) "Ritme staat aan" else "Start vandaag"

internal fun homeStepsValue(status: HealthConnectStatus): String = when (status.state) {
    HealthConnectState.CONNECTED,
    HealthConnectState.NO_DATA -> when (status.stepDataFreshness) {
        HealthConnectStepDataFreshness.FRESH,
        HealthConnectStepDataFreshness.STALE_CACHE -> status.stepsToday?.toString() ?: "Geen data"
        else -> "Geen data"
    }
    else -> "Offline"
}

internal fun homeMomentumEncouragement(streak: Int, status: HealthConnectStatus): String {
    val stepsToday = status.stepsToday
    return when {
        streak <= 0 && status.state == HealthConnectState.NO_DATA ->
            "Start klein: log vandaag een maaltijd of pak je eerste wandeling. Dan wordt je coach direct scherper."
        streak <= 0 && stepsToday == null ->
            "Begin lokaal met een maaltijd of training. Je hoeft niet te wachten op Health Connect-data."
        stepsToday != null && stepsToday > 0 ->
            "Je beweging staat erin. Houd je voeding en training erbij, dan blijft het beeld compleet."
        streak > 0 ->
            "Je ritme staat aan. Blijf kleine logs toevoegen zodat je coach scherp blijft."
        else ->
            "Kleine logs houden je coach scherp."
    }
}

@Composable
private fun HomeHealthStatusRow(
    status: HealthConnectStatus,
    isRefreshingHealth: Boolean,
    refreshMessage: String?,
    onRefresh: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.trainIqColors.cardElevated,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.trainIqColors.cardBorder.copy(alpha = 0.72f)),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Health Connect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text(
                        homeHealthCompactSummary(status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.trainIqColors.mutedText,
                    )
                }
                TextButton(onClick = onRefresh, enabled = !isRefreshingHealth) {
                    Text(if (isRefreshingHealth) "Verversen..." else "Verversen")
                }
            }
            homeHealthSyncSummary(status)?.let { syncSummary ->
                Text(
                    syncSummary,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.trainIqColors.mutedText,
                )
            }
            refreshMessage?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

internal fun homeHealthCompactSummary(status: HealthConnectStatus): String = when (status.state) {
    HealthConnectState.PERMISSION_REQUIRED -> "Stappentoegang ontbreekt."
    HealthConnectState.PROVIDER_MISSING -> "Health Connect moet worden bijgewerkt."
    HealthConnectState.UNSUPPORTED -> "Health Connect wordt niet ondersteund."
    HealthConnectState.ERROR -> "Stappen konden niet worden bijgewerkt."
    HealthConnectState.CONNECTED,
    HealthConnectState.NO_DATA -> when (status.stepDataFreshness) {
        HealthConnectStepDataFreshness.FRESH -> status.stepsToday
            ?.let { "$it stappen · ${healthConnectStepSourceLabel(status)}" }
            ?: "Nog geen recente stappen."
        HealthConnectStepDataFreshness.STALE_CACHE -> status.stepsToday
            ?.let { "Laatst bekend: $it stappen · ${healthConnectStepSourceLabel(status)}" }
            ?: "Nog geen recente stappen."
        HealthConnectStepDataFreshness.PERMISSION_MISSING -> "Stappentoegang ontbreekt."
        HealthConnectStepDataFreshness.UNAVAILABLE -> "Health Connect is niet beschikbaar."
        HealthConnectStepDataFreshness.ERROR -> "Stappen konden niet worden bijgewerkt."
        HealthConnectStepDataFreshness.UNKNOWN -> "Stappenstatus wordt opgehaald."
    }
}

internal fun homeHealthSyncSummary(status: HealthConnectStatus): String? {
    val updatedAt = status.stepDataUpdatedAt ?: status.lastSyncedAt ?: return null
    return when (status.stepDataFreshness) {
        HealthConnectStepDataFreshness.FRESH -> "Bijgewerkt om ${formatHomeLastSync(updatedAt)}"
        HealthConnectStepDataFreshness.STALE_CACHE -> "Laatste update om ${formatHomeLastSync(updatedAt)}"
        else -> null
    }
}

internal fun showCompactHomeHealthCard(status: HealthConnectStatus): Boolean =
    (status.state == HealthConnectState.CONNECTED || status.state == HealthConnectState.NO_DATA) &&
        status.stepDataFreshness != HealthConnectStepDataFreshness.PERMISSION_MISSING

internal fun formatHomeLastSync(lastSyncedAt: Long?): String {
    lastSyncedAt ?: return "nog niet"
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return Instant.ofEpochMilli(lastSyncedAt)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

@Composable
private fun DiscoveryCard(onOpenCoach: () -> Unit) {
    AppCard(accent = MaterialTheme.colorScheme.tertiary) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
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
            PrimaryActionButton(onClick = onOpenCoach, modifier = Modifier.fillMaxWidth()) { Text("Profiel invullen") }
            SecondaryActionButton(onClick = onOpenTrain, modifier = Modifier.fillMaxWidth()) { Text("Routine maken") }
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
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text("Volgende training", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.amber, fontWeight = FontWeight.SemiBold)
            if (dashboard.nextWorkout == null) {
                Text("Geen sessie klaar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("Er staat nog geen actieve trainingsdag klaar. Ga naar Training om je eerste sessie in te stellen.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
                SecondaryActionButton(onClick = onOpenTrain) { Text("Training openen") }
            } else {
                val exerciseSummary = remember(dashboard.nextWorkout.exercises) {
                    dashboard.nextWorkout.exercises.joinToString { it.exercise.name }
                        .ifBlank { "Voeg oefeningen toe aan deze dag." }
                }
                Text(dashboard.nextWorkout.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    exerciseSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.trainIqColors.mutedText,
                )
                nextWorkoutIntensityLabel(dashboard.nextWorkout)?.let { intensityLabel ->
                    Text(
                        intensityLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                PrimaryActionButton(onClick = { onStartWorkout(dashboard.nextWorkout.id) }, modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) { Text("Training starten") }
            }
        }
    }
}

@Composable
private fun HomeStartupPlaceholder(modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) { index ->
                val widthFraction = when (index) {
                    0 -> 0.5f
                    3 -> 0.85f
                    else -> 1f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(widthFraction)
                        .height(if (index == 0) 24.dp else 16.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                )
            }
        }
    }
}

internal fun nextWorkoutIntensityLabel(day: com.trainiq.domain.model.WorkoutDay): String? {
    val plannedRpeValues = day.exercises
        .mapNotNull { exercise ->
            exercise.sets.map { it.targetRpe }.filter { it > 0.0 }.average().takeIf { !it.isNaN() }
                ?: exercise.targetRpe.takeIf { it > 0.0 }
        }
    if (plannedRpeValues.isEmpty()) return null
    val averageRpe = plannedRpeValues.average()
    return "Doel RPE ${formatHomeRpe(averageRpe)}"
}

private fun formatHomeRpe(rpe: Double): String {
    val rounded = kotlin.math.round(rpe * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

@Composable
private fun CoachInsightCard(
    insight: String,
    onOpenCoach: () -> Unit,
) {
    AppCard(accent = MaterialTheme.trainIqColors.amber) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.trainIqColors.amber.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.03f),
                        ),
                    ),
                )
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text("Coach-inzicht", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.amber, fontWeight = FontWeight.SemiBold)
            Text("Lokale analyse", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(insight, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
            SecondaryActionButton(onClick = onOpenCoach, modifier = Modifier.fillMaxWidth()) { Text("Coach openen") }
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
