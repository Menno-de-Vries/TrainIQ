package com.trainiq.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.util.MetricCard
import com.trainiq.core.util.ProgressCard
import com.trainiq.domain.model.HomeDashboard
import com.trainiq.domain.usecase.ObserveHomeDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHomeDashboardUseCase: ObserveHomeDashboardUseCase,
) : ViewModel() {
    val uiState: StateFlow<HomeDashboard?> = observeHomeDashboardUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun HomeRoute(onStartWorkout: (Long) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state = state, onStartWorkout = onStartWorkout)
}

@Composable
fun HomeScreen(state: HomeDashboard?, onStartWorkout: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("TrainIQ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        if ((state?.calorieTarget ?: 0) > 0) {
            item {
                ProgressCard(
                    title = "Calorie progress",
                    progress = (state?.calorieProgress ?: 0) / (state?.calorieTarget?.toFloat() ?: 1f),
                    current = "${state?.calorieProgress ?: 0} kcal",
                    target = "${state?.calorieTarget ?: 0} kcal",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if ((state?.proteinTarget ?: 0) > 0) {
            item {
                ProgressCard(
                    title = "Protein progress",
                    progress = (state?.proteinProgress ?: 0) / (state?.proteinTarget?.toFloat() ?: 1f),
                    current = "${state?.proteinProgress ?: 0} g",
                    target = "${state?.proteinTarget ?: 0} g",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    "Steps",
                    "${state?.steps ?: 0}",
                    if ((state?.steps ?: 0) > 0) "Health Connect synced" else "Health Connect unavailable or not connected",
                    Modifier.weight(1f),
                )
                MetricCard("Streak", "${state?.streak ?: 0} days", "Consistency streak", Modifier.weight(1f))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Next workout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(state?.nextWorkout?.name ?: "No active workout day")
                    Text(state?.nextWorkout?.exercises?.joinToString { it.exercise.name } ?: "Activate a routine to plan your next session.")
                    state?.nextWorkout?.id?.let { dayId ->
                        Button(onClick = { onStartWorkout(dayId) }) { Text("Start workout") }
                    }
                }
            }
        }
        if (!state?.aiInsight.isNullOrBlank()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AI Insight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(state?.aiInsight.orEmpty())
                    }
                }
            }
        } else if (state?.nextWorkout == null) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Start here", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Set up training, meals, and progress data to unlock your dashboard and coaching insights.")
                    }
                }
            }
        }
    }
}
