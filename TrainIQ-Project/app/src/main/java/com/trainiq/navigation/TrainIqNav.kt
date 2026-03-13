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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.trainiq.features.coach.CoachRoute
import com.trainiq.features.home.HomeRoute
import com.trainiq.features.nutrition.NutritionRoute
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

private data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun TrainIqApp() {
    val navController = rememberNavController()
    val items = listOf(
        TopLevelDestination(Home, Home::class, "Home", Icons.Default.Home),
        TopLevelDestination(Train, Train::class, "Train", Icons.AutoMirrored.Filled.DirectionsRun),
        TopLevelDestination(Nutrition, Nutrition::class, "Nutrition", Icons.Default.Restaurant),
        TopLevelDestination(Progress, Progress::class, "Progress", Icons.Default.AutoGraph),
        TopLevelDestination(Coach, Coach::class, "Coach", Icons.Default.SmartToy),
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentDestination?.hierarchy?.any { it.hasRoute(ActiveWorkout::class) } != true) {
                NavigationBar(
                    modifier = Modifier.height(80.dp),
                    tonalElevation = 6.dp,
                ) {
                    items.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.hasRoute(screen.routeClass) } == true
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
                                    imageVector = screen.icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .semantics { contentDescription = screen.label }
                                        .padding(vertical = 4.dp),
                                )
                            },
                            label = null,
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
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
        composable<Nutrition> { NutritionRoute() }
        composable<Progress> { ProgressRoute() }
        composable<Coach> { CoachRoute() }
        composable<Settings> { SettingsRoute() }
        composable<ActiveWorkout> { entry ->
            ActiveWorkoutRoute(
                dayId = entry.toRoute<ActiveWorkout>().dayId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
