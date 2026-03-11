package com.trainiq.features.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.util.SetLogger
import com.trainiq.core.util.WorkoutExerciseItem
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutDebrief
import com.trainiq.domain.model.WorkoutOverview
import com.trainiq.domain.usecase.FinishWorkoutUseCase
import com.trainiq.domain.usecase.GetWorkoutDayUseCase
import com.trainiq.domain.usecase.ObserveWorkoutOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    observeWorkoutOverviewUseCase: ObserveWorkoutOverviewUseCase,
    private val getWorkoutDayUseCase: GetWorkoutDayUseCase,
    private val finishWorkoutUseCase: FinishWorkoutUseCase,
) : ViewModel() {
    val overview: StateFlow<WorkoutOverview?> = observeWorkoutOverviewUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _activeWorkout = MutableStateFlow<WorkoutDay?>(null)
    val activeWorkout: StateFlow<WorkoutDay?> = _activeWorkout

    private val _debrief = MutableStateFlow<WorkoutDebrief?>(null)
    val debrief: StateFlow<WorkoutDebrief?> = _debrief

    fun loadWorkout(dayId: Long) {
        viewModelScope.launch { _activeWorkout.value = getWorkoutDayUseCase(dayId) }
    }

    fun finishWorkout(dayId: Long, loggedSets: List<LoggedSet>) {
        viewModelScope.launch { _debrief.value = finishWorkoutUseCase(dayId, 3_600, loggedSets) }
    }
}

@Composable
fun WorkoutRoute(onStartWorkout: (Long) -> Unit, viewModel: WorkoutViewModel = hiltViewModel()) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    WorkoutScreen(overview = overview, onStartWorkout = onStartWorkout)
}

@Composable
fun WorkoutScreen(overview: WorkoutOverview?, onStartWorkout: (Long) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Train", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Active Routine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(overview?.activeRoutine?.name ?: "No active routine")
                    Text(overview?.activeRoutine?.description ?: "Create a routine to start planning your training.")
                    overview?.activeRoutine?.days?.firstOrNull()?.let { day ->
                        Button(onClick = { onStartWorkout(day.id) }) { Text("Start ${day.name}") }
                    }
                }
            }
        }
        item { Text("Routines", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        if (overview?.routines.isNullOrEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No routines yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Create or import a workout routine to start training.")
                    }
                }
            }
        } else {
            items(overview?.routines ?: emptyList(), key = { it.id }) { routine ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(routine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(routine.description)
                        Text(
                            if (routine.days.isEmpty()) {
                                "No workout days configured."
                            } else {
                                routine.days.joinToString { "${it.orderIndex + 1}. ${it.name}" }
                            },
                        )
                    }
                }
            }
        }
        item { Text("Exercise Library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        if (overview?.exercises.isNullOrEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Exercise library is empty", fontWeight = FontWeight.SemiBold)
                        Text("Exercises will appear here once your training data is added.")
                    }
                }
            }
        } else {
            items(overview?.exercises ?: emptyList(), key = { it.id }) { exercise ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(exercise.name, fontWeight = FontWeight.SemiBold)
                        Text("${exercise.muscleGroup} • ${exercise.equipment}")
                    }
                }
            }
        }
        item { Text("History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        if (overview?.history.isNullOrEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No workout history", fontWeight = FontWeight.SemiBold)
                        Text("Completed sessions will appear here.")
                    }
                }
            }
        } else {
            items(overview?.history ?: emptyList(), key = { it.id }) { session ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Session ${session.id}", fontWeight = FontWeight.SemiBold)
                            Text("Volume ${session.totalVolume.toInt()} kg")
                        }
                        Text("${session.duration / 60} min")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutRoute(dayId: Long, onBack: () -> Unit, viewModel: WorkoutViewModel = hiltViewModel()) {
    val workout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val debrief by viewModel.debrief.collectAsStateWithLifecycle()
    LaunchedEffect(dayId) { viewModel.loadWorkout(dayId) }
    ActiveWorkoutScreen(
        workout = workout,
        debrief = debrief,
        onBack = onBack,
        onFinish = { sets -> viewModel.finishWorkout(dayId, sets) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    workout: WorkoutDay?,
    debrief: WorkoutDebrief?,
    onBack: () -> Unit,
    onFinish: (List<LoggedSet>) -> Unit,
) {
    val loggedSets = remember { mutableStateListOf<LoggedSet>() }
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var rpe by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.name ?: "Workout") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (workout == null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Workout unavailable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("This workout could not be loaded.")
                        }
                    }
                }
            }
            items(workout?.exercises ?: emptyList(), key = { it.id }) { plan ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WorkoutExerciseItem(plan = plan, loggedSetCount = loggedSets.count { it.exerciseId == plan.exercise.id })
                    SetLogger(weight, reps, rpe, { weight = it }, { reps = it }, { rpe = it })
                    Button(onClick = {
                        loggedSets += LoggedSet(
                            plan.exercise.id,
                            weight.toDoubleOrNull() ?: 0.0,
                            reps.toIntOrNull() ?: 0,
                            rpe.toDoubleOrNull() ?: 0.0,
                        )
                        weight = ""
                        reps = ""
                        rpe = ""
                    }) {
                        Text("Log set")
                    }
                }
            }
            if (workout != null) {
                item {
                    Button(onClick = { onFinish(loggedSets.toList()) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Finish workout")
                    }
                }
            }
            debrief?.let { result ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("AI Workout Debrief", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(result.summary)
                            Text(result.progressionFeedback)
                            Text(result.recommendation)
                        }
                    }
                }
            }
        }
    }
}
