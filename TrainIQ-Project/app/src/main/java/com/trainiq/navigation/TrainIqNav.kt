package com.trainiq.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trainiq.features.coach.CoachRoute
import com.trainiq.features.home.HomeRoute
import com.trainiq.features.nutrition.NutritionRoute
import com.trainiq.features.progress.ProgressRoute
import com.trainiq.features.settings.SettingsRoute
import com.trainiq.features.workout.ActiveWorkoutRoute
import com.trainiq.features.workout.WorkoutRoute

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Train : Screen("train", "Train", Icons.AutoMirrored.Filled.DirectionsRun)
    data object Nutrition : Screen("nutrition", "Nutrition", Icons.Default.Restaurant)
    data object Progress : Screen("progress", "Progress", Icons.Default.AutoGraph)
    data object Coach : Screen("coach", "Coach", Icons.Default.SmartToy)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object ActiveWorkout : Screen("active_workout/{dayId}", "Workout", Icons.AutoMirrored.Filled.DirectionsRun) {
        fun createRoute(dayId: Long) = "active_workout/$dayId"
    }
}

@Composable
fun TrainIqApp() {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Train, Screen.Nutrition, Screen.Progress, Screen.Coach)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentDestination?.route?.startsWith("active_workout") != true) {
                NavigationBar(
                    modifier = Modifier.height(72.dp),
                    tonalElevation = 6.dp,
                ) {
                    items.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (selected) return@NavigationBarItem
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = null,
                                    modifier = Modifier.semantics { contentDescription = screen.label },
                                )
                            },
                            label = null,
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                                indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Screen.Home.route) {
                HomeRoute(
                    onStartWorkout = { dayId -> navController.navigate(Screen.ActiveWorkout.createRoute(dayId)) },
                    onOpenCoach = { navController.navigate(Screen.Coach.route) },
                    onOpenTrain = { navController.navigate(Screen.Train.route) },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                )
            }
            composable(Screen.Train.route) {
                WorkoutRoute(onStartWorkout = { dayId -> navController.navigate(Screen.ActiveWorkout.createRoute(dayId)) })
            }
            composable(Screen.Nutrition.route) { NutritionRoute() }
            composable(Screen.Progress.route) { ProgressRoute() }
            composable(Screen.Coach.route) { CoachRoute() }
            composable(Screen.Settings.route) { SettingsRoute() }
            composable(
                route = Screen.ActiveWorkout.route,
                arguments = listOf(navArgument("dayId") { type = NavType.LongType }),
            ) { entry ->
                ActiveWorkoutRoute(
                    dayId = entry.arguments?.getLong("dayId") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
