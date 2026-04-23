package com.trainiq.features.workout

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.GeneratedRoutine
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.ProgressionSuggestion
import com.trainiq.domain.model.ReadinessLevel
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.StrengthCalculator
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutDebrief
import com.trainiq.domain.model.WorkoutExercisePlan
import com.trainiq.domain.model.WorkoutOverview
import com.trainiq.domain.model.WorkoutRoutine
import com.trainiq.domain.usecase.AddExerciseToDayUseCase
import com.trainiq.domain.usecase.AddExerciseToRoutineUseCase
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
import com.trainiq.domain.usecase.ReorderExercisesUseCase
import com.trainiq.domain.usecase.SaveGeneratedRoutineUseCase
import com.trainiq.domain.usecase.SetActiveRoutineUseCase
import com.trainiq.domain.usecase.SetSupersetGroupUseCase
import com.trainiq.domain.usecase.UpdateRoutineUseCase
import com.trainiq.ai.services.toAiUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem

data class SetInputDraft(
    val weight: String = "",
    val reps: String = "",
    val rpe: String = "",
    val setType: SetType = SetType.WORKING,
)

data class ActiveWorkoutUiState(
    val workout: WorkoutDay? = null,
    val progressionSuggestions: List<ProgressionSuggestion> = emptyList(),
    val loggedSetsThisSession: Map<Long, List<LoggedSet>> = emptyMap(),
    val restTimerSeconds: Int = 0,
    val restTimerTotalSeconds: Int = 0,
    val debrief: WorkoutDebrief? = null,
    val drafts: Map<Long, SetInputDraft> = emptyMap(),
    val message: String? = null,
)

private data class RoutineGenerationRequest(
    val daysPerWeek: Int,
    val equipment: String,
    val targetFocus: String,
    val experienceLevel: String,
    val sessionDurationMinutes: Int,
    val includeDeload: Boolean,
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
    private val addExerciseToRoutineUseCase: AddExerciseToRoutineUseCase,
    private val removeExerciseFromDayUseCase: RemoveExerciseFromDayUseCase,
    private val generateAiRoutineUseCase: GenerateAiRoutineUseCase,
    private val saveGeneratedRoutineUseCase: SaveGeneratedRoutineUseCase,
    private val reorderExercisesUseCase: ReorderExercisesUseCase,
    private val setSupersetGroupUseCase: SetSupersetGroupUseCase,
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
    private val _restTimerTotalSeconds = MutableStateFlow(0)

    private val _debrief = MutableStateFlow<WorkoutDebrief?>(null)
    val debrief: StateFlow<WorkoutDebrief?> = _debrief.asStateFlow()

    private val _drafts = MutableStateFlow<Map<Long, SetInputDraft>>(emptyMap())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _pendingGeneratedRoutine = MutableStateFlow<GeneratedRoutine?>(null)
    val pendingGeneratedRoutine: StateFlow<GeneratedRoutine?> = _pendingGeneratedRoutine.asStateFlow()

    val activeWorkoutUiState: StateFlow<ActiveWorkoutUiState> = combine(
        _activeWorkout,
        _progressionSuggestions,
        _loggedSetsThisSession,
        _restTimerSeconds,
        _debrief,
    ) { workout, suggestions, loggedSets, restTimerSeconds, debrief ->
        ActiveWorkoutUiState(
            workout = workout,
            progressionSuggestions = suggestions,
            loggedSetsThisSession = loggedSets,
            restTimerSeconds = restTimerSeconds,
            debrief = debrief,
        )
    }.combine(_restTimerTotalSeconds) { state, restTimerTotalSeconds ->
        state.copy(restTimerTotalSeconds = restTimerTotalSeconds)
    }.combine(_drafts) { state, drafts ->
        state.copy(drafts = drafts)
    }.combine(_message) { state, message ->
        state.copy(message = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState())

    private var restTimerJob: Job? = null
    private var sessionStartTime: Long = 0L
    private var lastGenerationRequest: RoutineGenerationRequest? = null

    fun loadWorkout(dayId: Long) {
        viewModelScope.launch {
            stopRestTimer()
            sessionStartTime = System.currentTimeMillis()
            _debrief.value = null
            _drafts.value = emptyMap()
            _message.value = null
            _loggedSetsThisSession.value = emptyMap()
            _activeWorkout.value = getWorkoutDayUseCase(dayId)
            _progressionSuggestions.value = getProgressionSuggestionsUseCase(dayId)
            seedDraftsFromSuggestions()
        }
    }

    fun updateSetDraft(exerciseId: Long, draft: SetInputDraft) {
        _drafts.value = _drafts.value.toMutableMap().apply { put(exerciseId, draft) }
    }

    fun updateLoggedSetType(exerciseId: Long, setIndex: Int, setType: SetType) {
        _loggedSetsThisSession.value = _loggedSetsThisSession.value.toMutableMap().apply {
            val sets = get(exerciseId).orEmpty()
            if (setIndex in sets.indices) {
                put(exerciseId, sets.toMutableList().apply {
                    this[setIndex] = this[setIndex].copy(setType = setType)
                })
            }
        }
    }

    fun editLoggedSet(exerciseId: Long, setIndex: Int) {
        val sets = _loggedSetsThisSession.value[exerciseId].orEmpty()
        val set = sets.getOrNull(setIndex) ?: return
        _drafts.value = _drafts.value.toMutableMap().apply { put(exerciseId, set.toDraft()) }
        deleteLoggedSet(exerciseId, setIndex, showMessage = false)
        _message.value = "Set ${setIndex + 1} staat klaar om te corrigeren."
    }

    fun deleteLoggedSet(exerciseId: Long, setIndex: Int, showMessage: Boolean = true) {
        _loggedSetsThisSession.value = _loggedSetsThisSession.value.toMutableMap().apply {
            val sets = get(exerciseId).orEmpty()
            if (setIndex in sets.indices) {
                val next = sets.toMutableList().apply { removeAt(setIndex) }
                if (next.isEmpty()) remove(exerciseId) else put(exerciseId, next)
            }
        }
        if (showMessage) _message.value = "Set verwijderd."
    }

    fun logSet(plan: WorkoutExercisePlan): Boolean {
        val draft = _drafts.value[plan.exercise.id] ?: SetInputDraft()
        val parsedRpe = draft.rpe.toDoubleOrNull()?.coerceIn(0.0, 10.0) ?: 0.0
        val loggedSet = LoggedSet(
            exerciseId = plan.exercise.id,
            weight = draft.weight.replace(',', '.').toDoubleOrNull() ?: 0.0,
            reps = draft.reps.toIntOrNull() ?: 0,
            rpe = parsedRpe,
            repsInReserve = StrengthCalculator.estimateRepsInReserve(parsedRpe),
            setType = draft.setType,
        )
        if (loggedSet.weight <= 0.0 || loggedSet.reps <= 0) {
            _message.value = "Voer gewicht en reps in om een set te loggen."
            return false
        }
        val updated = _loggedSetsThisSession.value.toMutableMap()
        updated[plan.exercise.id] = updated[plan.exercise.id].orEmpty() + loggedSet
        _loggedSetsThisSession.value = updated
        _drafts.value = _drafts.value.toMutableMap().apply { put(plan.exercise.id, loggedSet.toDraft()) }
        _message.value = when (loggedSet.setType) {
            SetType.TOP_SET -> "Top set voltooid voor ${plan.exercise.name}."
            else -> "Set ${updated[plan.exercise.id].orEmpty().size} gelogd voor ${plan.exercise.name}."
        }
        startRestTimer(plan.restSeconds)
        return true
    }

    fun logSameAgain(plan: WorkoutExercisePlan): Boolean {
        val lastSet = _loggedSetsThisSession.value[plan.exercise.id].orEmpty().lastOrNull() ?: return false
        _drafts.value = _drafts.value.toMutableMap().apply { put(plan.exercise.id, lastSet.toDraft()) }
        return logSet(plan)
    }

    fun finishWorkout(dayId: Long) {
        viewModelScope.launch {
            stopRestTimer()
            val sets = _loggedSetsThisSession.value.values.flatten()
            val durationSeconds = ((System.currentTimeMillis() - sessionStartTime) / 1_000).coerceAtLeast(1)
            _debrief.value = finishWorkoutUseCase(dayId, durationSeconds, sets)
            _progressionSuggestions.value = getProgressionSuggestionsUseCase(dayId)
        }
    }

    fun createRoutine(name: String, description: String) {
        if (name.isBlank()) {
            _message.value = "Routine name is required."
            return
        }
        val exists = overview.value?.routines?.any {
            it.name.equals(name.trim(), ignoreCase = true)
        } == true
        if (exists) {
            _message.value = "Een routine met deze naam bestaat al."
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
        lastGenerationRequest = RoutineGenerationRequest(
            daysPerWeek = daysPerWeek,
            equipment = equipment,
            targetFocus = targetFocus,
            experienceLevel = experienceLevel,
            sessionDurationMinutes = sessionDurationMinutes,
            includeDeload = includeDeload,
        )
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
            }.onSuccess { generated ->
                _pendingGeneratedRoutine.value = generated
                _message.value = "Routine generated!"
            }.onFailure {
                _message.value = it.toAiUserMessage("Failed to generate routine.")
            }
        }
    }

    fun retryGeneratedRoutine() {
        val request = lastGenerationRequest ?: return
        _pendingGeneratedRoutine.value = null
        generateAiRoutine(
            daysPerWeek = request.daysPerWeek,
            equipment = request.equipment,
            targetFocus = request.targetFocus,
            experienceLevel = request.experienceLevel,
            sessionDurationMinutes = request.sessionDurationMinutes,
            includeDeload = request.includeDeload,
        )
    }

    fun savePendingGeneratedRoutine() {
        val routine = _pendingGeneratedRoutine.value ?: return
        viewModelScope.launch {
            runCatching {
                saveGeneratedRoutineUseCase(routine)
            }.onSuccess {
                _pendingGeneratedRoutine.value = null
                _message.value = "Routine saved."
            }.onFailure {
                _message.value = it.toAiUserMessage("Failed to save routine.")
            }
        }
    }

    fun dismissPendingGeneratedRoutine() {
        _pendingGeneratedRoutine.value = null
    }

    fun updateRoutine(routineId: Long, name: String, description: String) {
        if (name.isBlank()) {
            _message.value = "Routine name is required."
            return
        }
        val exists = overview.value?.routines?.any {
            it.id != routineId && it.name.equals(name.trim(), ignoreCase = true)
        } == true
        if (exists) {
            _message.value = "Een routine met deze naam bestaat al."
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
        viewModelScope.launch {
            addWorkoutDayUseCase(routineId, name.trim())
            _message.value = "Session added."
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

    fun addExerciseToRoutine(
        routineId: Long,
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
            addExerciseToRoutineUseCase(
                routineId = routineId,
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

    fun reorderExercises(dayId: Long, orderedIds: List<Long>) {
        viewModelScope.launch {
            reorderExercisesUseCase(dayId, orderedIds)
            _message.value = "Exercise order updated."
        }
    }

    fun setSupersetGroup(workoutExerciseIds: List<Long>, groupId: Long?) {
        viewModelScope.launch {
            setSupersetGroupUseCase(workoutExerciseIds, groupId)
            _message.value = if (groupId == null) "Superset removed." else "Superset linked."
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
        _restTimerTotalSeconds.value = restSeconds
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
        _restTimerTotalSeconds.value = 0
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        val next = (_restTimerSeconds.value + deltaSeconds).coerceAtLeast(0)
        if (next == 0) {
            stopRestTimer()
            return
        }
        startRestTimer(next)
    }

    fun skipRestTimer() {
        stopRestTimer()
        _message.value = "Rusttimer overgeslagen."
    }

    fun restartRestTimer(restSeconds: Int) {
        startRestTimer(restSeconds)
        _message.value = "Rusttimer opnieuw gestart."
    }

    private fun seedDraftsFromSuggestions() {
        _drafts.value = _progressionSuggestions.value
            .mapNotNull { suggestion ->
                val draft = suggestion.toLastSessionDraft() ?: return@mapNotNull null
                suggestion.exerciseId to draft
            }
            .toMap()
    }
}

@Composable
fun WorkoutRoute(onStartWorkout: (Long) -> Unit, viewModel: WorkoutViewModel = hiltViewModel()) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val pendingGeneratedRoutine by viewModel.pendingGeneratedRoutine.collectAsStateWithLifecycle()
    WorkoutScreen(
        overview = overview,
        message = message,
        pendingGeneratedRoutine = pendingGeneratedRoutine,
        onDismissMessage = viewModel::clearMessage,
        onStartWorkout = onStartWorkout,
        onCreateRoutine = viewModel::createRoutine,
        onGenerateAiRoutine = viewModel::generateAiRoutine,
        onSaveGeneratedRoutine = viewModel::savePendingGeneratedRoutine,
        onRetryGeneratedRoutine = viewModel::retryGeneratedRoutine,
        onDismissGeneratedRoutine = viewModel::dismissPendingGeneratedRoutine,
        onUpdateRoutine = viewModel::updateRoutine,
        onDeleteRoutine = viewModel::deleteRoutine,
        onSetActiveRoutine = viewModel::setActiveRoutine,
        onAddDay = viewModel::addDay,
        onRemoveDay = viewModel::removeDay,
        onAddExercise = viewModel::addExercise,
        onAddExerciseToRoutine = viewModel::addExerciseToRoutine,
        onRemoveExercise = viewModel::removeExercise,
        onReorderExercises = viewModel::reorderExercises,
        onSetSupersetGroup = viewModel::setSupersetGroup,
        onDeleteWorkoutSession = viewModel::deleteWorkoutSession,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    overview: WorkoutOverview?,
    message: String?,
    pendingGeneratedRoutine: GeneratedRoutine?,
    onDismissMessage: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    onCreateRoutine: (String, String) -> Unit,
    onGenerateAiRoutine: (Int, String, String, String, Int, Boolean) -> Unit,
    onSaveGeneratedRoutine: () -> Unit,
    onRetryGeneratedRoutine: () -> Unit,
    onDismissGeneratedRoutine: () -> Unit,
    onUpdateRoutine: (Long, String, String) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onSetActiveRoutine: (Long) -> Unit,
    onAddDay: (Long, String) -> Unit,
    onRemoveDay: (Long) -> Unit,
    onAddExercise: (Long, String, String, String, String, String, String) -> Unit,
    onAddExerciseToRoutine: (Long, String, String, String, String, String, String) -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onReorderExercises: (Long, List<Long>) -> Unit,
    onSetSupersetGroup: (List<Long>, Long?) -> Unit,
    onDeleteWorkoutSession: (Long) -> Unit,
) {
    var showAiDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    LaunchedEffect(message) {
        if (message == "Routine generated!" || message?.startsWith("Failed", ignoreCase = true) == true) {
            isGenerating = false
            showAiDialog = false
        }
    }
    pendingGeneratedRoutine?.let { routine ->
        GeneratedRoutinePreviewDialog(
            routine = routine,
            onSave = onSaveGeneratedRoutine,
            onRetry = {
                isGenerating = true
                showAiDialog = true
                onRetryGeneratedRoutine()
            },
            onDismiss = onDismissGeneratedRoutine,
        )
    }
    if (showCreateDialog) {
        CreateRoutineDialog(
            onConfirm = { name ->
                onCreateRoutine(name, "")
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }
    if (showAiDialog) {
        RoutineGeneratorDialog(
            isLoading = isGenerating,
            onDismiss = { if (!isGenerating) showAiDialog = false },
            onGenerate = { days, equipment, focus, level, duration, includeDeload ->
                isGenerating = true
                onGenerateAiRoutine(days, equipment, focus, level, duration, includeDeload)
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
        item { RoutineCreationCard(onShowCreateDialog = { showCreateDialog = true }, onShowAiDialog = { showAiDialog = true }) }
        item { ActiveRoutineCard(activeRoutine = overview.activeRoutine, onStartWorkout = onStartWorkout) }
        item { SectionHeader("Routines") }
        if (overview.routines.isEmpty()) {
            item { EmptyCard("No routines yet", "Create a routine, add workout days, and attach exercises to get started.") }
        } else {
            items(overview.routines, key = { it.id }) { routine ->
                RoutineCard(
                    routine = routine,
                    exerciseLibrary = overview.exercises,
                    onStartWorkout = onStartWorkout,
                    onUpdateRoutine = onUpdateRoutine,
                    onDeleteRoutine = onDeleteRoutine,
                    onSetActiveRoutine = onSetActiveRoutine,
                    onAddDay = onAddDay,
                    onRemoveDay = onRemoveDay,
                    onAddExercise = onAddExercise,
                    onAddExerciseToRoutine = onAddExerciseToRoutine,
                    onRemoveExercise = onRemoveExercise,
                    onReorderExercises = onReorderExercises,
                    onSetSupersetGroup = onSetSupersetGroup,
                )
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
private fun CreateRoutineDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create routine") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Routine name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RoutineCreationCard(onShowCreateDialog: () -> Unit, onShowAiDialog: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Create routine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Start with a blank template and add days manually, or let AI build a routine around your level and schedule.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onShowCreateDialog, modifier = Modifier.fillMaxWidth()) {
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
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (Int, String, String, String, Int, Boolean) -> Unit,
) {
    var focus by remember { mutableStateOf("") }
    var daysPerWeek by remember { mutableStateOf("3") }
    var equipment by remember { mutableStateOf("") }
    var experienceLevel by remember { mutableStateOf("intermediate") }
    var sessionDuration by remember { mutableFloatStateOf(60f) }
    var includeDeload by remember { mutableStateOf(true) }
    val focusSuggestions = listOf("Push/Pull/Legs", "Upper/Lower", "Full Body", "Bro Split", "PHAT")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate AI Routine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(focusSuggestions) { suggestion ->
                        SuggestionChip(
                            onClick = { focus = suggestion },
                            label = { Text(suggestion) },
                        )
                    }
                }
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
            Button(
                onClick = {
                    onGenerate(daysPerWeek.toIntOrNull() ?: 3, equipment, focus, experienceLevel, sessionDuration.toInt(), includeDeload)
                },
                enabled = !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Generate")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") } },
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
    exerciseLibrary: List<Exercise>,
    onStartWorkout: (Long) -> Unit,
    onUpdateRoutine: (Long, String, String) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onSetActiveRoutine: (Long) -> Unit,
    onAddDay: (Long, String) -> Unit,
    onRemoveDay: (Long) -> Unit,
    onAddExercise: (Long, String, String, String, String, String, String) -> Unit,
    onAddExerciseToRoutine: (Long, String, String, String, String, String, String) -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onReorderExercises: (Long, List<Long>) -> Unit,
    onSetSupersetGroup: (List<Long>, Long?) -> Unit,
) {
    var isEditing by remember(routine.id) { mutableStateOf(false) }
    var editName by remember(routine.id) { mutableStateOf(routine.name) }
    var editDescription by remember(routine.id) { mutableStateOf(routine.description) }
    var dayName by remember(routine.id) { mutableStateOf("") }
    var starterTargetSets by remember(routine.id) { mutableStateOf("3") }
    var starterRepRange by remember(routine.id) { mutableStateOf("8-12") }
    var starterRestSeconds by remember(routine.id) { mutableStateOf("90") }
    var showStarterExercisePicker by remember(routine.id) { mutableStateOf(false) }
    var showStarterCustomExerciseDialog by remember(routine.id) { mutableStateOf(false) }

    if (showStarterExercisePicker) {
        ExercisePickerSheet(
            exercises = exerciseLibrary,
            targetSets = starterTargetSets,
            repRange = starterRepRange,
            restSeconds = starterRestSeconds,
            onTargetSetsChange = { starterTargetSets = it },
            onRepRangeChange = { starterRepRange = it },
            onRestSecondsChange = { starterRestSeconds = it },
            onSelect = { exercise ->
                onAddExerciseToRoutine(
                    routine.id,
                    exercise.name,
                    exercise.muscleGroup,
                    exercise.equipment,
                    starterTargetSets,
                    starterRepRange,
                    starterRestSeconds,
                )
                showStarterExercisePicker = false
            },
            onCustomExercise = {
                showStarterExercisePicker = false
                showStarterCustomExerciseDialog = true
            },
            onDismiss = { showStarterExercisePicker = false },
        )
    }
    if (showStarterCustomExerciseDialog) {
        CustomExerciseDialog(
            targetSets = starterTargetSets,
            repRange = starterRepRange,
            restSeconds = starterRestSeconds,
            onTargetSetsChange = { starterTargetSets = it },
            onRepRangeChange = { starterRepRange = it },
            onRestSecondsChange = { starterRestSeconds = it },
            onConfirm = { name, muscleGroup, equipment ->
                onAddExerciseToRoutine(routine.id, name, muscleGroup, equipment, starterTargetSets, starterRepRange, starterRestSeconds)
                showStarterCustomExerciseDialog = false
            },
            onDismiss = { showStarterCustomExerciseDialog = false },
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isEditing) {
                OutlinedTextField(editName, { editName = it }, label = { Text("Routine name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(editDescription, { editDescription = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onUpdateRoutine(routine.id, editName, editDescription)
                            isEditing = false
                        },
                    ) {
                        Text("Save")
                    }
                    TextButton(
                        onClick = {
                            editName = routine.name
                            editDescription = routine.description
                            isEditing = false
                        },
                    ) {
                        Text("Annuleer")
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(routine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            routine.description.ifBlank { "No description yet." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { isEditing = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit routine")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSetActiveRoutine(routine.id) }) { Text(if (routine.active) "Active" else "Set active") }
                TextButton(onClick = { onDeleteRoutine(routine.id) }) { Text("Delete") }
            }
            HorizontalDivider()
            Text("Add session", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(dayName, { dayName = it }, label = { Text("Session name (optional)") }, modifier = Modifier.weight(1f))
                Button(onClick = { onAddDay(routine.id, dayName); dayName = "" }) { Text("Add") }
            }
            if (routine.days.isEmpty()) {
                Text(
                    "No sessions yet. Add an exercise now and TrainIQ will create Session 1 automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { showStarterExercisePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("Add first exercise")
                }
            } else {
                routine.days.forEach { day ->
                    WorkoutDayEditor(
                        day = day,
                        exerciseLibrary = exerciseLibrary,
                        onStartWorkout = onStartWorkout,
                        onRemoveDay = onRemoveDay,
                        onAddExercise = onAddExercise,
                        onRemoveExercise = onRemoveExercise,
                        onReorderExercises = onReorderExercises,
                        onSetSupersetGroup = onSetSupersetGroup,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutDayEditor(
    day: WorkoutDay,
    exerciseLibrary: List<Exercise>,
    onStartWorkout: (Long) -> Unit,
    onRemoveDay: (Long) -> Unit,
    onAddExercise: (Long, String, String, String, String, String, String) -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onReorderExercises: (Long, List<Long>) -> Unit,
    onSetSupersetGroup: (List<Long>, Long?) -> Unit,
) {
    var targetSets by remember(day.id) { mutableStateOf("3") }
    var repRange by remember(day.id) { mutableStateOf("8-12") }
    var restSeconds by remember(day.id) { mutableStateOf("90") }
    var showExercisePicker by remember(day.id) { mutableStateOf(false) }
    var showCustomExerciseDialog by remember(day.id) { mutableStateOf(false) }
    var orderedPlans by remember(day.id, day.exercises) { mutableStateOf(day.exercises) }

    if (showExercisePicker) {
        ExercisePickerSheet(
            exercises = exerciseLibrary,
            targetSets = targetSets,
            repRange = repRange,
            restSeconds = restSeconds,
            onTargetSetsChange = { targetSets = it },
            onRepRangeChange = { repRange = it },
            onRestSecondsChange = { restSeconds = it },
            onSelect = { exercise ->
                onAddExercise(day.id, exercise.name, exercise.muscleGroup, exercise.equipment, targetSets, repRange, restSeconds)
                showExercisePicker = false
            },
            onCustomExercise = {
                showExercisePicker = false
                showCustomExerciseDialog = true
            },
            onDismiss = { showExercisePicker = false },
        )
    }
    if (showCustomExerciseDialog) {
        CustomExerciseDialog(
            targetSets = targetSets,
            repRange = repRange,
            restSeconds = restSeconds,
            onTargetSetsChange = { targetSets = it },
            onRepRangeChange = { repRange = it },
            onRestSecondsChange = { restSeconds = it },
            onConfirm = { name, muscleGroup, equipment ->
                onAddExercise(day.id, name, muscleGroup, equipment, targetSets, repRange, restSeconds)
                showCustomExerciseDialog = false
            },
            onDismiss = { showCustomExerciseDialog = false },
        )
    }

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
            ReorderableColumn(
                list = orderedPlans,
                onSettle = { fromIndex, toIndex ->
                    orderedPlans = orderedPlans.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }
                    onReorderExercises(day.id, orderedPlans.map { it.id })
                },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { _, plan, _ ->
                key(plan.id) {
                    ReorderableItem {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(
                                    modifier = Modifier.draggableHandle(),
                                    onClick = {},
                                ) {
                                    Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder exercise")
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(plan.exercise.name, fontWeight = FontWeight.SemiBold)
                                    ExercisePrescriptionChips(plan)
                                    plan.supersetGroupId?.let {
                                        Text("Superset $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    TextButton(
                                        onClick = { toggleSupersetGroup(orderedPlans, plan, onSetSupersetGroup) },
                                        enabled = orderedPlans.size > 1,
                                    ) {
                                        Text(if (plan.supersetGroupId == null) "Koppel als superset" else "Ontkoppel")
                                    }
                                    TextButton(onClick = { onRemoveExercise(plan.id) }) { Text("Remove") }
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(targetSets, { targetSets = it }, label = { Text("Sets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(repRange, { repRange = it }, label = { Text("Rep range") }, modifier = Modifier.weight(1f))
                OutlinedTextField(restSeconds, { restSeconds = it }, label = { Text("Rest (s)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Button(onClick = { showExercisePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add exercise")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExercisePrescriptionChips(plan: WorkoutExercisePlan) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        SuggestionChip(onClick = {}, label = { Text("${plan.targetSets} sets") })
        SuggestionChip(onClick = {}, label = { Text("${plan.repRange} reps") })
        SuggestionChip(onClick = {}, label = { Text("${plan.restSeconds}s rest") })
        SuggestionChip(onClick = {}, label = { Text(plan.setType.label()) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    exercises: List<Exercise>,
    targetSets: String,
    repRange: String,
    restSeconds: String,
    onTargetSetsChange: (String) -> Unit,
    onRepRangeChange: (String) -> Unit,
    onRestSecondsChange: (String) -> Unit,
    onSelect: (Exercise) -> Unit,
    onCustomExercise: () -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredExercises by remember(query, exercises) {
        derivedStateOf {
            val normalized = query.trim()
            if (normalized.isBlank()) {
                exercises
            } else {
                exercises.filter {
                    it.name.contains(normalized, ignoreCase = true) ||
                        it.muscleGroup.contains(normalized, ignoreCase = true) ||
                        it.equipment.contains(normalized, ignoreCase = true)
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Add exercise", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search exercises") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(targetSets, onTargetSetsChange, label = { Text("Sets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(repRange, onRepRangeChange, label = { Text("Rep range") }, modifier = Modifier.weight(1f))
                OutlinedTextField(restSeconds, onRestSecondsChange, label = { Text("Rest") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredExercises, key = { it.id }) { exercise ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(exercise.name, fontWeight = FontWeight.SemiBold)
                                Text("${exercise.muscleGroup} - ${exercise.equipment}", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onSelect(exercise) }) { Text("Select") }
                        }
                    }
                }
                item {
                    TextButton(onClick = onCustomExercise, modifier = Modifier.fillMaxWidth()) {
                        Text("Voeg eigen oefening toe")
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomExerciseDialog(
    targetSets: String,
    repRange: String,
    restSeconds: String,
    onTargetSetsChange: (String) -> Unit,
    onRepRangeChange: (String) -> Unit,
    onRestSecondsChange: (String) -> Unit,
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var exerciseName by remember { mutableStateOf("") }
    var muscleGroup by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Voeg eigen oefening toe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(exerciseName, { exerciseName = it }, label = { Text("Exercise name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(muscleGroup, { muscleGroup = it }, label = { Text("Muscle group") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(equipment, { equipment = it }, label = { Text("Equipment") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(targetSets, onTargetSetsChange, label = { Text("Sets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(repRange, onRepRangeChange, label = { Text("Reps") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(restSeconds, onRestSecondsChange, label = { Text("Rest") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(exerciseName, muscleGroup, equipment) },
                enabled = exerciseName.isNotBlank() && muscleGroup.isNotBlank() && equipment.isNotBlank(),
            ) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutRoute(dayId: Long, onBack: () -> Unit, viewModel: WorkoutViewModel = hiltViewModel()) {
    val uiState by viewModel.activeWorkoutUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(dayId) { viewModel.loadWorkout(dayId) }
    DisposableEffect(context) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    ActiveWorkoutScreen(
        uiState = uiState,
        onBack = onBack,
        onDraftChange = viewModel::updateSetDraft,
        onSetTypeChange = viewModel::updateLoggedSetType,
        onEditSet = viewModel::editLoggedSet,
        onDeleteSet = { exerciseId, setIndex -> viewModel.deleteLoggedSet(exerciseId, setIndex) },
        onDismissMessage = viewModel::clearMessage,
        onLogSet = viewModel::logSet,
        onLogSameAgain = viewModel::logSameAgain,
        onAdjustRestTimer = viewModel::adjustRestTimer,
        onSkipRestTimer = viewModel::skipRestTimer,
        onRestartRestTimer = viewModel::restartRestTimer,
        onFinish = { viewModel.finishWorkout(dayId) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    uiState: ActiveWorkoutUiState,
    onBack: () -> Unit,
    onDraftChange: (Long, SetInputDraft) -> Unit,
    onSetTypeChange: (Long, Int, SetType) -> Unit,
    onEditSet: (Long, Int) -> Unit,
    onDeleteSet: (Long, Int) -> Unit,
    onDismissMessage: () -> Unit,
    onLogSet: (WorkoutExercisePlan) -> Boolean,
    onLogSameAgain: (WorkoutExercisePlan) -> Boolean,
    onAdjustRestTimer: (Int) -> Unit,
    onSkipRestTimer: () -> Unit,
    onRestartRestTimer: (Int) -> Unit,
    onFinish: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var previousRestTimerSeconds by remember { mutableStateOf(uiState.restTimerSeconds) }
    val currentOnDismissMessage by rememberUpdatedState(onDismissMessage)

    LaunchedEffect(uiState.restTimerSeconds) {
        if (uiState.restTimerSeconds > 0 && uiState.restTimerSeconds % 10 == 0 && uiState.restTimerSeconds != previousRestTimerSeconds) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        if (previousRestTimerSeconds > 0 && uiState.restTimerSeconds == 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        previousRestTimerSeconds = uiState.restTimerSeconds
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(uiState.workout?.name ?: "Workout") },
                    navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                )
                if (uiState.restTimerSeconds > 0) {
                    val progress = if (uiState.restTimerTotalSeconds > 0) {
                        uiState.restTimerSeconds / uiState.restTimerTotalSeconds.toFloat()
                    } else {
                        0f
                    }
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "${uiState.restTimerSeconds}s rust",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            uiState.message?.let { message ->
                item { MessageCard(message = message, onDismiss = currentOnDismissMessage) }
            }
            if (uiState.workout == null) {
                item { EmptyCard("Workout unavailable", "This workout could not be loaded.") }
                return@LazyColumn
            }
            item { WorkoutSessionStatusCard(uiState = uiState) }
            if (uiState.restTimerSeconds > 0) {
                item {
                    RestTimerCard(
                        restTimerSeconds = uiState.restTimerSeconds,
                        totalSeconds = uiState.restTimerTotalSeconds,
                        onAdjust = onAdjustRestTimer,
                        onSkip = onSkipRestTimer,
                        onRestart = {
                            val nextRest = uiState.workout.exercises.firstOrNull()?.restSeconds ?: uiState.restTimerTotalSeconds
                            onRestartRestTimer(nextRest)
                        },
                    )
                }
            }
            items(
                workoutExerciseGroups(uiState.workout.exercises),
                key = { group -> group.joinToString("-") { it.id.toString() } },
            ) { group ->
                if (group.size > 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Superset", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        group.forEach { plan ->
                            ActiveWorkoutPlanCard(
                                plan = plan,
                                uiState = uiState,
                                isResting = uiState.restTimerSeconds > 0,
                                hapticOnSuccess = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                                onDraftChange = onDraftChange,
                                onSetTypeChange = onSetTypeChange,
                                onEditSet = onEditSet,
                                onDeleteSet = onDeleteSet,
                                onLogSet = onLogSet,
                                onLogSameAgain = onLogSameAgain,
                            )
                        }
                    }
                } else {
                    ActiveWorkoutPlanCard(
                        plan = group.first(),
                        uiState = uiState,
                        isResting = uiState.restTimerSeconds > 0,
                        hapticOnSuccess = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onDraftChange = onDraftChange,
                        onSetTypeChange = onSetTypeChange,
                        onEditSet = onEditSet,
                        onDeleteSet = onDeleteSet,
                        onLogSet = onLogSet,
                        onLogSameAgain = onLogSameAgain,
                    )
                }
            }
            item { Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Finish workout") } }
            if (uiState.debrief != null) item { WorkoutDebriefCard(uiState.debrief, uiState) }
        }
    }
}

@Composable
private fun ActiveWorkoutPlanCard(
    plan: WorkoutExercisePlan,
    uiState: ActiveWorkoutUiState,
    isResting: Boolean,
    hapticOnSuccess: () -> Unit,
    onDraftChange: (Long, SetInputDraft) -> Unit,
    onSetTypeChange: (Long, Int, SetType) -> Unit,
    onEditSet: (Long, Int) -> Unit,
    onDeleteSet: (Long, Int) -> Unit,
    onLogSet: (WorkoutExercisePlan) -> Boolean,
    onLogSameAgain: (WorkoutExercisePlan) -> Boolean,
) {
    val draft = uiState.drafts[plan.exercise.id] ?: SetInputDraft()
    val loggedSets = uiState.loggedSetsThisSession[plan.exercise.id].orEmpty()
    ActiveExerciseCard(
        plan = plan,
        loggedSets = loggedSets,
        suggestion = uiState.progressionSuggestions.firstOrNull { it.exerciseId == plan.exercise.id },
        draft = draft,
        isResting = isResting,
        onDraftChange = { next -> onDraftChange(plan.exercise.id, next) },
        onSetTypeChange = { setIndex, setType -> onSetTypeChange(plan.exercise.id, setIndex, setType) },
        onEditSet = { setIndex -> onEditSet(plan.exercise.id, setIndex) },
        onDeleteSet = { setIndex -> onDeleteSet(plan.exercise.id, setIndex) },
        onCopyLastSet = {
            loggedSets.lastOrNull()?.let { lastSet ->
                onDraftChange(plan.exercise.id, lastSet.toDraft())
            }
        },
        onLogSet = {
            if (onLogSet(plan)) {
                hapticOnSuccess()
            }
        },
        onLogSameAgain = {
            if (onLogSameAgain(plan)) {
                hapticOnSuccess()
            }
        },
    )
}

@Composable
private fun RestTimerCard(
    restTimerSeconds: Int,
    totalSeconds: Int,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit,
    onRestart: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Rest timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(if (restTimerSeconds <= 15) "Bijna klaar" else "Herstel", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(formatTimer(restTimerSeconds), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(
                progress = { if (totalSeconds > 0) (restTimerSeconds / totalSeconds.toFloat()).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { onAdjust(-30) }) { Icon(Icons.Rounded.Remove, contentDescription = "Min 30 seconds") }
                IconButton(onClick = { onAdjust(30) }) { Icon(Icons.Rounded.Add, contentDescription = "Plus 30 seconds") }
                IconButton(onClick = onRestart) { Icon(Icons.Rounded.Replay, contentDescription = "Restart timer") }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onSkip) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = null)
                    Text("Skip")
                }
            }
        }
    }
}

@Composable
private fun ActiveExerciseCard(
    plan: WorkoutExercisePlan,
    loggedSets: List<LoggedSet>,
    suggestion: ProgressionSuggestion?,
    draft: SetInputDraft,
    isResting: Boolean,
    onDraftChange: (SetInputDraft) -> Unit,
    onSetTypeChange: (Int, SetType) -> Unit,
    onEditSet: (Int) -> Unit,
    onDeleteSet: (Int) -> Unit,
    onCopyLastSet: () -> Unit,
    onLogSet: () -> Unit,
    onLogSameAgain: () -> Unit,
) {
    val targetWeight = draft.weight.toFloatOrNull() ?: suggestion?.suggestedWeightKg?.toFloat()
    val platePlan = targetWeight?.let { StrengthCalculator.calculatePlates(it) }.orEmpty()
    val liveOneRepMax by remember(draft) {
        derivedStateOf {
            val weight = draft.weight.replace(',', '.').toDoubleOrNull() ?: 0.0
            val reps = draft.reps.toIntOrNull() ?: 0
            if (weight > 0.0 && reps > 0) StrengthCalculator.estimateOneRepMax(weight, reps) else null
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (suggestion != null) ExerciseProgressionHint(suggestion)
            Text(plan.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${plan.targetSets} sets - ${plan.repRange} reps - ${plan.restSeconds}s rest", style = MaterialTheme.typography.bodySmall)
            suggestion?.let { SuggestedNextSetRow(it) }
            repeat(plan.targetSets) { index ->
                SetRow(
                    index = index + 1,
                    repRange = plan.repRange,
                    loggedSet = loggedSets.getOrNull(index),
                    isCurrent = index == loggedSets.size && loggedSets.size < plan.targetSets,
                    onCycleType = {
                        loggedSets.getOrNull(index)?.let { set ->
                            onSetTypeChange(index, set.setType.next())
                        }
                    },
                    onEdit = { onEditSet(index) },
                    onDelete = { onDeleteSet(index) },
                )
            }
            if (platePlan.isNotEmpty()) {
                PlateBarDiagram(
                    plates = platePlan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                )
            }
            AnimatedContent(targetState = isResting, label = "exercise-rest-state") { resting ->
                Text(
                    text = if (resting) {
                        "Rustfase actief. Timer-haptics lopen door terwijl je herstelt voor de volgende set."
                    } else {
                        "Log je volgende set met gewicht, reps en optioneel RPE voor een automatische RIR-inschatting."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SetTypeSelector(
                    selectedType = draft.setType,
                    onSelectedTypeChange = { onDraftChange(draft.copy(setType = it)) },
                )
                SetLoggerFields(
                    draft = draft,
                    lastSession = suggestion?.toLastSessionDraft(),
                    onDraftChange = onDraftChange,
                )
                liveOneRepMax?.let { oneRepMax ->
                    Text(
                        "Geschatte 1RM: ${formatWeight(oneRepMax)} kg",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onCopyLastSet,
                        enabled = loggedSets.isNotEmpty(),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy vorige set")
                    }
                    TextButton(
                        onClick = onLogSameAgain,
                        enabled = loggedSets.isNotEmpty(),
                    ) {
                        Text("Zelfde opnieuw")
                    }
                    Button(onClick = onLogSet, modifier = Modifier.weight(1f)) { Text("Log set") }
                }
            }
        }
    }
}

@Composable
private fun SetTypeSelector(selectedType: SetType, onSelectedTypeChange: (SetType) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(SetType.entries) { type ->
            FilterChip(
                selected = type == selectedType,
                onClick = { onSelectedTypeChange(type) },
                label = { Text(type.label()) },
            )
        }
    }
}

@Composable
private fun SetLoggerFields(
    draft: SetInputDraft,
    lastSession: SetInputDraft?,
    onDraftChange: (SetInputDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickNumberField(
                value = draft.weight,
                label = "Gewicht",
                suffix = "kg",
                keyboardType = KeyboardType.Decimal,
                step = 2.5,
                fallback = lastSession?.weight,
                modifier = Modifier.weight(1f),
                onValueChange = { onDraftChange(draft.copy(weight = it)) },
            )
            QuickNumberField(
                value = draft.reps,
                label = "Reps",
                suffix = "",
                keyboardType = KeyboardType.Number,
                step = 1.0,
                fallback = lastSession?.reps,
                modifier = Modifier.weight(1f),
                onValueChange = { onDraftChange(draft.copy(reps = it)) },
            )
            QuickNumberField(
                value = draft.rpe,
                label = "RPE",
                suffix = "",
                keyboardType = KeyboardType.Decimal,
                step = 0.5,
                fallback = null,
                modifier = Modifier.weight(1f),
                onValueChange = { onDraftChange(draft.copy(rpe = it)) },
            )
        }
    }
}

@Composable
private fun QuickNumberField(
    value: String,
    label: String,
    suffix: String,
    keyboardType: KeyboardType,
    step: Double,
    fallback: String?,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.replace(',', '.')) },
            label = { Text(if (suffix.isBlank()) label else "$label ($suffix)") },
            placeholder = { Text(fallback.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.defaultMinSize(minHeight = 64.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { onValueChange(adjustNumberText(value.ifBlank { fallback.orEmpty() }, -step)) }) {
                Icon(Icons.Rounded.Remove, contentDescription = "Decrease $label")
            }
            IconButton(onClick = { onValueChange(adjustNumberText(value.ifBlank { fallback.orEmpty() }, step)) }) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase $label")
            }
        }
    }
}

@Composable
private fun SetRow(
    index: Int,
    repRange: String,
    loggedSet: LoggedSet?,
    isCurrent: Boolean,
    onCycleType: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val background = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val rpeColor = loggedSet?.let { intensityContainerColor(it.rpe) } ?: background
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rpeColor, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (loggedSet != null && loggedSet.weight > 0.0 && loggedSet.reps > 0) {
                Icon(Icons.Default.Check, contentDescription = "Completed set")
            }
            Text("Set $index")
            loggedSet?.let { set ->
                FilterChip(
                    selected = true,
                    onClick = onCycleType,
                    label = { Text(set.setType.label()) },
                )
            }
        }
        Text(
            loggedSet?.let {
                buildString {
                    append("${formatWeight(it.weight)} kg x ${it.reps}")
                    if (it.rpe > 0.0) append(" | RPE ${formatWeight(it.rpe)}")
                    if (it.repsInReserve != null) append(" | RIR ${it.repsInReserve}")
                }
            } ?: repRange,
        )
        loggedSet?.let {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit set")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete set")
                }
            }
        }
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
private fun SuggestedNextSetRow(suggestion: ProgressionSuggestion) {
    val action = when (suggestion.readinessSignal) {
        ReadinessLevel.INCREASE -> "Verhoog"
        ReadinessLevel.DELOAD -> "Deload"
        ReadinessLevel.PLATEAU -> "Plateau: voeg reps of kleinere stap toe"
        ReadinessLevel.MAINTAIN -> "Behouden"
    }
    Text(
        "Aanbevolen: ${formatWeight(suggestion.suggestedWeightKg)} kg x ${displayRepTarget(suggestion.suggestedReps)} bij RPE 7-8 - $action",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun WorkoutSessionStatusCard(uiState: ActiveWorkoutUiState) {
    val workout = uiState.workout ?: return
    val loggedSets = uiState.loggedSetsThisSession.values.flatten()
    val targetSets = workout.exercises.sumOf { it.targetSets }
    val remainingSets = (targetSets - loggedSets.size).coerceAtLeast(0)
    val volume = loggedSets.sumOf { it.weight * it.reps }
    val avgRpe = loggedSets.map { it.rpe }.filter { it > 0.0 }.average().takeIf { !it.isNaN() }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusMetric("Sets", "${loggedSets.size}/$targetSets")
                StatusMetric("Rest", if (uiState.restTimerSeconds > 0) formatTimer(uiState.restTimerSeconds) else "Klaar")
                StatusMetric("Volume", "${volume.toInt()} kg")
            }
            LinearProgressIndicator(
                progress = { if (targetSets > 0) (loggedSets.size / targetSets.toFloat()).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "$remainingSets sets resterend${avgRpe?.let { " - gemiddelde RPE ${formatWeight(it)}" }.orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WorkoutDebriefCard(result: WorkoutDebrief, uiState: ActiveWorkoutUiState) {
    val sets = uiState.loggedSetsThisSession.values.flatten()
    val volume = sets.sumOf { it.weight * it.reps }
    val topSet = sets.maxByOrNull { it.weight * it.reps }
    val highestRpe = sets.maxOfOrNull { it.rpe } ?: 0.0
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Workout samenvatting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusMetric("Volume", "${volume.toInt()} kg")
                StatusMetric("Sets", sets.size.toString())
                StatusMetric("Hoogste RPE", if (highestRpe > 0.0) formatWeight(highestRpe) else "-")
            }
            topSet?.let {
                Text("Top set: ${formatWeight(it.weight)} kg x ${it.reps}", style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider()
            Text("Coachkaart", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Wat ging goed: ${result.wins.firstOrNull() ?: result.summary}")
            Text("Waarop letten: ${result.risks.firstOrNull() ?: result.progressionFeedback}")
            Text("Volgende training: ${result.nextLoadTarget.ifBlank { result.nextSessionFocus }}", fontWeight = FontWeight.Medium)
            Text("Actie: ${result.recommendation}")
            if (result.recoveryAdvice.isNotBlank()) {
                Text("Herstel: ${result.recoveryAdvice}")
            }
            Text("Intensity signal: ${result.intensitySignal}", color = intensityContentColor(result.intensitySignal))
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
    ReadinessLevel.PLATEAU -> suggestion.suggestedWeightKg
    ReadinessLevel.MAINTAIN -> suggestion.suggestedWeightKg
}

private fun displayRepTarget(repRange: String): String = repRange.substringAfter('-', repRange)

private fun toggleSupersetGroup(
    plans: List<WorkoutExercisePlan>,
    plan: WorkoutExercisePlan,
    onSetSupersetGroup: (List<Long>, Long?) -> Unit,
) {
    val currentGroup = plan.supersetGroupId
    if (currentGroup != null) {
        val groupIds = plans.filter { it.supersetGroupId == currentGroup }.map { it.id }
        onSetSupersetGroup(groupIds.ifEmpty { listOf(plan.id) }, null)
        return
    }

    val index = plans.indexOfFirst { it.id == plan.id }
    val partner = plans.getOrNull(index - 1) ?: plans.getOrNull(index + 1) ?: return
    val groupId = partner.supersetGroupId ?: minOf(plan.id, partner.id)
    val linkedIds = plans
        .filter { it.supersetGroupId == groupId }
        .map { it.id }
        .plus(listOf(plan.id, partner.id))
        .distinct()
    onSetSupersetGroup(linkedIds, groupId)
}

private fun workoutExerciseGroups(plans: List<WorkoutExercisePlan>): List<List<WorkoutExercisePlan>> {
    val grouped = plans.filter { it.supersetGroupId != null }.groupBy { it.supersetGroupId }
    val consumedGroups = mutableSetOf<Long?>()
    return plans.mapNotNull { plan ->
        val groupId = plan.supersetGroupId
        if (groupId == null) {
            listOf(plan)
        } else if (consumedGroups.add(groupId)) {
            grouped[groupId].orEmpty()
        } else {
            null
        }
    }
}

private fun formatWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toInt().toString() else "%.1f".format(Locale.US, weight)

private fun formatPlateWeight(weight: Float): String =
    if (weight % 1f == 0f) weight.toInt().toString() else "%.2f".format(Locale.US, weight)

private fun formatTimer(seconds: Int): String = "%d:%02d".format(Locale.US, seconds / 60, seconds % 60)

private fun adjustNumberText(value: String, delta: Double): String {
    val current = value.replace(',', '.').toDoubleOrNull() ?: 0.0
    val next = (current + delta).coerceAtLeast(0.0)
    return formatWeight(next)
}

@Composable
private fun intensityContainerColor(rpe: Double): Color = when {
    rpe >= 9.5 -> MaterialTheme.colorScheme.errorContainer
    rpe >= 8.0 -> MaterialTheme.colorScheme.tertiaryContainer
    rpe > 0.0 -> MaterialTheme.colorScheme.primaryContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun intensityContentColor(signal: String): Color = when (signal.uppercase(Locale.US)) {
    "DELOAD" -> MaterialTheme.colorScheme.error
    "INCREASE" -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun LoggedSet.toDraft() = SetInputDraft(
    weight = formatWeight(weight),
    reps = reps.toString(),
    rpe = if (rpe > 0.0) formatWeight(rpe) else "",
    setType = setType,
)

private fun ProgressionSuggestion.toLastSessionDraft(): SetInputDraft? {
    val weight = lastLoggedWeightKg ?: return null
    val reps = lastLoggedReps?.takeIf { it.isNotBlank() } ?: return null
    return SetInputDraft(
        weight = formatWeight(weight),
        reps = reps,
    )
}

private fun SetType.label(): String = when (this) {
    SetType.WARMUP -> "Warmup"
    SetType.WORKING -> "Working"
    SetType.TOP_SET -> "Top set"
    SetType.BACKOFF -> "Backoff"
}

private fun SetType.next(): SetType = when (this) {
    SetType.WARMUP -> SetType.WORKING
    SetType.WORKING -> SetType.TOP_SET
    SetType.TOP_SET -> SetType.BACKOFF
    SetType.BACKOFF -> SetType.WARMUP
}
