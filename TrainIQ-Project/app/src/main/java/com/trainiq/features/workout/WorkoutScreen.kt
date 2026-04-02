package com.trainiq.features.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.ui.MessageCard
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.ProgressionSuggestion
import com.trainiq.domain.model.ReadinessLevel
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutDebrief
import com.trainiq.domain.model.WorkoutExercisePlan
import com.trainiq.domain.model.WorkoutOverview
import com.trainiq.domain.model.WorkoutRoutine
import com.trainiq.domain.usecase.AddExerciseToDayUseCase
import com.trainiq.domain.usecase.AddWorkoutDayUseCase
import com.trainiq.domain.usecase.CreateRoutineUseCase
import com.trainiq.domain.usecase.DeleteRoutineUseCase
import com.trainiq.domain.usecase.DeleteWorkoutSessionUseCase
import com.trainiq.domain.usecase.FinishWorkoutUseCase
import com.trainiq.domain.usecase.GenerateAiRoutineUseCase
import com.trainiq.domain.usecase.GetProgressionSuggestionsUseCase
import com.trainiq.domain.usecase.GetWorkoutDayUseCase
import com.trainiq.domain.usecase.ObserveWorkoutOverviewUseCase
import com.trainiq.domain.usecase.RemoveExerciseFromDayUseCase
import com.trainiq.domain.usecase.RemoveWorkoutDayUseCase
import com.trainiq.domain.usecase.SetActiveRoutineUseCase
import com.trainiq.domain.usecase.UpdateRoutineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class SetInputDraft(
    val weight: String = "",
    val reps: String = "",
    val rpe: String = "",
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    observeWorkoutOverviewUseCase: ObserveWorkoutOverviewUseCase,
    private val getWorkoutDayUseCase: GetWorkoutDayUseCase,
    private val getProgressionSuggestionsUseCase: GetProgressionSuggestionsUseCase,
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
    private val generateAiRoutineUseCase: GenerateAiRoutineUseCase,
) : ViewModel() {
    val overview: StateFlow<WorkoutOverview?> = observeWorkoutOverviewUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _activeWorkout = MutableStateFlow<WorkoutDay?>(null)
    val activeWorkout: StateFlow<WorkoutDay?> = _activeWorkout.asStateFlow()

    private val _progressionSuggestions = MutableStateFlow<List<ProgressionSuggestion>>(emptyList())
    val progressionSuggestions: StateFlow<List<ProgressionSuggestion>> = _progressionSuggestions.asStateFlow()

    private val _loggedSetsThisSession = MutableStateFlow<Map<Long, List<LoggedSet>>>(emptyMap())
    val loggedSetsThisSession: StateFlow<Map<Long, List<LoggedSet>>> = _loggedSetsThisSession.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()

    private val _debrief = MutableStateFlow<WorkoutDebrief?>(null)
    val debrief: StateFlow<WorkoutDebrief?> = _debrief.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var restTimerJob: Job? = null

    fun loadWorkout(dayId: Long) {
        viewModelScope.launch {
            stopRestTimer()
            _debrief.value = null
            _loggedSetsThisSession.value = emptyMap()
            _activeWorkout.value = getWorkoutDayUseCase(dayId)
            _progressionSuggestions.value = getProgressionSuggestionsUseCase(dayId)
        }
    }

    fun logSet(plan: WorkoutExercisePlan, weight: String, reps: String, rpe: String): Boolean {
        val loggedSet = LoggedSet(
            exerciseId = plan.exercise.id,
            weight = weight.toDoubleOrNull() ?: 0.0,
            reps = reps.toIntOrNull() ?: 0,
            rpe = rpe.toDoubleOrNull() ?: 0.0,
        )
        if (loggedSet.weight <= 0.0 || loggedSet.reps <= 0) {
            _message.value = "Voer gewicht en reps in om een set te loggen."
            return false
        }
        val updated = _loggedSetsThisSession.value.toMutableMap()
        updated[plan.exercise.id] = updated[plan.exercise.id].orEmpty() + loggedSet
        _loggedSetsThisSession.value = updated
        startRestTimer(plan.restSeconds)
        return true
    }

    fun finishWorkout(dayId: Long) {
        viewModelScope.launch {
            stopRestTimer()
            val sets = _loggedSetsThisSession.value.values.flatten()
            _debrief.value = finishWorkoutUseCase(dayId, 3_600, sets)
            _progressionSuggestions.value = getProgressionSuggestionsUseCase(dayId)
        }
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

    fun generateAiRoutine(
        daysPerWeek: Int,
        equipment: String,
        targetFocus: String,
        experienceLevel: String,
        sessionDurationMinutes: Int,
        includeDeload: Boolean,
    ) {
        _message.value = "Generating AI routine..."
        viewModelScope.launch {
            runCatching {
                generateAiRoutineUseCase(
                    daysPerWeek = daysPerWeek,
                    equipment = equipment,
                    targetFocus = targetFocus,
                    experienceLevel = experienceLevel,
                    sessionDurationMinutes = sessionDurationMinutes,
                    includeDeload = includeDeload,
                )
            }.onSuccess {
                _message.value = "Routine generated!"
            }.onFailure {
                _message.value = it.message ?: "Failed to generate routine."
            }
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

    private fun startRestTimer(restSeconds: Int) {
        stopRestTimer()
        if (restSeconds <= 0) return
        _restTimerSeconds.value = restSeconds
        restTimerJob = viewModelScope.launch {
            repeat(restSeconds) {
                delay(1_000)
                _restTimerSeconds.value = (_restTimerSeconds.value - 1).coerceAtLeast(0)
            }
        }
    }

    private fun stopRestTimer() {
        restTimerJob?.cancel()
        restTimerJob = null
        _restTimerSeconds.value = 0
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
        onGenerateAiRoutine = viewModel::generateAiRoutine,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    overview: WorkoutOverview?,
    message: String?,
    onDismissMessage: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    onCreateRoutine: (String, String) -> Unit,
    onGenerateAiRoutine: (Int, String, String, String, Int, Boolean) -> Unit,
    onUpdateRoutine: (Long, String, String) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onSetActiveRoutine: (Long) -> Unit,
    onAddDay: (Long, String) -> Unit,
    onRemoveDay: (Long) -> Unit,
    onAddExercise: (Long, String, String, String, String, String, String) -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onDeleteWorkoutSession: (Long) -> Unit,
) {
    var showAiDialog by remember { mutableStateOf(false) }
    if (showAiDialog) {
        RoutineGeneratorDialog(
            onDismiss = { showAiDialog = false },
            onGenerate = { days, equipment, focus, level, duration, includeDeload ->
                onGenerateAiRoutine(days, equipment, focus, level, duration, includeDeload)
                showAiDialog = false
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader(title = "Train") }
        if (message != null) item { MessageCard(message = message, onDismiss = onDismissMessage) }
        if (overview == null) {
            item { ShimmerCardPlaceholder(lineCount = 4) }
            item { ShimmerCardPlaceholder(lineCount = 3) }
            item { ShimmerCardPlaceholder(lineCount = 5) }
            return@LazyColumn
        }
        item { RoutineCreationCard(onCreateRoutine = onCreateRoutine, onShowAiDialog = { showAiDialog = true }) }
        item { ActiveRoutineCard(activeRoutine = overview.activeRoutine, onStartWorkout = onStartWorkout) }
        item { SectionHeader("Routines") }
        if (overview.routines.isEmpty()) {
            item { EmptyCard("No routines yet", "Create a routine, add workout days, and attach exercises to get started.") }
        } else {
            items(overview.routines, key = { it.id }) { routine ->
                RoutineCard(routine, onStartWorkout, onUpdateRoutine, onDeleteRoutine, onSetActiveRoutine, onAddDay, onRemoveDay, onAddExercise, onRemoveExercise)
            }
        }
        item { SectionHeader("Exercise Library") }
        if (overview.exercises.isEmpty()) {
            item { Text("No exercises available yet.") }
        } else {
            items(overview.exercises, key = { it.id }) { exercise ->
                ExerciseLibraryCard(exercise.name, exercise.muscleGroup, exercise.equipment)
            }
        }
        item { SectionHeader("History") }
        if (overview.history.isEmpty()) {
            item { EmptyCard("No workout history", "Complete a workout and your session history will appear here.") }
        } else {
            items(overview.history, key = { it.id }) { session ->
                HistoryCard(session.id, session.totalVolume, session.duration, onDeleteWorkoutSession)
            }
        }
    }
}

@Composable
private fun RoutineCreationCard(onCreateRoutine: (String, String) -> Unit, onShowAiDialog: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Create routine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Start with a blank template and add days manually, or let AI build a routine around your level and schedule.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { onCreateRoutine("New Routine", "") }, modifier = Modifier.fillMaxWidth()) {
                Text("Create blank routine")
            }
            Button(onClick = onShowAiDialog, modifier = Modifier.fillMaxWidth()) {
                Text("Generate with AI")
            }
        }
    }
}

@Composable
private fun ActiveRoutineCard(activeRoutine: WorkoutRoutine?, onStartWorkout: (Long) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Active Routine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (activeRoutine == null) {
                Text("No active routine yet. Create one below and mark it active.")
            } else {
                Text(activeRoutine.name)
                Text(activeRoutine.description.ifBlank { "No description yet." })
                activeRoutine.days.firstOrNull()?.let { day ->
                    Button(onClick = { onStartWorkout(day.id) }) { Text("Start ${day.name}") }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body)
        }
    }
}

@Composable
private fun ExerciseLibraryCard(name: String, muscleGroup: String, equipment: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text("$muscleGroup - $equipment")
        }
    }
}

@Composable
private fun HistoryCard(sessionId: Long, totalVolume: Double, durationSeconds: Long, onDelete: (Long) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Session $sessionId", fontWeight = FontWeight.SemiBold)
                Text("Volume ${totalVolume.toInt()} kg")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${durationSeconds / 60} min")
                TextButton(onClick = { onDelete(sessionId) }) { Text("Delete") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineGeneratorDialog(
    onDismiss: () -> Unit,
    onGenerate: (Int, String, String, String, Int, Boolean) -> Unit,
) {
    var focus by remember { mutableStateOf("") }
    var daysPerWeek by remember { mutableStateOf("3") }
    var equipment by remember { mutableStateOf("") }
    var experienceLevel by remember { mutableStateOf("intermediate") }
    var sessionDuration by remember { mutableFloatStateOf(60f) }
    var includeDeload by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate AI Routine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(focus, { focus = it }, label = { Text("Training focus / split") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = daysPerWeek,
                    onValueChange = { daysPerWeek = it },
                    label = { Text("Days per week") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(equipment, { equipment = it }, label = { Text("Available equipment") }, modifier = Modifier.fillMaxWidth())
                ExperienceLevelSelector(experienceLevel, onSelected = { experienceLevel = it })
                SessionDurationSlider(durationMinutes = sessionDuration.toInt(), onValueChange = { sessionDuration = it })
                IncludeDeloadRow(enabled = includeDeload, onCheckedChange = { includeDeload = it })
            }
        },
        confirmButton = {
            Button(onClick = {
                onGenerate(daysPerWeek.toIntOrNull() ?: 3, equipment, focus, experienceLevel, sessionDuration.toInt(), includeDeload)
            }) { Text("Generate") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ExperienceLevelSelector(experienceLevel: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Experience level", style = MaterialTheme.typography.labelMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf("beginner", "intermediate", "advanced").forEachIndexed { index, option ->
                SegmentedButton(
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index = index, count = 3),
                    onClick = { onSelected(option) },
                    selected = option == experienceLevel,
                ) {
                    Text(option.replaceFirstChar { it.uppercase() })
                }
            }
        }
    }
}

@Composable
private fun SessionDurationSlider(durationMinutes: Int, onValueChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Session duration: $durationMinutes min", style = MaterialTheme.typography.labelMedium)
        Slider(value = durationMinutes.toFloat(), onValueChange = onValueChange, valueRange = 30f..90f, steps = 3)
    }
}

@Composable
private fun IncludeDeloadRow(enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Include deload guidance", style = MaterialTheme.typography.labelMedium)
            Text("Adds recovery instructions for lower-stress weeks.", style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = enabled, onCheckedChange = onCheckedChange)
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
            OutlinedTextField(editName, { editName = it }, label = { Text("Routine name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(editDescription, { editDescription = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onUpdateRoutine(routine.id, editName, editDescription) }) { Text("Save") }
                Button(onClick = { onSetActiveRoutine(routine.id) }) { Text(if (routine.active) "Active" else "Set active") }
                TextButton(onClick = { onDeleteRoutine(routine.id) }) { Text("Delete") }
            }
            HorizontalDivider()
            Text("Add workout day", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(dayName, { dayName = it }, label = { Text("Day name") }, modifier = Modifier.weight(1f))
                Button(onClick = { onAddDay(routine.id, dayName); dayName = "" }) { Text("Add") }
            }
            if (routine.days.isEmpty()) {
                Text("No workout days yet. Add a day to start building this routine.", style = MaterialTheme.typography.bodySmall)
            } else {
                routine.days.forEach { day ->
                    WorkoutDayEditor(day, onStartWorkout, onRemoveDay, onAddExercise, onRemoveExercise)
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
                    Text("${day.exercises.size} exercise${if (day.exercises.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { onStartWorkout(day.id) }) { Text("Start") }
                    TextButton(onClick = { onRemoveDay(day.id) }) { Text("Remove") }
                }
            }
            day.exercises.forEach { plan ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plan.exercise.name, fontWeight = FontWeight.SemiBold)
                            Text("${plan.exercise.muscleGroup} - ${plan.targetSets} sets - ${plan.repRange}", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { onRemoveExercise(plan.id) }) { Text("Remove") }
                    }
                }
            }
            HorizontalDivider()
            OutlinedTextField(exerciseName, { exerciseName = it }, label = { Text("Exercise name") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(muscleGroup, { muscleGroup = it }, label = { Text("Muscle group") }, modifier = Modifier.weight(1f))
                OutlinedTextField(equipment, { equipment = it }, label = { Text("Equipment") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(targetSets, { targetSets = it }, label = { Text("Sets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(repRange, { repRange = it }, label = { Text("Rep range") }, modifier = Modifier.weight(1f))
                OutlinedTextField(restSeconds, { restSeconds = it }, label = { Text("Rest (s)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    onAddExercise(day.id, exerciseName, muscleGroup, equipment, targetSets, repRange, restSeconds)
                    exerciseName = ""
                    muscleGroup = ""
                    equipment = ""
                    targetSets = "3"
                    repRange = "8-12"
                    restSeconds = "90"
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add exercise") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutRoute(dayId: Long, onBack: () -> Unit, viewModel: WorkoutViewModel = hiltViewModel()) {
    val workout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val loggedSetsThisSession by viewModel.loggedSetsThisSession.collectAsStateWithLifecycle()
    val progressionSuggestions by viewModel.progressionSuggestions.collectAsStateWithLifecycle()
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsStateWithLifecycle()
    val debrief by viewModel.debrief.collectAsStateWithLifecycle()

    LaunchedEffect(dayId) { viewModel.loadWorkout(dayId) }

    ActiveWorkoutScreen(
        workout = workout,
        loggedSetsThisSession = loggedSetsThisSession,
        progressionSuggestions = progressionSuggestions,
        restTimerSeconds = restTimerSeconds,
        debrief = debrief,
        onBack = onBack,
        onLogSet = viewModel::logSet,
        onFinish = { viewModel.finishWorkout(dayId) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    workout: WorkoutDay?,
    loggedSetsThisSession: Map<Long, List<LoggedSet>>,
    progressionSuggestions: List<ProgressionSuggestion>,
    restTimerSeconds: Int,
    debrief: WorkoutDebrief?,
    onBack: () -> Unit,
    onLogSet: (WorkoutExercisePlan, String, String, String) -> Boolean,
    onFinish: () -> Unit,
) {
    val drafts = remember { mutableStateOf(mapOf<Long, SetInputDraft>()) }
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
            if (restTimerSeconds > 0) item { RestTimerCard(restTimerSeconds) }
            if (workout == null) {
                item { EmptyCard("Workout unavailable", "This workout could not be loaded.") }
                return@LazyColumn
            }
            items(workout.exercises, key = { it.id }) { plan ->
                val draft = drafts.value[plan.exercise.id] ?: SetInputDraft()
                ActiveExerciseCard(
                    plan = plan,
                    loggedSets = loggedSetsThisSession[plan.exercise.id].orEmpty(),
                    suggestion = progressionSuggestions.firstOrNull { it.exerciseId == plan.exercise.id },
                    draft = draft,
                    onDraftChange = { next -> drafts.value = drafts.value.toMutableMap().apply { put(plan.exercise.id, next) } },
                    onLogSet = {
                        if (onLogSet(plan, draft.weight, draft.reps, draft.rpe)) {
                            drafts.value = drafts.value.toMutableMap().apply { put(plan.exercise.id, SetInputDraft()) }
                        }
                    },
                )
            }
            item { Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish workout") } }
            if (debrief != null) item { WorkoutDebriefCard(debrief) }
        }
    }
}

@Composable
private fun RestTimerCard(restTimerSeconds: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rest timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("$restTimerSeconds s remaining", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun ActiveExerciseCard(
    plan: WorkoutExercisePlan,
    loggedSets: List<LoggedSet>,
    suggestion: ProgressionSuggestion?,
    draft: SetInputDraft,
    onDraftChange: (SetInputDraft) -> Unit,
    onLogSet: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (suggestion != null) ExerciseProgressionHint(suggestion)
            Text(plan.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${plan.targetSets} sets - ${plan.repRange} reps - ${plan.restSeconds}s rest", style = MaterialTheme.typography.bodySmall)
            repeat(plan.targetSets) { index ->
                SetRow(
                    index = index + 1,
                    repRange = plan.repRange,
                    loggedSet = loggedSets.getOrNull(index),
                    isCurrent = index == loggedSets.size && loggedSets.size < plan.targetSets,
                )
            }
            SetLoggerFields(draft = draft, onDraftChange = onDraftChange)
            Button(onClick = onLogSet, modifier = Modifier.fillMaxWidth()) { Text("Log set") }
        }
    }
}

@Composable
private fun SetLoggerFields(draft: SetInputDraft, onDraftChange: (SetInputDraft) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(draft.weight, { onDraftChange(draft.copy(weight = it)) }, label = { Text("Weight") }, modifier = Modifier.weight(1f))
        OutlinedTextField(draft.reps, { onDraftChange(draft.copy(reps = it)) }, label = { Text("Reps") }, modifier = Modifier.weight(1f))
        OutlinedTextField(draft.rpe, { onDraftChange(draft.copy(rpe = it)) }, label = { Text("RPE") }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SetRow(index: Int, repRange: String, loggedSet: LoggedSet?, isCurrent: Boolean) {
    val background = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (loggedSet != null && loggedSet.weight > 0.0 && loggedSet.reps > 0) {
                Icon(Icons.Default.Check, contentDescription = "Completed set")
            }
            Text("Set $index")
        }
        Text(loggedSet?.let { "${formatWeight(it.weight)} kg x ${it.reps}" } ?: repRange)
    }
}

@Composable
private fun ExerciseProgressionHint(suggestion: ProgressionSuggestion) {
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                "Vorige sessie: ${formatWeight(previousWeight(suggestion))}kg x ${displayRepTarget(suggestion.suggestedReps)} | Suggestie: ${formatWeight(suggestion.suggestedWeightKg)}kg x ${displayRepTarget(suggestion.suggestedReps)}",
            )
        },
    )
}

@Composable
private fun WorkoutDebriefCard(result: WorkoutDebrief) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AI Workout Debrief", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(result.summary)
            Text(result.progressionFeedback)
            Text(result.recommendation)
            Text("Next session: ${result.nextSessionFocus}", fontWeight = FontWeight.Medium)
            Text("Intensity signal: ${result.intensitySignal}")
            LinearProgressIndicator(
                progress = { result.recoveryScore.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Recovery score: ${result.recoveryScore}/100", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun previousWeight(suggestion: ProgressionSuggestion): Double = when (suggestion.readinessSignal) {
    ReadinessLevel.INCREASE -> (suggestion.suggestedWeightKg - 2.5).coerceAtLeast(0.0)
    ReadinessLevel.DELOAD -> if (suggestion.suggestedWeightKg == 0.0) 0.0 else suggestion.suggestedWeightKg / 0.9
    ReadinessLevel.MAINTAIN -> suggestion.suggestedWeightKg
}

private fun displayRepTarget(repRange: String): String = repRange.substringAfter('-', repRange)

private fun formatWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toInt().toString() else "%.1f".format(weight)
