package com.trainiq.features.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
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
import com.trainiq.core.util.ChartComposable
import com.trainiq.core.util.MetricCard
import com.trainiq.domain.model.ProgressOverview
import com.trainiq.domain.usecase.ObserveProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ProgressViewModel @Inject constructor(
    observeProgressUseCase: ObserveProgressUseCase,
) : ViewModel() {
    val overview: StateFlow<ProgressOverview?> = observeProgressUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ProgressRoute(viewModel: ProgressViewModel = hiltViewModel()) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    ProgressScreen(overview)
}

@Composable
fun ProgressScreen(overview: ProgressOverview?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Progress", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        if (overview == null || (
                overview.weightTrend.isEmpty() &&
                    overview.bodyFatTrend.isEmpty() &&
                    overview.strengthTrend.isEmpty() &&
                    overview.volumeTrend.isEmpty()
                )
        ) {
            item {
                MetricCard(
                    title = "No progress data",
                    value = "Empty",
                    subtitle = "Log workouts and body measurements to see trends here.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            item {
                MetricCard("Estimated 1RM", "${overview.estimatedOneRepMax.toInt()} kg", "Calculated with Epley formula", Modifier.fillMaxWidth())
            }
            item {
                MetricCard("Fatigue Index", String.format("%.2f", overview.fatigueIndex), "weeklyVolume / baselineVolume", Modifier.fillMaxWidth())
            }
            item { ChartComposable("Body weight", overview.weightTrend, Modifier.fillMaxWidth()) }
            item { ChartComposable("Body fat", overview.bodyFatTrend, Modifier.fillMaxWidth()) }
            item { ChartComposable("Strength progression", overview.strengthTrend, Modifier.fillMaxWidth()) }
            item { ChartComposable("Training volume", overview.volumeTrend, Modifier.fillMaxWidth()) }
        }
    }
}
