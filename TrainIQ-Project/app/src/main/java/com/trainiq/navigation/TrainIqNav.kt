package com.trainiq.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trainiq.core.datastore.OnboardingPreferences
import com.trainiq.features.coach.CoachRoute
import com.trainiq.features.home.HomeRoute
import com.trainiq.features.nutrition.CameraScannerRoute
import com.trainiq.features.nutrition.NutritionRoute
import com.trainiq.features.nutrition.ScannerMode
import com.trainiq.features.onboarding.OnboardingRoute
import com.trainiq.features.onboarding.shouldShowGuidedTour
import com.trainiq.features.progress.ProgressRoute
import com.trainiq.features.settings.SettingsRoute
import com.trainiq.features.workout.ActiveWorkoutRoute
import com.trainiq.features.workout.ExerciseHistoryRoute
import com.trainiq.features.workout.WorkoutCompletionRoute
import com.trainiq.features.workout.WorkoutProcessingRoute
import com.trainiq.features.workout.WorkoutRoute
import com.trainiq.core.diagnostics.DiagnosticsTracker
import com.trainiq.core.theme.radii
import com.trainiq.core.theme.trainIqColors
import com.trainiq.core.ui.AppScaffold
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data object Onboarding

@Serializable
data object Train

@Serializable
data object Nutrition

@Serializable
data object Progress

@Serializable
data object Coach

@Serializable
data object Settings

@Serializable
data class ActiveWorkout(val dayId: Long)

@Serializable
data class WorkoutProcessing(val dayId: Long)

@Serializable
data class WorkoutCompletion(val sessionId: Long)

@Serializable
data class ExerciseHistory(val exerciseId: Long)

@Serializable
data class CameraScanner(val contextHint: String = "", val scannerMode: ScannerMode = ScannerMode.AI_MEAL)

enum class TrainIqWindowWidthClass {
    Compact,
    Medium,
    Expanded,
}

private data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val bottomLabel: String = bottomNavigationLabel(label),
    val icon: ImageVector,
)

private data class GuidedTourStep(
    val destination: TopLevelDestination,
    val title: String,
    val description: String,
)

@Composable
fun TrainIqApp(
    diagnosticsTracker: DiagnosticsTracker,
    windowWidthClass: TrainIqWindowWidthClass = TrainIqWindowWidthClass.Compact,
    onboardingPreferences: OnboardingPreferences = OnboardingPreferences(completed = true, guidedTourCompleted = true),
    markGuidedTourCompleted: () -> Unit = {},
    markGuidedTourSkipped: () -> Unit = {},
) {
    val navController = rememberNavController()
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val items = topLevelDestinations()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentTopLevelIndex = items.indexOfFirst { screen ->
        currentDestination?.hierarchy?.any { it.hasRoute(screen.routeClass) } == true
    }.takeIf { it >= 0 }
    val compactSelectedRouteClass = compactSelectedNavigationRouteClass(
        currentTopLevelIndex?.let { items[it].routeClass },
    )
    val isOnboardingDestination = currentDestination?.hierarchy?.any { it.hasRoute(Onboarding::class) } == true
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val useNavigationRail = shouldUseNavigationRail(windowWidthClass)
    val useCompactShortBottomBar = shouldUseCompactShortBottomBar(
        widthClass = windowWidthClass,
        screenHeightDp = configuration.screenHeightDp,
    )
    val navigationItems = if (useNavigationRail) {
        items
    } else {
        bottomNavigationDestinations(items = items, windowWidthClass = windowWidthClass)
    }
    var navVisible by remember { mutableStateOf(true) }
    var trainDetailMode by remember { mutableStateOf(false) }
    var guidedTourIndex by remember(
        onboardingPreferences.completed,
        onboardingPreferences.guidedTourCompleted,
        onboardingPreferences.guidedTourSkipped,
    ) { mutableStateOf(0) }
    val guidedTourSteps = remember(items) { guidedTourSteps(items) }
    val showGuidedTour = shouldShowGuidedTour(onboardingPreferences) && !isOnboardingDestination
    val navOffset by animateDpAsState(
        targetValue = if (navVisible) 0.dp else 28.dp,
        animationSpec = tween(durationMillis = 420),
        label = "nav-offset",
    )

    LaunchedEffect(Unit) {
        navVisible = true
    }
    LaunchedEffect(currentDestination?.route) {
        diagnosticsTracker.screen(currentDestination.screenName())
        val isTrainDestination = currentDestination?.hierarchy?.any { it.hasRoute(Train::class) } == true
        if (shouldClearTrainDetailMode(isTrainDestination, currentTopLevelIndex != null)) {
            trainDetailMode = false
        }
    }
    LaunchedEffect(showGuidedTour, guidedTourIndex) {
        if (!showGuidedTour) return@LaunchedEffect
        guidedTourSteps.getOrNull(guidedTourIndex)?.let { step ->
            navController.navigateTopLevel(step.destination)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail && currentTopLevelIndex != null && !imeVisible && !trainDetailMode && !isOnboardingDestination) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(92.dp)
                        .navigationBarsPadding()
                        .padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.background,
                    ) {
                        items.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.hasRoute(screen.routeClass) } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    diagnosticsTracker.tap("Nav:${screen.label}")
                                    haptics.performHapticFeedback(
                                        if (screen.routeClass == Coach::class) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
                                    )
                                    if (selected) return@NavigationRailItem
                                    navController.navigateTopLevel(screen)
                                },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.label,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                                label = { Text(screen.bottomLabel, maxLines = 1) },
                                alwaysShowLabel = false,
                            )
                        }
                    }
                }
            }
        AppScaffold(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            bottomBar = {
                if (!useNavigationRail && currentTopLevelIndex != null && !imeVisible && !trainDetailMode && !isOnboardingDestination) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = navOffset)
                            .padding(
                                horizontal = 0.dp,
                                vertical = 0.dp,
                            )
                            .navigationBarsPadding(),
                        color = MaterialTheme.trainIqColors.card,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.trainIqColors.cardBorder),
                        shape = RoundedCornerShape(2.dp),
                    ) {
                        NavigationBar(
                            modifier = Modifier.height(if (useCompactShortBottomBar) 50.dp else 62.dp),
                            tonalElevation = 0.dp,
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ) {
                            navigationItems.forEach { screen ->
                                val isCurrentRoute = currentDestination?.hierarchy?.any { it.hasRoute(screen.routeClass) } == true
                                val selected = compactSelectedRouteClass == screen.routeClass
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        diagnosticsTracker.tap("Nav:${screen.label}")
                                        haptics.performHapticFeedback(
                                            if (screen.routeClass == Coach::class) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
                                        )
                                        if (isCurrentRoute) return@NavigationBarItem
                                        navController.navigateTopLevel(screen)
                                    },
                                    icon = {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 42.dp, height = if (useCompactShortBottomBar) 30.dp else 28.dp)
                                                .background(
                                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f) else Color.Transparent,
                                                    shape = CircleShape,
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = if (useCompactShortBottomBar) screen.label else null,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    },
                                    label = if (useCompactShortBottomBar) null else {
                                        { Text(screen.bottomLabel, maxLines = 1) }
                                    },
                                    alwaysShowLabel = !useCompactShortBottomBar,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = MaterialTheme.trainIqColors.mutedText,
                                        unselectedTextColor = MaterialTheme.trainIqColors.mutedText,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->
            TrainIqNavHost(
                navController = navController,
                topLevelDestinations = items,
                windowWidthClass = windowWidthClass,
                onboardingPreferences = onboardingPreferences,
                onTrainDetailModeChanged = { trainDetailMode = it },
                modifier = Modifier
                    .padding(padding),
            )
        }
        }
        if (showGuidedTour) {
            val step = guidedTourSteps[guidedTourIndex.coerceIn(0, guidedTourSteps.lastIndex)]
            GuidedTourOverlay(
                step = step,
                index = guidedTourIndex,
                total = guidedTourSteps.size,
                onBack = { guidedTourIndex = (guidedTourIndex - 1).coerceAtLeast(0) },
                onNext = {
                    if (guidedTourIndex >= guidedTourSteps.lastIndex) {
                        markGuidedTourCompleted()
                    } else {
                        guidedTourIndex += 1
                    }
                },
                onSkip = markGuidedTourSkipped,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(
                        bottom = guidedTourBottomPaddingDp(
                            useNavigationRail = useNavigationRail,
                            useCompactShortBottomBar = useCompactShortBottomBar,
                        ).dp,
                    ),
            )
        }
    }
}

private fun NavHostController.navigateTopLevel(screen: TopLevelDestination) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateTopLevelAfterCompletedWorkout(screen: TopLevelDestination) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = false }
        launchSingleTop = true
        restoreState = false
    }
}

private fun NavHostController.navigateToActiveWorkout(dayId: Long) {
    val currentActiveWorkoutDayId = currentBackStackEntry
        ?.takeIf { entry -> entry.destination.hierarchy.any { it.hasRoute(ActiveWorkout::class) } }
        ?.toRoute<ActiveWorkout>()
        ?.dayId
    if (!shouldNavigateToActiveWorkout(currentActiveWorkoutDayId, dayId)) return
    val currentActiveWorkoutDestinationId = currentBackStackEntry
        ?.takeIf { currentActiveWorkoutDayId != null }
        ?.destination
        ?.id
    navigate(ActiveWorkout(dayId)) {
        currentActiveWorkoutDestinationId?.let { activeDestinationId ->
            popUpTo(activeDestinationId) { inclusive = true }
        }
        launchSingleTop = true
    }
}

internal fun shouldNavigateToActiveWorkout(currentActiveWorkoutDayId: Long?, requestedDayId: Long): Boolean =
    currentActiveWorkoutDayId != requestedDayId

internal fun shouldClearTrainDetailMode(isTrainDestination: Boolean, isTopLevelDestination: Boolean): Boolean =
    !isTrainDestination && isTopLevelDestination

internal fun shouldUseNavigationRail(widthClass: TrainIqWindowWidthClass): Boolean =
    widthClass != TrainIqWindowWidthClass.Compact

internal fun shouldUseCompactShortBottomBar(
    widthClass: TrainIqWindowWidthClass,
    screenHeightDp: Int,
): Boolean =
    widthClass == TrainIqWindowWidthClass.Compact && screenHeightDp <= 640

internal fun guidedTourBottomPaddingDp(
    useNavigationRail: Boolean,
    useCompactShortBottomBar: Boolean,
): Int = when {
    useNavigationRail -> 20
    useCompactShortBottomBar -> 58
    else -> 70
}

private fun bottomNavigationDestinations(
    items: List<TopLevelDestination>,
    windowWidthClass: TrainIqWindowWidthClass,
): List<TopLevelDestination> =
    if (windowWidthClass == TrainIqWindowWidthClass.Compact) {
        items.filterNot { it.routeClass == Progress::class }
    } else {
        items
    }

private fun topLevelDestinations(): List<TopLevelDestination> = listOf(
    TopLevelDestination(Home, Home::class, "Start", icon = Icons.Default.Home),
    TopLevelDestination(Train, Train::class, "Training", icon = Icons.AutoMirrored.Filled.DirectionsRun),
    TopLevelDestination(Nutrition, Nutrition::class, "Voeding", icon = Icons.Default.Restaurant),
    TopLevelDestination(Progress, Progress::class, "Voortgang", icon = Icons.Default.AutoGraph),
    TopLevelDestination(Coach, Coach::class, "Coach", icon = Icons.Default.SmartToy),
    TopLevelDestination(Settings, Settings::class, "Instellingen", icon = Icons.Default.Settings),
)

internal fun compactBottomNavigationRouteClasses(): List<KClass<*>> =
    bottomNavigationDestinations(
        items = topLevelDestinations(),
        windowWidthClass = TrainIqWindowWidthClass.Compact,
    ).map { it.routeClass }

internal fun navigationRailRouteClasses(): List<KClass<*>> = topLevelDestinations().map { it.routeClass }

internal fun compactSelectedNavigationRouteClass(currentRouteClass: KClass<*>?): KClass<*>? =
    if (currentRouteClass == Progress::class) Settings::class else currentRouteClass

internal fun guidedTourTopLevelRouteClasses(): List<KClass<*>> =
    listOf(Home::class, Train::class, Nutrition::class, Progress::class, Coach::class, Settings::class)

private fun guidedTourSteps(items: List<TopLevelDestination>): List<GuidedTourStep> {
    val byRoute = items.associateBy { it.routeClass }
    return listOf(
        GuidedTourStep(
            destination = byRoute.getValue(Home::class),
            title = "Start",
            description = "Je volgende training, energie-inname en Health Connect-signalen staan hier bij elkaar. Check je dagstatus en open je volgende actie.",
        ),
        GuidedTourStep(
            destination = byRoute.getValue(Train::class),
            title = "Training",
            description = "Bouw routines, start een workout en bekijk je krachtgeschiedenis. Maak of start je eerste routine.",
        ),
        GuidedTourStep(
            destination = byRoute.getValue(Nutrition::class),
            title = "Voeding",
            description = "Log maaltijden handmatig, met barcode of met AI-fotoherkenning. Leg je eerste maaltijd of product vast.",
        ),
        GuidedTourStep(
            destination = byRoute.getValue(Progress::class),
            title = "Voortgang",
            description = "Volg lichaamsmetingen, krachttrend en historie. Voeg een lichaamsmeting toe of bekijk je trends.",
        ),
        GuidedTourStep(
            destination = byRoute.getValue(Coach::class),
            title = "Coach",
            description = "Beheer je profiel, caloriedoel, automatische macro's en advies. Vul je profiel en calorie doel in voor betere coaching.",
        ),
        GuidedTourStep(
            destination = byRoute.getValue(Settings::class),
            title = "Instellingen",
            description = "Beheer Health Connect, AI, privacy, export, thema en reminders. Controleer Health Connect, AI en reminders wanneer je setup nog openstaat.",
        ),
    )
}

fun adaptiveDashboardGridColumns(widthClass: TrainIqWindowWidthClass): Int = when (widthClass) {
    TrainIqWindowWidthClass.Compact -> 2
    TrainIqWindowWidthClass.Medium -> 3
    TrainIqWindowWidthClass.Expanded -> 4
}

fun adaptiveContentMaxWidthDp(widthClass: TrainIqWindowWidthClass): Int = when (widthClass) {
    TrainIqWindowWidthClass.Compact -> Int.MAX_VALUE
    TrainIqWindowWidthClass.Medium -> 840
    TrainIqWindowWidthClass.Expanded -> 1120
}

internal fun bottomNavigationLabel(label: String): String = when (label) {
    "Voortgang" -> "Trend"
    "Instellingen" -> "Meer"
    else -> label
}

private fun androidx.navigation.NavDestination?.screenName(): String = when {
    this == null -> "Unknown"
    hierarchy.any { it.hasRoute(Home::class) } -> "Home"
    hierarchy.any { it.hasRoute(Train::class) } -> "Train"
    hierarchy.any { it.hasRoute(Nutrition::class) } -> "Nutrition"
    hierarchy.any { it.hasRoute(Progress::class) } -> "Voortgang"
    hierarchy.any { it.hasRoute(Coach::class) } -> "Coach"
    hierarchy.any { it.hasRoute(Settings::class) } -> "Instellingen"
    hierarchy.any { it.hasRoute(CameraScanner::class) } -> "CameraScanner"
    hierarchy.any { it.hasRoute(ActiveWorkout::class) } -> "ActiveWorkout"
    hierarchy.any { it.hasRoute(WorkoutProcessing::class) } -> "WorkoutProcessing"
    hierarchy.any { it.hasRoute(WorkoutCompletion::class) } -> "WorkoutCompletion"
    hierarchy.any { it.hasRoute(ExerciseHistory::class) } -> "ExerciseHistory"
    else -> route.orEmpty().ifBlank { "Unknown" }
}

@Composable
private fun GuidedTourOverlay(
    step: GuidedTourStep,
    index: Int,
    total: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .heightIn(min = 0.dp),
        color = MaterialTheme.trainIqColors.card,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.trainIqColors.cardBorder),
        shape = RoundedCornerShape(MaterialTheme.radii.card),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Stap ${index + 1} van $total",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Later afronden", maxLines = 1)
                }
            }
            Text(step.title, style = MaterialTheme.typography.titleMedium)
            Text(step.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = index > 0,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Terug", maxLines = 1)
                }
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Text(if (index == total - 1) "Tour afronden" else "Volgende", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TrainIqNavHost(
    navController: NavHostController,
    topLevelDestinations: List<TopLevelDestination>,
    windowWidthClass: TrainIqWindowWidthClass,
    onboardingPreferences: OnboardingPreferences,
    onTrainDetailModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = if (onboardingPreferences.completed) Home else Onboarding,
        modifier = modifier,
    ) {
        composable<Onboarding> {
            OnboardingRoute(
                onFinished = {
                    navController.navigate(Home) {
                        popUpTo<Onboarding> { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<Home> {
            HomeRoute(
                onStartWorkout = { dayId -> navController.navigateToActiveWorkout(dayId) },
                onOpenCoach = { navController.navigateTopLevel(topLevelDestinations.first { it.routeClass == Coach::class }) },
                onOpenTrain = { navController.navigateTopLevel(topLevelDestinations.first { it.routeClass == Train::class }) },
                onOpenSettings = { navController.navigateTopLevel(topLevelDestinations.first { it.routeClass == Settings::class }) },
            )
        }
        composable<Train> {
            WorkoutRoute(
                onStartWorkout = { dayId -> navController.navigateToActiveWorkout(dayId) },
                onOpenExerciseHistory = { exerciseId -> navController.navigate(ExerciseHistory(exerciseId)) },
                onDetailModeChanged = onTrainDetailModeChanged,
                windowWidthClass = windowWidthClass,
            )
        }
        composable<Nutrition> { entry ->
            val pendingBarcode by entry.savedStateHandle
                .getStateFlow(BarcodeScanResultKey, "")
                .collectAsStateWithLifecycle()
            NutritionRoute(
                onAiScanner = { contextHint -> navController.navigate(CameraScanner(contextHint)) },
                onOpenBarcodeScanner = { navController.navigate(CameraScanner(scannerMode = ScannerMode.BARCODE)) },
                pendingBarcode = pendingBarcode.takeIf { it.isNotEmpty() },
                onBarcodeClear = { entry.clearBarcodeScanResult() },
                windowWidthClass = windowWidthClass,
            )
        }
        composable<Progress> { entry ->
            val pendingScaleWeight by entry.savedStateHandle
                .getStateFlow(ScaleWeightResultKey, "")
                .collectAsStateWithLifecycle()
            val pendingScaleBodyFat by entry.savedStateHandle
                .getStateFlow(ScaleBodyFatResultKey, "")
                .collectAsStateWithLifecycle()
            val pendingScaleMuscleMass by entry.savedStateHandle
                .getStateFlow(ScaleMuscleMassResultKey, "")
                .collectAsStateWithLifecycle()
            val pendingScaleNotes by entry.savedStateHandle
                .getStateFlow(ScaleNotesResultKey, "")
                .collectAsStateWithLifecycle()
            ProgressRoute(
                windowWidthClass = windowWidthClass,
                pendingScaleWeight = pendingScaleWeight.takeIf { it.isNotBlank() },
                pendingScaleBodyFat = pendingScaleBodyFat.takeIf { it.isNotBlank() },
                pendingScaleMuscleMass = pendingScaleMuscleMass.takeIf { it.isNotBlank() },
                pendingScaleNotes = pendingScaleNotes.takeIf { it.isNotBlank() },
                onScaleResultConsumed = { entry.clearScaleMeasurementResult() },
                onOpenScaleScanner = { navController.navigate(CameraScanner(contextHint = "Lees gewicht, vetpercentage en spiermassa uit van de smart-weegschaal.", scannerMode = ScannerMode.AI_SCALE)) },
            )
        }
        composable<Coach> { CoachRoute(windowWidthClass = windowWidthClass) }
        composable<Settings> {
            SettingsRoute(
                windowWidthClass = windowWidthClass,
                onOpenProgress = {
                    navController.navigateTopLevel(topLevelDestinations.first { it.routeClass == Progress::class })
                },
                onOpenOnboarding = {
                    navController.navigate(Onboarding) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<CameraScanner> { entry ->
            val route = entry.toRoute<CameraScanner>()
            CameraScannerRoute(
                contextHint = route.contextHint,
                scannerMode = route.scannerMode,
                onBack = { navController.popBackStack() },
                onBarcodeScanned = { barcode ->
                    navController.previousBackStackEntry?.setBarcodeScanResult(barcode)
                    navController.popBackStack()
                },
                onScaleMeasurementScanned = { result ->
                    navController.previousBackStackEntry?.setScaleMeasurementResult(result)
                },
            )
        }
        composable<ActiveWorkout> { entry ->
            val route = entry.toRoute<ActiveWorkout>()
            ActiveWorkoutRoute(
                dayId = route.dayId,
                onBack = { navController.popBackStack() },
                onSwitchActiveWorkout = { nextDayId -> navController.navigateToActiveWorkout(nextDayId) },
                onOpenExerciseHistory = { exerciseId -> navController.navigate(ExerciseHistory(exerciseId)) },
                onWorkoutCompleted = { sessionId ->
                    val activeDestinationId = navController.currentBackStackEntry?.destination?.id
                    navController.navigate(WorkoutCompletion(sessionId)) {
                        if (activeDestinationId != null) {
                            popUpTo(activeDestinationId) { inclusive = true }
                        }
                        launchSingleTop = true
                    }
                },
                onWorkoutProcessing = { dayId ->
                    navController.navigate(WorkoutProcessing(dayId)) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<WorkoutProcessing> { entry ->
            WorkoutProcessingRoute(
                dayId = entry.toRoute<WorkoutProcessing>().dayId,
                onComplete = { sessionId ->
                    navController.navigate(WorkoutCompletion(sessionId)) {
                        popUpTo<Train> {
                            inclusive = false
                            saveState = false
                        }
                        launchSingleTop = true
                    }
                },
                onBackToTraining = { navController.popBackStack() },
            )
        }
        composable<WorkoutCompletion> { entry ->
            WorkoutCompletionRoute(
                sessionId = entry.toRoute<WorkoutCompletion>().sessionId,
                onBackToTraining = {
                    navController.navigateTopLevelAfterCompletedWorkout(topLevelDestinations.first { it.routeClass == Train::class })
                },
                onHome = {
                    navController.navigateTopLevelAfterCompletedWorkout(topLevelDestinations.first { it.routeClass == Home::class })
                },
            )
        }
        composable<ExerciseHistory> { entry ->
            ExerciseHistoryRoute(
                exerciseId = entry.toRoute<ExerciseHistory>().exerciseId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

internal const val BarcodeScanResultKey = "scanned_barcode"
internal const val ScaleWeightResultKey = "scale_weight"
internal const val ScaleBodyFatResultKey = "scale_body_fat"
internal const val ScaleMuscleMassResultKey = "scale_muscle_mass"
internal const val ScaleNotesResultKey = "scale_notes"

internal fun NavBackStackEntry.setBarcodeScanResult(barcode: String) {
    savedStateHandle[BarcodeScanResultKey] = barcode
}

internal fun NavBackStackEntry.clearBarcodeScanResult() {
    savedStateHandle[BarcodeScanResultKey] = ""
}

internal fun NavBackStackEntry.setScaleMeasurementResult(result: com.trainiq.domain.model.BodyMeasurementPhotoResult) {
    savedStateHandle[ScaleWeightResultKey] = result.weight.takeIf { it > 0.0 }?.toString().orEmpty()
    savedStateHandle[ScaleBodyFatResultKey] = result.bodyFat.takeIf { it > 0.0 }?.toString().orEmpty()
    savedStateHandle[ScaleMuscleMassResultKey] = result.muscleMass.takeIf { it > 0.0 }?.toString().orEmpty()
    savedStateHandle[ScaleNotesResultKey] = result.notes.orEmpty()
}

internal fun NavBackStackEntry.clearScaleMeasurementResult() {
    savedStateHandle[ScaleWeightResultKey] = ""
    savedStateHandle[ScaleBodyFatResultKey] = ""
    savedStateHandle[ScaleMuscleMassResultKey] = ""
    savedStateHandle[ScaleNotesResultKey] = ""
}
