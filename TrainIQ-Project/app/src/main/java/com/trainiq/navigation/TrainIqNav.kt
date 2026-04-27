package com.trainiq.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
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
data class ExerciseHistory(val exerciseId: Long)

@Serializable
data class CameraScanner(val contextHint: String = "", val scannerMode: String = ScannerMode.AI_MEAL.name)

private data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun TrainIqApp(diagnosticsTracker: DiagnosticsTracker) {
    val navController = rememberNavController()
    val haptics = LocalHapticFeedback.current
    val items = listOf(
        TopLevelDestination(Home, Home::class, "Start", Icons.Default.Home),
        TopLevelDestination(Train, Train::class, "Training", Icons.AutoMirrored.Filled.DirectionsRun),
        TopLevelDestination(Nutrition, Nutrition::class, "Voeding", Icons.Default.Restaurant),
        TopLevelDestination(Progress, Progress::class, "Voortgang", Icons.Default.AutoGraph),
        TopLevelDestination(Coach, Coach::class, "Coach", Icons.Default.SmartToy),
        TopLevelDestination(Settings, Settings::class, "Instellingen", Icons.Default.Settings),
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentTopLevelIndex = items.indexOfFirst { screen ->
        currentDestination?.hierarchy?.any { it.hasRoute(screen.routeClass) } == true
    }.takeIf { it >= 0 }
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    var navVisible by remember { mutableStateOf(false) }
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
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppScaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (currentTopLevelIndex != null && !imeVisible) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = navOffset)
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        color = MaterialTheme.trainIqColors.card,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.trainIqColors.cardBorder),
                        shape = RoundedCornerShape(MaterialTheme.radii.nav),
                    ) {
                        NavigationBar(
                            modifier = Modifier.height(64.dp),
                            tonalElevation = 0.dp,
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ) {
                            items.forEach { screen ->
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
                                                contentDescription = screen.label,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    },
                                    alwaysShowLabel = false,
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
                modifier = Modifier
                    .padding(padding)
                    .topLevelTabSwipeNavigation(
                        enabled = currentTopLevelIndex != null && !imeVisible,
                        onSwipeLeft = {
                            val currentIndex = currentTopLevelIndex ?: return@topLevelTabSwipeNavigation
                            val next = items.getOrNull(currentIndex + 1) ?: return@topLevelTabSwipeNavigation
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.navigateTopLevel(next)
                        },
                        onSwipeRight = {
                            val currentIndex = currentTopLevelIndex ?: return@topLevelTabSwipeNavigation
                            val previous = items.getOrNull(currentIndex - 1) ?: return@topLevelTabSwipeNavigation
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.navigateTopLevel(previous)
                        },
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

private fun NavHostController.navigateToActiveWorkout(dayId: Long) {
    val alreadyActiveWorkout = currentBackStackEntry?.destination?.hierarchy?.any { it.hasRoute(ActiveWorkout::class) } == true
    if (alreadyActiveWorkout) return
    navigate(ActiveWorkout(dayId)) {
        launchSingleTop = true
    }
}

private fun androidx.navigation.NavDestination?.screenName(): String = when {
    this == null -> "Unknown"
    hierarchy.any { it.hasRoute(Home::class) } -> "Home"
    hierarchy.any { it.hasRoute(Train::class) } -> "Train"
    hierarchy.any { it.hasRoute(Nutrition::class) } -> "Nutrition"
    hierarchy.any { it.hasRoute(Progress::class) } -> "Progress"
    hierarchy.any { it.hasRoute(Coach::class) } -> "Coach"
    hierarchy.any { it.hasRoute(Settings::class) } -> "Settings"
    hierarchy.any { it.hasRoute(CameraScanner::class) } -> "CameraScanner"
    hierarchy.any { it.hasRoute(ActiveWorkout::class) } -> "ActiveWorkout"
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
                onOpenCoach = { navController.navigate(Coach) },
                onOpenTrain = { navController.navigate(Train) },
                onOpenSettings = { navController.navigate(Settings) },
            )
        }
        composable<Train> {
            WorkoutRoute(
                onStartWorkout = { dayId -> navController.navigateToActiveWorkout(dayId) },
                onOpenExerciseHistory = { exerciseId -> navController.navigate(ExerciseHistory(exerciseId)) },
            )
        }
        composable<Nutrition> { entry ->
            val pendingBarcode by entry.savedStateHandle
                .getStateFlow("scanned_barcode", "")
                .collectAsStateWithLifecycle()
            NutritionRoute(
                onOpenAiScanner = { contextHint -> navController.navigate(CameraScanner(contextHint)) },
                onOpenBarcodeScanner = { navController.navigate(CameraScanner(scannerMode = ScannerMode.BARCODE.name)) },
                pendingBarcode = pendingBarcode.takeIf { it.isNotEmpty() },
                onBarcodeClear = { entry.savedStateHandle.remove<String>("scanned_barcode") },
            )
        }
        composable<Progress> { ProgressRoute() }
        composable<Coach> { CoachRoute() }
        composable<Settings> { SettingsRoute() }
        composable<CameraScanner> { entry ->
            val route = entry.toRoute<CameraScanner>()
            CameraScannerRoute(
                contextHint = route.contextHint,
                scannerMode = scannerModeFromRoute(route.scannerMode),
                onBack = { navController.popBackStack() },
                onBarcodeScanned = { barcode ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("scanned_barcode", barcode)
                    navController.popBackStack()
                },
            )
        }
        composable<ActiveWorkout> { entry ->
            ActiveWorkoutRoute(
                dayId = entry.toRoute<ActiveWorkout>().dayId,
                onBack = { navController.popBackStack() },
                onOpenExerciseHistory = { exerciseId -> navController.navigate(ExerciseHistory(exerciseId)) },
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

internal fun scannerModeFromRoute(value: String): ScannerMode =
    runCatching { ScannerMode.valueOf(value) }.getOrDefault(ScannerMode.AI_MEAL)
