@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.trainiq.features.workout

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.ui.MessageCard
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.core.ui.AppCard
import com.trainiq.core.ui.AppChip
import com.trainiq.core.ui.AppLinearProgress
import com.trainiq.core.ui.clearFocusOnTapOutside
import com.trainiq.core.ui.clearFocusOnScrollOrDrag
import com.trainiq.core.ui.EmptyStateCard
import com.trainiq.core.ui.PrimaryActionButton
import com.trainiq.core.ui.SecondaryActionButton
import com.trainiq.core.ui.bringIntoViewOnFocus
import com.trainiq.core.audio.RestTimerSoundPlayer
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.datastore.WorkoutFeedbackPreferences
import com.trainiq.core.theme.spacing
import com.trainiq.core.theme.trainIqColors
import com.trainiq.domain.model.ActiveWorkoutFocusTarget
import com.trainiq.domain.model.ActiveWorkoutSession
import com.trainiq.domain.model.ActiveWorkoutSetDraft
import com.trainiq.domain.model.ChartPoint
import com.trainiq.domain.model.ExerciseHistory
import com.trainiq.domain.model.ExerciseHistorySession
import com.trainiq.domain.model.ExerciseRankProgress
import com.trainiq.domain.model.ExerciseStats
import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.GeneratedRoutine
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.ProgressionSuggestion
import com.trainiq.domain.model.ReadinessLevel
import com.trainiq.domain.model.RoutineSet
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.StrengthCalculator
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutDebrief
import com.trainiq.domain.model.WorkoutExercisePlan
import com.trainiq.domain.model.WorkoutLoggingSummary
import com.trainiq.domain.model.WorkoutOverview
import com.trainiq.domain.model.WorkoutRoutine
import com.trainiq.domain.usecase.AddExerciseToDayUseCase
import com.trainiq.domain.usecase.AddExerciseToRoutineUseCase
import com.trainiq.domain.usecase.AddSetToExerciseUseCase
import com.trainiq.domain.usecase.AddWorkoutDayUseCase
import com.trainiq.domain.usecase.CreateRoutineUseCase
import com.trainiq.domain.usecase.DeleteRoutineUseCase
import com.trainiq.domain.usecase.DeleteWorkoutSessionUseCase
import com.trainiq.domain.usecase.DeleteActiveWorkoutSetUseCase
import com.trainiq.domain.usecase.DiscardActiveWorkoutUseCase
import com.trainiq.domain.usecase.FinishActiveWorkoutUseCase
import com.trainiq.domain.usecase.GenerateAiRoutineUseCase
import com.trainiq.domain.usecase.GetProgressionSuggestionsUseCase
import com.trainiq.domain.usecase.GetOrStartActiveWorkoutSessionUseCase
import com.trainiq.domain.usecase.GetWorkoutDayUseCase
import com.trainiq.domain.usecase.LogActiveWorkoutSetUseCase
import com.trainiq.domain.usecase.DeleteRoutineSetUseCase
import com.trainiq.domain.usecase.ObserveWorkoutOverviewUseCase
import com.trainiq.domain.usecase.ObserveWorkoutLoggingSummaryUseCase
import com.trainiq.domain.usecase.ObserveExerciseHistoryUseCase
import com.trainiq.domain.usecase.RemoveExerciseFromDayUseCase
import com.trainiq.domain.usecase.RemoveWorkoutDayUseCase
import com.trainiq.domain.usecase.MoveRoutineSetUseCase
import com.trainiq.domain.usecase.ReplaceExerciseInPlanUseCase
import com.trainiq.domain.usecase.ReorderExercisesUseCase
import com.trainiq.domain.usecase.SaveGeneratedRoutineUseCase
import com.trainiq.domain.usecase.SetActiveRoutineUseCase
import com.trainiq.domain.usecase.SetActiveWorkoutCollapsedUseCase
import com.trainiq.domain.usecase.SetSupersetGroupUseCase
import com.trainiq.domain.usecase.UpdateActiveWorkoutDraftUseCase
import com.trainiq.domain.usecase.UpdateActiveWorkoutRestTimerUseCase
import com.trainiq.domain.usecase.UpdateActiveWorkoutSetTypeUseCase
import com.trainiq.domain.usecase.UndoWorkoutLogEventUseCase
import com.trainiq.domain.usecase.UpdateRoutineSetUseCase
import com.trainiq.domain.usecase.UpdateRoutineUseCase
import com.trainiq.domain.usecase.UpdateWorkoutExercisePlanUseCase
import com.trainiq.ai.services.toAiUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem

data class SetInputDraft(
    val weight: String = "",
    val reps: String = "",
    val rpe: String = "",
    val setType: SetType = SetType.NORMAL,
)

data class ActiveWorkoutUiState(
    val workout: WorkoutDay? = null,
    val activeSession: ActiveWorkoutSession? = null,
    val progressionSuggestions: List<ProgressionSuggestion> = emptyList(),
    val loggedSetsThisSession: Map<Long, List<LoggedSet>> = emptyMap(),
    val restTimerSeconds: Int = 0,
    val restTimerTotalSeconds: Int = 0,
    val debrief: WorkoutDebrief? = null,
    val drafts: Map<Long, SetInputDraft> = emptyMap(),
    val collapsedExerciseIds: Set<Long> = emptySet(),
    val elapsedSeconds: Long = 0L,
    val completedSets: Int = 0,
    val targetSets: Int = 0,
    val totalVolume: Double = 0.0,
    val canFinish: Boolean = false,
    val needsFinishConfirmation: Boolean = false,
    val loggingSummary: WorkoutLoggingSummary = WorkoutLoggingSummary(),
    val activeFocusTarget: ActiveWorkoutFocusTarget? = null,
    val message: String? = null,
)

private data class ExercisePlanInput(
    val targetSets: Int,
    val repRange: String,
    val restSeconds: Int,
    val targetWeightKg: Double,
    val targetRpe: Double,
)

sealed interface WorkoutUiEvent {
    val id: Long

    data class RestTimerFinished(
        override val id: Long,
        val message: String,
    ) : WorkoutUiEvent

    data class SetLogged(
        override val id: Long,
        val message: String,
        val undoEventId: Long?,
    ) : WorkoutUiEvent
}

private val BuilderActionWidth = 40.dp
private val BuilderRowActionWidth = 44.dp
private val ActiveSetActionWidth = 96.dp
private val ActiveSetLeadingWidth = 76.dp
private val TopLevelBottomContentPadding = 132.dp
private val ActiveWorkoutBottomContentPadding = 96.dp

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
    private val preferencesRepository: UserPreferencesRepository,
    private val observeExerciseHistoryUseCase: ObserveExerciseHistoryUseCase,
    private val observeWorkoutLoggingSummaryUseCase: ObserveWorkoutLoggingSummaryUseCase,
    private val getWorkoutDayUseCase: GetWorkoutDayUseCase,
    private val getProgressionSuggestionsUseCase: GetProgressionSuggestionsUseCase,
    private val getOrStartActiveWorkoutSessionUseCase: GetOrStartActiveWorkoutSessionUseCase,
    private val updateActiveWorkoutDraftUseCase: UpdateActiveWorkoutDraftUseCase,
    private val logActiveWorkoutSetUseCase: LogActiveWorkoutSetUseCase,
    private val updateActiveWorkoutSetTypeUseCase: UpdateActiveWorkoutSetTypeUseCase,
    private val deleteActiveWorkoutSetUseCase: DeleteActiveWorkoutSetUseCase,
    private val undoWorkoutLogEventUseCase: UndoWorkoutLogEventUseCase,
    private val setActiveWorkoutCollapsedUseCase: SetActiveWorkoutCollapsedUseCase,
    private val updateActiveWorkoutRestTimerUseCase: UpdateActiveWorkoutRestTimerUseCase,
    private val finishActiveWorkoutUseCase: FinishActiveWorkoutUseCase,
    private val discardActiveWorkoutUseCase: DiscardActiveWorkoutUseCase,
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
    private val replaceExerciseInPlanUseCase: ReplaceExerciseInPlanUseCase,
    private val updateWorkoutExercisePlanUseCase: UpdateWorkoutExercisePlanUseCase,
    private val addSetToExerciseUseCase: AddSetToExerciseUseCase,
    private val updateRoutineSetUseCase: UpdateRoutineSetUseCase,
    private val deleteRoutineSetUseCase: DeleteRoutineSetUseCase,
    private val moveRoutineSetUseCase: MoveRoutineSetUseCase,
) : ViewModel() {
    val overview: StateFlow<WorkoutOverview?> = observeWorkoutOverviewUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val workoutFeedbackPreferences: StateFlow<WorkoutFeedbackPreferences> = preferencesRepository.workoutFeedbackPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutFeedbackPreferences())

    private val _activeWorkout = MutableStateFlow<WorkoutDay?>(null)
    val activeWorkout: StateFlow<WorkoutDay?> = _activeWorkout.asStateFlow()
    private val _activeSession = MutableStateFlow<ActiveWorkoutSession?>(null)

    private val _progressionSuggestions = MutableStateFlow<List<ProgressionSuggestion>>(emptyList())
    val progressionSuggestions: StateFlow<List<ProgressionSuggestion>> = _progressionSuggestions.asStateFlow()

    private val _loggedSetsThisSession = MutableStateFlow<Map<Long, List<LoggedSet>>>(emptyMap())
    val loggedSetsThisSession: StateFlow<Map<Long, List<LoggedSet>>> = _loggedSetsThisSession.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()
    private val _restTimerTotalSeconds = MutableStateFlow(0)
    private val _elapsedSeconds = MutableStateFlow(0L)

    private val _debrief = MutableStateFlow<WorkoutDebrief?>(null)
    val debrief: StateFlow<WorkoutDebrief?> = _debrief.asStateFlow()

    private val _drafts = MutableStateFlow<Map<Long, SetInputDraft>>(emptyMap())
    private val _loggingSummary = MutableStateFlow(WorkoutLoggingSummary())
    private val _activeFocusTarget = MutableStateFlow<ActiveWorkoutFocusTarget?>(null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _pendingGeneratedRoutine = MutableStateFlow<GeneratedRoutine?>(null)
    val pendingGeneratedRoutine: StateFlow<GeneratedRoutine?> = _pendingGeneratedRoutine.asStateFlow()

    private val _events = MutableSharedFlow<WorkoutUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<WorkoutUiEvent> = _events.asSharedFlow()

    val activeWorkoutUiState: StateFlow<ActiveWorkoutUiState> = combine(
        _activeWorkout,
        _activeSession,
        _progressionSuggestions,
        _loggedSetsThisSession,
        _restTimerSeconds,
    ) { workout, activeSession, suggestions, loggedSets, restTimerSeconds ->
        val allSets = loggedSets.values.flatten()
        val targetSets = workout?.exercises?.sumOf { it.plannedSetCount() } ?: 0
        ActiveWorkoutUiState(
            workout = workout,
            activeSession = activeSession,
            progressionSuggestions = suggestions,
            loggedSetsThisSession = loggedSets,
            restTimerSeconds = restTimerSeconds,
            collapsedExerciseIds = activeSession?.collapsedExerciseIds.orEmpty(),
            completedSets = allSets.size,
            targetSets = targetSets,
            totalVolume = allSets.sumOf { it.weight * it.reps },
            canFinish = allSets.isNotEmpty(),
            needsFinishConfirmation = allSets.isEmpty() || (targetSets > 0 && allSets.size < targetSets),
        )
    }.combine(_debrief) { state, debrief ->
        state.copy(debrief = debrief)
    }.combine(_elapsedSeconds) { state, elapsedSeconds ->
        state.copy(elapsedSeconds = elapsedSeconds)
    }.combine(_restTimerTotalSeconds) { state, restTimerTotalSeconds ->
        state.copy(restTimerTotalSeconds = restTimerTotalSeconds)
    }.combine(_drafts) { state, drafts ->
        state.copy(drafts = drafts)
    }.combine(_loggingSummary) { state, loggingSummary ->
        state.copy(loggingSummary = loggingSummary)
    }.combine(_activeFocusTarget) { state, activeFocusTarget ->
        state.copy(activeFocusTarget = activeFocusTarget)
    }.combine(_message) { state, message ->
        state.copy(message = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState())

    private var restTimerJob: Job? = null
    private var loggingSummaryJob: Job? = null
    private var sessionStartTime: Long = 0L
    private var lastGenerationRequest: RoutineGenerationRequest? = null
    private var observedRestTimerEndsAt: Long? = null
    private var restTimerFinishHandled = true
    private var restTimerClearRequested = false
    private var eventId = 0L

    fun observeExerciseHistory(exerciseId: Long): StateFlow<ExerciseHistory?> =
        observeExerciseHistoryUseCase(exerciseId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun loadWorkout(dayId: Long) {
        viewModelScope.launch {
            _debrief.value = null
            _message.value = null
            val workout = getWorkoutDayUseCase(dayId)
            val suggestions = getProgressionSuggestionsUseCase(dayId)
            _activeWorkout.value = workout
            _progressionSuggestions.value = suggestions
            val suggestionDrafts = suggestions
                .mapNotNull { suggestion ->
                    val draft = suggestion.toLastSessionDraft() ?: return@mapNotNull null
                    suggestion.exerciseId to draft.toDomainDraft()
                }
                .toMap()
            val planDrafts = workout?.exercises.orEmpty()
                .mapNotNull { plan ->
                    val draft = plan.nextPlannedDraft(loggedCount = 0).takeIf { it.weight.isNotBlank() || it.reps.isNotBlank() || it.rpe.isNotBlank() } ?: return@mapNotNull null
                    plan.exercise.id to draft.toDomainDraft()
                }
                .toMap()
            val initialDrafts = planDrafts + suggestionDrafts
            val active = getOrStartActiveWorkoutSessionUseCase(dayId, initialDrafts)
            applyActiveSession(active)
            observeLoggingSummary(dayId)
            sessionStartTime = active.startedAt
            if (active.loggedSets.isNotEmpty()) {
                _message.value = "Actieve training hersteld."
            }
            startSessionTicker()
        }
    }

    fun updateSetDraft(exerciseId: Long, draft: SetInputDraft) {
        _drafts.value = _drafts.value.toMutableMap().apply { put(exerciseId, draft) }
        viewModelScope.launch {
            updateActiveWorkoutDraftUseCase(exerciseId, draft.toDomainDraft())?.let(::applyActiveSession)
        }
    }

    fun updateLoggedSetType(exerciseId: Long, setIndex: Int, setType: SetType) {
        viewModelScope.launch {
            updateActiveWorkoutSetTypeUseCase(exerciseId, setIndex, setType)?.let(::applyActiveSession)
        }
    }

    fun editLoggedSet(exerciseId: Long, setIndex: Int) {
        val sets = _loggedSetsThisSession.value[exerciseId].orEmpty()
        val set = sets.getOrNull(setIndex) ?: return
        val draft = set.toDraft()
        _drafts.value = _drafts.value.toMutableMap().apply { put(exerciseId, draft) }
        viewModelScope.launch {
            updateActiveWorkoutDraftUseCase(exerciseId, draft.toDomainDraft())?.let(::applyActiveSession)
        }
        deleteLoggedSet(exerciseId, setIndex, showMessage = false)
        _message.value = "Set ${setIndex + 1} staat klaar om te corrigeren."
    }

    fun deleteLoggedSet(exerciseId: Long, setIndex: Int, showMessage: Boolean = true) {
        viewModelScope.launch {
            deleteActiveWorkoutSetUseCase(exerciseId, setIndex)?.let(::applyActiveSession)
            if (showMessage) _message.value = "Set verwijderd."
        }
    }

    fun logSet(plan: WorkoutExercisePlan): Boolean {
        val loggedCount = _loggedSetsThisSession.value[plan.exercise.id].orEmpty().size
        val draft = _drafts.value[plan.exercise.id] ?: plan.nextPlannedDraft(loggedCount)
        val parsedWeight = draft.weight.normalizedDecimal().toDoubleOrNull()
        val parsedReps = draft.reps.toIntOrNull()
        val parsedRpe = draft.rpe.normalizedDecimal().toDoubleOrNull() ?: 0.0
        if (parsedWeight == null || parsedWeight <= 0.0 || parsedWeight > MaxWeightKg) {
            _message.value = "Voer een gewicht tussen 0 en ${MaxWeightKg.toInt()} kg in."
            return false
        }
        if (parsedReps == null || parsedReps <= 0 || parsedReps > MaxReps) {
            _message.value = "Voer reps tussen 1 en $MaxReps in."
            return false
        }
        if (parsedRpe !in 0.0..10.0) {
            _message.value = "RPE moet tussen 0 en 10 liggen."
            return false
        }
        val loggedSet = LoggedSet(
            exerciseId = plan.exercise.id,
            weight = parsedWeight,
            reps = parsedReps,
            rpe = parsedRpe,
            repsInReserve = StrengthCalculator.estimateRepsInReserve(parsedRpe),
            setType = draft.setType,
        )
        viewModelScope.launch {
            val nextDraft = plan.nextPlannedDraft(loggedCount + 1).takeIf {
                it.weight.isNotBlank() || it.reps.isNotBlank() || it.rpe.isNotBlank()
            } ?: loggedSet.toDraft()
            val dayId = _activeWorkout.value?.id ?: return@launch
            val active = logActiveWorkoutSetUseCase(
                dayId = dayId,
                set = loggedSet,
                draft = nextDraft.toDomainDraft(),
                restSeconds = plan.plannedRestSeconds(loggedCount),
            )
            applyActiveSession(active)
            val loggedSetsByExerciseId = active.loggedSets
                .groupBy { it.exerciseId }
                .mapValues { (_, sets) -> sets.map { it.toLoggedSet() } }
            _activeFocusTarget.value = resolveNextFocusTarget(
                plans = _activeWorkout.value?.exercises.orEmpty(),
                loggedSetsByExerciseId = loggedSetsByExerciseId,
                justLoggedExerciseId = plan.exercise.id,
            )
            val summary = observeWorkoutLoggingSummaryUseCase(dayId).first()
            _loggingSummary.value = summary.copy(activeFocusTarget = _activeFocusTarget.value)
            val message = when (loggedSet.setType) {
                SetType.FAILURE -> "Failure set voltooid voor ${plan.exercise.name}."
                SetType.DROP_SET -> "Drop set voltooid voor ${plan.exercise.name}."
                else -> "Set ${active.loggedSets.count { it.exerciseId == plan.exercise.id }} gelogd voor ${plan.exercise.name}."
            }
            _events.emit(
                WorkoutUiEvent.SetLogged(
                    id = ++eventId,
                    message = message,
                    undoEventId = summary.lastUndoableEventId,
                ),
            )
        }
        return true
    }

    fun undoWorkoutLogEvent(eventId: Long) {
        viewModelScope.launch {
            undoWorkoutLogEventUseCase(eventId)?.let(::applyActiveSession)
            _activeFocusTarget.value = null
            _message.value = "Laatste set hersteld."
        }
    }

    fun logSameAgain(plan: WorkoutExercisePlan): Boolean {
        val lastSet = _loggedSetsThisSession.value[plan.exercise.id].orEmpty().lastOrNull() ?: return false
        _drafts.value = _drafts.value.toMutableMap().apply { put(plan.exercise.id, lastSet.toDraft()) }
        return logSet(plan)
    }

    fun finishWorkout(dayId: Long) {
        viewModelScope.launch {
            stopRestTimer(persist = true)
            _debrief.value = finishActiveWorkoutUseCase(dayId)
            _activeSession.value = null
            _loggedSetsThisSession.value = emptyMap()
            _drafts.value = emptyMap()
            _progressionSuggestions.value = getProgressionSuggestionsUseCase(dayId)
        }
    }

    fun discardWorkout(dayId: Long) {
        viewModelScope.launch {
            stopRestTimer(persist = false)
            discardActiveWorkoutUseCase(dayId)
            _activeSession.value = null
            _loggedSetsThisSession.value = emptyMap()
            _drafts.value = emptyMap()
            _message.value = "Actieve training weggegooid."
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
            addWorkoutDayUseCase(routineId, name.trim().ifBlank { "Session" })
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
        targetWeightKg: String,
        targetRpe: String,
    ) {
        if (name.isBlank() || muscleGroup.isBlank() || equipment.isBlank()) {
            _message.value = "Exercise name, muscle group, and equipment are required."
            return
        }
        val input = parseExercisePlanInput(targetSets, repRange, restSeconds, targetWeightKg, targetRpe)
            ?: run {
                _message.value = PlanValidationMessage
                return
            }
        viewModelScope.launch {
            addExerciseToDayUseCase(
                dayId = dayId,
                name = name.trim(),
                muscleGroup = muscleGroup.trim(),
                equipment = equipment.trim(),
                targetSets = input.targetSets,
                repRange = input.repRange,
                restSeconds = input.restSeconds,
                targetWeightKg = input.targetWeightKg,
                targetRpe = input.targetRpe,
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
        targetWeightKg: String,
        targetRpe: String,
    ) {
        if (name.isBlank() || muscleGroup.isBlank() || equipment.isBlank()) {
            _message.value = "Exercise name, muscle group, and equipment are required."
            return
        }
        val input = parseExercisePlanInput(targetSets, repRange, restSeconds, targetWeightKg, targetRpe)
            ?: run {
                _message.value = PlanValidationMessage
                return
            }
        viewModelScope.launch {
            addExerciseToRoutineUseCase(
                routineId = routineId,
                name = name.trim(),
                muscleGroup = muscleGroup.trim(),
                equipment = equipment.trim(),
                targetSets = input.targetSets,
                repRange = input.repRange,
                restSeconds = input.restSeconds,
                targetWeightKg = input.targetWeightKg,
                targetRpe = input.targetRpe,
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

    fun replaceExerciseInPlan(workoutExerciseId: Long, exercise: Exercise) {
        viewModelScope.launch {
            replaceExerciseInPlanUseCase(workoutExerciseId, exercise.id)
            _message.value = "Exercise replaced with ${exercise.name}."
        }
    }

    fun updateWorkoutExercisePlan(
        workoutExerciseId: Long,
        targetSets: String,
        repRange: String,
        restSeconds: String,
        targetWeightKg: String,
        targetRpe: String,
        setType: SetType,
    ) {
        val input = parseExercisePlanInput(targetSets, repRange, restSeconds, targetWeightKg, targetRpe)
            ?: run {
                _message.value = PlanValidationMessage
                return
            }
        viewModelScope.launch {
            updateWorkoutExercisePlanUseCase(
                workoutExerciseId = workoutExerciseId,
                targetSets = input.targetSets,
                repRange = input.repRange,
                restSeconds = input.restSeconds,
                targetWeightKg = input.targetWeightKg,
                targetRpe = input.targetRpe,
                setType = setType,
            )
            _message.value = "Exercise plan updated."
        }
    }

    fun addSetToExercise(workoutExerciseId: Long) {
        viewModelScope.launch {
            addSetToExerciseUseCase(workoutExerciseId)
            _message.value = "Set toegevoegd."
        }
    }

    fun updateRoutineSet(set: RoutineSet) {
        if (!set.isValidForSave()) {
            _message.value = RoutineSetValidationMessage
            return
        }
        viewModelScope.launch {
            updateRoutineSetUseCase(set)
        }
    }

    fun deleteRoutineSet(setId: Long) {
        viewModelScope.launch {
            deleteRoutineSetUseCase(setId)
            _message.value = "Set verwijderd."
        }
    }

    fun moveRoutineSet(workoutExerciseId: Long, orderedSetIds: List<Long>) {
        viewModelScope.launch {
            moveRoutineSetUseCase(workoutExerciseId, orderedSetIds)
            _message.value = "Set volgorde bijgewerkt."
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

    private fun startSessionTicker() {
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (true) {
                val startedAt = _activeSession.value?.startedAt ?: sessionStartTime
                if (startedAt > 0L) {
                    _elapsedSeconds.value = ((System.currentTimeMillis() - startedAt) / 1_000).coerceAtLeast(0)
                }
                updateRestTimerFromSession()
                delay(1_000)
            }
        }
    }

    private fun observeLoggingSummary(dayId: Long) {
        loggingSummaryJob?.cancel()
        loggingSummaryJob = viewModelScope.launch {
            observeWorkoutLoggingSummaryUseCase(dayId).collect { summary ->
                _loggingSummary.value = summary.copy(activeFocusTarget = _activeFocusTarget.value)
            }
        }
    }

    private fun startRestTimer(restSeconds: Int) {
        if (restSeconds <= 0) return
        val endsAt = System.currentTimeMillis() + restSeconds * 1_000L
        observedRestTimerEndsAt = endsAt
        restTimerFinishHandled = false
        restTimerClearRequested = false
        viewModelScope.launch {
            updateActiveWorkoutRestTimerUseCase(endsAt, restSeconds)?.let(::applyActiveSession)
        }
    }

    private fun stopRestTimer(persist: Boolean = false) {
        _restTimerSeconds.value = 0
        _restTimerTotalSeconds.value = 0
        observedRestTimerEndsAt = null
        restTimerFinishHandled = true
        restTimerClearRequested = true
        if (persist) {
            viewModelScope.launch {
                updateActiveWorkoutRestTimerUseCase(null, 0)?.let(::applyActiveSession)
            }
        }
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        val next = (_restTimerSeconds.value + deltaSeconds).coerceAtLeast(0)
        if (next == 0) {
            stopRestTimer(persist = true)
            return
        }
        val endsAt = System.currentTimeMillis() + next * 1_000L
        observedRestTimerEndsAt = endsAt
        restTimerFinishHandled = false
        restTimerClearRequested = false
        viewModelScope.launch {
            updateActiveWorkoutRestTimerUseCase(endsAt, next)?.let(::applyActiveSession)
        }
    }

    fun skipRestTimer() {
        stopRestTimer(persist = true)
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

    fun setExerciseCollapsed(exerciseId: Long, collapsed: Boolean) {
        viewModelScope.launch {
            setActiveWorkoutCollapsedUseCase(exerciseId, collapsed)?.let(::applyActiveSession)
        }
    }

    private fun applyActiveSession(session: ActiveWorkoutSession) {
        _activeSession.value = session
        _loggedSetsThisSession.value = session.loggedSets
            .groupBy { it.exerciseId }
            .mapValues { (_, sets) -> sets.map { it.toLoggedSet() } }
        _drafts.value = session.drafts.mapValues { it.value.toUiDraft() }
        _elapsedSeconds.value = ((System.currentTimeMillis() - session.startedAt) / 1_000).coerceAtLeast(0)
        updateRestTimerFromSession()
    }

    private fun updateRestTimerFromSession() {
        val active = _activeSession.value
        val endsAt = active?.restTimerEndsAt
        val remaining = endsAt?.let { ((it - System.currentTimeMillis()) / 1_000).toInt().coerceAtLeast(0) } ?: 0
        val previousRemaining = _restTimerSeconds.value
        if (endsAt != observedRestTimerEndsAt) {
            observedRestTimerEndsAt = endsAt
            restTimerFinishHandled = remaining == 0
            restTimerClearRequested = endsAt == null
        }
        _restTimerSeconds.value = remaining
        _restTimerTotalSeconds.value = if (remaining > 0) active?.restTimerTotalSeconds ?: 0 else 0
        if (endsAt != null && previousRemaining > 0 && remaining == 0 && !restTimerFinishHandled) {
            restTimerFinishHandled = true
            restTimerClearRequested = true
            _message.value = "Rusttijd klaar - volgende set ready"
            _events.tryEmit(
                WorkoutUiEvent.RestTimerFinished(
                    id = ++eventId,
                    message = "Rusttijd klaar - volgende set ready",
                ),
            )
            viewModelScope.launch {
                updateActiveWorkoutRestTimerUseCase(null, 0)?.let(::applyActiveSession)
            }
        } else if (endsAt != null && remaining == 0 && !restTimerClearRequested) {
            restTimerClearRequested = true
            viewModelScope.launch {
                updateActiveWorkoutRestTimerUseCase(null, 0)?.let(::applyActiveSession)
            }
        }
    }
}

@Composable
fun WorkoutRoute(
    onStartWorkout: (Long) -> Unit,
    onOpenExerciseHistory: (Long) -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val pendingGeneratedRoutine by viewModel.pendingGeneratedRoutine.collectAsStateWithLifecycle()
    WorkoutScreen(
        overview = overview,
        message = message,
        pendingGeneratedRoutine = pendingGeneratedRoutine,
        onDismissMessage = viewModel::clearMessage,
        onStartWorkout = onStartWorkout,
        onOpenExerciseHistory = onOpenExerciseHistory,
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
        onReplaceExercise = viewModel::replaceExerciseInPlan,
        onUpdateWorkoutExercisePlan = viewModel::updateWorkoutExercisePlan,
        onAddSetToExercise = viewModel::addSetToExercise,
        onUpdateRoutineSet = viewModel::updateRoutineSet,
        onDeleteRoutineSet = viewModel::deleteRoutineSet,
        onMoveRoutineSet = viewModel::moveRoutineSet,
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
    onOpenExerciseHistory: (Long) -> Unit,
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
    onAddExercise: (Long, String, String, String, String, String, String, String, String) -> Unit,
    onAddExerciseToRoutine: (Long, String, String, String, String, String, String, String, String) -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onReorderExercises: (Long, List<Long>) -> Unit,
    onSetSupersetGroup: (List<Long>, Long?) -> Unit,
    onReplaceExercise: (Long, Exercise) -> Unit,
    onUpdateWorkoutExercisePlan: (Long, String, String, String, String, String, SetType) -> Unit,
    onAddSetToExercise: (Long) -> Unit,
    onUpdateRoutineSet: (RoutineSet) -> Unit,
    onDeleteRoutineSet: (Long) -> Unit,
    onMoveRoutineSet: (Long, List<Long>) -> Unit,
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
        modifier = Modifier
            .fillMaxSize()
            .clearFocusOnScrollOrDrag()
            .imeNestedScroll()
            .navigationBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.medium,
            top = MaterialTheme.spacing.medium,
            end = MaterialTheme.spacing.medium,
            bottom = TopLevelBottomContentPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader(title = "Train", subtitle = "Routines, progressie en actieve sessies") }
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
                    onOpenExerciseHistory = onOpenExerciseHistory,
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
                    onReplaceExercise = onReplaceExercise,
                    onUpdateWorkoutExercisePlan = onUpdateWorkoutExercisePlan,
                    onAddSetToExercise = onAddSetToExercise,
                    onUpdateRoutineSet = onUpdateRoutineSet,
                    onDeleteRoutineSet = onDeleteRoutineSet,
                    onMoveRoutineSet = onMoveRoutineSet,
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
                    .focusRequester(focusRequester)
                    .bringIntoViewOnFocus(),
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
    com.trainiq.core.ui.SectionHeader(title = title)
}

@Composable
private fun EmptyCard(title: String, body: String) {
    EmptyStateCard(title = title, body = body, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ExerciseLibraryCard(name: String, muscleGroup: String, equipment: String) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text("$muscleGroup • $equipment", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
    }
}

@Composable
private fun HistoryCard(sessionId: Long, totalVolume: Double, durationSeconds: Long, onDelete: (Long) -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.blue) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Session $sessionId", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("Volume ${totalVolume.toInt()} kg", color = MaterialTheme.trainIqColors.mutedText)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppChip(label = "${durationSeconds / 60} min", accent = MaterialTheme.trainIqColors.blue)
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
    onOpenExerciseHistory: (Long) -> Unit,
    onUpdateRoutine: (Long, String, String) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onSetActiveRoutine: (Long) -> Unit,
    onAddDay: (Long, String) -> Unit,
    onRemoveDay: (Long) -> Unit,
    onAddExercise: (Long, String, String, String, String, String, String, String, String) -> Unit,
    onAddExerciseToRoutine: (Long, String, String, String, String, String, String, String, String) -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onReorderExercises: (Long, List<Long>) -> Unit,
    onSetSupersetGroup: (List<Long>, Long?) -> Unit,
    onReplaceExercise: (Long, Exercise) -> Unit,
    onUpdateWorkoutExercisePlan: (Long, String, String, String, String, String, SetType) -> Unit,
    onAddSetToExercise: (Long) -> Unit,
    onUpdateRoutineSet: (RoutineSet) -> Unit,
    onDeleteRoutineSet: (Long) -> Unit,
    onMoveRoutineSet: (Long, List<Long>) -> Unit,
) {
    var isEditing by remember(routine.id) { mutableStateOf(false) }
    var editName by remember(routine.id) { mutableStateOf(routine.name) }
    var editDescription by remember(routine.id) { mutableStateOf(routine.description) }
    var dayName by remember(routine.id) { mutableStateOf("") }
    var starterTargetSets by remember(routine.id) { mutableStateOf("3") }
    var starterRepRange by remember(routine.id) { mutableStateOf("8-12") }
    var starterRestSeconds by remember(routine.id) { mutableStateOf("90") }
    var starterTargetWeight by remember(routine.id) { mutableStateOf("") }
    var starterTargetRpe by remember(routine.id) { mutableStateOf("") }
    var showStarterExercisePicker by remember(routine.id) { mutableStateOf(false) }
    var showStarterCustomExerciseDialog by remember(routine.id) { mutableStateOf(false) }
    var showDeleteRoutineConfirm by remember(routine.id) { mutableStateOf(false) }

    if (showStarterExercisePicker) {
        ExercisePickerSheet(
            exercises = exerciseLibrary,
            targetSets = starterTargetSets,
            repRange = starterRepRange,
            restSeconds = starterRestSeconds,
            targetWeightKg = starterTargetWeight,
            targetRpe = starterTargetRpe,
            onTargetSetsChange = { starterTargetSets = it },
            onRepRangeChange = { starterRepRange = it },
            onRestSecondsChange = { starterRestSeconds = it },
            onTargetWeightChange = { starterTargetWeight = it },
            onTargetRpeChange = { starterTargetRpe = it },
            onSelect = { exercise ->
                onAddExerciseToRoutine(
                    routine.id,
                    exercise.name,
                    exercise.muscleGroup,
                    exercise.equipment,
                    starterTargetSets,
                    starterRepRange,
                    starterRestSeconds,
                    starterTargetWeight,
                    starterTargetRpe,
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
            targetWeightKg = starterTargetWeight,
            targetRpe = starterTargetRpe,
            onTargetSetsChange = { starterTargetSets = it },
            onRepRangeChange = { starterRepRange = it },
            onRestSecondsChange = { starterRestSeconds = it },
            onTargetWeightChange = { starterTargetWeight = it },
            onTargetRpeChange = { starterTargetRpe = it },
            onConfirm = { name, muscleGroup, equipment ->
                onAddExerciseToRoutine(routine.id, name, muscleGroup, equipment, starterTargetSets, starterRepRange, starterRestSeconds, starterTargetWeight, starterTargetRpe)
                showStarterCustomExerciseDialog = false
            },
            onDismiss = { showStarterCustomExerciseDialog = false },
        )
    }
    if (showDeleteRoutineConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteRoutineConfirm = false },
            title = { Text("Routine verwijderen?") },
            text = { Text("Deze routine en alle sessies in de routine worden verwijderd. Workout history blijft bewaard.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteRoutineConfirm = false
                        onDeleteRoutine(routine.id)
                    },
                ) { Text("Verwijderen") }
            },
            dismissButton = { TextButton(onClick = { showDeleteRoutineConfirm = false }) { Text("Annuleren") } },
        )
    }

    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isEditing) {
                OutlinedTextField(editName, { editName = it }, label = { Text("Routine name") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                OutlinedTextField(editDescription, { editDescription = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
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
                        Text("Routine builder", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "${routineFocusLabel(routine)} focus · ${routineExerciseCount(routine)} oefeningen · ±${routineEstimatedMinutes(routine)} min",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.trainIqColors.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            routine.name + routine.description.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.trainIqColors.mutedText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (routine.days.firstOrNull() != null) {
                            PrimaryActionButton(onClick = { onStartWorkout(routine.days.first().id) }) {
                                Text("Start")
                            }
                        }
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Routine bewerken")
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryActionButton(onClick = { onSetActiveRoutine(routine.id) }) { Text(if (routine.active) "Actief" else "Actief maken") }
                TextButton(onClick = { showDeleteRoutineConfirm = true }) { Text("Verwijderen") }
            }
            HorizontalDivider()
            Text("Add session", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(dayName, { dayName = it }, label = { Text("Session name (optional)") }, modifier = Modifier.weight(1f).bringIntoViewOnFocus())
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
                        onOpenExerciseHistory = onOpenExerciseHistory,
                        onRemoveDay = onRemoveDay,
                        onAddExercise = onAddExercise,
                        onRemoveExercise = onRemoveExercise,
                        onReorderExercises = onReorderExercises,
                        onSetSupersetGroup = onSetSupersetGroup,
                        onReplaceExercise = onReplaceExercise,
                        onUpdateWorkoutExercisePlan = onUpdateWorkoutExercisePlan,
                        onAddSetToExercise = onAddSetToExercise,
                        onUpdateRoutineSet = onUpdateRoutineSet,
                        onDeleteRoutineSet = onDeleteRoutineSet,
                        onMoveRoutineSet = onMoveRoutineSet,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutDayEditor(
    day: WorkoutDay,
    exerciseLibrary: List<Exercise>,
    onStartWorkout: (Long) -> Unit,
    onOpenExerciseHistory: (Long) -> Unit,
    onRemoveDay: (Long) -> Unit,
    onAddExercise: (Long, String, String, String, String, String, String, String, String) -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onReorderExercises: (Long, List<Long>) -> Unit,
    onSetSupersetGroup: (List<Long>, Long?) -> Unit,
    onReplaceExercise: (Long, Exercise) -> Unit,
    onUpdateWorkoutExercisePlan: (Long, String, String, String, String, String, SetType) -> Unit,
    onAddSetToExercise: (Long) -> Unit,
    onUpdateRoutineSet: (RoutineSet) -> Unit,
    onDeleteRoutineSet: (Long) -> Unit,
    onMoveRoutineSet: (Long, List<Long>) -> Unit,
) {
    var targetSets by remember(day.id) { mutableStateOf("3") }
    var repRange by remember(day.id) { mutableStateOf("8-12") }
    var restSeconds by remember(day.id) { mutableStateOf("90") }
    var targetWeight by remember(day.id) { mutableStateOf("") }
    var targetRpe by remember(day.id) { mutableStateOf("") }
    var showExercisePicker by remember(day.id) { mutableStateOf(false) }
    var showCustomExerciseDialog by remember(day.id) { mutableStateOf(false) }
    var showRemoveDayConfirm by remember(day.id) { mutableStateOf(false) }
    var sessionMenuExpanded by remember(day.id) { mutableStateOf(false) }
    var pendingRemoveExercise by remember(day.id) { mutableStateOf<WorkoutExercisePlan?>(null) }
    var replacingPlan by remember(day.id) { mutableStateOf<WorkoutExercisePlan?>(null) }
    var editingPlan by remember(day.id) { mutableStateOf<WorkoutExercisePlan?>(null) }
    var orderedPlans by remember(day.id, day.exercises) { mutableStateOf(day.exercises) }

    if (showExercisePicker) {
        ExercisePickerSheet(
            exercises = exerciseLibrary,
            targetSets = targetSets,
            repRange = repRange,
            restSeconds = restSeconds,
            targetWeightKg = targetWeight,
            targetRpe = targetRpe,
            onTargetSetsChange = { targetSets = it },
            onRepRangeChange = { repRange = it },
            onRestSecondsChange = { restSeconds = it },
            onTargetWeightChange = { targetWeight = it },
            onTargetRpeChange = { targetRpe = it },
            onSelect = { exercise ->
                onAddExercise(day.id, exercise.name, exercise.muscleGroup, exercise.equipment, targetSets, repRange, restSeconds, targetWeight, targetRpe)
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
            targetWeightKg = targetWeight,
            targetRpe = targetRpe,
            onTargetSetsChange = { targetSets = it },
            onRepRangeChange = { repRange = it },
            onRestSecondsChange = { restSeconds = it },
            onTargetWeightChange = { targetWeight = it },
            onTargetRpeChange = { targetRpe = it },
            onConfirm = { name, muscleGroup, equipment ->
                onAddExercise(day.id, name, muscleGroup, equipment, targetSets, repRange, restSeconds, targetWeight, targetRpe)
                showCustomExerciseDialog = false
            },
            onDismiss = { showCustomExerciseDialog = false },
        )
    }
    replacingPlan?.let { plan ->
        ExercisePickerSheet(
            exercises = exerciseLibrary,
            title = "Oefening vervangen",
            showDefaults = false,
            targetSets = plan.targetSets.toString(),
            repRange = plan.repRange,
            restSeconds = plan.restSeconds.toString(),
            targetWeightKg = plan.targetWeightKg.takeIf { it > 0.0 }?.let(::formatWeight).orEmpty(),
            targetRpe = plan.targetRpe.takeIf { it > 0.0 }?.let(::formatWeight).orEmpty(),
            onTargetSetsChange = {},
            onRepRangeChange = {},
            onRestSecondsChange = {},
            onTargetWeightChange = {},
            onTargetRpeChange = {},
            onSelect = { exercise ->
                replacingPlan = null
                onReplaceExercise(plan.id, exercise)
            },
            onCustomExercise = {},
            onDismiss = { replacingPlan = null },
        )
    }
    if (showRemoveDayConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveDayConfirm = false },
            title = { Text("Sessie verwijderen?") },
            text = { Text("Alle oefeningen in ${day.name} worden uit deze routine verwijderd.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveDayConfirm = false
                        onRemoveDay(day.id)
                    },
                ) { Text("Verwijderen") }
            },
            dismissButton = { TextButton(onClick = { showRemoveDayConfirm = false }) { Text("Annuleren") } },
        )
    }
    pendingRemoveExercise?.let { plan ->
        AlertDialog(
            onDismissRequest = { pendingRemoveExercise = null },
            title = { Text("Oefening verwijderen?") },
            text = { Text("${plan.exercise.name} wordt uit deze sessie verwijderd. Een lopende actieve sessie wordt ook opgeschoond.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRemoveExercise = null
                        onRemoveExercise(plan.id)
                    },
                ) { Text("Verwijderen") }
            },
            dismissButton = { TextButton(onClick = { pendingRemoveExercise = null }) { Text("Annuleren") } },
        )
    }
    editingPlan?.let { plan ->
        ExercisePlanEditDialog(
            plan = plan,
            onConfirm = { sets, reps, rest, weight, rpe, setType ->
                editingPlan = null
                onUpdateWorkoutExercisePlan(plan.id, sets, reps, rest, weight, rpe, setType)
            },
            onDismiss = { editingPlan = null },
        )
    }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.trainIqColors.cyan,
        elevated = true,
        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
    ) {
        val sessionMeta = "${dayFocusLabel(day)} - ${day.exercises.size} ${if (day.exercises.size == 1) "oefening" else "oefeningen"} - ±${dayEstimatedMinutes(day)} min"
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        day.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        sessionMeta,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.trainIqColors.mutedText,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryActionButton(onClick = { onStartWorkout(day.id) }) { Text("Start") }
                    Box {
                        IconButton(onClick = { sessionMenuExpanded = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Sessie acties")
                        }
                        DropdownMenu(
                            expanded = sessionMenuExpanded,
                            onDismissRequest = { sessionMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sessie verwijderen") },
                                onClick = {
                                    sessionMenuExpanded = false
                                    showRemoveDayConfirm = true
                                },
                            )
                        }
                    }
                }
            }
            if (orderedPlans.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            }
            if (orderedPlans.isEmpty()) {
                EmptyStateCard(
                    title = "Nog geen oefeningen",
                    body = "Voeg een oefening toe om sets, rust en targets te plannen.",
                    actionLabel = "Oefening toevoegen",
                    onAction = { showExercisePicker = true },
                )
            } else {
                ReorderableColumn(
                    list = orderedPlans,
                    onSettle = { fromIndex, toIndex ->
                        orderedPlans = orderedPlans.toMutableList().apply {
                            add(toIndex, removeAt(fromIndex))
                        }
                        onReorderExercises(day.id, orderedPlans.map { it.id })
                    },
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) { _, plan, _ ->
                    key(plan.id) {
                        ReorderableItem {
                            RoutineExerciseCard(
                                plan = plan,
                                onOpenHistory = { onOpenExerciseHistory(plan.exercise.id) },
                                exerciseDragHandle = Modifier.draggableHandle(),
                                canSuperset = orderedPlans.size > 1,
                                onEditExercise = { editingPlan = plan },
                                onReplaceExercise = { replacingPlan = plan },
                                onRemoveExercise = { pendingRemoveExercise = plan },
                                onToggleSuperset = { toggleSupersetGroup(orderedPlans, plan, onSetSupersetGroup) },
                                onAddSet = { onAddSetToExercise(plan.id) },
                                onUpdateSet = onUpdateRoutineSet,
                                onDeleteSet = onDeleteRoutineSet,
                                onMoveSet = { orderedIds -> onMoveRoutineSet(plan.id, orderedIds) },
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            SecondaryActionButton(onClick = { showExercisePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text("Oefening toevoegen")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineExerciseCard(
    plan: WorkoutExercisePlan,
    onOpenHistory: () -> Unit,
    exerciseDragHandle: Modifier,
    canSuperset: Boolean,
    onEditExercise: () -> Unit,
    onReplaceExercise: () -> Unit,
    onRemoveExercise: () -> Unit,
    onToggleSuperset: () -> Unit,
    onAddSet: () -> Unit,
    onUpdateSet: (RoutineSet) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onMoveSet: (List<Long>) -> Unit,
) {
    var collapsed by remember(plan.id) { mutableStateOf(false) }
    var pendingDeleteSet by remember(plan.id) { mutableStateOf<RoutineSet?>(null) }
    var editingSet by remember(plan.id) { mutableStateOf<RoutineSet?>(null) }
    var menuExpanded by remember(plan.id) { mutableStateOf(false) }
    var orderedSets by remember(plan.id, plan.sets) {
        mutableStateOf(plan.sets.sortedWith(compareBy<RoutineSet> { it.orderIndex }.thenBy { it.id }))
    }

    pendingDeleteSet?.let { set ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSet = null },
            title = { Text("Set verwijderen?") },
            text = { Text("Alleen set ${orderedSets.indexOfFirst { it.id == set.id } + 1} wordt uit deze oefening verwijderd.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteSet = null
                        onDeleteSet(set.id)
                    },
                ) { Text("Verwijderen") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteSet = null }) { Text("Annuleren") } },
        )
    }
    editingSet?.let { set ->
        EditSetBottomSheet(
            set = set,
            setNumber = orderedSets.indexOfFirst { it.id == set.id }.takeIf { it >= 0 }?.plus(1) ?: 1,
            onSave = { updated ->
                editingSet = null
                onUpdateSet(updated)
            },
            onDismiss = { editingSet = null },
        )
    }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        accent = if (plan.supersetGroupId != null) MaterialTheme.trainIqColors.purple else MaterialTheme.colorScheme.primary,
        elevated = false,
        contentPadding = PaddingValues(MaterialTheme.spacing.compact),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = exerciseDragHandle.size(BuilderActionWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.DragHandle, contentDescription = "Oefening verplaatsen")
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenHistory),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        plan.exercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${plan.exercise.muscleGroup} - ${plan.exercise.equipment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ExerciseSummaryMetaRow(plan)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { collapsed = !collapsed }) {
                        Icon(
                            if (collapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                            contentDescription = if (collapsed) "Open oefening" else "Klap oefening in",
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Oefening acties")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Geschiedenis") },
                                onClick = {
                                    menuExpanded = false
                                    onOpenHistory()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Oefening vervangen") },
                                leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onReplaceExercise()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Bewerken") },
                                onClick = {
                                    menuExpanded = false
                                    onEditExercise()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (plan.supersetGroupId != null) "Superset ontkoppelen" else "Superset koppelen") },
                                enabled = canSuperset,
                                onClick = {
                                    menuExpanded = false
                                    onToggleSuperset()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Verwijderen") },
                                onClick = {
                                    menuExpanded = false
                                    onRemoveExercise()
                                },
                            )
                        }
                    }
                }
            }
            if (collapsed) return@Column
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            if (orderedSets.isEmpty()) {
                EmptyStateCard(
                    title = "Nog geen sets",
                    body = "Voeg een warm-up of werkset toe.",
                    actionLabel = "Set toevoegen",
                    onAction = onAddSet,
                )
            } else {
                RoutineSetHeaderRow()
                ReorderableColumn(
                    list = orderedSets,
                    onSettle = { fromIndex, toIndex ->
                        orderedSets = orderedSets.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                        onMoveSet(orderedSets.map { it.id })
                    },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { index, set, _ ->
                    key(set.id) {
                        ReorderableItem {
                            RoutineSetRow(
                                index = index + 1,
                                set = set,
                                dragHandle = Modifier.draggableHandle(),
                                onEdit = { editingSet = set },
                                onDelete = { pendingDeleteSet = set },
                            )
                        }
                    }
                }
            }
            SecondaryActionButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text("Set toevoegen")
            }
        }
    }
}

@Composable
private fun ExerciseSummaryMetaRow(plan: WorkoutExercisePlan, modifier: Modifier = Modifier) {
    Text(
        text = exerciseSummaryMeta(plan),
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.trainIqColors.mutedText,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseActionRow(
    supersetLinked: Boolean,
    canSuperset: Boolean,
    onToggleSuperset: () -> Unit,
    onAddSet: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(
            onClick = onToggleSuperset,
            enabled = canSuperset,
            modifier = Modifier.defaultMinSize(minHeight = 44.dp),
        ) {
            Text(
                if (supersetLinked) "Superset ontkoppelen" else "Superset koppelen",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            onClick = onAddSet,
            modifier = Modifier.defaultMinSize(minHeight = 44.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text("Set toevoegen", maxLines = 1)
        }
    }
}

@Composable
private fun RoutineSetHeaderRow() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small, vertical = MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(BuilderRowActionWidth))
            HeaderLabel("Sets", Modifier.weight(1f))
            HeaderLabel("Reps - Kg - Rest - RPE", textAlign = TextAlign.End)
            Spacer(modifier = Modifier.width(BuilderRowActionWidth))
        }
    }
}

@Composable
private fun HeaderLabel(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
    )
}

@Composable
private fun RoutineSetRow(
    index: Int,
    set: RoutineSet,
    dragHandle: Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val detail = routineSetDetailText(set)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f), MaterialTheme.shapes.medium)
            .clickable(onClick = onEdit)
            .padding(horizontal = MaterialTheme.spacing.small, vertical = MaterialTheme.spacing.compact),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = dragHandle.size(BuilderRowActionWidth),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.DragHandle,
                contentDescription = "Set verplaatsen",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                "#$index - ${set.setType.label()}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(modifier = Modifier.size(BuilderRowActionWidth), onClick = onDelete) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = "Set verwijderen",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun routineSetDetailText(set: RoutineSet): String = listOf(
    set.targetReps.takeIf { it > 0 }?.let { "$it reps" } ?: "Reps -",
    set.targetWeightKg.takeIf { it > 0.0 }?.let { "${formatWeight(it)} kg" } ?: "Kg -",
    set.restSeconds.takeIf { it > 0 }?.let { "${it}s rest" } ?: "Rest -",
    set.targetRpe.takeIf { it > 0.0 }?.let { "RPE ${formatWeight(it)}" } ?: "RPE -",
).joinToString(" - ")

@Composable
private fun RpeInfoButton(
    compactText: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var showInfo by remember { mutableStateOf(false) }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("RPE") },
            text = {
                Text(
                    "RPE staat voor Rate of Perceived Exertion: hoe zwaar een set voelde op een schaal van 1 tot 10. RPE 10 betekent maximaal, geen reps meer over. Dit veld is optioneel.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text("Begrepen") }
            },
        )
    }
    TextButton(
        onClick = { showInfo = true },
        modifier = modifier.defaultMinSize(minHeight = 36.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp))
        if (!compactText) {
            Text("RPE uitleg", maxLines = 1)
        } else {
            Text("RPE?", maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditSetBottomSheet(
    set: RoutineSet,
    setNumber: Int,
    onSave: (RoutineSet) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedType by remember(set.id) { mutableStateOf(set.setType) }
    var reps by remember(set.id) { mutableStateOf(set.targetReps.takeIf { it > 0 }?.toString().orEmpty()) }
    var weight by remember(set.id) { mutableStateOf(set.targetWeightKg.takeIf { it > 0.0 }?.let(::formatWeight).orEmpty()) }
    var rest by remember(set.id) { mutableStateOf(set.restSeconds.takeIf { it > 0 }?.toString().orEmpty()) }
    var rpe by remember(set.id) { mutableStateOf(set.targetRpe.takeIf { it > 0.0 }?.let(::formatWeight).orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.trainIqColors.card,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Set #$setNumber bewerken", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("Pas alleen de geplande waarden aan.", color = MaterialTheme.trainIqColors.mutedText)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Sluiten", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.mutedText)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SetType.entries.forEach { type ->
                    AppChip(
                        label = type.label(),
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SheetNumberField(
                    value = reps,
                    label = "Reps",
                    keyboardType = KeyboardType.Number,
                    step = 1.0,
                    modifier = Modifier.weight(1f),
                    onValueChange = { reps = it },
                )
                SheetNumberField(
                    value = weight,
                    label = "Gewicht",
                    suffix = "kg",
                    keyboardType = KeyboardType.Decimal,
                    step = 2.5,
                    modifier = Modifier.weight(1f),
                    onValueChange = { weight = it },
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SheetNumberField(
                    value = rest,
                    label = "Rust",
                    suffix = "s",
                    keyboardType = KeyboardType.Number,
                    step = 15.0,
                    modifier = Modifier.weight(1f),
                    onValueChange = { rest = it },
                )
                SheetNumberField(
                    value = rpe,
                    label = "RPE",
                    keyboardType = KeyboardType.Decimal,
                    step = 0.5,
                    modifier = Modifier.weight(1f),
                    onValueChange = { rpe = it },
                )
            }
            PrimaryActionButton(
                onClick = {
                    val nextSet = set.copy(
                        setType = selectedType,
                        targetReps = reps.toIntOrNull() ?: -1,
                        targetWeightKg = weight.normalizedDecimal().takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: 0.0,
                        restSeconds = rest.toIntOrNull() ?: -1,
                        targetRpe = rpe.normalizedDecimal().takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: 0.0,
                    )
                    onSave(nextSet)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Set opslaan")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SheetNumberField(
    value: String,
    label: String,
    keyboardType: KeyboardType,
    step: Double,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            suffix = suffix?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewOnFocus(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            SecondaryActionButton(
                onClick = { onValueChange(adjustNumberText(value, -step)) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "$label verlagen")
            }
            SecondaryActionButton(
                onClick = { onValueChange(adjustNumberText(value, step)) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "$label verhogen")
            }
        }
    }
}

@Composable
private fun CommittingSetNumberField(
    fieldKey: Any,
    value: String,
    label: String,
    suffix: String?,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onValueCommit: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var text by remember(fieldKey) { mutableStateOf(value) }
    var isFocused by remember(fieldKey) { mutableStateOf(false) }
    val commit = {
        val committed = if (keyboardType == KeyboardType.Decimal) text.normalizedDecimal() else text
        if (committed != text) text = committed
        onValueCommit(committed)
    }

    LaunchedEffect(value) {
        if (!isFocused && value != text) {
            text = value
        }
    }

    CompactSetNumberField(
        value = text,
        label = label,
        suffix = suffix,
        keyboardType = keyboardType,
        modifier = modifier.onFocusChanged { focusState ->
            if (isFocused && !focusState.isFocused) {
                commit()
            }
            isFocused = focusState.isFocused
        },
        isError = isError,
        imeAction = ImeAction.Done,
        keyboardActions = KeyboardActions(
            onDone = {
                commit()
                focusManager.clearFocus(force = true)
            },
        ),
        onValueChange = { text = it },
    )
}

@Composable
private fun CompactSetNumberField(
    value: String,
    label: String,
    suffix: String?,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("0") },
        suffix = suffix?.let { { Text(it, style = MaterialTheme.typography.labelSmall) } },
        isError = isError,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
        modifier = modifier
            .defaultMinSize(minHeight = 58.dp)
            .bringIntoViewOnFocus(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExercisePrescriptionChips(plan: WorkoutExercisePlan) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        SuggestionChip(onClick = {}, label = { Text("${plan.plannedSetCount()} sets") })
        SuggestionChip(onClick = {}, label = { Text("${plan.repRange} reps") })
        SuggestionChip(onClick = {}, label = { Text("${plan.restSeconds}s rest") })
        if (plan.targetWeightKg > 0.0) SuggestionChip(onClick = {}, label = { Text("${formatWeight(plan.targetWeightKg)} kg") })
        if (plan.targetRpe > 0.0) SuggestionChip(onClick = {}, label = { Text("RPE ${formatWeight(plan.targetRpe)}") })
        SuggestionChip(onClick = {}, label = { Text(plan.setType.label()) })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExercisePickerSheet(
    exercises: List<Exercise>,
    title: String = "Oefening toevoegen",
    showDefaults: Boolean = true,
    targetSets: String,
    repRange: String,
    restSeconds: String,
    targetWeightKg: String,
    targetRpe: String,
    onTargetSetsChange: (String) -> Unit,
    onRepRangeChange: (String) -> Unit,
    onRestSecondsChange: (String) -> Unit,
    onTargetWeightChange: (String) -> Unit,
    onTargetRpeChange: (String) -> Unit,
    onSelect: (Exercise) -> Unit,
    onCustomExercise: () -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var defaultsExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clearFocusOnScrollOrDrag()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Oefening zoeken") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus(),
                )
            }
            if (showDefaults) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Defaults voor deze oefening", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        "$targetSets sets - $repRange reps - ${restSeconds}s rust",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RpeInfoButton(compactText = true)
                                    IconButton(onClick = { defaultsExpanded = !defaultsExpanded }) {
                                        Icon(
                                            if (defaultsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = if (defaultsExpanded) "Defaults inklappen" else "Defaults openen",
                                        )
                                    }
                                }
                            }
                            if (defaultsExpanded) {
                                Text(
                                    "Worden toegepast wanneer je een oefening kiest.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    CompactSetNumberField(targetSets, "Sets", null, KeyboardType.Number, Modifier.weight(1f), onValueChange = onTargetSetsChange)
                                    CompactSetNumberField(repRange, "Reps", null, KeyboardType.Text, Modifier.weight(1f), onValueChange = onRepRangeChange)
                                    CompactSetNumberField(restSeconds, "Rust", "sec", KeyboardType.Number, Modifier.weight(1f), onValueChange = onRestSecondsChange)
                                    CompactSetNumberField(targetWeightKg, "Gewicht", "kg", KeyboardType.Decimal, Modifier.weight(1f), onValueChange = onTargetWeightChange)
                                    CompactSetNumberField(targetRpe, "RPE", null, KeyboardType.Decimal, Modifier.weight(1f), onValueChange = onTargetRpeChange)
                                }
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = onCustomExercise, modifier = Modifier.fillMaxWidth()) {
                        Text("Voeg eigen oefening toe")
                    }
                }
            }
            itemsIndexed(filteredExercises, key = { index, exercise -> "${exercise.id}-$index" }) { _, exercise ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                exercise.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${exercise.muscleGroup} - ${exercise.equipment}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        TextButton(onClick = { onSelect(exercise) }) { Text(if (showDefaults) "Toevoegen" else "Vervangen") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomExerciseDialog(
    targetSets: String,
    repRange: String,
    restSeconds: String,
    targetWeightKg: String,
    targetRpe: String,
    onTargetSetsChange: (String) -> Unit,
    onRepRangeChange: (String) -> Unit,
    onRestSecondsChange: (String) -> Unit,
    onTargetWeightChange: (String) -> Unit,
    onTargetRpeChange: (String) -> Unit,
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
                OutlinedTextField(exerciseName, { exerciseName = it }, label = { Text("Oefening") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                OutlinedTextField(muscleGroup, { muscleGroup = it }, label = { Text("Spiergroep") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                OutlinedTextField(equipment, { equipment = it }, label = { Text("Equipment") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Defaults", style = MaterialTheme.typography.labelLarge)
                    RpeInfoButton(compactText = true)
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CompactSetNumberField(targetSets, "Sets", null, KeyboardType.Number, Modifier.weight(1f), onValueChange = onTargetSetsChange)
                    CompactSetNumberField(repRange, "Reps", null, KeyboardType.Text, Modifier.weight(1f), onValueChange = onRepRangeChange)
                    CompactSetNumberField(restSeconds, "Rust", "sec", KeyboardType.Number, Modifier.weight(1f), onValueChange = onRestSecondsChange)
                    CompactSetNumberField(targetWeightKg, "Gewicht", "kg", KeyboardType.Decimal, Modifier.weight(1f), onValueChange = onTargetWeightChange)
                    CompactSetNumberField(targetRpe, "RPE", null, KeyboardType.Decimal, Modifier.weight(1f), onValueChange = onTargetRpeChange)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(exerciseName, muscleGroup, equipment) },
                enabled = exerciseName.isNotBlank() && muscleGroup.isNotBlank() && equipment.isNotBlank(),
            ) {
                Text("Toevoegen")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } },
    )
}

@Composable
private fun ExercisePlanEditDialog(
    plan: WorkoutExercisePlan,
    onConfirm: (String, String, String, String, String, SetType) -> Unit,
    onDismiss: () -> Unit,
) {
    var targetSets by remember(plan.id) { mutableStateOf(plan.targetSets.toString()) }
    var repRange by remember(plan.id) { mutableStateOf(plan.repRange) }
    var restSeconds by remember(plan.id) { mutableStateOf(plan.restSeconds.toString()) }
    var targetWeightKg by remember(plan.id) { mutableStateOf(plan.targetWeightKg.takeIf { it > 0.0 }?.let(::formatWeight).orEmpty()) }
    var targetRpe by remember(plan.id) { mutableStateOf(plan.targetRpe.takeIf { it > 0.0 }?.let(::formatWeight).orEmpty()) }
    var setType by remember(plan.id) { mutableStateOf(plan.setType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(plan.exercise.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(targetSets, { targetSets = it }, label = { Text("Sets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f).bringIntoViewOnFocus())
                    OutlinedTextField(repRange, { repRange = it }, label = { Text("Reps") }, modifier = Modifier.weight(1f).bringIntoViewOnFocus())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(restSeconds, { restSeconds = it }, label = { Text("Rest s") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f).bringIntoViewOnFocus())
                    OutlinedTextField(targetWeightKg, { targetWeightKg = it }, label = { Text("Kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f).bringIntoViewOnFocus())
                    OutlinedTextField(targetRpe, { targetRpe = it }, label = { Text("RPE") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f).bringIntoViewOnFocus())
                }
                SetTypeSelector(
                    selectedType = setType,
                    onSelectedTypeChange = { setType = it },
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(targetSets, repRange, restSeconds, targetWeightKg, targetRpe, setType) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryRoute(
    exerciseId: Long,
    onBack: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val historyFlow = remember(exerciseId) { viewModel.observeExerciseHistory(exerciseId) }
    val history by historyFlow.collectAsStateWithLifecycle()
    ExerciseHistoryScreen(history = history, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseHistoryScreen(
    history: ExerciseHistory?,
    onBack: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.clearFocusOnTapOutside(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        history?.exercise?.name ?: "Oefening",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("Terug") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnScrollOrDrag()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (history == null) {
                item { ShimmerCardPlaceholder() }
                return@LazyColumn
            }

            item { ExerciseStatsHeader(history) }
            item { ExerciseRankCard(history.rank) }

            if (history.sessions.isEmpty()) {
                item { EmptyExerciseHistoryState() }
                return@LazyColumn
            }

            item {
                ExerciseProgressChart(
                    title = "Volume per sessie",
                    points = history.volumePoints,
                    valueSuffix = "kg",
                )
            }
            item {
                ExerciseProgressChart(
                    title = "Beste gewicht",
                    points = history.bestWeightPoints,
                    valueSuffix = "kg",
                )
            }
            item {
                ExerciseProgressChart(
                    title = "Geschatte 1RM",
                    points = history.estimatedOneRepMaxPoints,
                    valueSuffix = "kg",
                )
            }
            item {
                Text(
                    "Geschiedenis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(history.sessions, key = { it.sessionId }) { session ->
                ExerciseSessionLogCard(session)
            }
        }
    }
}

@Composable
private fun ExerciseStatsHeader(history: ExerciseHistory) {
    val stats = history.stats
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    history.exercise?.name ?: "Onbekende oefening",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = listOfNotNull(
                    history.exercise?.muscleGroup?.takeIf { it.isNotBlank() },
                    history.exercise?.equipment?.takeIf { it.isNotBlank() },
                ).joinToString(" • ")
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    stats.lastPerformedAt?.let { "Laatste keer: ${formatHistoryDate(it)}" } ?: "Nog niet uitgevoerd",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                HistoryMetric("Sessies", stats.completedSessions.toString(), Modifier.weight(1f))
                HistoryMetric("Beste kg", formatWeight(stats.highestWeightKg), Modifier.weight(1f))
                HistoryMetric("Beste 1RM", formatWeight(stats.bestEstimatedOneRepMax), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                HistoryMetric("Meeste reps", stats.mostReps.toString(), Modifier.weight(1f))
                HistoryMetric("Volume", "${formatWeight(stats.totalVolume)} kg", Modifier.weight(1f))
                HistoryMetric("Gem. RPE", stats.averageRpe?.let(::formatWeight) ?: "-", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HistoryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = 64.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExerciseRankCard(rank: ExerciseRankProgress) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Rank", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rank.rank.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                AssistChip(
                    onClick = {},
                    label = { Text("${formatWeight(rank.score)} score") },
                )
            }
            LinearProgressIndicator(
                progress = { rank.progressToNext.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                rank.nextRank?.let { "Nog ${formatWeight(rank.pointsToNext)} punten tot ${it.label}. Meer volume, zwaardere sets of extra sessies tellen mee." }
                    ?: "Hoogste rank bereikt voor deze oefening.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExerciseProgressChart(
    title: String,
    points: List<ChartPoint>,
    valueSuffix: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    points.lastOrNull()?.let { "${formatWeight(it.value)} $valueSuffix" } ?: "-",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (points.isEmpty()) {
                Text(
                    "Nog geen data om te tonen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SimpleLineChart(points = points, modifier = Modifier.fillMaxWidth().height(132.dp))
            }
        }
    }
}

@Composable
private fun SimpleLineChart(points: List<ChartPoint>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val chartPoints = points.takeLast(12)
        val maxValue = chartPoints.maxOfOrNull { it.value }?.takeIf { it > 0.0 } ?: 1.0
        val minValue = chartPoints.minOfOrNull { it.value } ?: 0.0
        val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
        val left = 6.dp.toPx()
        val right = size.width - 6.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 12.dp.toPx()
        val chartHeight = bottom - top

        repeat(3) { index ->
            val y = top + chartHeight * (index / 2f)
            drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(left, y), end = androidx.compose.ui.geometry.Offset(right, y), strokeWidth = 1.dp.toPx())
        }

        val offsets = chartPoints.mapIndexed { index, point ->
            val x = if (chartPoints.size == 1) (left + right) / 2f else left + ((right - left) * index / (chartPoints.lastIndex).toFloat())
            val normalized = ((point.value - minValue) / range).toFloat()
            val y = bottom - normalized * chartHeight
            androidx.compose.ui.geometry.Offset(x, y)
        }
        val path = Path()
        offsets.forEachIndexed { index, offset ->
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }
        if (offsets.size > 1) {
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        offsets.forEach { offset ->
            drawCircle(color = dotColor, radius = 4.dp.toPx(), center = offset)
        }
    }
}

@Composable
private fun ExerciseSessionLogCard(session: ExerciseHistorySession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(formatHistoryDate(session.startedAt), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${formatTimer(session.durationSeconds.toInt())} • ${session.sets.count { it.completed }} sets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${formatWeight(session.totalVolume)} kg",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider()
            session.sets.forEachIndexed { index, set ->
                PerformedSetRow(index + 1, set)
            }
        }
    }
}

@Composable
private fun PerformedSetRow(index: Int, set: com.trainiq.domain.model.ExerciseHistorySet) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(index.toString(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(24.dp))
        AssistChip(onClick = {}, label = { Text(set.setType.label()) })
        Text(
            "${set.reps} reps",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${formatWeight(set.weightKg)} kg",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (set.rpe > 0.0) "RPE ${formatWeight(set.rpe)}" else "RPE -",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyExerciseHistoryState() {
    MessageCard(
        message = "Nog geen uitgevoerde sets voor deze oefening. Voltooi een training om progressie op te bouwen.",
        onDismiss = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutRoute(
    dayId: Long,
    onBack: () -> Unit,
    onOpenExerciseHistory: (Long) -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.activeWorkoutUiState.collectAsStateWithLifecycle()
    val workoutFeedbackPreferences by viewModel.workoutFeedbackPreferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val soundPlayer = remember { RestTimerSoundPlayer() }
    val currentFeedbackPreferences by rememberUpdatedState(workoutFeedbackPreferences)

    LaunchedEffect(dayId) { viewModel.loadWorkout(dayId) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WorkoutUiEvent.RestTimerFinished -> {
                    if (currentFeedbackPreferences.restTimerSoundEnabled) {
                        soundPlayer.play()
                    }
                    if (currentFeedbackPreferences.workoutHapticsEnabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    snackbarHostState.showSnackbar(event.message)
                }
                is WorkoutUiEvent.SetLogged -> {
                    if (currentFeedbackPreferences.workoutHapticsEnabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    val undoEventId = event.undoEventId
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = if (undoEventId != null) "Ongedaan maken" else null,
                    )
                    if (result == SnackbarResult.ActionPerformed && undoEventId != null) {
                        viewModel.undoWorkoutLogEvent(undoEventId)
                    }
                }
            }
        }
    }
    DisposableEffect(context) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    DisposableEffect(soundPlayer) {
        onDispose { soundPlayer.release() }
    }

    ActiveWorkoutScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        workoutHapticsEnabled = workoutFeedbackPreferences.workoutHapticsEnabled,
        onBack = onBack,
        onOpenExerciseHistory = onOpenExerciseHistory,
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
        onToggleExerciseCollapsed = viewModel::setExerciseCollapsed,
        onFinish = { viewModel.finishWorkout(dayId) },
        onDiscard = { viewModel.discardWorkout(dayId) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    uiState: ActiveWorkoutUiState,
    snackbarHostState: SnackbarHostState,
    workoutHapticsEnabled: Boolean,
    onBack: () -> Unit,
    onOpenExerciseHistory: (Long) -> Unit,
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
    onToggleExerciseCollapsed: (Long, Boolean) -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var showFinishConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val currentOnDismissMessage by rememberUpdatedState(onDismissMessage)

    Scaffold(
        modifier = Modifier.clearFocusOnTapOutside(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ScreenHeader(
                    title = "Actieve training",
                    subtitle = "${uiState.workout?.name ?: "Workout"} · ${formatTimer(uiState.elapsedSeconds.toInt())}",
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryActionButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Pauze") }
                    SecondaryActionButton(onClick = { showDiscardConfirm = true }, modifier = Modifier.weight(1f), accent = MaterialTheme.colorScheme.error) { Text("Weggooien") }
                }
                ActiveWorkoutStickyStatus(uiState)
            }
        },
        bottomBar = {
            ActiveWorkoutBottomBar(
                uiState = uiState,
                onFinishClick = {
                    if (uiState.needsFinishConfirmation) showFinishConfirm = true else onFinish()
                },
            )
        },
    ) { padding ->
        LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clearFocusOnScrollOrDrag()
                    .imeNestedScroll()
                    .padding(padding)
                    .navigationBarsPadding()
                    .imePadding(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                top = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                bottom = ActiveWorkoutBottomContentPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.message?.let { message ->
                item { MessageCard(message = message, onDismiss = currentOnDismissMessage) }
            }
            if (uiState.workout == null) {
                item { EmptyCard("Workout unavailable", "This workout could not be loaded.") }
                return@LazyColumn
            }
            if (uiState.workout.exercises.isEmpty()) {
                item { EmptyCard("Geen oefeningen", "Voeg oefeningen toe aan deze routine voordat je een training start.") }
                return@LazyColumn
            }
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
                                isAutoAdvanceTarget = uiState.activeFocusTarget?.exerciseId == plan.exercise.id,
                                hapticOnSuccess = {
                                    if (workoutHapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onOpenHistory = { onOpenExerciseHistory(plan.exercise.id) },
                                onDraftChange = onDraftChange,
                                onSetTypeChange = onSetTypeChange,
                                onEditSet = onEditSet,
                                onDeleteSet = onDeleteSet,
                                onLogSet = onLogSet,
                                onLogSameAgain = onLogSameAgain,
                                onToggleCollapsed = onToggleExerciseCollapsed,
                            )
                        }
                    }
                } else {
                    ActiveWorkoutPlanCard(
                        plan = group.first(),
                        uiState = uiState,
                        isResting = uiState.restTimerSeconds > 0,
                        isAutoAdvanceTarget = uiState.activeFocusTarget?.exerciseId == group.first().exercise.id,
                        hapticOnSuccess = {
                            if (workoutHapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onOpenHistory = { onOpenExerciseHistory(group.first().exercise.id) },
                        onDraftChange = onDraftChange,
                        onSetTypeChange = onSetTypeChange,
                        onEditSet = onEditSet,
                        onDeleteSet = onDeleteSet,
                        onLogSet = onLogSet,
                        onLogSameAgain = onLogSameAgain,
                        onToggleCollapsed = onToggleExerciseCollapsed,
                    )
                }
            }
            if (uiState.debrief != null) item { WorkoutDebriefCard(uiState.debrief, uiState) }
        }
    }
    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = { Text("Training afronden?") },
            text = {
                Text(
                    if (uiState.completedSets == 0) {
                        "Je hebt nog geen sets gelogd. Rond alleen af als je deze sessie niet wilt opslaan."
                    } else {
                        "Je hebt ${uiState.completedSets} van ${uiState.targetSets} sets gelogd. De sessie wordt opgeslagen met wat nu klaar staat."
                    },
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishConfirm = false
                        if (uiState.canFinish) {
                            onFinish()
                        } else {
                            onDiscard()
                            onBack()
                        }
                    },
                ) { Text(if (uiState.canFinish) "Opslaan" else "Weggooien") }
            },
            dismissButton = { TextButton(onClick = { showFinishConfirm = false }) { Text("Verder trainen") } },
        )
    }
    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Actieve training weggooien?") },
            text = { Text("Geloggde sets en ingevulde waarden voor deze actieve sessie worden verwijderd.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardConfirm = false
                        onDiscard()
                        onBack()
                    },
                ) { Text("Weggooien") }
            },
            dismissButton = { TextButton(onClick = { showDiscardConfirm = false }) { Text("Annuleren") } },
        )
    }
}

@Composable
private fun ActiveWorkoutStickyStatus(uiState: ActiveWorkoutUiState) {
    val progress = if (uiState.targetSets > 0) {
        (uiState.completedSets / uiState.targetSets.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    AppCard(modifier = Modifier.padding(top = 12.dp), accent = MaterialTheme.colorScheme.primary) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusMetric("Tijd", formatTimer(uiState.elapsedSeconds.toInt()), Modifier.weight(1f))
                StatusMetric("Sets", "${uiState.completedSets}/${uiState.targetSets}", Modifier.weight(1f))
                StatusMetric("Volume", "${uiState.totalVolume.toInt()} kg", Modifier.weight(1f))
                StatusMetric("Rust", if (uiState.restTimerSeconds > 0) formatTimer(uiState.restTimerSeconds) else "Klaar", Modifier.weight(1f))
            }
            if (uiState.loggingSummary.pendingCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.CloudQueue,
                        contentDescription = "${uiState.loggingSummary.pendingCount} workout events pending sync",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${uiState.loggingSummary.pendingCount} lokaal in sync-wachtrij",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            AppLinearProgress(progress = progress)
        }
    }
}

@Composable
private fun ActiveWorkoutBottomBar(uiState: ActiveWorkoutUiState, onFinishClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (uiState.restTimerSeconds > 0) "Rust ${formatTimer(uiState.restTimerSeconds)}" else "Klaar voor volgende set",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${uiState.completedSets} sets gelogd",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PrimaryActionButton(onClick = onFinishClick, enabled = uiState.workout != null, modifier = Modifier.weight(1f)) {
                Text("Training afronden")
            }
        }
    }
}

@Composable
private fun ActiveWorkoutPlanCard(
    plan: WorkoutExercisePlan,
    uiState: ActiveWorkoutUiState,
    isResting: Boolean,
    isAutoAdvanceTarget: Boolean,
    hapticOnSuccess: () -> Unit,
    onOpenHistory: () -> Unit,
    onDraftChange: (Long, SetInputDraft) -> Unit,
    onSetTypeChange: (Long, Int, SetType) -> Unit,
    onEditSet: (Long, Int) -> Unit,
    onDeleteSet: (Long, Int) -> Unit,
    onLogSet: (WorkoutExercisePlan) -> Boolean,
    onLogSameAgain: (WorkoutExercisePlan) -> Boolean,
    onToggleCollapsed: (Long, Boolean) -> Unit,
) {
    val draft = uiState.drafts[plan.exercise.id] ?: SetInputDraft()
    val loggedSets = uiState.loggedSetsThisSession[plan.exercise.id].orEmpty()
    val collapsed = plan.exercise.id in uiState.collapsedExerciseIds
    ActiveExerciseCard(
        plan = plan,
        loggedSets = loggedSets,
        suggestion = uiState.progressionSuggestions.firstOrNull { it.exerciseId == plan.exercise.id },
        draft = draft,
        isResting = isResting,
        isAutoAdvanceTarget = isAutoAdvanceTarget,
        collapsed = collapsed,
        onOpenHistory = onOpenHistory,
        onDraftChange = { next -> onDraftChange(plan.exercise.id, next) },
        onSetTypeChange = { setIndex, setType -> onSetTypeChange(plan.exercise.id, setIndex, setType) },
        onEditSet = { setIndex -> onEditSet(plan.exercise.id, setIndex) },
        onDeleteSet = { setIndex -> onDeleteSet(plan.exercise.id, setIndex) },
        onToggleCollapsed = { onToggleCollapsed(plan.exercise.id, !collapsed) },
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
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber, elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Rusttimer",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (restTimerSeconds <= 15) "Bijna klaar" else "Herstel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.trainIqColors.amber,
                    maxLines = 1,
                )
            }
            Text(
                formatTimer(restTimerSeconds),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.trainIqColors.amber,
                textAlign = TextAlign.Center,
            )
            AppLinearProgress(
                progress = if (totalSeconds > 0) (restTimerSeconds / totalSeconds.toFloat()).coerceIn(0f, 1f) else 0f,
                accent = MaterialTheme.trainIqColors.amber,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onAdjust(-30) }) { Icon(Icons.Rounded.Remove, contentDescription = "30 seconden minder") }
                IconButton(onClick = { onAdjust(30) }) { Icon(Icons.Rounded.Add, contentDescription = "30 seconden meer") }
                IconButton(onClick = onRestart) { Icon(Icons.Rounded.Replay, contentDescription = "Timer opnieuw starten") }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onSkip) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = null)
                    Text("Overslaan")
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
    isAutoAdvanceTarget: Boolean,
    collapsed: Boolean,
    onOpenHistory: () -> Unit,
    onDraftChange: (SetInputDraft) -> Unit,
    onSetTypeChange: (Int, SetType) -> Unit,
    onEditSet: (Int) -> Unit,
    onDeleteSet: (Int) -> Unit,
    onToggleCollapsed: () -> Unit,
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
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        accent = when {
            isAutoAdvanceTarget -> MaterialTheme.colorScheme.secondary
            loggedSets.size >= plan.plannedSetCount() -> MaterialTheme.trainIqColors.mint
            else -> MaterialTheme.colorScheme.primary
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenHistory),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        plan.exercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${loggedSets.size}/${plan.plannedSetCount()} sets - ${plan.repRange} reps - ${plan.restSeconds}s rust",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.trainIqColors.mutedText,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onToggleCollapsed) {
                    Icon(
                        if (collapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                        contentDescription = if (collapsed) "Open oefening" else "Klap oefening in",
                    )
                }
            }
            suggestion?.let { CompactPreviousPerformance(it) } ?: PlannedPerformanceFallback(plan)
            val visibleSetRows = maxOf(plan.plannedSetCount(), loggedSets.size + 1)
            val plannedSets = plan.sets.sortedWith(compareBy<RoutineSet> { it.orderIndex }.thenBy { it.id })
            repeat(visibleSetRows) { index ->
                SetRow(
                    index = index + 1,
                    repRange = plan.repRange,
                    plannedSet = plannedSets.getOrNull(index),
                    loggedSet = loggedSets.getOrNull(index),
                    isCurrent = index == loggedSets.size,
                    onCycleType = {
                        loggedSets.getOrNull(index)?.let { set ->
                            onSetTypeChange(index, set.setType.next())
                        }
                    },
                    onEdit = { onEditSet(index) },
                    onDelete = { onDeleteSet(index) },
                )
            }
            if (collapsed) return@Column
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
                        "Rustfase actief."
                    } else {
                        "Vul je volgende set in."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.trainIqColors.mutedText,
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
                    SecondaryActionButton(
                        onClick = onCopyLastSet,
                        enabled = loggedSets.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy vorige set")
                    }
                    TextButton(
                        onClick = onLogSameAgain,
                        enabled = loggedSets.isNotEmpty(),
                    ) {
                        Text("Zelfde opnieuw")
                    }
                    PrimaryActionButton(onClick = onLogSet, modifier = Modifier.weight(1f)) {
                        Text(if (loggedSets.size >= plan.plannedSetCount()) "Extra set" else "Set loggen")
                    }
                }
            }
        }
    }
}

@Composable
private fun SetTypeSelector(
    selectedType: SetType,
    onSelectedTypeChange: (SetType) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (compact) {
        var expanded by remember(selectedType) { mutableStateOf(false) }
        Box(modifier = modifier) {
            FilterChip(
                selected = selectedType != SetType.NORMAL,
                onClick = { expanded = !expanded },
                label = {
                    Text(
                        selectedType.label(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                SetType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label()) },
                        onClick = {
                            expanded = false
                            onSelectedTypeChange(type)
                        },
                    )
                }
            }
        }
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
            items(SetType.entries) { type ->
                FilterChip(
                    selected = type == selectedType,
                    onClick = { onSelectedTypeChange(type) },
                    label = { Text(type.label()) },
                )
            }
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
    val focusManager = LocalFocusManager.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(if (suffix.isBlank()) label else "$label ($suffix)") },
            placeholder = { Text(fallback.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(force = true) }),
            modifier = Modifier
                .defaultMinSize(minHeight = 64.dp)
                .bringIntoViewOnFocus(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = { onValueChange(adjustNumberText(value.ifBlank { fallback.orEmpty() }, -step)) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = "Decrease $label")
            }
            IconButton(
                onClick = { onValueChange(adjustNumberText(value.ifBlank { fallback.orEmpty() }, step)) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase $label")
            }
        }
    }
}

@Composable
private fun SetRow(
    index: Int,
    repRange: String,
    plannedSet: RoutineSet?,
    loggedSet: LoggedSet?,
    isCurrent: Boolean,
    onCycleType: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember(index, loggedSet) { mutableStateOf(false) }
    val background = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val rpeColor = loggedSet?.let { intensityContainerColor(it.rpe) } ?: background
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Set verwijderen?") },
            text = { Text("Deze voltooide set wordt uit je actieve training verwijderd.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                ) { Text("Verwijderen") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuleren") } },
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rpeColor, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.width(ActiveSetLeadingWidth),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                if (loggedSet != null && loggedSet.weight > 0.0 && loggedSet.reps > 0) {
                    Icon(Icons.Default.Check, contentDescription = "Completed set", modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Set $index",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                (loggedSet?.setType ?: plannedSet?.setType)?.let { type ->
                    Text(
                        type.label(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(enabled = loggedSet != null, onClick = onCycleType),
                    )
                }
            }
        }
        Text(
            loggedSet?.let {
                buildString {
                    append("${formatWeight(it.weight)} kg x ${it.reps}")
                    if (it.rpe > 0.0) append(" - RPE ${formatWeight(it.rpe)}")
                    if (it.repsInReserve != null) append(" - RIR ${it.repsInReserve}")
                }
            } ?: plannedSet?.let {
                buildString {
                    append(if (it.targetReps > 0) "${it.targetReps} reps" else repRange)
                    if (it.targetWeightKg > 0.0) append(" - ${formatWeight(it.targetWeightKg)} kg")
                    if (it.restSeconds > 0) append(" - ${it.restSeconds}s")
                    if (it.targetRpe > 0.0) append(" - RPE ${formatWeight(it.targetRpe)}")
                }
            } ?: repRange,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (loggedSet != null) {
            Box(modifier = Modifier.width(ActiveSetActionWidth), contentAlignment = Alignment.CenterEnd) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit set")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete set")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseProgressionHint(suggestion: ProgressionSuggestion) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                "Vorige sessie: ${formatWeight(previousWeight(suggestion))}kg x ${displayRepTarget(suggestion.suggestedReps)} | Suggestie: ${formatWeight(suggestion.suggestedWeightKg)}kg x ${displayRepTarget(suggestion.suggestedReps)}",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun CompactPreviousPerformance(suggestion: ProgressionSuggestion) {
    val previous = suggestion.toLastSessionDraft()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            previous?.let { "Vorige: ${it.weight} kg x ${it.reps}" } ?: "Nog geen vorige prestatie",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "Doel: ${formatWeight(suggestion.suggestedWeightKg)} kg x ${displayRepTarget(suggestion.suggestedReps)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun PlannedPerformanceFallback(plan: WorkoutExercisePlan) {
    val target = buildString {
        if (plan.targetWeightKg > 0.0) append("${formatWeight(plan.targetWeightKg)} kg")
        if (plan.targetRpe > 0.0) {
            if (isNotEmpty()) append(" • ")
            append("RPE ${formatWeight(plan.targetRpe)}")
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Nog geen vorige prestatie",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (target.isNotBlank()) {
            Text(
                "Plan: $target",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
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
    val targetSets = workout.exercises.sumOf { it.plannedSetCount() }
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
private fun StatusMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

private fun routineExerciseCount(routine: WorkoutRoutine): Int = routine.days.sumOf { it.exercises.size }

private fun routineEstimatedMinutes(routine: WorkoutRoutine): Int =
    routine.days.firstOrNull()?.let(::dayEstimatedMinutes) ?: 0

private fun routineFocusLabel(routine: WorkoutRoutine): String =
    routine.days.flatMap { it.exercises }.focusLabel()

private fun dayEstimatedMinutes(day: WorkoutDay): Int {
    val seconds = day.exercises.sumOf { plan ->
        val setCount = plan.plannedSetCount().coerceAtLeast(1)
        (setCount * 75) + (setCount * plan.restSeconds.coerceAtLeast(45))
    }
    return (seconds / 60).coerceAtLeast(if (day.exercises.isEmpty()) 0 else 10)
}

private fun dayFocusLabel(day: WorkoutDay): String = day.exercises.focusLabel()

private fun List<WorkoutExercisePlan>.focusLabel(): String {
    val groups = map { it.exercise.muscleGroup.trim() }
        .filter { it.isNotBlank() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { it.key }
    return groups.take(2).joinToString(" + ").ifBlank { "Full body" }
}

private fun String.normalizedDecimal(): String = trim().replace(',', '.')

private const val MaxTargetSets = 20
private const val MaxReps = 100
private const val MaxRestSeconds = 900
private const val MaxWeightKg = 1000.0
private const val PlanValidationMessage =
    "Gebruik geldige waarden: sets 1-20, rust 0-900s, gewicht 0-1000kg en RPE 0-10."
private const val RoutineSetValidationMessage =
    "Set niet opgeslagen. Gebruik reps 1-100, rust 0-900s, gewicht 0-1000kg en RPE 0-10."

private fun parseExercisePlanInput(
    targetSets: String,
    repRange: String,
    restSeconds: String,
    targetWeightKg: String,
    targetRpe: String,
): ExercisePlanInput? {
    val parsedSets = targetSets.trim().takeIf { it.isNotBlank() }?.toIntOrNull() ?: 3
    val parsedRest = restSeconds.trim().takeIf { it.isNotBlank() }?.toIntOrNull() ?: 90
    val parsedWeight = targetWeightKg.normalizedDecimal().takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: 0.0
    val parsedRpe = targetRpe.normalizedDecimal().takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: 0.0
    if (parsedSets !in 1..MaxTargetSets) return null
    if (parsedRest !in 0..MaxRestSeconds) return null
    if (parsedWeight !in 0.0..MaxWeightKg) return null
    if (parsedRpe !in 0.0..10.0) return null
    return ExercisePlanInput(
        targetSets = parsedSets,
        repRange = repRange.trim().ifBlank { "8-12" },
        restSeconds = parsedRest,
        targetWeightKg = parsedWeight,
        targetRpe = parsedRpe,
    )
}

private fun RoutineSet.isValidForSave(): Boolean =
    targetReps in 1..MaxReps &&
        restSeconds in 0..MaxRestSeconds &&
        targetWeightKg in 0.0..MaxWeightKg &&
        targetRpe in 0.0..10.0

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

internal fun resolveNextFocusTarget(
    plans: List<WorkoutExercisePlan>,
    loggedSetsByExerciseId: Map<Long, List<LoggedSet>>,
    justLoggedExerciseId: Long,
): ActiveWorkoutFocusTarget? {
    val currentIndex = plans.indexOfFirst { it.exercise.id == justLoggedExerciseId }
    if (currentIndex < 0) return null
    val current = plans[currentIndex]
    val currentGroupId = current.supersetGroupId
    if (currentGroupId != null) {
        val group = plans.filter { it.supersetGroupId == currentGroupId }
        val currentGroupIndex = group.indexOfFirst { it.exercise.id == justLoggedExerciseId }
        val orderedCandidates = group.drop(currentGroupIndex + 1) + group.take(currentGroupIndex + 1)
        orderedCandidates.firstOrNull { plan ->
            loggedSetsByExerciseId[plan.exercise.id].orEmpty().size < plan.plannedSetCount()
        }?.let { plan ->
            return ActiveWorkoutFocusTarget(
                exerciseId = plan.exercise.id,
                setIndex = loggedSetsByExerciseId[plan.exercise.id].orEmpty().size,
            )
        }
    }
    val currentLoggedCount = loggedSetsByExerciseId[justLoggedExerciseId].orEmpty().size
    if (currentLoggedCount < current.plannedSetCount()) {
        return ActiveWorkoutFocusTarget(justLoggedExerciseId, currentLoggedCount)
    }
    val next = plans.drop(currentIndex + 1).firstOrNull { plan ->
        loggedSetsByExerciseId[plan.exercise.id].orEmpty().size < plan.plannedSetCount()
    }
    return next?.let {
        ActiveWorkoutFocusTarget(
            exerciseId = it.exercise.id,
            setIndex = loggedSetsByExerciseId[it.exercise.id].orEmpty().size,
        )
    }
}

private fun formatWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toInt().toString() else "%.1f".format(Locale.US, weight)

private fun formatPlateWeight(weight: Float): String =
    if (weight % 1f == 0f) weight.toInt().toString() else "%.2f".format(Locale.US, weight)

private fun formatTimer(seconds: Int): String = "%d:%02d".format(Locale.US, seconds / 60, seconds % 60)

private val historyDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale("nl", "NL"))

private fun formatHistoryDate(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(historyDateFormatter)

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

private fun SetInputDraft.toDomainDraft() = ActiveWorkoutSetDraft(
    weight = weight,
    reps = reps,
    rpe = rpe,
    setType = setType,
)

private fun ActiveWorkoutSetDraft.toUiDraft() = SetInputDraft(
    weight = weight,
    reps = reps,
    rpe = rpe,
    setType = setType,
)

private fun exerciseSummaryMeta(plan: WorkoutExercisePlan): String {
    val rpe = plan.targetRpe.takeIf { it > 0.0 }?.let { "RPE ${formatWeight(it)}" } ?: "RPE -"
    val superset = plan.supersetGroupId?.let { " · Superset $it" }.orEmpty()
    return "${plan.plannedSetCount()} sets · ${plan.repRange} reps · ${plan.restSeconds}s rest · $rpe$superset"
}

private fun WorkoutExercisePlan.plannedSetCount(): Int = sets.size.takeIf { it > 0 } ?: targetSets

private fun WorkoutExercisePlan.plannedRestSeconds(setIndex: Int): Int =
    sets.sortedWith(compareBy<RoutineSet> { it.orderIndex }.thenBy { it.id })
        .getOrNull(setIndex)
        ?.restSeconds
        ?: restSeconds

private fun WorkoutExercisePlan.nextPlannedDraft(loggedCount: Int): SetInputDraft =
    sets.sortedWith(compareBy<RoutineSet> { it.orderIndex }.thenBy { it.id })
        .getOrNull(loggedCount)
        ?.toDraft()
        ?: toPlannedDraft()

private fun RoutineSet.toDraft() = SetInputDraft(
    weight = targetWeightKg.takeIf { it > 0.0 }?.let(::formatWeight).orEmpty(),
    reps = targetReps.takeIf { it > 0 }?.toString().orEmpty(),
    rpe = targetRpe.takeIf { it > 0.0 }?.let(::formatWeight).orEmpty(),
    setType = setType,
)

private fun WorkoutExercisePlan.toPlannedDraft() = SetInputDraft(
    weight = targetWeightKg.takeIf { it > 0.0 }?.let(::formatWeight).orEmpty(),
    reps = displayRepTarget(repRange).takeIf { it.isNotBlank() && it != repRange } ?: repRange.toIntOrNull()?.toString().orEmpty(),
    rpe = targetRpe.takeIf { it > 0.0 }?.let(::formatWeight).orEmpty(),
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
    SetType.NORMAL -> "Normaal"
    SetType.WARM_UP -> "Warm-up"
    SetType.DROP_SET -> "Drop set"
    SetType.FAILURE -> "Failure"
    SetType.BACK_OFF -> "Back-off"
}

private fun SetType.next(): SetType = when (this) {
    SetType.NORMAL -> SetType.WARM_UP
    SetType.WARM_UP -> SetType.DROP_SET
    SetType.DROP_SET -> SetType.FAILURE
    SetType.FAILURE -> SetType.BACK_OFF
    SetType.BACK_OFF -> SetType.NORMAL
}
