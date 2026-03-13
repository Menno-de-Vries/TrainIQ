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
import androidx.compose.material3.OutlinedTextField
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
import com.trainiq.domain.model.WorkoutRoutine
import com.trainiq.domain.usecase.AddExerciseToDayUseCase
import com.trainiq.domain.usecase.AddWorkoutDayUseCase
import com.trainiq.domain.usecase.CreateRoutineUseCase
import com.trainiq.domain.usecase.DeleteRoutineUseCase
import com.trainiq.domain.usecase.FinishWorkoutUseCase
import com.trainiq.domain.usecase.GetWorkoutDayUseCase
import com.trainiq.domain.usecase.DeleteWorkoutSessionUseCase
import com.trainiq.domain.usecase.ObserveWorkoutOverviewUseCase
import com.trainiq.domain.usecase.RemoveExerciseFromDayUseCase
import com.trainiq.domain.usecase.RemoveWorkoutDayUseCase
import com.trainiq.domain.usecase.SetActiveRoutineUseCase
import com.trainiq.domain.usecase.UpdateRoutineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    observeWorkoutOverviewUseCase: ObserveWorkoutOverviewUseCase,
    private val getWorkoutDayUseCase: GetWorkoutDayUseCase,
    private val finishWorkoutUseCase: FinishWorkoutUseCase,
    private val deleteWorkoutSessionUseCase: DeleteWorkoutSessionUseCase,
    private val createRoutineUseCase: CreateRoutineUseCase,
    private val updateRoutineUseCase: UpdateRoutineUseCase,
    private val deleteRoutineUseCase: DeleteRoutineUseCase,
    private val setActiveRoutineUseCase: SetActiveRoutineUseCase,
    private val addWorkoutDayUseCase: AddWorkoutDayUseCase,
    private val removeWorkoutDayUseCase: RemoveWorkoutDayUseCase,
    private val addExerciseToDayUseCase: AddExerciseToDayUseCase,
    private val removeExerciseFromDayUseCase: RemoveExerciseFromDayUseCase,
) : ViewModel() {
    val overview: StateFlow<WorkoutOverview?> = observeWorkoutOverviewUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _activeWorkout = MutableStateFlow<WorkoutDay?>(null)
    val activeWorkout: StateFlow<WorkoutDay?> = _activeWorkout.asStateFlow()

    private val _debrief = MutableStateFlow<WorkoutDebrief?>(null)
    val debrief: StateFlow<WorkoutDebrief?> = _debrief.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun loadWorkout(dayId: Long) {
        viewModelScope.launch { _activeWorkout.value = getWorkoutDayUseCase(dayId) }
    }

    fun finishWorkout(dayId: Long, loggedSets: List<LoggedSet>) {
        viewModelScope.launch { _debrief.value = finishWorkoutUseCase(dayId, 3_600, loggedSets) }
    }

    fun createRoutine(name: String, description: String) {
        if (name.isBlank()) {
            _message.value = "Routine name is required."
            return
        }
        viewModelScope.launch {
            createRoutineUseCase(name.trim(), description.trim())
            _message.value = "Routine created."
        }
    }

    fun updateRoutine(routineId: Long, name: String, description: String) {
        if (name.isBlank()) {
            _message.value = "Routine name is required."
            return
        }
        viewModelScope.launch {
            updateRoutineUseCase(routineId, name.trim(), description.trim())
            _message.value = "Routine updated."
        }
    }

    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch {
            deleteRoutineUseCase(routineId)
            _message.value = "Routine deleted."
        }
    }

    fun setActiveRoutine(routineId: Long) {
        viewModelScope.launch {
            setActiveRoutineUseCase(routineId)
            _message.value = "Active routine updated."
        }
    }

    fun addDay(routineId: Long, name: String) {
        if (name.isBlank()) {
            _message.value = "Workout day name is required."
            return
        }
        viewModelScope.launch {
            addWorkoutDayUseCase(routineId, name.trim())
            _message.value = "Workout day added."
        }
    }

    fun removeDay(dayId: Long) {
        viewModelScope.launch {
            removeWorkoutDayUseCase(dayId)
            _message.value = "Workout day removed."
        }
    }

    fun addExercise(
        dayId: Long,
        name: String,
        muscleGroup: String,
        equipment: String,
        targetSets: String,
        repRange: String,
        restSeconds: String,
    ) {
        if (name.isBlank() || muscleGroup.isBlank() || equipment.isBlank()) {
            _message.value = "Exercise name, muscle group, and equipment are required."
            return
        }
        viewModelScope.launch {
            addExerciseToDayUseCase(
                dayId = dayId,
                name = name.trim(),
                muscleGroup = muscleGroup.trim(),
                equipment = equipment.trim(),
                targetSets = targetSets.toIntOrNull() ?: 3,
                repRange = repRange.ifBlank { "8-12" },
                restSeconds = restSeconds.toIntOrNull() ?: 90,
            )
            _message.value = "Exercise added."
        }
    }

    fun removeExercise(workoutExerciseId: Long) {
        viewModelScope.launch {
            removeExerciseFromDayUseCase(workoutExerciseId)
            _message.value = "Exercise removed."
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun deleteWorkoutSession(sessionId: Long) {
        viewModelScope.launch {
            deleteWorkoutSessionUseCase(sessionId)
            _message.value = "Workout session deleted."
        }
    }
}

@Composable
fun WorkoutRoute(onStartWorkout: (Long) -> Unit, viewModel: WorkoutViewModel = hiltViewModel()) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    WorkoutScreen(
        overview = overview,
        message = message,
        onDismissMessage = viewModel::clearMessage,
        onStartWorkout = onStartWorkout,
        onCreateRoutine = viewModel::createRoutine,
        onUpdateRoutine = viewModel::updateRoutine,
        onDeleteRoutine = viewModel::deleteRoutine,
        onSetActiveRoutine = viewModel::setActiveRoutine,
        onAddDay = viewModel::addDay,
        onRemoveDay = viewModel::removeDay,
        onAddExercise = viewModel::addExercise,
        onRemoveExercise = viewModel::removeExercise,
        onDeleteWorkoutSession = viewModel::deleteWorkoutSession,
    )
}

@Composable
fun WorkoutScreen(
    overview: WorkoutOverview?,
    message: String?,
    onDismissMessage: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    onCreateRoutine: (String, String) -> Unit,
    onUpdateRoutine: (Long, String, String) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onSetActiveRoutine: (Long) -> Unit,
    onAddDay: (Long, String) -> Unit,
    onRemoveDay: (Long) -> Unit,
    onAddExercise: (Long, String, String, String, String, String, String) -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onDeleteWorkoutSession: (Long) -> Unit,
) {
    var routineName by remember { mutableStateOf("") }
    var routineDescription by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Train", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        message?.let {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(it, modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismissMessage) { Text("Dismiss") }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create routine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(value = routineName, onValueChange = { routineName = it }, label = { Text("Routine name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = routineDescription,
                        onValueChange = { routineDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = {
                        onCreateRoutine(routineName, routineDescription)
                        routineName = ""
                        routineDescription = ""
                    }) { Text("Create routine") }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Active Routine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (overview?.activeRoutine == null) {
                        Text("No active routine yet. Create one below and mark it active.")
                    } else {
                        Text(overview.activeRoutine.name)
                        Text(overview.activeRoutine.description.ifBlank { "No description yet." })
                        overview.activeRoutine.days.firstOrNull()?.let { day ->
                            Button(onClick = { onStartWorkout(day.id) }) { Text("Start ${day.name}") }
                        }
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
                        Text("Create a routine, add workout days, and attach exercises to get started.")
                    }
                }
            }
        } else {
            items(overview?.routines ?: emptyList(), key = { it.id }) { routine ->
                RoutineCard(
                    routine = routine,
                    onStartWorkout = onStartWorkout,
                    onUpdateRoutine = onUpdateRoutine,
                    onDeleteRoutine = onDeleteRoutine,
                    onSetActiveRoutine = onSetActiveRoutine,
                    onAddDay = onAddDay,
                    onRemoveDay = onRemoveDay,
                    onAddExercise = onAddExercise,
                    onRemoveExercise = onRemoveExercise,
                )
            }
        }

        item { Text("Exercise Library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        if (overview?.exercises.isNullOrEmpty()) {
            item { Text("No exercises available yet.") }
        } else {
            items(overview?.exercises ?: emptyList(), key = { it.id }) { exercise ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        Text("Complete a workout and your session history will appear here.")
                    }
                }
            }
        } else {
            items(overview?.history ?: emptyList(), key = { it.id }) { session ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Session ${session.id}", fontWeight = FontWeight.SemiBold)
                            Text("Volume ${session.totalVolume.toInt()} kg")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${session.duration / 60} min")
                            TextButton(onClick = { onDeleteWorkoutSession(session.id) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineCard(
    routine: WorkoutRoutine,
    onStartWorkout: (Long) -> Unit,
    onUpdateRoutine: (Long, String, String) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onSetActiveRoutine: (Long) -> Unit,
    onAddDay: (Long, String) -> Unit,
    onRemoveDay: (Long) -> Unit,
    onAddExercise: (Long, String, String, String, String, String, String) -> Unit,
    onRemoveExercise: (Long) -> Unit,
) {
    var editName by remember(routine.id) { mutableStateOf(routine.name) }
    var editDescription by remember(routine.id) { mutableStateOf(routine.description) }
    var dayName by remember(routine.id) { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Routine name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = editDescription,
                onValueChange = { editDescription = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onUpdateRoutine(routine.id, editName, editDescription) }) { Text("Save") }
                OutlinedTextField(
                    value = dayName,
                    onValueChange = { dayName = it },
                    label = { Text("Add day") },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    onAddDay(routine.id, dayName)
                    dayName = ""
                }) { Text("Add") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSetActiveRoutine(routine.id) }) {
                    Text(if (routine.active) "Active" else "Set active")
                }
                TextButton(onClick = { onDeleteRoutine(routine.id) }) { Text("Delete") }
            }
            if (routine.days.isEmpty()) {
                Text("No workout days yet. Add a day to start building this routine.")
            } else {
                routine.days.forEach { day ->
                    WorkoutDayEditor(
                        day = day,
                        onStartWorkout = onStartWorkout,
                        onRemoveDay = onRemoveDay,
                        onAddExercise = onAddExercise,
                        onRemoveExercise = onRemoveExercise,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutDayEditor(
    day: WorkoutDay,
    onStartWorkout: (Long) -> Unit,
    onRemoveDay: (Long) -> Unit,
    onAddExercise: (Long, String, String, String, String, String, String) -> Unit,
    onRemoveExercise: (Long) -> Unit,
) {
    var exerciseName by remember(day.id) { mutableStateOf("") }
    var muscleGroup by remember(day.id) { mutableStateOf("") }
    var equipment by remember(day.id) { mutableStateOf("") }
    var targetSets by remember(day.id) { mutableStateOf("3") }
    var repRange by remember(day.id) { mutableStateOf("8-12") }
    var restSeconds by remember(day.id) { mutableStateOf("90") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(day.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${day.exercises.size} exercises")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onStartWorkout(day.id) }) { Text("Start") }
                    TextButton(onClick = { onRemoveDay(day.id) }) { Text("Remove") }
                }
            }
            if (day.exercises.isEmpty()) {
                Text("No exercises yet. Add at least one to make this workout usable.")
            } else {
                day.exercises.forEach { plan ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plan.exercise.name, fontWeight = FontWeight.SemiBold)
                                Text("${plan.exercise.muscleGroup} • ${plan.targetSets} sets • ${plan.repRange}")
                            }
                            TextButton(onClick = { onRemoveExercise(plan.id) }) { Text("Remove") }
                        }
                    }
                }
            }
            OutlinedTextField(value = exerciseName, onValueChange = { exerciseName = it }, label = { Text("Exercise name") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = muscleGroup, onValueChange = { muscleGroup = it }, label = { Text("Muscle") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = equipment, onValueChange = { equipment = it }, label = { Text("Equipment") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = targetSets, onValueChange = { targetSets = it }, label = { Text("Sets") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = repRange, onValueChange = { repRange = it }, label = { Text("Rep range") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = restSeconds, onValueChange = { restSeconds = it }, label = { Text("Rest") }, modifier = Modifier.weight(1f))
            }
            Button(onClick = {
                onAddExercise(day.id, exerciseName, muscleGroup, equipment, targetSets, repRange, restSeconds)
                exerciseName = ""
                muscleGroup = ""
                equipment = ""
                targetSets = "3"
                repRange = "8-12"
                restSeconds = "90"
            }) { Text("Add exercise") }
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
