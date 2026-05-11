package com.trainiq.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
import com.trainiq.features.coach.CoachRoute
import com.trainiq.features.home.HomeRoute
import com.trainiq.features.nutrition.CameraScannerRoute
import com.trainiq.features.nutrition.NutritionRoute
import com.trainiq.features.nutrition.ScannerMode
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
import kotlin.math.abs
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

@Serializable
data object Home

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

@Composable
fun TrainIqApp(
    diagnosticsTracker: DiagnosticsTracker,
    windowWidthClass: TrainIqWindowWidthClass = TrainIqWindowWidthClass.Compact,
) {
    val navController = rememberNavController()
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val items = listOf(
        TopLevelDestination(Home, Home::class, "Start", icon = Icons.Default.Home),
        TopLevelDestination(Train, Train::class, "Training", icon = Icons.AutoMirrored.Filled.DirectionsRun),
        TopLevelDestination(Nutrition, Nutrition::class, "Voeding", icon = Icons.Default.Restaurant),
        TopLevelDestination(Progress, Progress::class, "Voortgang", icon = Icons.Default.AutoGraph),
        TopLevelDestination(Coach, Coach::class, "Coach", icon = Icons.Default.SmartToy),
        TopLevelDestination(Settings, Settings::class, "Instellingen", icon = Icons.Default.Settings),
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentTopLevelIndex = items.indexOfFirst { screen ->
        currentDestination?.hierarchy?.any { it.hasRoute(screen.routeClass) } == true
    }.takeIf { it >= 0 }
    val isNutritionDestination = currentDestination?.hierarchy?.any { it.hasRoute(Nutrition::class) } == true
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
    val currentNavigationIndex = navigationItems.indexOfFirst { screen ->
        currentDestination?.hierarchy?.any { it.hasRoute(screen.routeClass) } == true
    }.takeIf { it >= 0 }
    var navVisible by remember { mutableStateOf(true) }
    var trainDetailMode by remember { mutableStateOf(false) }
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail && currentTopLevelIndex != null && !imeVisible && !trainDetailMode) {
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
                if (!useNavigationRail && currentTopLevelIndex != null && !imeVisible && !trainDetailMode) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = navOffset)
                            .padding(
                                horizontal = 10.dp,
                                vertical = if (useCompactShortBottomBar) 4.dp else 10.dp,
                            )
                            .navigationBarsPadding(),
                        color = MaterialTheme.trainIqColors.card,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.trainIqColors.cardBorder),
                        shape = RoundedCornerShape(MaterialTheme.radii.nav),
                    ) {
                        NavigationBar(
                            modifier = Modifier.height(if (useCompactShortBottomBar) 58.dp else 82.dp),
                            tonalElevation = 0.dp,
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ) {
                            navigationItems.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any { it.hasRoute(screen.routeClass) } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        diagnosticsTracker.tap("Nav:${screen.label}")
                                        haptics.performHapticFeedback(
                                            if (screen.routeClass == Coach::class) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
                                        )
                                        if (selected) return@NavigationBarItem
                                        navController.navigateTopLevel(screen)
                                    },
                                    icon = {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 44.dp, height = 34.dp)
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
                onTrainDetailModeChanged = { trainDetailMode = it },
                modifier = Modifier
                    .padding(padding)
                    .topLevelTabSwipeNavigation(
                        enabled = currentNavigationIndex != null && !imeVisible && !isNutritionDestination,
                        onSwipeLeft = {
                            val currentIndex = currentNavigationIndex ?: return@topLevelTabSwipeNavigation
                            val next = navigationItems.getOrNull(currentIndex + 1) ?: return@topLevelTabSwipeNavigation
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.navigateTopLevel(next)
                        },
                        onSwipeRight = {
                            val currentIndex = currentNavigationIndex ?: return@topLevelTabSwipeNavigation
                            val previous = navigationItems.getOrNull(currentIndex - 1) ?: return@topLevelTabSwipeNavigation
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.navigateTopLevel(previous)
                        },
                    ),
            )
        }
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
    val alreadyActiveWorkout = currentBackStackEntry?.destination?.hierarchy?.any { it.hasRoute(ActiveWorkout::class) } == true
    if (alreadyActiveWorkout) return
    navigate(ActiveWorkout(dayId)) {
        launchSingleTop = true
    }
}

internal fun shouldClearTrainDetailMode(isTrainDestination: Boolean, isTopLevelDestination: Boolean): Boolean =
    !isTrainDestination && isTopLevelDestination

internal fun shouldUseNavigationRail(widthClass: TrainIqWindowWidthClass): Boolean =
    widthClass != TrainIqWindowWidthClass.Compact

internal fun shouldUseCompactShortBottomBar(
    widthClass: TrainIqWindowWidthClass,
    screenHeightDp: Int,
): Boolean =
    widthClass == TrainIqWindowWidthClass.Compact && screenHeightDp <= 640

private fun bottomNavigationDestinations(
    items: List<TopLevelDestination>,
    windowWidthClass: TrainIqWindowWidthClass,
): List<TopLevelDestination> =
    if (windowWidthClass == TrainIqWindowWidthClass.Compact) {
        items.filterNot { it.routeClass == Progress::class }
    } else {
        items
    }

internal fun compactBottomNavigationRouteClasses(): List<KClass<*>> =
    bottomNavigationDestinations(
        items = listOf(
            TopLevelDestination(Home, Home::class, "Start", icon = Icons.Default.Home),
            TopLevelDestination(Train, Train::class, "Training", icon = Icons.AutoMirrored.Filled.DirectionsRun),
            TopLevelDestination(Nutrition, Nutrition::class, "Voeding", icon = Icons.Default.Restaurant),
            TopLevelDestination(Progress, Progress::class, "Voortgang", icon = Icons.Default.AutoGraph),
            TopLevelDestination(Coach, Coach::class, "Coach", icon = Icons.Default.SmartToy),
            TopLevelDestination(Settings, Settings::class, "Instellingen", icon = Icons.Default.Settings),
        ),
        windowWidthClass = TrainIqWindowWidthClass.Compact,
    ).map { it.routeClass }

internal fun compactSwipeNavigationRouteClasses(): List<KClass<*>> =
    compactBottomNavigationRouteClasses()

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

private fun Modifier.topLevelTabSwipeNavigation(
    enabled: Boolean,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(onSwipeLeft, onSwipeRight) {
        val threshold = 112.dp.toPx()
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            var totalX = 0f
            var totalY = 0f
            var childConsumedGesture = false
            do {
                val event = awaitPointerEvent(pass = PointerEventPass.Final)
                if (event.changes.any { it.isConsumed }) {
                    childConsumedGesture = true
                }
                event.changes.forEach { change ->
                    val delta = change.positionChange()
                    totalX += delta.x
                    totalY += delta.y
                }
            } while (event.changes.any { it.pressed })

            if (!childConsumedGesture && abs(totalX) > threshold && abs(totalX) > abs(totalY) * 1.6f) {
                if (totalX < 0f) onSwipeLeft() else onSwipeRight()
            }
        }
    }
}

@Composable
private fun TrainIqNavHost(
    navController: NavHostController,
    topLevelDestinations: List<TopLevelDestination>,
    windowWidthClass: TrainIqWindowWidthClass,
    onTrainDetailModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier,
    ) {
        composable<Home> {
            HomeRoute(
                onStartWorkout = { dayId -> navController.navigateToActiveWorkout(dayId) },
                onOpenCoach = { navController.navigateTopLevel(topLevelDestinations.first { it.routeClass == Coach::class }) },
                onOpenTrain = { navController.navigateTopLevel(topLevelDestinations.first { it.routeClass == Train::class }) },
                onOpenSettings = { navController.navigateTopLevel(topLevelDestinations.first { it.routeClass == Settings::class }) },
                windowWidthClass = windowWidthClass,
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
                onOpenAiScanner = { contextHint -> navController.navigate(CameraScanner(contextHint)) },
                onOpenBarcodeScanner = { navController.navigate(CameraScanner(scannerMode = ScannerMode.BARCODE)) },
                pendingBarcode = pendingBarcode.takeIf { it.isNotEmpty() },
                onBarcodeClear = { entry.clearBarcodeScanResult() },
                windowWidthClass = windowWidthClass,
            )
        }
        composable<Progress> { ProgressRoute(windowWidthClass = windowWidthClass) }
        composable<Coach> { CoachRoute(windowWidthClass = windowWidthClass) }
        composable<Settings> {
            SettingsRoute(
                windowWidthClass = windowWidthClass,
                onOpenProgress = { navController.navigateTopLevel(topLevelDestinations.first { it.routeClass == Progress::class }) },
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
            )
        }
        composable<ActiveWorkout> { entry ->
            val route = entry.toRoute<ActiveWorkout>()
            ActiveWorkoutRoute(
                dayId = route.dayId,
                onBack = { navController.popBackStack() },
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

internal fun NavBackStackEntry.setBarcodeScanResult(barcode: String) {
    savedStateHandle[BarcodeScanResultKey] = barcode
}

internal fun NavBackStackEntry.clearBarcodeScanResult() {
    savedStateHandle.remove<String>(BarcodeScanResultKey)
}
