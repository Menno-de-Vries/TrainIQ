package com.trainiq.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.trainiq.features.workout.WorkoutRoute
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
data class CameraScanner(val contextHint: String = "", val scannerMode: String = ScannerMode.AI_MEAL.name)

private data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun TrainIqApp() {
    val navController = rememberNavController()
    val haptics = LocalHapticFeedback.current
    val items = listOf(
        TopLevelDestination(Home, Home::class, "Home", Icons.Default.Home),
        TopLevelDestination(Train, Train::class, "Train", Icons.AutoMirrored.Filled.DirectionsRun),
        TopLevelDestination(Nutrition, Nutrition::class, "Nutrition", Icons.Default.Restaurant),
        TopLevelDestination(Progress, Progress::class, "Progress", Icons.Default.AutoGraph),
        TopLevelDestination(Coach, Coach::class, "Coach", Icons.Default.SmartToy),
        TopLevelDestination(Settings, Settings::class, "Settings", Icons.Default.Settings),
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var navVisible by remember { mutableStateOf(false) }
    val navOffset by animateDpAsState(
        targetValue = if (navVisible) 0.dp else 28.dp,
        animationSpec = tween(durationMillis = 420),
        label = "nav-offset",
    )

    LaunchedEffect(Unit) {
        navVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            bottomBar = {
                if (
                    currentDestination?.hierarchy?.any {
                        it.hasRoute(ActiveWorkout::class) || it.hasRoute(CameraScanner::class)
                    } != true
                ) {
                    Surface(
                        modifier = Modifier
                            .offset(y = navOffset)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 0.dp,
                        shape = RoundedCornerShape(28.dp),
                    ) {
                        NavigationBar(
                            modifier = Modifier.height(90.dp),
                            tonalElevation = 8.dp,
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            items.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any { it.hasRoute(screen.routeClass) } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        haptics.performHapticFeedback(
                                            if (screen.routeClass == Coach::class) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
                                        )
                                        if (selected) return@NavigationBarItem
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        AnimatedContent(
                                            targetState = selected,
                                            transitionSpec = { fadeIn(animationSpec = spring(stiffness = 450f)) togetherWith fadeOut(animationSpec = spring(stiffness = 450f)) },
                                            label = "nav-icon",
                                        ) { isSelected ->
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                                        shape = CircleShape,
                                                    )
                                                    .padding(12.dp),
                                            ) {
                                                Icon(
                                                    imageVector = screen.icon,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .semantics { contentDescription = screen.label }
                                                        .scale(if (isSelected) 1.15f else 1f),
                                                )
                                            }
                                        }
                                    },
                                    label = null,
                                    alwaysShowLabel = false,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurface,
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
                modifier = Modifier.padding(padding),
            )
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
                onStartWorkout = { dayId -> navController.navigate(ActiveWorkout(dayId)) },
                onOpenCoach = { navController.navigate(Coach) },
                onOpenTrain = { navController.navigate(Train) },
                onOpenSettings = { navController.navigate(Settings) },
            )
        }
        composable<Train> {
            WorkoutRoute(onStartWorkout = { dayId -> navController.navigate(ActiveWorkout(dayId)) })
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
                scannerMode = ScannerMode.valueOf(route.scannerMode),
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
            )
        }
    }
}
