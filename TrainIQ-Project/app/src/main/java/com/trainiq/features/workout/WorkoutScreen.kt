@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.trainiq.features.workout

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AssistChip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.trainiq.core.ui.MessageCard
import com.trainiq.core.ui.ScreenHeader
import com.trainiq.core.ui.ScreenUiState
import com.trainiq.core.ui.ShimmerCardPlaceholder
import com.trainiq.core.ui.AppCard
import com.trainiq.core.ui.AppChip
import com.trainiq.core.ui.AppLinearProgress
import com.trainiq.core.ui.CompactSectionTabItem
import com.trainiq.core.ui.CompactSectionTabs
import com.trainiq.core.ui.clearFocusOnTapOutside
import com.trainiq.core.ui.clearFocusOnScrollOrDrag
import com.trainiq.core.ui.EmptyStateCard
import com.trainiq.core.ui.PrimaryActionButton
import com.trainiq.core.ui.lineChartContentDescription
import com.trainiq.core.ui.SecondaryActionButton
import com.trainiq.core.ui.TapOnlyOutlinedTextField
import com.trainiq.core.ui.WrappingActionRow
import com.trainiq.core.ui.bringIntoViewOnFocus
import com.trainiq.core.audio.RestTimerSoundPlayer
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.datastore.WorkoutFeedbackPreferences
import com.trainiq.core.diagnostics.DiagnosticsTracker
import com.trainiq.core.theme.spacing
import com.trainiq.core.theme.trainIqColors
import com.trainiq.domain.model.ActiveWorkoutFocusTarget
import com.trainiq.domain.model.ActiveWorkoutSession
import com.trainiq.domain.model.ActiveWorkoutSetEntry
import com.trainiq.domain.model.ActiveWorkoutSetDraft
import com.trainiq.domain.model.ChartPoint
import com.trainiq.domain.model.ExerciseHistory
import com.trainiq.domain.model.ExerciseHistorySession
import com.trainiq.domain.model.ExerciseLibraryItem
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
import com.trainiq.domain.model.WorkoutDebriefSource
import com.trainiq.domain.model.WorkoutCompletionExercise
import com.trainiq.domain.model.WorkoutCompletionSummary
import com.trainiq.domain.model.WorkoutCompletionUiState
import com.trainiq.domain.model.WorkoutExercisePlan
import com.trainiq.domain.model.WorkoutLoggingSummary
import com.trainiq.domain.model.WorkoutOverview
import com.trainiq.domain.model.WorkoutRoutine
import com.trainiq.domain.model.WorkoutSessionSummary
import com.trainiq.domain.usecase.AddExerciseToDayUseCase
import com.trainiq.domain.usecase.AddExerciseToRoutineUseCase
import com.trainiq.domain.usecase.AddSetToExerciseUseCase
import com.trainiq.domain.usecase.AddWorkoutDayUseCase
import com.trainiq.domain.usecase.CreateRoutineUseCase
import com.trainiq.domain.usecase.DeleteRoutineUseCase
import com.trainiq.domain.usecase.DeleteWorkoutSessionUseCase
import com.trainiq.domain.usecase.DeleteActiveWorkoutSetUseCase
import com.trainiq.domain.usecase.DiscardActiveWorkoutUseCase
import com.trainiq.domain.usecase.DiscardActiveWorkoutSessionUseCase
import com.trainiq.domain.usecase.FinishActiveWorkoutUseCase
import com.trainiq.domain.usecase.GenerateAiRoutineUseCase
import com.trainiq.domain.usecase.GetCurrentActiveWorkoutSessionUseCase
import com.trainiq.domain.usecase.GetProgressionSuggestionsUseCase
import com.trainiq.domain.usecase.GetWorkoutCompletionSummaryUseCase
import com.trainiq.domain.usecase.GetWorkoutDayUseCase
import com.trainiq.domain.usecase.LogActiveWorkoutSetUseCase
import com.trainiq.domain.usecase.DeleteRoutineSetUseCase
import com.trainiq.domain.usecase.ObserveWorkoutOverviewUseCase
import com.trainiq.domain.usecase.ObserveWorkoutLoggingSummaryUseCase
import com.trainiq.domain.usecase.ObserveExerciseHistoryUseCase
import com.trainiq.domain.usecase.RemoveExerciseFromDayUseCase
import com.trainiq.domain.usecase.RemoveWorkoutDayUseCase
import com.trainiq.domain.usecase.MoveRoutineSetUseCase
import com.trainiq.domain.usecase.ReplaceExerciseInActiveWorkoutUseCase
import com.trainiq.domain.usecase.ReplaceExerciseInPlanUseCase
import com.trainiq.domain.usecase.ReorderExercisesUseCase
import com.trainiq.domain.usecase.SaveGeneratedRoutineUseCase
import com.trainiq.domain.usecase.SetActiveRoutineUseCase
import com.trainiq.domain.usecase.SetActiveWorkoutCollapsedUseCase
import com.trainiq.domain.usecase.SetSupersetGroupUseCase
import com.trainiq.domain.usecase.StartWorkoutSessionUseCase
import com.trainiq.domain.usecase.UpdateActiveWorkoutDraftUseCase
import com.trainiq.domain.usecase.UpdateActiveWorkoutRestTimerUseCase
import com.trainiq.domain.usecase.UpdateActiveWorkoutSetUseCase
import com.trainiq.domain.usecase.UpdateActiveWorkoutSetTypeUseCase
import com.trainiq.domain.usecase.UndoWorkoutLogEventUseCase
import com.trainiq.domain.usecase.UpdateRoutineSetUseCase
import com.trainiq.domain.usecase.UpdateRoutineUseCase
import com.trainiq.domain.usecase.UpdateWorkoutExercisePlanUseCase
import com.trainiq.navigation.TrainIqWindowWidthClass
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
import kotlin.math.min
import kotlin.math.roundToInt

data class SetInputDraft(
    val weight: String = "",
    val reps: String = "",
    val restSeconds: String = "",
    val rpe: String = "",
    val setType: SetType = SetType.NORMAL,
)

data class SetInputFieldErrors(
    val weight: String? = null,
    val reps: String? = null,
    val restSeconds: String? = null,
    val rpe: String? = null,
)

data class ActiveWorkoutStartConflict(
    val requestedDayId: Long,
    val activeDayId: Long,
    val activeSessionId: Long,
    val loggedSetCount: Int,
)

data class ActiveWorkoutUiState(
    val workout: WorkoutDay? = null,
    val activeSession: ActiveWorkoutSession? = null,
    val progressionSuggestions: List<ProgressionSuggestion> = emptyList(),
    val loggedSetsThisSession: Map<Long, List<LoggedSet>> = emptyMap(),
    val pendingLoggingExerciseIds: Set<Long> = emptySet(),
    val pendingCorrectionSetIds: Map<Long, Long> = emptyMap(),
    val restTimerSeconds: Int = 0,
    val restTimerTotalSeconds: Int = 0,
    val debrief: WorkoutDebrief? = null,
    val drafts: Map<Long, SetInputDraft> = emptyMap(),
    val draftErrors: Map<Long, SetInputFieldErrors> = emptyMap(),
    val exerciseRestOverrides: Map<Long, Int> = emptyMap(),
    val collapsedExerciseIds: Set<Long> = emptySet(),
    val elapsedSeconds: Long = 0L,
    val completedSets: Int = 0,
    val targetSets: Int = 0,
    val totalVolume: Double = 0.0,
    val canFinish: Boolean = false,
    val needsFinishConfirmation: Boolean = false,
    val loggingSummary: WorkoutLoggingSummary = WorkoutLoggingSummary(),
    val activeFocusTarget: ActiveWorkoutFocusTarget? = null,
    val pendingStartConflict: ActiveWorkoutStartConflict? = null,
    val message: String? = null,
)

private data class ActiveWorkoutExerciseUiState(
    val loggedSets: List<LoggedSet>,
    val activeRestSeconds: Int,
    val draft: SetInputDraft,
    val draftErrors: SetInputFieldErrors,
    val isSessionFinished: Boolean,
    val isAutoAdvanceTarget: Boolean,
    val isLogPending: Boolean,
    val pendingCorrectionSetId: Long?,
    val collapsed: Boolean,
)

data class WorkoutUiContent(
    val overview: WorkoutOverview?,
    val workoutFeedbackPreferences: WorkoutFeedbackPreferences,
    val activeWorkout: ActiveWorkoutUiState,
    val message: String?,
    val pendingGeneratedRoutine: GeneratedRoutine?,
    val isSavingGeneratedRoutine: Boolean,
)

internal fun workoutScreenUiState(content: WorkoutUiContent): ScreenUiState<WorkoutUiContent> =
    ScreenUiState.Success(content)

private fun ScreenUiState<WorkoutUiContent>.workoutContentOrDefault(): WorkoutUiContent =
    when (this) {
        ScreenUiState.Loading,
        is ScreenUiState.Error -> WorkoutUiContent(
            overview = null,
            workoutFeedbackPreferences = WorkoutFeedbackPreferences(),
            activeWorkout = ActiveWorkoutUiState(),
            message = null,
            pendingGeneratedRoutine = null,
            isSavingGeneratedRoutine = false,
        )
        is ScreenUiState.Success -> content
    }

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

    data class WorkoutCompleted(
        override val id: Long,
        val sessionId: Long,
    ) : WorkoutUiEvent
}

private val BuilderActionWidth = 48.dp
private val BuilderRowActionWidth = 48.dp
private val RoutineSessionHorizontalPadding = 12.dp
private val RoutineExerciseHorizontalPadding = 8.dp
private const val ActiveWorkoutStartBlockedMessage = "Rond je actieve training af of verwijder die voordat je een andere training start."
private val RoutineSetHorizontalPadding = 6.dp
private val ActiveSetActionWidth = 104.dp
private val ActiveSetLeadingWidth = 76.dp
private val ActiveSetHeaderMinHeight = 56.dp
private val ActiveSetStackedActionBreakpoint = 320.dp
private val TopLevelBottomContentPadding = 132.dp
private val ActiveWorkoutBottomContentPadding = 156.dp
private val ExercisePickerHandleDismissThreshold = 96.dp
private val SetEditorHandleDismissThreshold = 96.dp
private const val SetEditorSurfaceMaxHeightFraction = 0.92f

internal fun activeWorkoutBottomContentPaddingForFeedback() = ActiveWorkoutBottomContentPadding

internal fun activeSetHeaderMinHeightForLabels() = ActiveSetHeaderMinHeight

internal enum class ActiveSetActionLayout { Wrapped, Stacked }

internal fun activeSetActionLayoutForWidth(width: Dp): ActiveSetActionLayout =
    if (width < ActiveSetStackedActionBreakpoint) ActiveSetActionLayout.Stacked else ActiveSetActionLayout.Wrapped

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
    private val startWorkoutSessionUseCase: StartWorkoutSessionUseCase,
    private val getCurrentActiveWorkoutSessionUseCase: GetCurrentActiveWorkoutSessionUseCase,
    private val updateActiveWorkoutDraftUseCase: UpdateActiveWorkoutDraftUseCase,
    private val logActiveWorkoutSetUseCase: LogActiveWorkoutSetUseCase,
    private val updateActiveWorkoutSetUseCase: UpdateActiveWorkoutSetUseCase,
    private val updateActiveWorkoutSetTypeUseCase: UpdateActiveWorkoutSetTypeUseCase,
    private val deleteActiveWorkoutSetUseCase: DeleteActiveWorkoutSetUseCase,
    private val undoWorkoutLogEventUseCase: UndoWorkoutLogEventUseCase,
    private val setActiveWorkoutCollapsedUseCase: SetActiveWorkoutCollapsedUseCase,
    private val updateActiveWorkoutRestTimerUseCase: UpdateActiveWorkoutRestTimerUseCase,
    private val finishActiveWorkoutUseCase: FinishActiveWorkoutUseCase,
    private val discardActiveWorkoutUseCase: DiscardActiveWorkoutUseCase,
    private val discardActiveWorkoutSessionUseCase: DiscardActiveWorkoutSessionUseCase,
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
    private val replaceExerciseInActiveWorkoutUseCase: ReplaceExerciseInActiveWorkoutUseCase,
    private val updateWorkoutExercisePlanUseCase: UpdateWorkoutExercisePlanUseCase,
    private val addSetToExerciseUseCase: AddSetToExerciseUseCase,
    private val updateRoutineSetUseCase: UpdateRoutineSetUseCase,
    private val deleteRoutineSetUseCase: DeleteRoutineSetUseCase,
    private val moveRoutineSetUseCase: MoveRoutineSetUseCase,
    private val diagnosticsTracker: DiagnosticsTracker,
) : ViewModel() {
    private val overview: StateFlow<WorkoutOverview?> = observeWorkoutOverviewUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val workoutFeedbackPreferences: StateFlow<WorkoutFeedbackPreferences> = preferencesRepository.workoutFeedbackPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutFeedbackPreferences())

    private val _activeWorkout = MutableStateFlow<WorkoutDay?>(null)
    private val activeWorkout: StateFlow<WorkoutDay?> = _activeWorkout.asStateFlow()
    private val _activeSession = MutableStateFlow<ActiveWorkoutSession?>(null)

    private val _progressionSuggestions = MutableStateFlow<List<ProgressionSuggestion>>(emptyList())
    private val progressionSuggestions: StateFlow<List<ProgressionSuggestion>> = _progressionSuggestions.asStateFlow()

    private val _loggedSetsThisSession = MutableStateFlow<Map<Long, List<LoggedSet>>>(emptyMap())
    private val loggedSetsThisSession: StateFlow<Map<Long, List<LoggedSet>>> = _loggedSetsThisSession.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow(0)
    private val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()
    private val _restTimerTotalSeconds = MutableStateFlow(0)
    private val restTimerTotalSeconds: StateFlow<Int> = _restTimerTotalSeconds.asStateFlow()
    private val _elapsedSeconds = MutableStateFlow(0L)

    private val _debrief = MutableStateFlow<WorkoutDebrief?>(null)
    private val debrief: StateFlow<WorkoutDebrief?> = _debrief.asStateFlow()

    private val _drafts = MutableStateFlow<Map<Long, SetInputDraft>>(emptyMap())
    private val _draftErrors = MutableStateFlow<Map<Long, SetInputFieldErrors>>(emptyMap())
    private val _loggingSummary = MutableStateFlow(WorkoutLoggingSummary())
    private val _activeFocusTarget = MutableStateFlow<ActiveWorkoutFocusTarget?>(null)
    private val _pendingLoggingExerciseIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _pendingCorrectionSetIds = MutableStateFlow<Map<Long, Long>>(emptyMap())
    private val _exerciseRestOverrides = MutableStateFlow<Map<Long, Int>>(emptyMap())

    private val _pendingStartConflict = MutableStateFlow<ActiveWorkoutStartConflict?>(null)
    private val _message = MutableStateFlow<String?>(null)
    private val message: StateFlow<String?> = _message.asStateFlow()

    private val _pendingGeneratedRoutine = MutableStateFlow<GeneratedRoutine?>(null)
    private val pendingGeneratedRoutine: StateFlow<GeneratedRoutine?> = _pendingGeneratedRoutine.asStateFlow()
    private val _isSavingGeneratedRoutine = MutableStateFlow(false)
    private val isSavingGeneratedRoutine: StateFlow<Boolean> = _isSavingGeneratedRoutine.asStateFlow()

    private val _events = MutableSharedFlow<WorkoutUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<WorkoutUiEvent> = _events.asSharedFlow()
    private val _workoutCompletions = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val workoutCompletions: SharedFlow<Long> = _workoutCompletions.asSharedFlow()

    private val activeWorkoutUiState: StateFlow<ActiveWorkoutUiState> = combine(
        _activeWorkout,
        _activeSession,
        _progressionSuggestions,
        _loggedSetsThisSession,
    ) { workout, activeSession, suggestions, loggedSets ->
        val allSets = loggedSets.values.flatten()
        val targetSets = workout?.exercises?.sumOf { it.plannedSetCount() } ?: 0
        ActiveWorkoutUiState(
            workout = workout,
            activeSession = activeSession,
            progressionSuggestions = suggestions,
            loggedSetsThisSession = loggedSets,
            collapsedExerciseIds = activeSession?.collapsedExerciseIds.orEmpty(),
            completedSets = allSets.size,
            targetSets = targetSets,
            totalVolume = allSets.sumOf { it.weight * it.reps },
            canFinish = allSets.isNotEmpty(),
            needsFinishConfirmation = allSets.isEmpty() || (targetSets > 0 && allSets.size < targetSets),
        )
    }.combine(_pendingLoggingExerciseIds) { state, pendingLoggingExerciseIds ->
        state.copy(pendingLoggingExerciseIds = pendingLoggingExerciseIds)
    }.combine(_pendingCorrectionSetIds) { state, pendingCorrectionSetIds ->
        val correctionCount = pendingCorrectionSetIds.count { (key, setId) ->
            state.loggedSetsThisSession[key].orEmpty().any { it.id == setId }
        }
        state.copy(
            pendingCorrectionSetIds = pendingCorrectionSetIds,
            completedSets = (state.completedSets - correctionCount).coerceAtLeast(0),
        )
    }.combine(_debrief) { state, debrief ->
        state.copy(debrief = debrief)
    }.combine(_elapsedSeconds) { state, elapsedSeconds ->
        state.copy(elapsedSeconds = elapsedSeconds)
    }.combine(_restTimerSeconds) { state, restTimerSeconds ->
        state.copy(restTimerSeconds = restTimerSeconds)
    }.combine(_restTimerTotalSeconds) { state, restTimerTotalSeconds ->
        state.copy(restTimerTotalSeconds = restTimerTotalSeconds)
    }.combine(_drafts) { state, drafts ->
        state.copy(drafts = drafts)
    }.combine(_draftErrors) { state, draftErrors ->
        state.copy(draftErrors = draftErrors)
    }.combine(_exerciseRestOverrides) { state, exerciseRestOverrides ->
        state.copy(exerciseRestOverrides = exerciseRestOverrides)
    }.combine(_loggingSummary) { state, loggingSummary ->
        state.copy(loggingSummary = loggingSummary)
    }.combine(_activeFocusTarget) { state, activeFocusTarget ->
        state.copy(activeFocusTarget = activeFocusTarget)
    }.combine(_pendingStartConflict) { state, pendingStartConflict ->
        state.copy(pendingStartConflict = pendingStartConflict)
    }.combine(_message) { state, message ->
        state.copy(message = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState())

    val uiState: StateFlow<ScreenUiState<WorkoutUiContent>> = combine(
        combine(
            overview,
            workoutFeedbackPreferences,
            activeWorkoutUiState,
            message,
            pendingGeneratedRoutine,
        ) { currentOverview, feedback, activeState, currentMessage, generatedRoutine ->
            WorkoutUiContent(
                overview = currentOverview,
                workoutFeedbackPreferences = feedback,
                activeWorkout = activeState,
                message = currentMessage,
                pendingGeneratedRoutine = generatedRoutine,
                isSavingGeneratedRoutine = false,
            )
        },
        isSavingGeneratedRoutine,
    ) { content, savingGeneratedRoutine ->
        workoutScreenUiState(
            content.copy(isSavingGeneratedRoutine = savingGeneratedRoutine),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScreenUiState.Loading)

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
        diagnosticsTracker.state("Workout:Start")
        viewModelScope.launch {
            _debrief.value = null
            _message.value = null
            _pendingStartConflict.value = null
            val started = runCatching { startWorkoutSessionUseCase(dayId) }
                .getOrElse {
                    val currentActive = getCurrentActiveWorkoutSessionUseCase()
                    if (currentActive != null && currentActive.dayId != dayId) {
                        _pendingStartConflict.value = ActiveWorkoutStartConflict(
                            requestedDayId = dayId,
                            activeDayId = currentActive.dayId,
                            activeSessionId = currentActive.sessionId,
                            loggedSetCount = currentActive.loggedSets.size,
                        )
                    } else {
                        _message.value = it.message ?: ActiveWorkoutStartBlockedMessage
                    }
                    return@launch
                }
            val workout = started.workout
            if (workout.exercises.isEmpty()) {
                _activeWorkout.value = null
                _activeSession.value = null
                _loggedSetsThisSession.value = emptyMap()
                _drafts.value = emptyMap()
                _draftErrors.value = emptyMap()
                _exerciseRestOverrides.value = emptyMap()
                _message.value = "Voeg eerst oefeningen toe aan deze sessie voordat je start."
                return@launch
            }
            val suggestions = started.progressionSuggestions
            _activeWorkout.value = workout
            _progressionSuggestions.value = suggestions
            val active = started.session
            applyActiveSession(active)
            observeLoggingSummary(dayId)
            sessionStartTime = active.startedAt
            if (active.loggedSets.isNotEmpty()) {
                _message.value = "Actieve training hersteld."
            }
            startSessionTicker()
        }
    }

    fun dismissStartConflict() {
        _pendingStartConflict.value = null
    }

    fun replaceConflictingActiveWorkout(conflict: ActiveWorkoutStartConflict) {
        viewModelScope.launch {
            discardActiveWorkoutSessionUseCase(conflict.activeSessionId)
            _pendingStartConflict.value = null
            loadWorkout(conflict.requestedDayId)
        }
    }

    fun updateSetDraft(exerciseId: Long, draft: SetInputDraft) {
        _drafts.value = _drafts.value.toMutableMap().apply { put(exerciseId, draft) }
        clearSetInputError(exerciseId)
        viewModelScope.launch {
            updateActiveWorkoutDraftUseCase(exerciseId, draft.toDomainDraft())
        }
    }

    fun updateLoggedSetType(setId: Long, setType: SetType) {
        viewModelScope.launch {
            updateActiveWorkoutSetTypeUseCase(setId, setType)?.let(::applyActiveSession)
        }
    }

    fun editLoggedSet(exerciseId: Long, setId: Long) {
        val sets = _loggedSetsThisSession.value[exerciseId].orEmpty()
        val set = sets.firstOrNull { it.id == setId } ?: return
        val draft = set.toDraft()
        _drafts.value = _drafts.value.toMutableMap().apply { put(exerciseId, draft) }
        _pendingCorrectionSetIds.value = _pendingCorrectionSetIds.value.toMutableMap().apply { put(exerciseId, setId) }
        clearSetInputError(exerciseId)
        viewModelScope.launch {
            updateActiveWorkoutDraftUseCase(exerciseId, draft.toDomainDraft())
        }
        _message.value = "Set staat klaar voor correctie. Pas de invoer aan en kies Wijzig loggen."
    }

    fun deleteLoggedSet(setId: Long, showMessage: Boolean = true) {
        _pendingCorrectionSetIds.value = _pendingCorrectionSetIds.value.filterValues { it != setId }
        viewModelScope.launch {
            deleteActiveWorkoutSetUseCase(setId)?.let(::applyActiveSession)
            if (showMessage) _message.value = "Set verwijderd."
        }
    }

    fun relogSet(exerciseId: Long, setId: Long) {
        val set = _loggedSetsThisSession.value[exerciseId].orEmpty().firstOrNull { it.id == setId } ?: return
        _drafts.value = _drafts.value.toMutableMap().apply { put(exerciseId, set.toDraft()) }
        _pendingCorrectionSetIds.value = _pendingCorrectionSetIds.value - exerciseId
        clearSetInputError(exerciseId)
        viewModelScope.launch {
            deleteActiveWorkoutSetUseCase(setId)?.let(::applyActiveSession)
            _message.value = "Set teruggezet. Pas aan en log opnieuw."
        }
    }

    fun adjustExerciseRest(exerciseId: Long, baseRestSeconds: Int, deltaSeconds: Int) {
        val current = _exerciseRestOverrides.value[exerciseId] ?: baseRestSeconds
        val next = activeExerciseRestSeconds(baseRestSeconds, current + deltaSeconds)
        _exerciseRestOverrides.value = _exerciseRestOverrides.value.toMutableMap().apply {
            put(exerciseId, next)
        }
        _message.value = "Rust voor deze oefening ingesteld op ${next}s."
    }

    fun logSet(plan: WorkoutExercisePlan): Boolean {
        diagnosticsTracker.tap("Workout:SetLogClicked")
        val key = plan.activeKey
        val correctionSetId = _pendingCorrectionSetIds.value[key]
        val loggedSets = _loggedSetsThisSession.value[key].orEmpty()
        val correctionSet = correctionSetId?.let { id -> loggedSets.firstOrNull { it.id == id } }
        val loggedCount = loggedSets.size
        val draftRestSeconds = activeExerciseRestSeconds(
            baseRestSeconds = plan.plannedRestSeconds(correctionSet?.orderIndex ?: loggedCount),
            overrideRestSeconds = _exerciseRestOverrides.value[key],
        )
        val draft = activeSetUiDraft(
            savedDraft = _drafts.value[key],
            plan = plan,
            loggedSetCount = loggedCount,
            activeRestSeconds = draftRestSeconds,
        )
        val validation = validateSetInput(draft)
        if (validation is SetLogValidationResult.Invalid) {
            _draftErrors.value = _draftErrors.value.toMutableMap().apply { put(key, validation.fieldErrors) }
            _message.value = validation.message
            return false
        }
        clearSetInputError(key)
        val dayId = _activeWorkout.value?.id ?: return false
        when (val start = tryStartSetLog(_pendingLoggingExerciseIds.value, key)) {
            is SetLogStartResult.AlreadyPending -> {
                _message.value = "Deze set wordt al opgeslagen."
                return false
            }
            is SetLogStartResult.Started -> {
                _pendingLoggingExerciseIds.value = start.pendingExerciseIds
            }
        }
        val validInput = validation as SetLogValidationResult.Valid
        val loggedSet = LoggedSet(
            id = correctionSet?.id ?: 0L,
            exerciseId = plan.exercise.id,
            performedExerciseId = correctionSet?.performedExerciseId ?: 0L,
            sourceWorkoutExerciseId = correctionSet?.sourceWorkoutExerciseId ?: plan.id,
            weight = validInput.weight,
            reps = validInput.reps,
            rpe = validInput.rpe,
            repsInReserve = StrengthCalculator.estimateRepsInReserve(validInput.rpe),
            setType = draft.setType,
            restSeconds = validInput.restSeconds,
            orderIndex = correctionSet?.orderIndex ?: 0,
        )
        viewModelScope.launch {
            val nextDraftIndex = if (correctionSet != null) loggedCount else loggedCount + 1
            val nextDraft = plan.nextPlannedDraft(nextDraftIndex).takeIf {
                it.weight.isNotBlank() || it.reps.isNotBlank() || it.rpe.isNotBlank()
            } ?: SetInputDraft(setType = draft.setType)
            try {
                val restSeconds = validInput.restSeconds
                val active = if (correctionSet != null) {
                    updateActiveWorkoutSetUseCase(
                        setId = correctionSet.id,
                        set = loggedSet,
                        draft = nextDraft.toDomainDraft(),
                        restSeconds = restSeconds,
                    ) ?: return@launch
                } else {
                    logActiveWorkoutSetUseCase(
                        dayId = dayId,
                        set = loggedSet,
                        draft = nextDraft.toDomainDraft(),
                        restSeconds = restSeconds,
                    )
                }
                applyActiveSession(active)
                if (correctionSet != null) {
                    _pendingCorrectionSetIds.value = _pendingCorrectionSetIds.value - key
                }
                clearSetInputError(key)
                val loggedSetsByExerciseId = active.loggedSets
                    .groupBy { it.activeKey }
                    .mapValues { (_, sets) -> sets.map { it.toLoggedSet() } }
                _activeFocusTarget.value = resolveNextFocusTarget(
                    plans = _activeWorkout.value?.exercises.orEmpty(),
                    loggedSetsByPlanKey = loggedSetsByExerciseId,
                    justLoggedPlanKey = key,
                )
                val summary = observeWorkoutLoggingSummaryUseCase(dayId).first()
                _loggingSummary.value = summary.copy(activeFocusTarget = _activeFocusTarget.value)
                val message = if (correctionSet != null) {
                    "Set ${correctionSet.orderIndex + 1} bijgewerkt voor ${plan.exercise.name}."
                } else when (loggedSet.setType) {
                    SetType.FAILURE -> "Failure set voltooid voor ${plan.exercise.name}."
                    SetType.DROP_SET -> "Drop set voltooid voor ${plan.exercise.name}."
                    else -> "Set ${active.loggedSets.count { it.activeKey == key }} gelogd voor ${plan.exercise.name}."
                }
                _events.tryEmit(
                    WorkoutUiEvent.SetLogged(
                        id = ++eventId,
                        message = message,
                        undoEventId = if (correctionSet == null) summary.lastUndoableEventId else null,
                    ),
                )
                diagnosticsTracker.state("Workout:SetLogged")
            } catch (_: Exception) {
                _message.value = "Set loggen is mislukt. Probeer opnieuw."
            } finally {
                _pendingLoggingExerciseIds.value = finishSetLog(_pendingLoggingExerciseIds.value, key)
            }
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
        val key = plan.activeKey
        val lastSet = _loggedSetsThisSession.value[key].orEmpty().lastOrNull() ?: return false
        _drafts.value = _drafts.value.toMutableMap().apply { put(key, lastSet.toDraft()) }
        clearSetInputError(key)
        return logSet(plan)
    }

    fun finishWorkout(dayId: Long) {
        diagnosticsTracker.state("Workout:Finished")
        viewModelScope.launch {
            stopRestTimer(persist = true)
            val result = finishActiveWorkoutUseCase(dayId)
            _debrief.value = result.debrief
            _activeSession.value = null
            _drafts.value = emptyMap()
            _draftErrors.value = emptyMap()
            _exerciseRestOverrides.value = emptyMap()
            _progressionSuggestions.value = getProgressionSuggestionsUseCase(dayId)
            if (result.sessionId > 0L) {
                _workoutCompletions.tryEmit(result.sessionId)
            }
        }
    }

    fun discardWorkout(dayId: Long) {
        diagnosticsTracker.state("Workout:Discarded")
        viewModelScope.launch {
            stopRestTimer(persist = false)
            discardActiveWorkoutUseCase(dayId)
            _activeSession.value = null
            _loggedSetsThisSession.value = emptyMap()
            _drafts.value = emptyMap()
            _draftErrors.value = emptyMap()
            _exerciseRestOverrides.value = emptyMap()
            _message.value = "Actieve training weggegooid."
        }
    }

    private fun clearSetInputError(exerciseId: Long) {
        if (exerciseId !in _draftErrors.value) return
        _draftErrors.value = _draftErrors.value - exerciseId
    }

    fun createRoutine(name: String, description: String) {
        if (name.isBlank()) {
            _message.value = "Routinenaam is verplicht."
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
            _message.value = "Routine aangemaakt."
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
        _message.value = "AI-routine maken..."
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
                _message.value = "Routine gegenereerd."
            }.onFailure {
                _message.value = it.toAiUserMessage("Routine genereren is mislukt.")
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
        if (_isSavingGeneratedRoutine.value) return
        val routine = _pendingGeneratedRoutine.value ?: return
        viewModelScope.launch {
            _isSavingGeneratedRoutine.value = true
            runCatching {
                saveGeneratedRoutineUseCase(routine)
            }.onSuccess {
                _pendingGeneratedRoutine.value = null
                _message.value = "Routine opgeslagen."
            }.onFailure {
                _message.value = it.toAiUserMessage("Routine opslaan is mislukt.")
            }.also {
                _isSavingGeneratedRoutine.value = false
            }
        }
    }

    fun dismissPendingGeneratedRoutine() {
        _pendingGeneratedRoutine.value = null
    }

    fun updateRoutine(routineId: Long, name: String, description: String): Boolean {
        if (name.isBlank()) {
            _message.value = "Routinenaam is verplicht."
            return false
        }
        val exists = overview.value?.routines?.any {
            it.id != routineId && it.name.equals(name.trim(), ignoreCase = true)
        } == true
        if (exists) {
            _message.value = "Een routine met deze naam bestaat al."
            return false
        }
        viewModelScope.launch {
            updateRoutineUseCase(routineId, name.trim(), description.trim())
            _message.value = "Routine bijgewerkt."
        }
        return true
    }

    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch {
            deleteRoutineUseCase(routineId)
            _message.value = "Routine verwijderd."
        }
    }

    fun setActiveRoutine(routineId: Long) {
        viewModelScope.launch {
            setActiveRoutineUseCase(routineId)
            _message.value = "Actieve routine bijgewerkt."
        }
    }

    fun addDay(routineId: Long, name: String) {
        viewModelScope.launch {
            addWorkoutDayUseCase(routineId, name.trim().ifBlank { defaultWorkoutDayName() })
            _message.value = "Sessie toegevoegd."
        }
    }

    fun removeDay(dayId: Long) {
        viewModelScope.launch {
            removeWorkoutDayUseCase(dayId)
            _message.value = "Sessie verwijderd."
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
            _message.value = "Oefeningsnaam, spiergroep en materiaal zijn verplicht."
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
            _message.value = "Oefening toegevoegd."
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
            _message.value = "Oefeningsnaam, spiergroep en materiaal zijn verplicht."
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
            _message.value = "Oefening toegevoegd."
        }
    }

    fun removeExercise(workoutExerciseId: Long) {
        viewModelScope.launch {
            removeExerciseFromDayUseCase(workoutExerciseId)
            _message.value = "Oefening verwijderd."
        }
    }

    fun reorderExercises(dayId: Long, orderedIds: List<Long>) {
        viewModelScope.launch {
            reorderExercisesUseCase(dayId, orderedIds)
            _message.value = "Oefenvolgorde bijgewerkt."
        }
    }

    fun setSupersetGroup(workoutExerciseIds: List<Long>, groupId: Long?) {
        viewModelScope.launch {
            setSupersetGroupUseCase(workoutExerciseIds, groupId)
            _message.value = if (groupId == null) "Superset losgekoppeld." else "Superset gekoppeld."
        }
    }

    fun replaceExerciseInPlan(workoutExerciseId: Long, exercise: Exercise) {
        viewModelScope.launch {
            replaceExerciseInPlanUseCase(workoutExerciseId, exercise.id)
            _message.value = "Oefening vervangen door ${exercise.name}."
        }
    }

    fun replaceExerciseInActiveWorkout(workoutExerciseId: Long, exercise: Exercise) {
        viewModelScope.launch {
            replaceExerciseInActiveWorkoutUseCase(workoutExerciseId, exercise.id)?.let(::applyActiveSession)
            _activeWorkout.value = getWorkoutDayUseCase(_activeWorkout.value?.id ?: return@launch)
            _message.value = "Oefening vervangen door ${exercise.name}."
        }
    }

    fun replaceActiveExerciseWithCustom(
        workoutExerciseId: Long,
        name: String,
        muscleGroup: String,
        equipment: String,
    ) {
        val workout = _activeWorkout.value ?: return
        val plan = workout.exercises.firstOrNull { it.id == workoutExerciseId } ?: return
        viewModelScope.launch {
            addExerciseToDayUseCase(
                dayId = workout.id,
                name = name,
                muscleGroup = muscleGroup,
                equipment = equipment,
                targetSets = plan.targetSets,
                repRange = plan.repRange,
                restSeconds = plan.restSeconds,
                targetWeightKg = plan.targetWeightKg,
                targetRpe = plan.targetRpe,
            )
            removeExerciseFromDayUseCase(workoutExerciseId)
            _activeWorkout.value = getWorkoutDayUseCase(workout.id)
            _message.value = "Oefening vervangen door $name."
        }
    }

    fun removeExerciseFromActiveWorkout(workoutExerciseId: Long) {
        val workout = _activeWorkout.value ?: return
        val currentPlan = workout.exercises.firstOrNull { it.id == workoutExerciseId } ?: return
        val key = currentPlan.activeKey
        if (_loggedSetsThisSession.value[key].orEmpty().isNotEmpty()) {
            _message.value = "Verwijder eerst gelogde sets voordat je deze oefening uit de actieve training haalt."
            return
        }
        _message.value = "Oefeningen verwijderen tijdens een actieve training is tijdelijk uitgeschakeld. Pas de routine aan voordat je start."
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
            _message.value = "Oefening bijgewerkt."
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
            _message.value = "Workoutsessie verwijderd."
        }
    }

    private fun startSessionTicker() {
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (true) {
                val startedAt = _activeSession.value?.startedAt ?: sessionStartTime
                if (startedAt > 0L) {
                    _elapsedSeconds.value = activeWorkoutElapsedSeconds(startedAt = startedAt, now = System.currentTimeMillis())
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
        diagnosticsTracker.state("Workout:TimerStarted")
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
        val previousDrafts = _drafts.value
        _activeSession.value = session
        _loggedSetsThisSession.value = session.loggedSets
            .groupBy { it.activeKey }
            .mapValues { (_, sets) -> sets.map { it.toLoggedSet() } }
        _pendingCorrectionSetIds.value = _pendingCorrectionSetIds.value.filter { (key, setId) ->
            session.loggedSets.any { it.activeKey == key && it.id == setId }
        }
        _drafts.value = session.drafts.mapValues { (key, draft) ->
            val previousRest = previousDrafts[key]?.restSeconds.orEmpty()
            draft.toUiDraft().copy(restSeconds = previousRest)
        }
        _activeWorkout.value?.exercises?.map { it.activeKey }?.toSet()?.let { activeKeys ->
            _exerciseRestOverrides.value = _exerciseRestOverrides.value.filterKeys { it in activeKeys }
        }
        _elapsedSeconds.value = activeWorkoutElapsedSeconds(startedAt = session.startedAt, now = System.currentTimeMillis())
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
            _message.value = restTimerFinishedMessage()
            _events.tryEmit(
                WorkoutUiEvent.RestTimerFinished(
                    id = ++eventId,
                    message = restTimerFinishedMessage(),
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
    onDetailModeChanged: (Boolean) -> Unit = {},
    windowWidthClass: TrainIqWindowWidthClass = TrainIqWindowWidthClass.Compact,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val screenState by viewModel.uiState.collectAsStateWithLifecycle()
    val content = screenState.workoutContentOrDefault()
    WorkoutScreen(
        overview = content.overview,
        message = content.message,
        pendingGeneratedRoutine = content.pendingGeneratedRoutine,
        isSavingGeneratedRoutine = content.isSavingGeneratedRoutine,
        onDismissMessage = viewModel::clearMessage,
        onStartWorkout = onStartWorkout,
        onOpenExerciseHistory = onOpenExerciseHistory,
        onDetailModeChanged = onDetailModeChanged,
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
    isSavingGeneratedRoutine: Boolean,
    onDismissMessage: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    onOpenExerciseHistory: (Long) -> Unit,
    onDetailModeChanged: (Boolean) -> Unit = {},
    onCreateRoutine: (String, String) -> Unit,
    onGenerateAiRoutine: (Int, String, String, String, Int, Boolean) -> Unit,
    onSaveGeneratedRoutine: () -> Unit,
    onRetryGeneratedRoutine: () -> Unit,
    onDismissGeneratedRoutine: () -> Unit,
    onUpdateRoutine: (Long, String, String) -> Boolean,
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
    var selectedRoutineId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedTrainingTab by rememberSaveable { mutableStateOf(WorkoutOverviewTab.Routines.key) }
    var exerciseLibraryQuery by rememberSaveable { mutableStateOf("") }
    var exerciseLibraryFilter by rememberSaveable { mutableStateOf(ExerciseLibraryFilter.All.key) }
    var previousSelectedRoutineId by rememberSaveable { mutableStateOf<Long?>(null) }
    val trainingListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(selectedRoutineId) {
        onDetailModeChanged(selectedRoutineId != null)
        if (selectedRoutineId != null && previousSelectedRoutineId == null) {
            trainingListState.scrollToItem(0)
        }
        previousSelectedRoutineId = selectedRoutineId
    }
    BackHandler(enabled = selectedRoutineId != null) {
        selectedRoutineId = null
    }
    LaunchedEffect(message) {
        if (message == "Routine gegenereerd." || message?.contains("mislukt", ignoreCase = true) == true) {
            isGenerating = false
            showAiDialog = false
        }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onDismissMessage()
        }
    }
    val selectedRoutine = remember(selectedRoutineId, overview?.routines) {
        selectedRoutineId?.let { id -> overview?.routines?.firstOrNull { it.id == id } }
    }
    val resolvedSelectedRoutineId = remember(selectedRoutineId, overview?.routines) {
        resolveSelectedRoutineId(selectedRoutineId, overview?.routines.orEmpty())
    }
    LaunchedEffect(selectedRoutineId, resolvedSelectedRoutineId, overview) {
        if (overview != null && selectedRoutineId != resolvedSelectedRoutineId) {
            selectedRoutineId = resolvedSelectedRoutineId
        }
    }
    pendingGeneratedRoutine?.let { routine ->
        GeneratedRoutinePreviewDialog(
            routine = routine,
            isSaving = isSavingGeneratedRoutine,
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

    Box(modifier = Modifier.fillMaxSize()) {
    TrainingWithoutOverscroll {
        LazyColumn(
            state = trainingListState,
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnScrollOrDrag()
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
        if (overview == null) {
            item { ShimmerCardPlaceholder(lineCount = 4) }
            item { ShimmerCardPlaceholder(lineCount = 3) }
            item { ShimmerCardPlaceholder(lineCount = 5) }
            return@LazyColumn
        }
        if (selectedRoutine != null) {
            item {
                RoutineDetailHeader(
                    routine = selectedRoutine,
                    onBack = { selectedRoutineId = null },
                )
            }
            item {
                RoutineCard(
                    routine = selectedRoutine,
                    exerciseLibrary = overview.exercises,
                    detailMode = true,
                    onOpenDetails = {},
                    onBackToOverview = { selectedRoutineId = null },
                    onStartWorkout = onStartWorkout,
                    onOpenExerciseHistory = onOpenExerciseHistory,
                    onUpdateRoutine = onUpdateRoutine,
                    onDeleteRoutine = {
                        onDeleteRoutine(it)
                        selectedRoutineId = null
                    },
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
            return@LazyColumn
        }
        item {
            WorkoutOverviewTabSwitcher(
                selectedTab = selectedTrainingTab,
                onSelectTab = { selectedTrainingTab = it.key },
            )
        }
        when (selectedTrainingTab) {
            WorkoutOverviewTab.Routines.key -> {
                item {
                    ActiveRoutineCard(
                        activeRoutine = overview.activeRoutine,
                        onStartWorkout = onStartWorkout,
                        onOpenDetails = { selectedRoutineId = it },
                    )
                }
                item { RoutineCreationCard(onShowCreateDialog = { showCreateDialog = true }, onShowAiDialog = { showAiDialog = true }) }
                item { SectionHeader("Routines") }
                val listedRoutines = overview.routines.filterNot { it.id == overview.activeRoutine?.id }
                val overlapProposal = overview.routines.bestRoutineOverlapProposal()
                if (overlapProposal != null) {
                    item {
                        RoutineOverlapProposalCard(
                            proposal = overlapProposal,
                            onOpenPrimary = { selectedRoutineId = overlapProposal.primary.id },
                            onOpenSecondary = { selectedRoutineId = overlapProposal.secondary.id },
                        )
                    }
                }
                if (overview.routines.isEmpty()) {
                    item { EmptyCard("Nog geen routines", "Maak een routine, voeg trainingsdagen toe en koppel oefeningen om te starten.") }
                } else if (listedRoutines.isEmpty()) {
                    item { EmptyCard("Alleen actieve routine", "Je actieve routine staat hierboven. Maak een extra routine als je wilt vergelijken of wisselen.") }
                } else {
                    items(listedRoutines, key = { workoutRoutineListKey(it.id) }) { routine ->
                        RoutineCard(
                            routine = routine,
                            exerciseLibrary = overview.exercises,
                            detailMode = false,
                            onOpenDetails = { selectedRoutineId = routine.id },
                            onBackToOverview = {},
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
            }
            WorkoutOverviewTab.Library.key -> {
                item { SectionHeader("Oefeningenbibliotheek") }
                item {
                    OutlinedTextField(
                        value = exerciseLibraryQuery,
                        onValueChange = { exerciseLibraryQuery = it },
                        label = { Text("Zoek oefening, spiergroep of materiaal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    ExerciseLibraryFilterRow(
                        selectedFilter = exerciseLibraryFilter,
                        onSelectFilter = { exerciseLibraryFilter = it.key },
                    )
                }
                val filteredExercises = overview.exerciseLibrary.filteredByExerciseLibraryQuery(
                    query = exerciseLibraryQuery,
                    filterKey = exerciseLibraryFilter,
                )
                if (filteredExercises.isEmpty()) {
                    item { EmptyCard("Geen oefeningen gevonden", "Pas je zoekterm aan of voeg een oefening toe vanuit een routine.") }
                } else {
                    items(filteredExercises, key = { workoutExerciseLibraryListKey(it.exercise.id) }) { item ->
                        ExerciseLibraryCard(item)
                    }
                }
            }
            WorkoutOverviewTab.History.key -> {
                item { SectionHeader("Geschiedenis") }
                if (overview.history.isEmpty()) {
                    item { EmptyCard("Nog geen trainingsgeschiedenis", "Voltooi een training en je sessiegeschiedenis verschijnt hier.") }
                } else {
                    items(overview.history, key = { workoutHistoryListKey(it.id) }) { session ->
                        HistoryCard(session, onDeleteWorkoutSession)
                    }
                }
            }
        }
        }
    }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(MaterialTheme.spacing.medium),
        )
    }
}

@HiltViewModel
class WorkoutCompletionViewModel @Inject constructor(
    private val getWorkoutCompletionSummaryUseCase: GetWorkoutCompletionSummaryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<WorkoutCompletionUiState>(WorkoutCompletionUiState.Loading)
    val uiState: StateFlow<WorkoutCompletionUiState> = _uiState.asStateFlow()

    fun load(sessionId: Long) {
        _uiState.value = WorkoutCompletionUiState.Loading
        viewModelScope.launch {
            val summary = getWorkoutCompletionSummaryUseCase(sessionId)
            _uiState.value = if (summary == null) {
                WorkoutCompletionUiState.Error("Deze afgeronde training kon niet worden geladen.")
            } else {
                WorkoutCompletionUiState.Success(summary)
            }
            if (summary?.debrief?.source == WorkoutDebriefSource.LOCAL_FALLBACK) {
                repeat(6) {
                    kotlinx.coroutines.delay(2_000L)
                    val refreshed = getWorkoutCompletionSummaryUseCase(sessionId) ?: return@repeat
                    _uiState.value = WorkoutCompletionUiState.Success(refreshed)
                    if (refreshed.debrief.source != WorkoutDebriefSource.LOCAL_FALLBACK) {
                        return@launch
                    }
                }
            }
        }
    }
}

sealed interface WorkoutProcessingUiState {
    data object SavingWorkout : WorkoutProcessingUiState
    data object BuildingSummary : WorkoutProcessingUiState
    data class Complete(val sessionId: Long) : WorkoutProcessingUiState
    data class Error(val message: String) : WorkoutProcessingUiState
}

@HiltViewModel
class WorkoutProcessingViewModel @Inject constructor(
    private val finishActiveWorkoutUseCase: FinishActiveWorkoutUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<WorkoutProcessingUiState>(WorkoutProcessingUiState.SavingWorkout)
    val uiState: StateFlow<WorkoutProcessingUiState> = _uiState.asStateFlow()
    private var started = false

    fun finish(dayId: Long) {
        if (started) return
        started = true
        viewModelScope.launch {
            _uiState.value = WorkoutProcessingUiState.SavingWorkout
            val result = runCatching {
                _uiState.value = WorkoutProcessingUiState.BuildingSummary
                finishActiveWorkoutUseCase(dayId)
            }
            _uiState.value = result.fold(
                onSuccess = { completion ->
                    if (completion.sessionId > 0L) {
                        WorkoutProcessingUiState.Complete(completion.sessionId)
                    } else {
                        started = false
                        WorkoutProcessingUiState.Error("Er zijn geen sets opgeslagen voor deze training.")
                    }
                },
                onFailure = {
                    started = false
                    WorkoutProcessingUiState.Error("Training afronden is mislukt. Probeer opnieuw.")
                },
            )
        }
    }
}

@Composable
private fun CreateRoutineDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    fun closeInput() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    AlertDialog(
        onDismissRequest = {
            closeInput()
            onDismiss()
        },
        title = { Text("Routine maken") },
        text = {
            TapOnlyOutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Routinenaam") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(),
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    closeInput()
                    onConfirm(name.trim())
                },
                enabled = name.isNotBlank(),
            ) {
                Text("Maken")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    closeInput()
                    onDismiss()
                },
            ) { Text("Annuleren") }
        },
    )
}

@Composable
private fun RoutineCreationCard(onShowCreateDialog: () -> Unit, onShowAiDialog: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nieuwe routine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Kies handmatig als je zelf wilt bouwen, of laat AI een voorstel maken dat eerst je bestaande oefeningenbibliotheek controleert.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onShowCreateDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Lege routine maken" },
            ) {
                Text("Handmatig starten")
            }
            Button(
                onClick = onShowAiDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Met AI genereren" },
            ) {
                Text("AI-voorstel maken")
            }
        }
    }
}

private enum class WorkoutOverviewTab(val key: String, val label: String) {
    Routines("routines", "Routines"),
    Library("library", "Bibliotheek"),
    History("history", "Geschiedenis"),
}

@Composable
private fun WorkoutOverviewTabSwitcher(
    selectedTab: String,
    onSelectTab: (WorkoutOverviewTab) -> Unit,
) {
    CompactSectionTabs(
        selectedKey = selectedTab,
        tabs = WorkoutOverviewTab.entries.map { CompactSectionTabItem(it.key, it.label) },
        onSelectTab = { selected -> WorkoutOverviewTab.entries.firstOrNull { it.key == selected.key }?.let(onSelectTab) },
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TrainingWithoutOverscroll(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        content()
    }
}

@Composable
private fun ActiveRoutineCard(
    activeRoutine: WorkoutRoutine?,
    onStartWorkout: (Long) -> Unit,
    onOpenDetails: (Long) -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.mint) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Actieve routine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (activeRoutine == null) {
                Text("Nog geen actieve routine. Maak er hieronder een en markeer die als actief.")
            } else {
                Text(
                    activeRoutine.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    activeRoutine.description.ifBlank { routineEmptyDescriptionText() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.trainIqColors.mutedText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                val startableDay = activeRoutine.firstStartableDay()
                if (startableDay == null) {
                    Text(activeRoutineNeedsExerciseText())
                    PrimaryActionButton(
                        onClick = { onOpenDetails(activeRoutine.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text(activeRoutineSetupLabel())
                    }
                } else {
                    ActiveRoutineActionRow(
                        activeRoutineId = activeRoutine.id,
                        startableDay = startableDay,
                        onStartWorkout = onStartWorkout,
                        onOpenDetails = onOpenDetails,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveRoutineActionRow(
    activeRoutineId: Long,
    startableDay: WorkoutDay,
    onStartWorkout: (Long) -> Unit,
    onOpenDetails: (Long) -> Unit,
) {
    val startLabel = activeRoutineStartLabel(startableDay.name)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val actionModifier = Modifier.weight(1f).fillMaxWidth().heightIn(min = 48.dp)
        PrimaryActionButton(
            onClick = { onStartWorkout(startableDay.id) },
            modifier = actionModifier,
        ) {
            Text(
                startLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SecondaryActionButton(
            onClick = { onOpenDetails(activeRoutineId) },
            modifier = actionModifier,
        ) {
            Icon(Icons.Rounded.Edit, contentDescription = null)
            Text("Routine aanpassen", maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
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

private enum class ExerciseLibraryFilter(val key: String, val label: String) {
    All("all", "Alles"),
    Scored("scored", "Met score"),
    Recent("recent", "Recent"),
    Untrained("untrained", "Nog niet getraind"),
}

@Composable
private fun ExerciseLibraryFilterRow(
    selectedFilter: String,
    onSelectFilter: (ExerciseLibraryFilter) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val chipColors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        ExerciseLibraryFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter.key,
                onClick = { onSelectFilter(filter) },
                label = { Text(filter.label) },
                colors = chipColors,
            )
        }
    }
}

@Composable
private fun ExerciseLibraryCard(item: ExerciseLibraryItem) {
    val exercise = item.exercise
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(exercise.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(
            exerciseHistorySubtitleText(exercise.muscleGroup, exercise.equipment),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.trainIqColors.mutedText,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            AppChip(
                label = if (item.completedSessions > 0) "${item.completedSessions} sessies" else "Nog niet getraind",
                accent = MaterialTheme.trainIqColors.mint,
            )
            AppChip(
                label = if (item.score > 0.0) "${item.rankLabel} - ${formatWeight(item.score)} score" else "Geen score",
                accent = MaterialTheme.trainIqColors.amber,
            )
            if (item.bestEstimatedOneRepMax > 0.0) {
                AppChip(label = "1RM ${formatWeight(item.bestEstimatedOneRepMax)} kg", accent = MaterialTheme.trainIqColors.blue)
            }
        }
        item.lastPerformedAt?.let {
            Text(
                "Laatste keer: ${formatHistoryDate(it)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryCard(session: WorkoutSessionSummary, onDelete: (Long) -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.blue) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(session.workoutName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(formatHistoryDate(session.date), color = MaterialTheme.trainIqColors.mutedText)
                }
                TextButton(onClick = { onDelete(session.id) }) { Text("Verwijderen") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HistoryMetricTile("Duur", "${session.duration / 60} min", MaterialTheme.trainIqColors.blue)
                HistoryMetricTile("Oefeningen", session.exerciseCount.toString(), MaterialTheme.trainIqColors.mint)
                HistoryMetricTile("Sets", session.setsLogged.toString(), MaterialTheme.trainIqColors.mint)
                HistoryMetricTile("Volume", "${session.totalVolume.toInt()} kg", MaterialTheme.trainIqColors.blue)
                if (session.strongestSetLabel.isNotBlank()) {
                    HistoryMetricTile("Topset", session.strongestSetLabel, MaterialTheme.trainIqColors.amber)
                }
                if (session.debriefRecoveryScore > 0) {
                    HistoryMetricTile("Herstel", "${session.debriefRecoveryScore}/100", intensityContentColor(session.debriefIntensitySignal))
                }
            }
            session.debriefSummary.takeIf { it.isNotBlank() }?.let {
                HistoryDebriefBlock(title = "Samenvatting", body = it)
            }
            session.debriefRecommendation.takeIf { it.isNotBlank() }?.let {
                HistoryDebriefBlock(title = "Advies", body = it)
            }
            session.debriefNextSessionFocus.takeIf { it.isNotBlank() }?.let {
                HistoryDebriefBlock(title = "Volgende focus", body = it)
            }
        }
    }
}

@Composable
private fun HistoryMetricTile(label: String, value: String, accent: Color) {
    Surface(
        modifier = Modifier
            .defaultMinSize(minWidth = 132.dp)
            .heightIn(min = 72.dp),
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HistoryDebriefBlock(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val focusSuggestions = remember { listOf("Push/pull/legs", "Upper/lower", "Volledig lichaam", "Onderlichaam", "Kracht") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI-routine genereren") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .clearFocusOnScrollOrDrag()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    focusSuggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { focus = suggestion },
                            label = { Text(suggestion) },
                        )
                    }
                }
                TapOnlyOutlinedTextField(focus, { focus = it }, label = { Text("Trainingsfocus / split") }, modifier = Modifier.fillMaxWidth())
                TapOnlyOutlinedTextField(
                    value = daysPerWeek,
                    onValueChange = { daysPerWeek = it },
                    label = { Text("Dagen per week") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                TapOnlyOutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    accessibilityLabel = "Beschikbaar materiaal",
                    label = { Text("Beschikbaar materiaal") },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                    Text("Genereren")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Annuleren") } },
    )
}

@Composable
private fun ExperienceLevelSelector(experienceLevel: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Ervaringsniveau", style = MaterialTheme.typography.labelMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("beginner", "intermediate", "advanced").forEach { option ->
                FilterChip(
                    onClick = { onSelected(option) },
                    selected = option == experienceLevel,
                    label = { Text(option.toDutchExperienceLabel()) },
                )
            }
        }
    }
}

@Composable
private fun SessionDurationSlider(durationMinutes: Int, onValueChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Sessieduur: $durationMinutes min", style = MaterialTheme.typography.labelMedium)
        Slider(value = durationMinutes.toFloat(), onValueChange = onValueChange, valueRange = 30f..90f, steps = 3)
    }
}

@Composable
private fun IncludeDeloadRow(enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Deload-richtlijn opnemen", style = MaterialTheme.typography.labelMedium)
            Text("Voegt hersteladvies toe voor lichtere weken.", style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics {
                contentDescription = "Deload-richtlijn opnemen"
            },
        )
    }
}

@Composable
private fun RoutineDetailHeader(routine: WorkoutRoutine, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) {
            Icon(Icons.Rounded.Close, contentDescription = null)
            Text("Terug naar routines")
        }
        ScreenHeader(
            title = routine.name,
            subtitle = "Routine details en bewerken",
        )
    }
}

@Composable
private fun RoutineCard(
    routine: WorkoutRoutine,
    exerciseLibrary: List<Exercise>,
    detailMode: Boolean,
    onOpenDetails: () -> Unit,
    onBackToOverview: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    onOpenExerciseHistory: (Long) -> Unit,
    onUpdateRoutine: (Long, String, String) -> Boolean,
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
    var isEditing by rememberSaveable(routine.id) { mutableStateOf(false) }
    var editName by rememberSaveable(routine.id, routine.name) { mutableStateOf(routine.name) }
    var editDescription by rememberSaveable(routine.id, routine.description) { mutableStateOf(routine.description) }
    var dayName by rememberSaveable(routine.id) { mutableStateOf("") }
    var starterTargetSets by rememberSaveable(routine.id) { mutableStateOf("3") }
    var starterRepRange by rememberSaveable(routine.id) { mutableStateOf("8-12") }
    var starterRestSeconds by rememberSaveable(routine.id) { mutableStateOf("90") }
    var starterTargetWeight by rememberSaveable(routine.id) { mutableStateOf("") }
    var starterTargetRpe by rememberSaveable(routine.id) { mutableStateOf("") }
    var showStarterExercisePicker by remember(routine.id) { mutableStateOf(false) }
    var showStarterCustomExerciseDialog by remember(routine.id) { mutableStateOf(false) }
    var showDeleteRoutineConfirm by remember(routine.id) { mutableStateOf(false) }
    var detailTab by rememberSaveable(routine.id) { mutableStateOf(initialRoutineDetailTab(routine)) }
    var editError by rememberSaveable(routine.id) { mutableStateOf<String?>(null) }

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
            text = { Text("Deze routine en alle sessies in de routine worden verwijderd. Trainingsgeschiedenis blijft bewaard.") },
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

    if (!detailMode) {
        AppCard(modifier = Modifier.fillMaxWidth(), accent = if (routine.active) MaterialTheme.colorScheme.primary else MaterialTheme.trainIqColors.cyan) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            routine.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            routine.description.ifBlank { routineEmptyDescriptionText() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.trainIqColors.mutedText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (routine.active) {
                        AppChip(label = "Actief", accent = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    routineMetadataText(
                        focus = routineFocusLabel(routine),
                        exerciseCount = routineExerciseCount(routine),
                        setCount = routineSetCount(routine),
                        estimatedMinutes = routineEstimatedMinutes(routine),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.trainIqColors.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                RoutineOverviewActionStrip(
                    routine = routine,
                    onOpenDetails = onOpenDetails,
                    onSetActiveRoutine = onSetActiveRoutine,
                    onStartWorkout = onStartWorkout,
                )
            }
        }
        return
    }

    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isEditing) {
                TapOnlyOutlinedTextField(editName, { editName = it }, label = { Text("Routinenaam") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                TapOnlyOutlinedTextField(editDescription, { editDescription = it }, label = { Text("Beschrijving") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                editError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val nextName = editName.trim()
                            if (nextName.isBlank()) {
                                editError = "Routinenaam is verplicht."
                                return@Button
                            }
                            if (onUpdateRoutine(routine.id, nextName, editDescription.trim())) {
                                editError = null
                                isEditing = false
                                onBackToOverview()
                            }
                        },
                    ) {
                        Text("Opslaan")
                    }
                    TextButton(
                        onClick = {
                            editName = routine.name
                            editDescription = routine.description
                            editError = null
                            isEditing = false
                        },
                    ) {
                        Text("Annuleren")
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            routine.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            routineDetailMetadataText(
                                focus = routineFocusLabel(routine),
                                exerciseCount = routineExerciseCount(routine),
                                estimatedMinutes = routineEstimatedMinutes(routine),
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.trainIqColors.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            routine.description.ifBlank { routineEmptyDescriptionText() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.trainIqColors.mutedText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                WrappingActionRow(labels = listOf("Start", "Bewerken")) { actionModifier ->
                    routine.firstStartableDay()?.let { startableDay ->
                        PrimaryActionButton(onClick = { onStartWorkout(startableDay.id) }, modifier = actionModifier) {
                            Text("Start")
                        }
                    }
                    SecondaryActionButton(
                        onClick = {
                            detailTab = "info"
                            isEditing = true
                        },
                        modifier = actionModifier,
                    ) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "Naam en beschrijving bewerken",
                        )
                        Text("Bewerken")
                    }
                }
            }
            WrappingActionRow(labels = listOf(if (routine.active) "Actief" else "Actief maken", "Verwijderen")) { actionModifier ->
                SecondaryActionButton(onClick = { onSetActiveRoutine(routine.id) }, modifier = actionModifier) {
                    Text(if (routine.active) "Actief" else "Actief maken", maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
                TextButton(onClick = { showDeleteRoutineConfirm = true }, modifier = actionModifier) {
                    Text("Verwijderen", maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
            HorizontalDivider()
            RoutineDetailTabSwitcher(
                selectedTab = detailTab,
                onInfoClick = { detailTab = "info" },
                onSessionsClick = {
                    detailTab = "sessions"
                    isEditing = false
                    editError = null
                },
            )
            if (detailTab == "info" && !isEditing) {
                Text("Routine-info", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(routine.description.ifBlank { routineEmptyDescriptionText() }, style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick = {
                        detailTab = "info"
                        isEditing = true
                    },
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Text("Naam en beschrijving bewerken")
                }
            }
            if (detailTab == "sessions") {
            Text("Sessie toevoegen", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TapOnlyOutlinedTextField(
                    value = dayName,
                    onValueChange = { dayName = it },
                    accessibilityLabel = "Sessienaam optioneel",
                    label = { Text("Sessienaam (optioneel)") },
                    modifier = Modifier.weight(1f).bringIntoViewOnFocus(),
                )
                Button(onClick = { onAddDay(routine.id, dayName); dayName = "" }) { Text("Toevoegen") }
            }
            if (routine.days.isEmpty()) {
                Text(
                    "Nog geen sessies. Voeg een oefening toe en TrainIQ maakt automatisch Sessie 1.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { showStarterExercisePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("Eerste oefening toevoegen")
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
}

@Composable
private fun RoutineOverviewActionStrip(
    routine: WorkoutRoutine,
    onOpenDetails: () -> Unit,
    onSetActiveRoutine: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val actionModifier = Modifier.weight(1f).fillMaxWidth().heightIn(min = 48.dp)
        SecondaryActionButton(onClick = onOpenDetails, modifier = actionModifier) {
            Icon(Icons.Rounded.Edit, contentDescription = null)
            Text("Details", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        SecondaryActionButton(
            onClick = { onSetActiveRoutine(routine.id) },
            modifier = actionModifier,
        ) {
            Text(
                if (routine.active) "Actief" else "Actief maken",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        val startableDay = routine.firstStartableDay()
        if (startableDay != null) {
            PrimaryActionButton(onClick = { onStartWorkout(startableDay.id) }, modifier = actionModifier) {
                Text("Start", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Spacer(modifier = actionModifier)
        }
    }
}

@Composable
private fun RoutineDetailTabSwitcher(
    selectedTab: String,
    onInfoClick: () -> Unit,
    onSessionsClick: () -> Unit,
) {
    CompactSectionTabs(
        selectedKey = selectedTab,
        tabs = listOf(
            CompactSectionTabItem("info", "Info"),
            CompactSectionTabItem("sessions", "Sessies"),
        ),
        onSelectTab = { selected ->
            when (selected.key) {
                "info" -> onInfoClick()
                "sessions" -> onSessionsClick()
            }
        },
    )
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
    var targetSets by rememberSaveable(day.id) { mutableStateOf("3") }
    var repRange by rememberSaveable(day.id) { mutableStateOf("8-12") }
    var restSeconds by rememberSaveable(day.id) { mutableStateOf("90") }
    var targetWeight by rememberSaveable(day.id) { mutableStateOf("") }
    var targetRpe by rememberSaveable(day.id) { mutableStateOf("") }
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
        contentPadding = PaddingValues(horizontal = RoutineSessionHorizontalPadding, vertical = MaterialTheme.spacing.medium),
    ) {
        val sessionMeta = routineSessionMetadataText(
            focus = dayFocusLabel(day),
            exerciseCount = day.exercises.size,
            estimatedMinutes = dayEstimatedMinutes(day),
        )
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
                    PrimaryActionButton(
                        onClick = { onStartWorkout(day.id) },
                        enabled = day.exercises.isNotEmpty(),
                    ) { Text("Start") }
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
        contentPadding = PaddingValues(horizontal = RoutineExerciseHorizontalPadding, vertical = MaterialTheme.spacing.compact),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = exerciseDragHandle.size(BuilderActionWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.DragHandle,
                        contentDescription = "Oefening verplaatsen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            onClickLabel = "Geschiedenis openen",
                            role = Role.Button,
                            onClick = onOpenHistory,
                        )
                        .semantics {
                            contentDescription = "Geschiedenis van ${plan.exercise.name} openen"
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        plan.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        exerciseHistorySubtitleText(plan.exercise.muscleGroup, plan.exercise.equipment),
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
                                text = { Text("Set toevoegen") },
                                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onAddSet()
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
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
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
                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
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
            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        ) {
            Text(
                if (supersetLinked) "Superset ontkoppelen" else "Superset koppelen",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(
            onClick = onAddSet,
            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
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
            modifier = Modifier.padding(horizontal = RoutineSetHorizontalPadding, vertical = MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(BuilderRowActionWidth))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                HeaderLabel("Setconfiguratie")
                Text(
                    "${RepetitionsMetricLabel} - Kg - Rust - RPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.trainIqColors.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
    val metricCells = routineSetMetricCells(set)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f), MaterialTheme.shapes.medium)
            .clickable(onClick = onEdit)
            .padding(horizontal = RoutineSetHorizontalPadding, vertical = MaterialTheme.spacing.compact),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "#$index",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                )
                Text(
                    set.setType.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
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
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            when (routineSetMetricLayoutForWidth(maxWidth)) {
                RoutineSetMetricLayout.OneRow -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        metricCells.forEach { cell ->
                            RoutineSetMetricValue(cell = cell, modifier = Modifier.weight(1f))
                        }
                    }
                }
                RoutineSetMetricLayout.BalancedGrid -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        metricCells.chunked(2).forEach { rowCells ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                rowCells.forEach { cell ->
                                    RoutineSetMetricValue(
                                        cell = cell,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineSetMetricValue(cell: RoutineSetMetricCell, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .defaultMinSize(minWidth = 52.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f), MaterialTheme.shapes.small)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            cell.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            cell.value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

internal data class RoutineSetMetricCell(
    val label: String,
    val value: String,
)

internal const val RepetitionsMetricLabel = "Herh."

internal enum class RoutineSetMetricLayout {
    OneRow,
    BalancedGrid,
}

internal fun routineSetMetricLayoutForWidth(availableWidth: Dp): RoutineSetMetricLayout =
    if (availableWidth >= 240.dp) RoutineSetMetricLayout.OneRow else RoutineSetMetricLayout.BalancedGrid

internal fun routineSetMetricCells(set: RoutineSet): List<RoutineSetMetricCell> = listOf(
    RoutineSetMetricCell(RepetitionsMetricLabel, set.targetReps.takeIf { it > 0 }?.toString() ?: "-"),
    RoutineSetMetricCell("Kg", set.targetWeightKg.takeIf { it > 0.0 }?.let { "${formatWeight(it)} kg" } ?: "-"),
    RoutineSetMetricCell("Rust", set.restSeconds.takeIf { it > 0 }?.let { "${it}s" } ?: "-"),
    RoutineSetMetricCell("RPE", set.targetRpe.takeIf { it > 0.0 }?.let(::formatWeight) ?: "-"),
)

internal fun activeSetMetricCells(
    repRange: String,
    plannedSet: RoutineSet?,
    loggedSet: LoggedSet?,
    activeRestSeconds: Int,
): List<RoutineSetMetricCell> {
    val reps = loggedSet?.reps?.takeIf { it > 0 }?.toString()
        ?: plannedSet?.targetReps?.takeIf { it > 0 }?.toString()
        ?: repRange.ifBlank { "-" }
    val weight = loggedSet?.weight?.takeIf { it > 0.0 }?.let { "${formatWeight(it)} kg" }
        ?: plannedSet?.targetWeightKg?.takeIf { it > 0.0 }?.let { "${formatWeight(it)} kg" }
        ?: "-"
    val rest = loggedSet?.restSeconds?.takeIf { it > 0 }?.let { "${it}s" }
        ?: activeRestSeconds.takeIf { it > 0 }?.let { "${it}s" }
        ?: "-"
    val rpe = loggedSet?.rpe?.takeIf { it > 0.0 }?.let(::formatWeight)
        ?: plannedSet?.targetRpe?.takeIf { it > 0.0 }?.let(::formatWeight)
        ?: "-"
    return listOf(
        RoutineSetMetricCell(RepetitionsMetricLabel, reps),
        RoutineSetMetricCell("Kg", weight),
        RoutineSetMetricCell("Rust", rest),
        RoutineSetMetricCell("RPE", rpe),
    )
}

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

@OptIn(ExperimentalLayoutApi::class)
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
    val scrollState = rememberScrollState()
    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current
    val view = LocalView.current
    val dismissThresholdPx = with(density) { SetEditorHandleDismissThreshold.toPx() }
    val imeBottomPx = with(density) { imeBottomPadding.toPx() }
    var scrollViewportBounds by remember { mutableStateOf<Rect?>(null) }
    val visibleViewportBottomProvider = {
        scrollViewportBounds?.let { viewportBounds ->
            setEditorVisibleViewportBottom(
                viewportBottom = viewportBounds.bottom,
                rootHeight = view.height.toFloat(),
                imeBottomPx = imeBottomPx,
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.08f)
                    .align(Alignment.TopCenter)
                    .clickable(onClick = onDismiss),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(SetEditorSurfaceMaxHeightFraction)
                    .imePadding(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.trainIqColors.card,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .onGloballyPositioned { coordinates ->
                                scrollViewportBounds = coordinates.boundsInRoot()
                            }
                            .clearFocusOnScrollOrDrag()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .fillMaxWidth()
                                .height(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(5.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                        shape = RoundedCornerShape(999.dp),
                                    ),
                            )
                        }
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
                            revealKey = "${set.id}:reps",
                            keyboardType = KeyboardType.Number,
                            step = 1.0,
                            modifier = Modifier.weight(1f),
                            scrollState = scrollState,
                            scrollViewportBoundsProvider = { scrollViewportBounds },
                            visibleViewportBottomProvider = visibleViewportBottomProvider,
                            onValueChange = { reps = it },
                        )
                        SheetNumberField(
                            value = weight,
                            label = "Gewicht",
                            revealKey = "${set.id}:weight",
                            suffix = "kg",
                            keyboardType = KeyboardType.Decimal,
                            step = 2.5,
                            modifier = Modifier.weight(1f),
                            scrollState = scrollState,
                            scrollViewportBoundsProvider = { scrollViewportBounds },
                            visibleViewportBottomProvider = visibleViewportBottomProvider,
                            onValueChange = { weight = it },
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SheetNumberField(
                            value = rest,
                            label = "Rust",
                            revealKey = "${set.id}:rest",
                            suffix = "s",
                            keyboardType = KeyboardType.Number,
                            step = 15.0,
                            showAdjustControls = true,
                            modifier = Modifier.weight(1f),
                            scrollState = scrollState,
                            scrollViewportBoundsProvider = { scrollViewportBounds },
                            visibleViewportBottomProvider = visibleViewportBottomProvider,
                            onValueChange = { rest = it },
                        )
                        SheetNumberField(
                            value = rpe,
                            label = "RPE",
                            revealKey = "${set.id}:rpe",
                            keyboardType = KeyboardType.Decimal,
                            step = 0.5,
                            modifier = Modifier.weight(1f),
                            scrollState = scrollState,
                            scrollViewportBoundsProvider = { scrollViewportBounds },
                            visibleViewportBottomProvider = visibleViewportBottomProvider,
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
                    Spacer(
                        modifier = Modifier.height(
                            exerciseEditTrailingScrollPadding(imeBottomPadding = imeBottomPadding),
                        ),
                    )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(72.dp)
                            .pointerInput(dismissThresholdPx, onDismiss) {
                                var verticalDragPx = 0f
                                var hasDismissed = false
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        verticalDragPx = 0f
                                        hasDismissed = false
                                    },
                                    onVerticalDrag = { _, dragAmount ->
                                        verticalDragPx = (verticalDragPx + dragAmount).coerceAtLeast(0f)
                                        if (!hasDismissed && shouldDismissSetEditorFromHandleDrag(verticalDragPx, dismissThresholdPx)) {
                                            hasDismissed = true
                                            onDismiss()
                                        }
                                    },
                                )
                            }
                            .semantics {
                                contentDescription = "Set editor handle"
                            },
                    )
                }
            }
        }
    }
}

internal fun setEditorUsesDialogBackedSurface(): Boolean = true

internal fun exerciseEditTrailingScrollPadding(
    imeBottomPadding: Dp = 0.dp,
    minimumTrailingPadding: Dp = 24.dp,
): Dp = minimumTrailingPadding + imeBottomPadding

@Composable
private fun Modifier.revealInSetEditorScrollOnFocus(
    revealKey: Any,
    scrollState: ScrollState?,
    scrollViewportBoundsProvider: () -> Rect?,
    visibleViewportBottomProvider: () -> Float?,
    margin: Dp = 48.dp,
    maxLayoutWaitFrames: Int = 10,
    imeSettleFrames: Int = 3,
): Modifier {
    if (scrollState == null) return this

    var fieldBounds by remember { mutableStateOf<Rect?>(null) }
    val scope = rememberCoroutineScope()
    val revealJob = remember(revealKey) { mutableStateOf<Job?>(null) }
    val marginPx = with(LocalDensity.current) { margin.toPx() }
    val currentScrollViewportBoundsProvider by rememberUpdatedState(scrollViewportBoundsProvider)
    val currentVisibleViewportBottomProvider by rememberUpdatedState(visibleViewportBottomProvider)

    suspend fun waitForMeasuredRevealTarget(requireImeInset: Boolean): Boolean {
        repeat(maxLayoutWaitFrames.coerceAtLeast(1)) {
            withFrameNanos { }
            val viewport = currentScrollViewportBoundsProvider()
            val field = fieldBounds
            val visibleBottom = currentVisibleViewportBottomProvider()
            if (
                field != null &&
                viewport != null &&
                (!requireImeInset || (visibleBottom != null && visibleBottom < viewport.bottom))
            ) {
                return true
            }
        }
        return fieldBounds != null && currentScrollViewportBoundsProvider() != null
    }

    suspend fun revealFocusedField(): Boolean {
        val field = fieldBounds ?: return false
        val viewport = currentScrollViewportBoundsProvider() ?: return false
        val visibleBottom = currentVisibleViewportBottomProvider() ?: viewport.bottom
        val scrollDelta = focusedInputRevealScrollDelta(
            fieldTop = field.top,
            fieldBottom = field.bottom,
            viewportTop = viewport.top,
            viewportBottom = visibleBottom,
            marginPx = marginPx,
        )
        if (scrollDelta != 0f) {
            val target = (scrollState.value + scrollDelta.roundToInt())
                .coerceIn(0, scrollState.maxValue)
            scrollState.animateScrollTo(target)
        }
        return scrollDelta != 0f
    }

    suspend fun revealAfterFocusGain() {
        val imeAlreadyApplied = run {
            val viewport = currentScrollViewportBoundsProvider()
            val visibleBottom = currentVisibleViewportBottomProvider()
            viewport != null && visibleBottom != null && visibleBottom < viewport.bottom
        }
        waitForMeasuredRevealTarget(requireImeInset = !imeAlreadyApplied)
        repeat(imeSettleFrames.coerceAtLeast(0)) {
            withFrameNanos { }
        }
        revealFocusedField()
        withFrameNanos { }
        if (!isFocusedInputVisibleInSetEditor(
                field = fieldBounds,
                viewport = currentScrollViewportBoundsProvider(),
                visibleViewportBottom = currentVisibleViewportBottomProvider(),
                marginPx = marginPx,
            )
        ) {
            revealFocusedField()
        }
    }

    DisposableEffect(revealKey) {
        onDispose { revealJob.value?.cancel() }
    }

    return onGloballyPositioned { coordinates ->
        fieldBounds = coordinates.boundsInRoot()
    }.onFocusChanged { focusState ->
        revealJob.value?.cancel()
        if (focusState.isFocused) {
            revealJob.value = scope.launch {
                revealAfterFocusGain()
            }
        }
    }
}

internal fun isFocusedInputVisibleInSetEditor(
    field: Rect?,
    viewport: Rect?,
    visibleViewportBottom: Float?,
    marginPx: Float,
): Boolean {
    if (field == null || viewport == null) return false
    val visibleBottom = (visibleViewportBottom ?: viewport.bottom) - marginPx
    val visibleTop = viewport.top + marginPx
    return field.top >= visibleTop && field.bottom <= visibleBottom
}

internal fun focusedInputRevealScrollDelta(
    fieldTop: Float,
    fieldBottom: Float,
    viewportTop: Float,
    viewportBottom: Float,
    marginPx: Float,
): Float {
    val visibleTop = viewportTop + marginPx
    val visibleBottom = viewportBottom - marginPx
    return when {
        fieldBottom > visibleBottom -> fieldBottom - visibleBottom
        fieldTop < visibleTop -> fieldTop - visibleTop
        else -> 0f
    }
}

internal fun setEditorVisibleViewportBottom(
    viewportBottom: Float,
    rootHeight: Float,
    imeBottomPx: Float,
): Float = min(viewportBottom, rootHeight - imeBottomPx)

@Composable
private fun SheetNumberField(
    value: String,
    label: String,
    revealKey: Any,
    keyboardType: KeyboardType,
    step: Double,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    showAdjustControls: Boolean = false,
    scrollState: ScrollState? = null,
    scrollViewportBoundsProvider: () -> Rect? = { null },
    visibleViewportBottomProvider: () -> Float? = { null },
    onValueChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val filterInput: (String) -> String = { input ->
        if (keyboardType == KeyboardType.Decimal) {
            filterDecimalInput(input, maxDecimals = if (label == "RPE") 1 else 2)
        } else {
            filterIntegerInput(input)
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TapOnlyOutlinedTextField(
            value = value,
            onValueChange = { onValueChange(filterInput(it)) },
            label = { Text(label) },
            suffix = suffix?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(force = true) }),
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewOnFocus()
                .revealInSetEditorScrollOnFocus(
                    revealKey = revealKey,
                    scrollState = scrollState,
                    scrollViewportBoundsProvider = scrollViewportBoundsProvider,
                    visibleViewportBottomProvider = visibleViewportBottomProvider,
                ),
        )
        if (showAdjustControls) {
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
    TapOnlyOutlinedTextField(
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
    allowCustomExercise: Boolean = showDefaults,
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
    val dismissThresholdPx = with(LocalDensity.current) { ExercisePickerHandleDismissThreshold.toPx() }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.08f)
                    .align(Alignment.TopCenter)
                    .clickable(onClick = onDismiss),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                TrainingWithoutOverscroll {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clearFocusOnScrollOrDrag()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(dismissThresholdPx, onDismiss) {
                                    var verticalDragPx = 0f
                                    var hasDismissed = false
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            verticalDragPx = 0f
                                            hasDismissed = false
                                        },
                                        onVerticalDrag = { _, dragAmount ->
                                            verticalDragPx = (verticalDragPx + dragAmount).coerceAtLeast(0f)
                                            if (!hasDismissed && shouldDismissExercisePickerFromHandleDrag(verticalDragPx, dismissThresholdPx)) {
                                                hasDismissed = true
                                                onDismiss()
                                            }
                                        },
                                    )
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp, bottom = 8.dp)
                                    .width(44.dp)
                                    .height(5.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                        shape = RoundedCornerShape(999.dp),
                                    ),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Oefeningkiezer sluiten")
                                }
                            }
                        }
                    }
                    item {
                        TapOnlyOutlinedTextField(
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
                            Text("Standaardwaarden voor deze oefening", style = MaterialTheme.typography.labelLarge)
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
                                    contentDescription = if (defaultsExpanded) "Standaardwaarden inklappen" else "Standaardwaarden openen",
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
                    }
                    if (allowCustomExercise) {
                        item {
                            TextButton(onClick = onCustomExercise, modifier = Modifier.fillMaxWidth()) {
                                Text("Voeg eigen oefening toe")
                            }
                        }
                    }
                    items(filteredExercises, key = { it.id }) { exercise ->
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
                                        exerciseHistorySubtitleText(exercise.muscleGroup, exercise.equipment),
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
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = { Text("Voeg eigen oefening toe") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .clearFocusOnScrollOrDrag()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TapOnlyOutlinedTextField(exerciseName, { exerciseName = it }, label = { Text("Oefening") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                TapOnlyOutlinedTextField(muscleGroup, { muscleGroup = it }, label = { Text("Spiergroep") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                TapOnlyOutlinedTextField(equipment, { equipment = it }, label = { Text("Materiaal") }, modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
            Text("Standaardwaarden", style = MaterialTheme.typography.labelLarge)
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
                Spacer(
                    modifier = Modifier.height(
                        exerciseEditTrailingScrollPadding(),
                    ),
                )
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
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = { Text(plan.exercise.name) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .clearFocusOnScrollOrDrag()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TapOnlyOutlinedTextField(targetSets, { targetSets = it }, label = { Text("Sets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f).bringIntoViewOnFocus())
                    TapOnlyOutlinedTextField(repRange, { repRange = it }, label = { Text("Reps") }, modifier = Modifier.weight(1f).bringIntoViewOnFocus())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TapOnlyOutlinedTextField(restSeconds, { restSeconds = it }, label = { Text("Rest s") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f).bringIntoViewOnFocus())
                    TapOnlyOutlinedTextField(targetWeightKg, { targetWeightKg = it }, label = { Text("Kg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f).bringIntoViewOnFocus())
                    TapOnlyOutlinedTextField(targetRpe, { targetRpe = it }, label = { Text("RPE") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f).bringIntoViewOnFocus())
                }
                SetTypeSelector(
                    selectedType = setType,
                    onSelectedTypeChange = { setType = it },
                )
                Spacer(
                    modifier = Modifier.height(
                        exerciseEditTrailingScrollPadding(),
                    ),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(targetSets, repRange, restSeconds, targetWeightKg, targetRpe, setType) }) {
                Text("Opslaan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuleren") } },
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
        TrainingWithoutOverscroll {
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
                val subtitle = exerciseHistorySubtitleText(
                    muscleGroup = history.exercise?.muscleGroup.orEmpty(),
                    equipment = history.exercise?.equipment.orEmpty(),
                )
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
                SimpleLineChart(
                    points = points,
                    chartName = title,
                    valueSuffix = valueSuffix,
                    modifier = Modifier.fillMaxWidth().height(132.dp),
                )
            }
        }
    }
}

@Composable
private fun SimpleLineChart(
    points: List<ChartPoint>,
    chartName: String,
    valueSuffix: String,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = MaterialTheme.colorScheme.primary
    val chartPoints = points.takeLast(12)
    Canvas(
        modifier = modifier.semantics {
            contentDescription = lineChartContentDescription(chartPoints, chartName, valueSuffix)
        },
    ) {
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
                        exerciseHistorySessionMetaText(
                            duration = formatTimer(session.durationSeconds.toInt()),
                            completedSets = session.sets.count { it.completed },
                        ),
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
    onSwitchActiveWorkout: (Long) -> Unit,
    onOpenExerciseHistory: (Long) -> Unit,
    onWorkoutCompleted: (Long) -> Unit,
    onWorkoutProcessing: (Long) -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val screenState by viewModel.uiState.collectAsStateWithLifecycle()
    val content = screenState.workoutContentOrDefault()
    val uiState = content.activeWorkout
    val workoutFeedbackPreferences = content.workoutFeedbackPreferences
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
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.message)
                }
                is WorkoutUiEvent.SetLogged -> {
                    if (currentFeedbackPreferences.workoutHapticsEnabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.undoEventId?.let { "Ongedaan maken" },
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.undoEventId?.let(viewModel::undoWorkoutLogEvent)
                    }
                }
                is WorkoutUiEvent.WorkoutCompleted -> onWorkoutCompleted(event.sessionId)
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.workoutCompletions.collect { sessionId ->
            onWorkoutCompleted(sessionId)
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
        restTimerSeconds = uiState.restTimerSeconds,
        restTimerTotalSeconds = uiState.restTimerTotalSeconds,
        exerciseLibrary = content.overview?.exercises.orEmpty(),
        snackbarHostState = snackbarHostState,
        workoutHapticsEnabled = workoutFeedbackPreferences.workoutHapticsEnabled,
        onBack = onBack,
        onOpenExerciseHistory = onOpenExerciseHistory,
        onDraftChange = viewModel::updateSetDraft,
        onSetTypeChange = viewModel::updateLoggedSetType,
        onEditSet = viewModel::editLoggedSet,
        onDeleteSet = { setId -> viewModel.deleteLoggedSet(setId) },
        onRelogSet = viewModel::relogSet,
        onDismissMessage = viewModel::clearMessage,
        onLogSet = viewModel::logSet,
        onLogSameAgain = viewModel::logSameAgain,
        onAdjustExerciseRest = viewModel::adjustExerciseRest,
        onAdjustRestTimer = viewModel::adjustRestTimer,
        onSkipRestTimer = viewModel::skipRestTimer,
        onRestartRestTimer = viewModel::restartRestTimer,
        onToggleExerciseCollapsed = viewModel::setExerciseCollapsed,
        onReplaceActiveExercise = viewModel::replaceExerciseInActiveWorkout,
        onReplaceActiveExerciseWithCustom = viewModel::replaceActiveExerciseWithCustom,
        onRemoveActiveExercise = viewModel::removeExerciseFromActiveWorkout,
        onResumeConflictingWorkout = { conflict ->
            viewModel.dismissStartConflict()
            onSwitchActiveWorkout(conflict.activeDayId)
        },
        onReplaceConflictingWorkout = viewModel::replaceConflictingActiveWorkout,
        onDismissStartConflict = viewModel::dismissStartConflict,
        onFinish = { onWorkoutProcessing(dayId) },
        onDiscard = { viewModel.discardWorkout(dayId) },
    )
}

@Composable
fun WorkoutProcessingRoute(
    dayId: Long,
    onComplete: (Long) -> Unit,
    onBackToTraining: () -> Unit,
    viewModel: WorkoutProcessingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(dayId) { viewModel.finish(dayId) }
    LaunchedEffect(uiState) {
        (uiState as? WorkoutProcessingUiState.Complete)?.let { onComplete(it.sessionId) }
    }
    WorkoutProcessingScreen(
        uiState = uiState,
        onRetry = { viewModel.finish(dayId) },
        onBackToTraining = onBackToTraining,
    )
}

@Composable
private fun WorkoutProcessingScreen(
    uiState: WorkoutProcessingUiState,
    onRetry: () -> Unit,
    onBackToTraining: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(MaterialTheme.spacing.large),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary, elevated = true) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (workoutProcessingUsesShimmerLoading()) {
                        repeat(3) {
                            ShimmerCardPlaceholder(lineCount = 1, modifier = Modifier.fillMaxWidth())
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                    Text("Training verwerken...", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text(
                        when (uiState) {
                            WorkoutProcessingUiState.SavingWorkout -> "Workout opslaan"
                            WorkoutProcessingUiState.BuildingSummary -> "Samenvatting maken"
                            is WorkoutProcessingUiState.Complete -> "Synchronisatie voorbereiden"
                            is WorkoutProcessingUiState.Error -> uiState.message
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.trainIqColors.mutedText,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppChip(label = "Opslaan", accent = MaterialTheme.colorScheme.primary)
                        AppChip(label = "AI-terugblik", accent = MaterialTheme.colorScheme.secondary)
                        AppChip(label = "Lokale fallback", accent = MaterialTheme.colorScheme.tertiary)
                    }
                    if (uiState is WorkoutProcessingUiState.Error) {
                        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                            Text("Opnieuw proberen")
                        }
                        Button(onClick = onBackToTraining, modifier = Modifier.fillMaxWidth()) {
                            Text("Terug naar training")
                        }
                    }
                }
            }
        }
    }
}

internal fun workoutProcessingUsesShimmerLoading(): Boolean = true

@Composable
fun WorkoutCompletionRoute(
    sessionId: Long,
    onBackToTraining: () -> Unit,
    onHome: () -> Unit,
    viewModel: WorkoutCompletionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    WorkoutCompletionScreen(
        uiState = uiState,
        onBackToTraining = onBackToTraining,
        onHome = onHome,
    )
}

@Composable
private fun WorkoutCompletionScreen(
    uiState: WorkoutCompletionUiState,
    onBackToTraining: () -> Unit,
    onHome: () -> Unit,
) {
    var autoReturnActive by rememberSaveable { mutableStateOf(true) }
    var countdown by rememberSaveable { mutableIntStateOf(12) }
    val listState = rememberLazyListState()
    val cancelAutoReturn = {
        autoReturnActive = false
    }
    LaunchedEffect(autoReturnActive) {
        if (!autoReturnActive) return@LaunchedEffect
        countdown = 12
        while (countdown > 0 && autoReturnActive) {
            delay(1_000L)
            countdown -= 1
        }
        if (autoReturnActive) onHome()
    }
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
            cancelAutoReturn()
        }
    }
    BackHandler { onBackToTraining() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed }) {
                                cancelAutoReturn()
                            }
                        }
                    }
                },
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.medium,
                top = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                bottom = 160.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (uiState) {
                WorkoutCompletionUiState.Loading -> {
                    item { ScreenHeader(title = "Training opgeslagen", subtitle = "Samenvatting wordt geladen") }
                    item { ShimmerCardPlaceholder() }
                    item { ShimmerCardPlaceholder() }
                }
                is WorkoutCompletionUiState.Error -> {
                    item { ScreenHeader(title = "Training opgeslagen", subtitle = "Samenvatting niet beschikbaar") }
                    item {
                        EmptyStateCard(
                            title = "Geen samenvatting gevonden",
                            body = uiState.message,
                            actionLabel = "Terug naar krachttraining",
                            onAction = onBackToTraining,
                        )
                    }
                }
                is WorkoutCompletionUiState.Success -> {
                    val summary = uiState.summary
                    item { CompletionHeader(summary) }
                    item { CompletionSmartSummary(summary) }
                    item { CompletionStats(summary) }
                    item { CompletionExerciseOverview(summary.exercises) }
                    item {
                        CompletionActions(
                            countdown = countdown,
                            autoReturnActive = autoReturnActive,
                            onBackToTraining = {
                                cancelAutoReturn()
                                onBackToTraining()
                            },
                            onHome = {
                                cancelAutoReturn()
                                onHome()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionHeader(summary: WorkoutCompletionSummary) {
    AppCard(accent = MaterialTheme.colorScheme.primary, elevated = true) {
        AppChip(label = "Voltooid", accent = MaterialTheme.colorScheme.primary)
        Text(
            summary.workoutName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${formatTimer(summary.durationSeconds.toInt())} - klaar om ${formatHistoryDate(summary.endedAt)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.trainIqColors.mutedText,
        )
        if (summary.strongestSetLabel.isNotBlank()) {
            Text("Sterkste set: ${summary.strongestSetLabel}", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CompletionStats(summary: WorkoutCompletionSummary) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppCard(modifier = Modifier.weight(1f), accent = MaterialTheme.colorScheme.primary) {
            StatusMetric("Oefeningen", summary.exercisesCompleted.toString())
        }
        AppCard(modifier = Modifier.weight(1f), accent = MaterialTheme.colorScheme.secondary) {
            StatusMetric("Sets", summary.setsLogged.toString())
        }
        AppCard(modifier = Modifier.weight(1f), accent = MaterialTheme.colorScheme.tertiary) {
            StatusMetric("Volume", "${summary.totalVolume.toInt()} kg")
        }
    }
}

@Composable
private fun CompletionSmartSummary(summary: WorkoutCompletionSummary) {
    val debrief = summary.debrief
    AppCard(accent = intensityContentColor(debrief.intensitySignal)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Slimme samenvatting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                AppChip(label = workoutDebriefSourceChipLabel(debrief.source), accent = intensityContentColor(debrief.intensitySignal))
            }
            AppChip(label = summary.recommendationLabel, accent = intensityContentColor(debrief.intensitySignal))
        }
        AiSummaryLead(text = debrief.summary)
        if (debrief.source == WorkoutDebriefSource.LOCAL_FALLBACK) {
            Text(
                "AI-feedback wordt opgehaald. Deze samenvatting wordt automatisch bijgewerkt zodra Gemini of OpenAI klaar is.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CompletionInsightChips(summary)
        AiBulletSection(title = "Hoogtepunten", items = debrief.wins.ifEmpty { listOf(debrief.progressionFeedback) })
        AiBulletSection(title = "Aandachtspunten", items = debrief.risks.ifEmpty { listOf("Geen duidelijke aandachtspunten op basis van de beschikbare trainingsdata.") })
        AiAdviceSection(
            recommendation = debrief.recommendation,
            nextWorkoutAdvice = debrief.nextLoadTarget.ifBlank { debrief.nextSessionFocus },
            recoveryAdvice = debrief.recoveryAdvice,
            recoveryScore = debrief.recoveryScore,
            accent = intensityContentColor(debrief.intensitySignal),
        )
    }
}

private fun WorkoutDebriefSource.shortLabel(): String = when (this) {
    WorkoutDebriefSource.GEMINI_2_5_FLASH -> "Gemini 2.5 Flash"
    WorkoutDebriefSource.OPENAI -> "OpenAI"
    WorkoutDebriefSource.LOCAL_FALLBACK -> "Lokale analyse"
}

internal fun workoutDebriefSourceChipLabel(source: WorkoutDebriefSource): String = when (source) {
    WorkoutDebriefSource.GEMINI_2_5_FLASH -> source.shortLabel()
    WorkoutDebriefSource.OPENAI -> source.shortLabel()
    WorkoutDebriefSource.LOCAL_FALLBACK -> "Lokale fallback"
}

@Composable
private fun AiSummaryLead(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CompletionInsightChips(summary: WorkoutCompletionSummary) {
    val debrief = summary.debrief
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppChip(label = "Volume ${summary.totalVolume.toInt()} kg", accent = MaterialTheme.colorScheme.primary)
        AppChip(label = "Sets ${summary.setsLogged}", accent = MaterialTheme.colorScheme.secondary)
        AppChip(
            label = when (debrief.intensitySignal.uppercase()) {
                "INCREASE" -> "Advies verhogen"
                "DELOAD" -> "Advies verlagen"
                else -> "Advies gelijk houden"
            },
            accent = intensityContentColor(debrief.intensitySignal),
        )
        AppChip(
            label = "Herstel ${debrief.recoveryScore.coerceIn(0, 100)}/100",
            accent = intensityContentColor(debrief.intensitySignal),
        )
    }
}

@Composable
private fun AiBulletSection(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        items.filter { it.isNotBlank() }.take(4).forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    cleanCompletionBulletText(item),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.trainIqColors.mutedText,
                )
            }
        }
    }
}

@Composable
private fun AiAdviceSection(
    recommendation: String,
    nextWorkoutAdvice: String,
    recoveryAdvice: String,
    recoveryScore: Int,
    accent: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Advies voor volgende training", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(recommendation, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        if (nextWorkoutAdvice.isNotBlank()) {
            Text(nextWorkoutAdvice, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
        }
        if (recoveryAdvice.isNotBlank()) {
            Text("Herstel: $recoveryAdvice", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.trainIqColors.mutedText)
        }
        AppLinearProgress(progress = recoveryScore.coerceIn(0, 100) / 100f, accent = accent)
    }
}

@Composable
private fun CompletionExerciseOverview(exercises: List<WorkoutCompletionExercise>) {
    AppCard(accent = MaterialTheme.colorScheme.secondary) {
        Text("Oefeningen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (exercises.isEmpty()) {
            Text("Geen gelogde sets gevonden voor deze sessie.", color = MaterialTheme.trainIqColors.mutedText)
        }
        exercises.forEach { exercise ->
            CompletionExerciseBlock(exercise)
        }
    }
}

@Composable
private fun CompletionExerciseBlock(exercise: WorkoutCompletionExercise) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${exercise.sets.size} sets - ${exercise.totalVolume.toInt()} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.trainIqColors.mutedText,
                )
            }
            if (exercise.bestSetLabel.isNotBlank()) {
                Text(exercise.bestSetLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        exercise.sets.forEach { set ->
            val rpe = if (set.rpe > 0.0) " - RPE ${formatWeight(set.rpe)}" else ""
            Text(
                "Set ${set.setNumber}: ${formatWeight(set.weightKg)} kg x ${set.reps}$rpe",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun CompletionActions(
    countdown: Int,
    autoReturnActive: Boolean,
    onBackToTraining: () -> Unit,
    onHome: () -> Unit,
) {
    AppCard {
        Text(
            if (autoReturnActive) "Automatisch terug naar start over $countdown seconden" else "Automatisch terugkeren gepauzeerd",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.trainIqColors.mutedText,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryActionButton(onClick = onBackToTraining, modifier = Modifier.weight(1f)) { Text("Terug naar krachttraining") }
            PrimaryActionButton(onClick = onHome, modifier = Modifier.weight(1f)) { Text("Naar start") }
        }
    }
}

@Composable
private fun ActiveWorkoutStartConflictDialog(
    conflict: ActiveWorkoutStartConflict,
    onResume: (ActiveWorkoutStartConflict) -> Unit,
    onReplace: (ActiveWorkoutStartConflict) -> Unit,
    onDismiss: () -> Unit,
) {
    val setCopy = if (conflict.loggedSetCount == 1) "1 gelogde set" else "${conflict.loggedSetCount} gelogde sets"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Oude training open") },
        text = {
            Text(
                "Er staat nog een actieve training open met $setCopy. Hervat die training of gooi hem weg voordat je deze nieuwe routine start.",
            )
        },
        confirmButton = {
            Button(onClick = { onReplace(conflict) }) {
                Text("Nieuwe training starten")
            }
        },
        dismissButton = {
            TextButton(onClick = { onResume(conflict) }) {
                Text("Oude training hervatten")
            }
            TextButton(onClick = onDismiss) {
                Text("Annuleren")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    uiState: ActiveWorkoutUiState,
    restTimerSeconds: Int,
    restTimerTotalSeconds: Int,
    exerciseLibrary: List<Exercise>,
    snackbarHostState: SnackbarHostState,
    workoutHapticsEnabled: Boolean,
    onBack: () -> Unit,
    onOpenExerciseHistory: (Long) -> Unit,
    onDraftChange: (Long, SetInputDraft) -> Unit,
    onSetTypeChange: (Long, SetType) -> Unit,
    onEditSet: (Long, Long) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onRelogSet: (Long, Long) -> Unit,
    onDismissMessage: () -> Unit,
    onLogSet: (WorkoutExercisePlan) -> Boolean,
    onLogSameAgain: (WorkoutExercisePlan) -> Boolean,
    onAdjustExerciseRest: (Long, Int, Int) -> Unit,
    onAdjustRestTimer: (Int) -> Unit,
    onSkipRestTimer: () -> Unit,
    onRestartRestTimer: (Int) -> Unit,
    onToggleExerciseCollapsed: (Long, Boolean) -> Unit,
    onReplaceActiveExercise: (Long, Exercise) -> Unit,
    onReplaceActiveExerciseWithCustom: (Long, String, String, String) -> Unit,
    onRemoveActiveExercise: (Long) -> Unit,
    onResumeConflictingWorkout: (ActiveWorkoutStartConflict) -> Unit,
    onReplaceConflictingWorkout: (ActiveWorkoutStartConflict) -> Unit,
    onDismissStartConflict: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var showFinishConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var replacingActivePlan by remember { mutableStateOf<WorkoutExercisePlan?>(null) }
    var creatingActiveReplacement by remember { mutableStateOf<WorkoutExercisePlan?>(null) }
    var pendingActiveReplacement by remember { mutableStateOf<Pair<WorkoutExercisePlan, Exercise>?>(null) }
    var pendingRemoveActivePlan by remember { mutableStateOf<WorkoutExercisePlan?>(null) }
    val currentOnDismissMessage by rememberUpdatedState(onDismissMessage)
    val workoutExercises = uiState.workout?.exercises.orEmpty()
    val exerciseGroups = remember(workoutExercises) { workoutExerciseGroups(workoutExercises) }
    val activeWorkoutListState = rememberLazyListState()
    val suggestionsByExerciseId = remember(uiState.progressionSuggestions) {
        uiState.progressionSuggestions.associateBy { it.exerciseId }
    }
    LaunchedEffect(uiState.message) {
        val currentMessage = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(currentMessage)
        currentOnDismissMessage()
    }

    uiState.pendingStartConflict?.let { conflict ->
        ActiveWorkoutStartConflictDialog(
            conflict = conflict,
            onResume = onResumeConflictingWorkout,
            onReplace = onReplaceConflictingWorkout,
            onDismiss = onDismissStartConflict,
        )
    }

    replacingActivePlan?.let { plan ->
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
                replacingActivePlan = null
                if (uiState.loggedSetsThisSession[plan.activeKey].orEmpty().isNotEmpty()) {
                    pendingActiveReplacement = plan to exercise
                } else {
                    onReplaceActiveExercise(plan.id, exercise)
                }
            },
            allowCustomExercise = true,
            onCustomExercise = {
                replacingActivePlan = null
                creatingActiveReplacement = plan
            },
            onDismiss = { replacingActivePlan = null },
        )
    }
    creatingActiveReplacement?.let { plan ->
        CustomExerciseDialog(
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
            onConfirm = { name, muscleGroup, equipment ->
                creatingActiveReplacement = null
                onReplaceActiveExerciseWithCustom(plan.id, name, muscleGroup, equipment)
            },
            onDismiss = { creatingActiveReplacement = null },
        )
    }

    Scaffold(
        modifier = Modifier.clearFocusOnTapOutside(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState.debrief == null) {
                ActiveWorkoutBottomBar(
                    uiState = uiState,
                    restTimerSeconds = restTimerSeconds,
                    restTimerTotalSeconds = restTimerTotalSeconds,
                    onAdjustRestTimer = onAdjustRestTimer,
                    onSkipRestTimer = onSkipRestTimer,
                    onFinishClick = {
                        if (uiState.needsFinishConfirmation) showFinishConfirm = true else onFinish()
                    },
                )
            }
        },
    ) { padding ->
        TrainingWithoutOverscroll {
            LazyColumn(
                state = activeWorkoutListState,
                modifier = Modifier
                    .fillMaxSize()
                    .clearFocusOnScrollOrDrag()
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
            item(
                key = "active-workout-header",
                contentType = "active-workout-header",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3,
                    ) {
                        SecondaryActionButton(
                            onClick = onBack,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { contentDescription = "Terug naar Training" },
                        ) { Text("Terug") }
                        if (uiState.debrief == null) {
                            SecondaryActionButton(
                                onClick = { showDiscardConfirm = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = "Actieve training weggooien" },
                                accent = MaterialTheme.colorScheme.error,
                            ) { Text("Weggooien") }
                        }
                    }
                    Text(
                        if (uiState.debrief == null) {
                            "Terug sluit dit scherm; je training blijft actief."
                        } else {
                            "Je sessie is lokaal opgeslagen."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.trainIqColors.mutedText,
                    )
                    ActiveWorkoutSessionSummary(uiState, restTimerSeconds)
                }
            }
            if (uiState.workout == null) {
                item { EmptyCard("Training niet beschikbaar", "Deze training kon niet worden geladen.") }
                return@LazyColumn
            }
            if (uiState.workout.exercises.isEmpty()) {
                item { EmptyCard("Geen oefeningen", "Voeg oefeningen toe aan deze routine voordat je een training start.") }
                return@LazyColumn
            }
            if (restTimerSeconds > 0) {
                item(
                    key = "active-workout-rest-timer",
                    contentType = "active-workout-rest-timer",
                ) {
                    RestTimerCard(
                        restTimerSeconds = restTimerSeconds,
                        totalSeconds = restTimerTotalSeconds,
                        onAdjust = onAdjustRestTimer,
                        onSkip = onSkipRestTimer,
                        onRestart = {
                            val nextRest = uiState.workout.exercises.firstOrNull()?.restSeconds ?: restTimerTotalSeconds
                            onRestartRestTimer(nextRest)
                        },
                    )
                }
            }
            items(
                exerciseGroups,
                key = { group -> group.joinToString("-") { it.id.toString() } },
                contentType = { group -> if (group.size > 1) "active-workout-superset" else "active-workout-exercise" },
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
                            val exerciseState = activeWorkoutExerciseUiState(
                                plan = plan,
                                uiState = uiState,
                            )
                            ActiveWorkoutPlanCard(
                                plan = plan,
                                exerciseState = exerciseState,
                                suggestion = suggestionsByExerciseId[plan.exercise.id],
                                hapticOnSuccess = {
                                    if (workoutHapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onOpenHistory = { onOpenExerciseHistory(plan.exercise.id) },
                                onDraftChange = onDraftChange,
                                onSetTypeChange = onSetTypeChange,
                                onEditSet = onEditSet,
                                onDeleteSet = onDeleteSet,
                                onRelogSet = onRelogSet,
                                onLogSet = onLogSet,
                                onLogSameAgain = onLogSameAgain,
                                onAdjustExerciseRest = onAdjustExerciseRest,
                                onToggleCollapsed = onToggleExerciseCollapsed,
                                onReplaceExercise = { replacingActivePlan = plan },
                                onRemoveExercise = { pendingRemoveActivePlan = plan },
                            )
                        }
                    }
                } else {
                    val plan = group.first()
                    val exerciseState = activeWorkoutExerciseUiState(
                        plan = plan,
                        uiState = uiState,
                    )
                    ActiveWorkoutPlanCard(
                        plan = plan,
                        exerciseState = exerciseState,
                        suggestion = suggestionsByExerciseId[plan.exercise.id],
                        hapticOnSuccess = {
                            if (workoutHapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onOpenHistory = { onOpenExerciseHistory(plan.exercise.id) },
                        onDraftChange = onDraftChange,
                        onSetTypeChange = onSetTypeChange,
                        onEditSet = onEditSet,
                        onDeleteSet = onDeleteSet,
                        onRelogSet = onRelogSet,
                        onLogSet = onLogSet,
                        onLogSameAgain = onLogSameAgain,
                        onAdjustExerciseRest = onAdjustExerciseRest,
                        onToggleCollapsed = onToggleExerciseCollapsed,
                        onReplaceExercise = { replacingActivePlan = plan },
                        onRemoveExercise = { pendingRemoveActivePlan = plan },
                    )
                }
            }
            if (uiState.debrief != null) {
                item(
                    key = "active-workout-debrief",
                    contentType = "active-workout-debrief",
                ) {
                    WorkoutDebriefCard(uiState.debrief, uiState)
                }
            }
        }
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
            text = { Text(discardActiveWorkoutBodyText()) },
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
    pendingRemoveActivePlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { pendingRemoveActivePlan = null },
            title = { Text("Oefening uit actieve training halen?") },
            text = { Text("${plan.exercise.name} wordt alleen uit deze actieve training verwijderd. Je opgeslagen routine blijft hetzelfde.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRemoveActivePlan = null
                        onRemoveActiveExercise(plan.id)
                    },
                ) { Text("Verwijderen") }
            },
            dismissButton = { TextButton(onClick = { pendingRemoveActivePlan = null }) { Text("Annuleren") } },
        )
    }
    pendingActiveReplacement?.let { (plan, exercise) ->
        AlertDialog(
            onDismissRequest = { pendingActiveReplacement = null },
            title = { Text("Oefening vervangen?") },
            text = {
                Text(
                    "Gelogde sets voor ${plan.exercise.name} blijven bewaard als uitgevoerde sets. Nieuwe sets gebruik je daarna voor ${exercise.name}.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingActiveReplacement = null
                        onReplaceActiveExercise(plan.id, exercise)
                    },
                ) { Text("Vervangen") }
            },
            dismissButton = { TextButton(onClick = { pendingActiveReplacement = null }) { Text("Annuleren") } },
        )
    }
}

@Composable
private fun ActiveWorkoutSessionSummary(uiState: ActiveWorkoutUiState, restTimerSeconds: Int) {
    val progress = if (uiState.targetSets > 0) {
        (uiState.completedSets / uiState.targetSets.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = activeWorkoutStickyStatusContentDescription(uiState, restTimerSeconds)
            },
        accent = MaterialTheme.trainIqColors.amber,
        elevated = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (uiState.debrief == null) "Actieve training" else "Training opgeslagen",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                displayWorkoutDayName(uiState.workout?.name ?: "Workout"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusMetric("Tijd", formatTimer(uiState.elapsedSeconds.toInt()))
                StatusMetric("Oefeningen", uiState.workout?.exercises?.size?.toString() ?: "-")
                StatusMetric("Sets", "${uiState.completedSets}/${uiState.targetSets}")
                StatusMetric("Rust", activeWorkoutBottomBarStatusText(restTimerSeconds).removePrefix("Rust "))
            }
            AppLinearProgress(progress = progress, accent = MaterialTheme.trainIqColors.amber)
        }
    }
}

@Composable
private fun ActiveWorkoutStickyStatus(uiState: ActiveWorkoutUiState, restTimerSeconds: Int) {
    val progress = if (uiState.targetSets > 0) {
        (uiState.completedSets / uiState.targetSets.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    AppCard(
        modifier = Modifier
            .padding(top = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = activeWorkoutStickyStatusContentDescription(uiState, restTimerSeconds)
            },
        accent = MaterialTheme.colorScheme.primary,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusMetric("Tijd", formatTimer(uiState.elapsedSeconds.toInt()), Modifier.weight(1f))
                StatusMetric("Sets", "${uiState.completedSets}/${uiState.targetSets}", Modifier.weight(1f))
                StatusMetric("Volume", "${uiState.totalVolume.toInt()} kg", Modifier.weight(1f))
                StatusMetric("Rust", if (restTimerSeconds > 0) formatTimer(restTimerSeconds) else "Klaar", Modifier.weight(1f))
            }
            if (uiState.loggingSummary.pendingCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.CloudQueue,
                        contentDescription = "${uiState.loggingSummary.pendingCount} workout-events wachten op synchronisatie",
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
private fun ActiveWorkoutBottomBar(
    uiState: ActiveWorkoutUiState,
    restTimerSeconds: Int,
    restTimerTotalSeconds: Int,
    onAdjustRestTimer: (Int) -> Unit,
    onSkipRestTimer: () -> Unit,
    onFinishClick: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 0.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = activeWorkoutBottomBarContentDescription(uiState, restTimerSeconds)
                },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (restTimerSeconds > 0) {
                AppLinearProgress(
                    progress = if (restTimerTotalSeconds > 0) {
                        restTimerSeconds / restTimerTotalSeconds.toFloat()
                    } else {
                        0f
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        if (restTimerSeconds > 0) "Rust ${formatTimer(restTimerSeconds)}" else "Klaar voor volgende set",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        activeLoggedSetCountText(uiState.visibleLoggedSetCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (restTimerSeconds > 0) {
                    IconButton(onClick = { onAdjustRestTimer(-30) }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Rounded.Remove, contentDescription = restTimerAdjustContentDescription(-30))
                    }
                    IconButton(onClick = { onAdjustRestTimer(30) }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = restTimerAdjustContentDescription(30))
                    }
                    IconButton(
                        onClick = onSkipRestTimer,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = restTimerSkipContentDescription())
                    }
                }
                PrimaryActionButton(
                    onClick = onFinishClick,
                    enabled = uiState.workout != null,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 42.dp, minHeight = 38.dp)
                        .semantics {
                            contentDescription = activeWorkoutFinishContentDescription(uiState.workout != null)
                        },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkoutPlanCard(
    plan: WorkoutExercisePlan,
    exerciseState: ActiveWorkoutExerciseUiState,
    suggestion: ProgressionSuggestion?,
    hapticOnSuccess: () -> Unit,
    onOpenHistory: () -> Unit,
    onDraftChange: (Long, SetInputDraft) -> Unit,
    onSetTypeChange: (Long, SetType) -> Unit,
    onEditSet: (Long, Long) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onRelogSet: (Long, Long) -> Unit,
    onLogSet: (WorkoutExercisePlan) -> Boolean,
    onLogSameAgain: (WorkoutExercisePlan) -> Boolean,
    onAdjustExerciseRest: (Long, Int, Int) -> Unit,
    onToggleCollapsed: (Long, Boolean) -> Unit,
    onReplaceExercise: () -> Unit,
    onRemoveExercise: () -> Unit,
) {
    val key = plan.activeKey
    ActiveExerciseCard(
        plan = plan,
        loggedSets = exerciseState.loggedSets,
        activeRestSeconds = exerciseState.activeRestSeconds,
        suggestion = suggestion,
        draft = exerciseState.draft,
        draftErrors = exerciseState.draftErrors,
        isSessionFinished = exerciseState.isSessionFinished,
        isAutoAdvanceTarget = exerciseState.isAutoAdvanceTarget,
        isLogPending = exerciseState.isLogPending,
        pendingCorrectionSetId = exerciseState.pendingCorrectionSetId,
        collapsed = exerciseState.collapsed,
        onOpenHistory = onOpenHistory,
        onDraftChange = { next -> onDraftChange(key, next) },
        onSetTypeChange = { setId, setType -> onSetTypeChange(setId, setType) },
        onEditSet = { setId -> onEditSet(key, setId) },
        onDeleteSet = { setId -> onDeleteSet(setId) },
        onRelogSet = { setId -> onRelogSet(key, setId) },
        onToggleCollapsed = { onToggleCollapsed(key, !exerciseState.collapsed) },
        onCopyLastSet = {
            exerciseState.loggedSets.lastOrNull()?.let { lastSet ->
                onDraftChange(key, lastSet.toDraft())
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
        onAdjustExerciseRest = { deltaSeconds -> onAdjustExerciseRest(key, exerciseState.activeRestSeconds, deltaSeconds) },
        onReplaceExercise = onReplaceExercise,
        onRemoveExercise = onRemoveExercise,
    )
}

private fun activeWorkoutExerciseUiState(
    plan: WorkoutExercisePlan,
    uiState: ActiveWorkoutUiState,
): ActiveWorkoutExerciseUiState {
    val key = plan.activeKey
    val loggedSets = uiState.loggedSetsThisSession[key].orEmpty()
    val activeRestSeconds = activeExerciseRestSeconds(
        plan.plannedRestSeconds(loggedSets.size),
        uiState.exerciseRestOverrides[key],
    )
    val draft = activeSetUiDraft(
        savedDraft = uiState.drafts[key],
        plan = plan,
        loggedSetCount = loggedSets.size,
        activeRestSeconds = activeRestSeconds,
    )
    return ActiveWorkoutExerciseUiState(
        loggedSets = loggedSets,
        activeRestSeconds = activeRestSeconds,
        draft = draft,
        draftErrors = uiState.draftErrors[key] ?: SetInputFieldErrors(),
        isSessionFinished = uiState.debrief != null,
        isAutoAdvanceTarget = uiState.activeFocusTarget?.exerciseId == key,
        isLogPending = uiState.pendingLoggingExerciseIds.contains(key),
        pendingCorrectionSetId = uiState.pendingCorrectionSetIds[key],
        collapsed = key in uiState.collapsedExerciseIds,
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
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = restTimerCardContentDescription(restTimerSeconds, totalSeconds)
            },
        accent = MaterialTheme.trainIqColors.amber,
        elevated = true,
    ) {
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
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onAdjust(-30) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Remove, contentDescription = restTimerAdjustContentDescription(-30))
                }
                IconButton(onClick = { onAdjust(30) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = restTimerAdjustContentDescription(30))
                }
                IconButton(onClick = onRestart, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Replay, contentDescription = restTimerRestartContentDescription())
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onSkip, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = restTimerSkipContentDescription())
                }
            }
        }
    }
}

@Composable
private fun ActiveExerciseCard(
    plan: WorkoutExercisePlan,
    loggedSets: List<LoggedSet>,
    activeRestSeconds: Int,
    suggestion: ProgressionSuggestion?,
    draft: SetInputDraft,
    draftErrors: SetInputFieldErrors,
    isSessionFinished: Boolean,
    isAutoAdvanceTarget: Boolean,
    isLogPending: Boolean,
    pendingCorrectionSetId: Long?,
    collapsed: Boolean,
    onOpenHistory: () -> Unit,
    onDraftChange: (SetInputDraft) -> Unit,
    onSetTypeChange: (Long, SetType) -> Unit,
    onEditSet: (Long) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onRelogSet: (Long) -> Unit,
    onToggleCollapsed: () -> Unit,
    onCopyLastSet: () -> Unit,
    onLogSet: () -> Unit,
    onLogSameAgain: () -> Unit,
    onAdjustExerciseRest: (Int) -> Unit,
    onReplaceExercise: () -> Unit,
    onRemoveExercise: () -> Unit,
) {
    var activeSetTargetDelta by rememberSaveable(plan.id) { mutableIntStateOf(0) }
    val plannedSetCount = plan.plannedSetCount()
    val activeSetTargetCount = (plannedSetCount + activeSetTargetDelta).coerceAtLeast(loggedSets.size)
    val hasPendingCorrection = pendingCorrectionSetId != null && loggedSets.any { it.id == pendingCorrectionSetId }
    val showLogger = shouldShowActiveSetLogger(
        isSessionFinished = isSessionFinished,
        loggedSetCount = loggedSets.size,
        activeSetTargetCount = activeSetTargetCount,
        hasPendingCorrection = hasPendingCorrection,
    )
    val targetWeight = draft.weight.toFloatOrNull() ?: suggestion?.suggestedWeightKg?.toFloat()
    var activeInputIndex by rememberSaveable(plan.id) { mutableIntStateOf(-1) }
    val platePlan = remember(targetWeight) {
        targetWeight?.let { StrengthCalculator.calculatePlates(it) }.orEmpty()
    }
    val liveOneRepMax by remember(draft) {
        derivedStateOf {
            val weight = draft.weight.replace(',', '.').toDoubleOrNull() ?: 0.0
            val reps = draft.reps.toIntOrNull() ?: 0
            if (weight > 0.0 && reps > 0) StrengthCalculator.estimateOneRepMax(weight, reps) else null
        }
    }
    var menuExpanded by remember(plan.id) { mutableStateOf(false) }
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        accent = when {
            isAutoAdvanceTarget -> MaterialTheme.colorScheme.secondary
            loggedSets.size >= plannedSetCount -> MaterialTheme.trainIqColors.mint
            else -> MaterialTheme.colorScheme.primary
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Open geschiedenis voor ${plan.exercise.name}" }
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Open geschiedenis",
                            onClick = onOpenHistory,
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        plan.exercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${loggedSets.size}/$plannedSetCount sets - ${plan.repRange} herh.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.trainIqColors.mutedText,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onToggleCollapsed) {
                        Icon(
                            if (collapsed) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                            contentDescription = if (collapsed) "Open oefening" else "Klap oefening in",
                        )
                    }
                    ActiveExerciseRestControl(
                        restSeconds = activeRestSeconds,
                        onDecrease = { onAdjustExerciseRest(-30) },
                        onIncrease = { onAdjustExerciseRest(30) },
                    )
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Actieve oefening acties")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Set toevoegen") },
                                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    activeSetTargetDelta += 1
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Geschiedenis") },
                                onClick = {
                                    menuExpanded = false
                                    onOpenHistory()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(activeExerciseReplaceLabel()) },
                                leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onReplaceExercise()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(activeExerciseDeleteLabel()) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onRemoveExercise()
                                },
                            )
                        }
                    }
                }
            }
            suggestion?.let { CompactPreviousPerformance(it) } ?: PlannedPerformanceFallback(plan)
            val visibleSetRows = visibleActiveSetRows(
                plannedSetCount = activeSetTargetCount,
                loggedSetCount = loggedSets.size,
                manualExtraSetRequested = false,
            )
            val plannedSets = remember(plan.sets) {
                plan.sets.sortedWith(compareBy<RoutineSet> { it.orderIndex }.thenBy { it.id })
            }
            repeat(visibleSetRows) { index ->
                val loggedSetForRow = loggedSets.getOrNull(index)
                val rowIsActiveInput = showLogger &&
                    !collapsed &&
                    (
                        pendingCorrectionSetId?.let { loggedSetForRow?.id == it } == true ||
                            (loggedSetForRow == null && (index == loggedSets.size || activeInputIndex == index))
                    )
                SetRow(
                    index = index + 1,
                    repRange = plan.repRange,
                    plannedSet = plannedSets.getOrNull(index),
                    loggedSet = loggedSetForRow,
                    activeRestSeconds = activeRestSeconds,
                    isCurrent = index == loggedSets.size || pendingCorrectionSetId?.let { loggedSets.getOrNull(index)?.id == it } == true,
                    isCorrecting = pendingCorrectionSetId?.let { loggedSets.getOrNull(index)?.id == it } == true,
                    isInputExpanded = rowIsActiveInput,
                    draft = draft,
                    draftErrors = draftErrors,
                    lastSession = suggestion?.toLastSessionDraft(),
                    onSetTypeSelected = { type ->
                        if (rowIsActiveInput) {
                            onDraftChange(draft.copy(setType = type))
                        } else {
                            loggedSetForRow?.let { set ->
                                onSetTypeChange(set.id, type)
                            }
                        }
                    },
                    onDraftChange = onDraftChange,
                    onSubmit = onLogSet,
                    onEdit = { loggedSetForRow?.let { onEditSet(it.id) } },
                    onDelete = { loggedSetForRow?.let { onDeleteSet(it.id) } },
                    canRemovePlanned = !isSessionFinished &&
                        loggedSets.getOrNull(index) == null &&
                        activeSetTargetCount > loggedSets.size,
                    onRemovePlanned = { activeSetTargetDelta -= 1 },
                    onRelog = { loggedSetForRow?.let { onRelogSet(it.id) } },
                    onActivate = {
                        if (loggedSetForRow == null && showLogger && !collapsed) activeInputIndex = index
                    },
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
            if (showLogger) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                liveOneRepMax?.let { oneRepMax ->
                    Text(
                        "Geschatte 1RM: ${formatWeight(oneRepMax)} kg",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val primaryLabel = activeSetLogButtonLabel(
                        isLogPending = isLogPending,
                        hasPendingCorrection = hasPendingCorrection,
                        loggedSetCount = loggedSets.size,
                        plannedSetCount = plannedSetCount,
                    )
                    when (activeSetActionLayoutForWidth(maxWidth)) {
                        ActiveSetActionLayout.Stacked -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    SecondaryActionButton(
                                        onClick = onCopyLastSet,
                                        enabled = loggedSets.isNotEmpty(),
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = copyPreviousSetContentDescription())
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Vorige", maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                    }
                                    TextButton(
                                        onClick = onLogSameAgain,
                                        enabled = loggedSets.isNotEmpty() && !isLogPending,
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp),
                                    ) {
                                        Text("Zelfde opnieuw", maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                    }
                                }
                                PrimaryActionButton(
                                    onClick = onLogSet,
                                    enabled = !isLogPending,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp),
                                ) {
                                    Text(primaryLabel, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        ActiveSetActionLayout.Wrapped -> {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                maxItemsInEachRow = 3,
                            ) {
                                SecondaryActionButton(
                                    onClick = onCopyLastSet,
                                    enabled = loggedSets.isNotEmpty(),
                                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = copyPreviousSetContentDescription())
                                }
                                TextButton(
                                    onClick = onLogSameAgain,
                                    enabled = loggedSets.isNotEmpty() && !isLogPending,
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                ) {
                                    Text("Zelfde opnieuw", maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                }
                                PrimaryActionButton(
                                    onClick = onLogSet,
                                    enabled = !isLogPending,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp),
                                ) {
                                    Text(primaryLabel, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveExerciseRestControl(
    restSeconds: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 36.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = activeExerciseRestControlDescription(restSeconds)
                }
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Remove, contentDescription = restTimerAdjustContentDescription(-30))
            }
            Text(
                "${restSeconds}s",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
            IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = restTimerAdjustContentDescription(30))
            }
        }
    }
}

private data class RoutineOverlapProposal(
    val primary: WorkoutRoutine,
    val secondary: WorkoutRoutine,
    val sharedExercises: Int,
)

@Composable
private fun RoutineOverlapProposalCard(
    proposal: RoutineOverlapProposal,
    onOpenPrimary: () -> Unit,
    onOpenSecondary: () -> Unit,
) {
    AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.trainIqColors.amber) {
        Text("Mogelijke dubbele routine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "${proposal.primary.name} en ${proposal.secondary.name} delen ${proposal.sharedExercises} oefeningen. Controleer beide routines voordat je er een samenvoegt of verwijdert.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.trainIqColors.mutedText,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onOpenPrimary, modifier = Modifier.weight(1f)) { Text("Beste optie") }
            OutlinedButton(onClick = onOpenSecondary, modifier = Modifier.weight(1f)) { Text("Vergelijk") }
        }
    }
}

private fun List<WorkoutRoutine>.bestRoutineOverlapProposal(): RoutineOverlapProposal? {
    if (size < 2) return null
    val pairs = flatMapIndexed { index, first ->
        drop(index + 1).mapNotNull { second ->
            val firstExercises = first.exerciseNameKeys()
            val secondExercises = second.exerciseNameKeys()
            val shared = firstExercises.intersect(secondExercises).size
            val smallest = minOf(firstExercises.size, secondExercises.size).coerceAtLeast(1)
            if (shared >= 2 && shared.toDouble() / smallest >= 0.5) {
                val primary = listOf(first, second).maxBy { it.routineCompletenessScore() }
                val secondary = if (primary.id == first.id) second else first
                RoutineOverlapProposal(primary, secondary, shared)
            } else {
                null
            }
        }
    }
    return pairs.maxByOrNull { it.sharedExercises }
}

private fun WorkoutRoutine.exerciseNameKeys(): Set<String> =
    days.flatMap { day -> day.exercises.map { it.exercise.name.normalizedWorkoutExerciseName() } }
        .filter { it.isNotBlank() }
        .toSet()

private fun WorkoutRoutine.routineCompletenessScore(): Int =
    days.size * 10 + days.sumOf { it.exercises.size }

private fun String.normalizedWorkoutExerciseName(): String =
    lowercase(Locale.getDefault())
        .replace("barbell", "halterstang")
        .replace("dumbbells", "dumbbell")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

private fun List<ExerciseLibraryItem>.filteredByExerciseLibraryQuery(
    query: String,
    filterKey: String,
): List<ExerciseLibraryItem> {
    val normalized = query.trim().lowercase(Locale.getDefault())
    return asSequence()
        .filter { item ->
            when (filterKey) {
                ExerciseLibraryFilter.Scored.key -> item.score > 0.0 && item.completedSessions > 0
                ExerciseLibraryFilter.Recent.key -> item.lastPerformedAt != null
                ExerciseLibraryFilter.Untrained.key -> item.completedSessions == 0
                else -> true
            }
        }
        .filter { item ->
            if (normalized.isBlank()) {
                true
            } else {
                val exercise = item.exercise
                exercise.name.lowercase(Locale.getDefault()).contains(normalized) ||
                    exercise.muscleGroup.lowercase(Locale.getDefault()).contains(normalized) ||
                    exercise.equipment.lowercase(Locale.getDefault()).contains(normalized) ||
                    item.rankLabel.lowercase(Locale.getDefault()).contains(normalized)
            }
        }
        .sortedWith(
            when (filterKey) {
                ExerciseLibraryFilter.Recent.key -> compareByDescending<ExerciseLibraryItem> { it.lastPerformedAt ?: 0L }
                ExerciseLibraryFilter.Scored.key -> compareByDescending { it.score }
                else -> compareBy { it.exercise.name }
            },
        )
        .toList()
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
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SetType.entries.forEach { type ->
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
private fun SetTypePill(
    setType: SetType,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val color = setTypeColor(setType)
    Surface(
        modifier = Modifier
            .defaultMinSize(minWidth = 34.dp, minHeight = 28.dp)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = "Settype wijzigen",
                onClick = onClick,
            ),
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = if (enabled) 0.26f else 0.14f),
        contentColor = color,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(setType.shortCode(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActiveSetInputMetrics(
    layout: RoutineSetMetricLayout,
    draft: SetInputDraft,
    errors: SetInputFieldErrors,
    lastSession: SetInputDraft?,
    onDraftChange: (SetInputDraft) -> Unit,
    onSubmit: () -> Unit,
) {
    val cells = listOf<@Composable (Modifier) -> Unit>(
        { modifier ->
            ActiveSetInputMetricValue(
                label = RepetitionsMetricLabel,
                value = draft.reps,
                suffix = "",
                keyboardType = KeyboardType.Number,
                fallback = lastSession?.reps,
                errorText = errors.reps,
                modifier = modifier,
                onValueChange = { onDraftChange(draft.copy(reps = filterIntegerInput(it))) },
                onSubmit = onSubmit,
            )
        },
        { modifier ->
            ActiveSetInputMetricValue(
                label = "Kg",
                value = draft.weight,
                suffix = "kg",
                keyboardType = KeyboardType.Decimal,
                fallback = lastSession?.weight,
                errorText = errors.weight,
                decimalPlaces = 2,
                modifier = modifier,
                onValueChange = { onDraftChange(draft.copy(weight = filterDecimalInput(it, maxDecimals = 2))) },
                onSubmit = onSubmit,
            )
        },
        { modifier ->
            ActiveSetInputMetricValue(
                label = "Rust",
                value = draft.restSeconds,
                suffix = "s",
                keyboardType = KeyboardType.Number,
                fallback = lastSession?.restSeconds,
                errorText = errors.restSeconds,
                modifier = modifier,
                onValueChange = { onDraftChange(draft.copy(restSeconds = filterIntegerInput(it))) },
                onSubmit = onSubmit,
            )
        },
        { modifier ->
            ActiveSetInputMetricValue(
                label = "RPE",
                value = draft.rpe,
                suffix = "",
                keyboardType = KeyboardType.Decimal,
                fallback = lastSession?.rpe,
                errorText = errors.rpe,
                decimalPlaces = 1,
                modifier = modifier,
                onValueChange = { onDraftChange(draft.copy(rpe = filterDecimalInput(it, maxDecimals = 1))) },
                onSubmit = onSubmit,
            )
        },
    )
    when (layout) {
        RoutineSetMetricLayout.OneRow -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                cells.forEach { cell -> cell(Modifier.weight(1f)) }
            }
        }
        RoutineSetMetricLayout.BalancedGrid -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                cells.chunked(2).forEach { rowCells ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        rowCells.forEach { cell -> cell(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveSetInputMetricValue(
    label: String,
    value: String,
    suffix: String,
    keyboardType: KeyboardType,
    fallback: String?,
    errorText: String?,
    modifier: Modifier = Modifier,
    decimalPlaces: Int = 0,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val content = if (suffix.isBlank()) label else "$label, $suffix"
    val textStyle = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Column(
        modifier = modifier
            .defaultMinSize(minWidth = 52.dp, minHeight = 52.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f), MaterialTheme.shapes.small)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .semantics {
                contentDescription = content
                errorText?.let { error(it) }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (errorText == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        BasicTextField(
            value = value,
            onValueChange = { raw ->
                val filtered = if (keyboardType == KeyboardType.Decimal) {
                    filterDecimalInput(raw, maxDecimals = decimalPlaces)
                } else {
                    filterIntegerInput(raw)
                }
                onValueChange(normalizeActiveSetMetricInput(previousValue = value, filteredInput = filtered))
            },
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onSubmit()
                    focusManager.clearFocus(force = true)
                },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 24.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (value.isBlank()) {
                        Text(
                            fallback.orEmpty(),
                            style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = if (value.isBlank()) Modifier.width(1.dp) else Modifier) {
                            innerTextField()
                        }
                        if (suffix.isNotBlank() && value.isNotBlank()) {
                            Text(
                                suffix,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun SetRow(
    index: Int,
    repRange: String,
    plannedSet: RoutineSet?,
    loggedSet: LoggedSet?,
    activeRestSeconds: Int,
    isCurrent: Boolean,
    isCorrecting: Boolean,
    isInputExpanded: Boolean,
    draft: SetInputDraft?,
    draftErrors: SetInputFieldErrors?,
    lastSession: SetInputDraft?,
    onSetTypeSelected: (SetType) -> Unit,
    onDraftChange: (SetInputDraft) -> Unit,
    onSubmit: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    canRemovePlanned: Boolean,
    onRemovePlanned: () -> Unit,
    onRelog: () -> Unit,
    onActivate: () -> Unit,
) {
    var showDeleteConfirm by remember(index, loggedSet) { mutableStateOf(false) }
    var setTypeMenuExpanded by remember(index, loggedSet?.id, plannedSet?.id) { mutableStateOf(false) }
    val visibleSetType = draft?.takeIf { isInputExpanded }?.setType ?: loggedSet?.setType ?: plannedSet?.setType
    val background = if (isCurrent) {
        MaterialTheme.trainIqColors.amber.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val rpeColor = loggedSet?.let { intensityContainerColor(it.rpe) } ?: background
    val metricCells = activeSetMetricCells(
        repRange = repRange,
        plannedSet = plannedSet,
        loggedSet = loggedSet,
        activeRestSeconds = activeRestSeconds,
    )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rpeColor, MaterialTheme.shapes.medium)
            .clickable(
                enabled = loggedSet == null,
                role = Role.Button,
                onClickLabel = "Set $index invullen",
                onClick = onActivate,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = ActiveSetHeaderMinHeight),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                if (loggedSet?.completed == true && !isCorrecting) {
                    Icon(Icons.Default.Check, contentDescription = "Voltooide set", modifier = Modifier.size(18.dp))
                } else if (isCorrecting) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Set wordt gecorrigeerd", modifier = Modifier.size(18.dp))
                }
            }
            Box {
                SetTypePill(
                    setType = visibleSetType ?: SetType.NORMAL,
                    enabled = loggedSet != null || isInputExpanded,
                    onClick = { setTypeMenuExpanded = true },
                )
                DropdownMenu(
                    expanded = setTypeMenuExpanded,
                    onDismissRequest = { setTypeMenuExpanded = false },
                ) {
                    SetType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label()) },
                            onClick = {
                                setTypeMenuExpanded = false
                                onSetTypeSelected(type)
                            },
                        )
                    }
                }
            }
            Text(
                text = "Set $index",
                modifier = Modifier
                    .weight(1f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (loggedSet != null) {
                IconButton(onClick = onRelog, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Replay, contentDescription = relogSetContentDescription())
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Gelogde set corrigeren")
                }
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Set verwijderen")
                }
            } else if (canRemovePlanned) {
                IconButton(onClick = onRemovePlanned, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Geplande set uit deze training verwijderen")
                }
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (isInputExpanded && draft != null && draftErrors != null) {
                ActiveSetInputMetrics(
                    layout = routineSetMetricLayoutForWidth(maxWidth),
                    draft = draft,
                    errors = draftErrors,
                    lastSession = lastSession,
                    onDraftChange = onDraftChange,
                    onSubmit = onSubmit,
                )
            } else when (routineSetMetricLayoutForWidth(maxWidth)) {
                RoutineSetMetricLayout.OneRow -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        metricCells.forEach { cell ->
                            RoutineSetMetricValue(cell = cell, modifier = Modifier.weight(1f))
                        }
                    }
                }
                RoutineSetMetricLayout.BalancedGrid -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        metricCells.chunked(2).forEach { rowCells ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                rowCells.forEach { cell ->
                                    RoutineSetMetricValue(
                                        cell = cell,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
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
    val target = plannedPerformanceTargetText(plan.targetWeightKg, plan.targetRpe)
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
    val summary = remember(uiState.loggedSetsThisSession, workout.exercises) {
        val loggedSets = uiState.loggedSetsThisSession.values.flatten()
        val targetSets = workout.exercises.sumOf { it.plannedSetCount() }
        WorkoutSessionUiSummary(
            loggedSetCount = loggedSets.size,
            targetSets = targetSets,
            remainingSets = (targetSets - loggedSets.size).coerceAtLeast(0),
            volume = loggedSets.sumOf { it.weight * it.reps },
            averageRpe = loggedSets.map { it.rpe }.filter { it > 0.0 }.average().takeIf { !it.isNaN() },
        )
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                StatusMetric("Sets", "${summary.loggedSetCount}/${summary.targetSets}", Modifier.weight(1f))
                StatusMetric(
                    activeWorkoutRestStatusLabel(),
                    if (uiState.restTimerSeconds > 0) formatTimer(uiState.restTimerSeconds) else "Klaar",
                    Modifier.weight(1f),
                )
                StatusMetric("Volume", "${summary.volume.toInt()} kg", Modifier.weight(1f))
            }
            LinearProgressIndicator(
                progress = { if (summary.targetSets > 0) (summary.loggedSetCount / summary.targetSets.toFloat()).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "${summary.remainingSets} sets resterend${summary.averageRpe?.let { " - gemiddelde RPE ${formatWeight(it)}" }.orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = statusMetricContentDescription(label, value)
        },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
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
    val summary = remember(uiState.loggedSetsThisSession) {
        val sets = uiState.loggedSetsThisSession.values.flatten()
        WorkoutDebriefUiSummary(
            setCount = sets.size,
            volume = sets.sumOf { it.weight * it.reps },
            topSet = sets.maxByOrNull { it.weight * it.reps },
            highestRpe = sets.maxOfOrNull { it.rpe } ?: 0.0,
        )
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Trainingssamenvatting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusMetric("Volume", "${summary.volume.toInt()} kg")
                StatusMetric("Sets", summary.setCount.toString())
                StatusMetric("Hoogste RPE", if (summary.highestRpe > 0.0) formatWeight(summary.highestRpe) else "-")
            }
            summary.topSet?.let {
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
            Text("Intensiteitssignaal: ${result.intensitySignal}", color = intensityContentColor(result.intensitySignal))
            LinearProgressIndicator(
                progress = { result.recoveryScore.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Herstelscore: ${result.recoveryScore}/100", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private data class WorkoutSessionUiSummary(
    val loggedSetCount: Int,
    val targetSets: Int,
    val remainingSets: Int,
    val volume: Double,
    val averageRpe: Double?,
)

private data class WorkoutDebriefUiSummary(
    val setCount: Int,
    val volume: Double,
    val topSet: LoggedSet?,
    val highestRpe: Double,
)

private fun previousWeight(suggestion: ProgressionSuggestion): Double = when (suggestion.readinessSignal) {
    ReadinessLevel.INCREASE -> (suggestion.suggestedWeightKg - 2.5).coerceAtLeast(0.0)
    ReadinessLevel.DELOAD -> if (suggestion.suggestedWeightKg == 0.0) 0.0 else suggestion.suggestedWeightKg / 0.9
    ReadinessLevel.PLATEAU -> suggestion.suggestedWeightKg
    ReadinessLevel.MAINTAIN -> suggestion.suggestedWeightKg
}

private fun displayRepTarget(repRange: String): String = repRange.substringAfter('-', repRange)

private fun routineExerciseCount(routine: WorkoutRoutine): Int = routine.days.sumOf { it.exercises.size }

private fun routineSetCount(routine: WorkoutRoutine): Int =
    routine.days.sumOf { day -> day.exercises.sumOf { it.plannedSetCount() } }

private fun routineEstimatedMinutes(routine: WorkoutRoutine): Int =
    routine.days.firstOrNull()?.let(::dayEstimatedMinutes) ?: 0

private fun routineFocusLabel(routine: WorkoutRoutine): String =
    routine.days.flatMap { it.exercises }.focusLabel()

internal fun activeRoutineStartLabel(dayName: String): String = "Training starten"

internal fun activeRoutineSetupLabel(): String = "Routine inrichten"

internal fun initialRoutineDetailTab(routine: WorkoutRoutine): String =
    if (routine.firstStartableDay() == null) "sessions" else "info"

internal fun activeRoutineNeedsExerciseText(): String =
    "Tik op Routine inrichten en voeg eerst een oefening toe voordat je start."

internal fun defaultWorkoutDayName(): String = "Sessie"

internal fun routineEmptyDescriptionText(): String = "Nog geen beschrijving."

internal fun activeExerciseReplaceLabel(): String = "Oefening vervangen"

internal fun activeExerciseDeleteLabel(): String = "Oefening verwijderen"

internal fun restTimerFinishedMessage(): String = "Rusttijd klaar - volgende set klaar"

internal fun statusMetricContentDescription(label: String, value: String): String = "$label: $value"

internal fun activeWorkoutRestStatusLabel(): String = "Rust"

internal fun restTimerAdjustContentDescription(deltaSeconds: Int): String =
    if (deltaSeconds < 0) {
        "Rusttimer ${-deltaSeconds} seconden korter"
    } else {
        "Rusttimer $deltaSeconds seconden langer"
    }

internal fun restTimerRestartContentDescription(): String = "Rusttimer opnieuw starten"

internal fun restTimerSkipLabel(): String = "Overslaan"

internal fun restTimerSkipContentDescription(): String = "Rusttimer overslaan"

internal fun relogSetContentDescription(): String = "Set opnieuw loggen"

internal fun activeExerciseRestControlDescription(restSeconds: Int): String =
    "Rust voor deze oefening: ${restSeconds}s"

internal fun restTimerCardContentDescription(restTimerSeconds: Int, totalSeconds: Int): String {
    val status = if (restTimerSeconds <= 15) "bijna klaar" else "herstel"
    val total = totalSeconds.takeIf { it > 0 }?.let { ", totaal ${formatTimer(it)}" }.orEmpty()
    return "Rusttimer: ${formatTimer(restTimerSeconds)} resterend, $status$total."
}

internal fun exerciseHistorySubtitleText(muscleGroup: String?, equipment: String?): String =
    listOfNotNull(
        muscleGroup?.takeIf { it.isNotBlank() }?.toDutchMuscleGroupLabel(),
        equipment?.takeIf { it.isNotBlank() }?.toDutchEquipmentLabel(),
    ).joinToString(" • ")

internal fun exerciseHistorySessionMetaText(duration: String, completedSets: Int): String =
    "$duration • $completedSets sets"

internal fun copyPreviousSetContentDescription(): String = "Vorige set kopiëren"

internal fun cleanCompletionBulletText(item: String): String =
    item.trim().trimStart('-', '•', '*').trim()

internal fun plannedPerformanceTargetText(targetWeightKg: Double, targetRpe: Double): String =
    buildList {
        if (targetWeightKg > 0.0) add("${formatWeight(targetWeightKg)} kg")
        if (targetRpe > 0.0) add("RPE ${formatWeight(targetRpe)}")
    }.joinToString(" - ")

internal fun exerciseSummaryMetaText(
    setCount: Int,
    repRange: String,
    restSeconds: Int,
    rpe: String,
    supersetGroupId: Long?,
): String = buildList {
    add("$setCount sets")
    add("$repRange herh.")
    add("${restSeconds}s rust")
    add(rpe)
    supersetGroupId?.let { add("Superset $it") }
}.joinToString(" - ")

internal fun displayWorkoutDayName(dayName: String): String {
    val trimmed = dayName.trim()
    val sessionNumber = trimmed.removePrefix("Session ").takeIf { it != trimmed && it.all(Char::isDigit) }
    return sessionNumber?.let { "Sessie $it" } ?: dayName
}

internal fun routineMetadataText(
    focus: String,
    exerciseCount: Int,
    setCount: Int,
    estimatedMinutes: Int,
): String = "Focus: ${focus.toDutchMuscleGroupLabel()} - $exerciseCount ${exerciseText(exerciseCount)} - $setCount sets - ca. $estimatedMinutes min"

internal fun routineDetailMetadataText(
    focus: String,
    exerciseCount: Int,
    estimatedMinutes: Int,
): String = "Focus: ${focus.toDutchMuscleGroupLabel()} - $exerciseCount ${exerciseText(exerciseCount)} - ca. $estimatedMinutes min"

internal fun routineSessionMetadataText(
    focus: String,
    exerciseCount: Int,
    estimatedMinutes: Int,
): String = "Focus: ${focus.toDutchMuscleGroupLabel()} - $exerciseCount ${exerciseText(exerciseCount)} - ca. $estimatedMinutes min"

private fun exerciseText(count: Int): String =
    if (count == 1) "oefening" else "oefeningen"

private fun String.toDutchMuscleGroupLabel(): String {
    val normalized = trim()
    return when (normalized.lowercase(Locale.US)) {
        "shoulders", "shoulder" -> "Schouders"
        "chest", "pecs" -> "Borst"
        "back", "lats" -> "Rug"
        "legs", "quads", "hamstrings" -> "Benen"
        "arms", "biceps", "triceps" -> "Armen"
        "glutes" -> "Billen"
        "core", "abs" -> "Core"
        "full body", "whole body" -> "Hele lichaam"
        else -> normalized.ifBlank { "Hele lichaam" }
    }
}

private fun String.toDutchEquipmentLabel(): String {
    val normalized = trim()
    return when (normalized.lowercase(Locale.US)) {
        "barbell" -> "Halterstang"
        "bodyweight", "body weight" -> "Lichaamsgewicht"
        "cable" -> "Kabel"
        "machine" -> "Machine"
        "dumbbell", "dumbbells" -> "Dumbbells"
        "kettlebell", "kettlebells" -> "Kettlebells"
        "ez bar", "ez-bar" -> "EZ-stang"
        else -> normalized.ifBlank { "Materiaal onbekend" }
    }
}

internal fun discardActiveWorkoutBodyText(): String =
    "Gelogde sets en ingevulde waarden voor deze actieve sessie worden verwijderd."

internal fun activeLoggedSetCountText(count: Int): String =
    "$count ${if (count == 1) "set" else "sets"} gelogd"

internal fun activeWorkoutBottomBarStatusText(restTimerSeconds: Int): String =
    if (restTimerSeconds > 0) "Rust ${formatTimer(restTimerSeconds)}" else "Klaar voor volgende set"

private const val MaxDisplayedActiveWorkoutSeconds: Long = 4 * 60 * 60

internal fun activeWorkoutElapsedSeconds(startedAt: Long, now: Long): Long {
    if (startedAt <= 0L || now <= startedAt) return 0L
    return ((now - startedAt) / 1_000L).coerceIn(0L, MaxDisplayedActiveWorkoutSeconds)
}

internal fun activeExerciseRestSeconds(baseRestSeconds: Int, overrideRestSeconds: Int?): Int =
    (overrideRestSeconds ?: baseRestSeconds).coerceIn(0, MaxRestSeconds)

internal fun activeWorkoutFinishContentDescription(enabled: Boolean): String =
    if (enabled) "Training afronden" else "Training afronden niet beschikbaar"

internal fun activeWorkoutBottomBarContentDescription(
    uiState: ActiveWorkoutUiState,
    restTimerSeconds: Int,
): String =
    "${activeWorkoutBottomBarStatusText(restTimerSeconds)}. " +
        "${activeLoggedSetCountText(uiState.visibleLoggedSetCount)}. " +
        activeWorkoutFinishContentDescription(uiState.workout != null)

internal fun activeWorkoutStickyStatusContentDescription(
    uiState: ActiveWorkoutUiState,
    restTimerSeconds: Int,
): String {
    val restText = if (restTimerSeconds > 0) formatTimer(restTimerSeconds) else "klaar"
    return "Actieve training: tijd ${formatTimer(uiState.elapsedSeconds.toInt())}, " +
        "sets ${uiState.completedSets} van ${uiState.targetSets}, " +
        "volume ${uiState.totalVolume.toInt()} kg, rust $restText."
}

internal val ActiveWorkoutUiState.visibleLoggedSetCount: Int
    get() = loggedSetsThisSession.values.sumOf { it.size }

internal fun activeSetTitleText(index: Int, setType: SetType?): String =
    setType?.let { "Set $index - ${it.label()}" } ?: "Set $index"

internal fun activeSetTypeCycleContentDescription(index: Int, currentType: SetType): String =
    "Set $index type ${currentType.label()}, wijzig naar ${currentType.next().label()}"

internal fun WorkoutRoutine.firstStartableDay(): WorkoutDay? =
    days.firstOrNull { it.exercises.isNotEmpty() }

internal fun resolveSelectedRoutineId(selectedRoutineId: Long?, routines: List<WorkoutRoutine>): Long? =
    selectedRoutineId?.takeIf { id -> routines.any { it.id == id } }

internal fun workoutOverviewListKeys(
    routines: List<WorkoutRoutine>,
    exercises: List<Exercise>,
    historySessionIds: List<Long>,
    hasMessage: Boolean,
): List<String> = buildList {
    add("workout:header")
    if (hasMessage) add("workout:message")
    add("workout:routine-creation")
    add("workout:active-routine")
    add("workout:routines-section")
    if (routines.isEmpty()) {
        add("workout:routines-empty")
    } else {
        routines.forEach { add(workoutRoutineListKey(it.id)) }
    }
    add("workout:exercise-library-section")
    if (exercises.isEmpty()) {
        add("workout:exercise-library-empty")
    } else {
        exercises.forEach { add(workoutExerciseLibraryListKey(it.id)) }
    }
    add("workout:history-section")
    if (historySessionIds.isEmpty()) {
        add("workout:history-empty")
    } else {
        historySessionIds.forEach { add(workoutHistoryListKey(it)) }
    }
}

private fun workoutRoutineListKey(routineId: Long): String = "workout:routine:$routineId"

private fun workoutExerciseLibraryListKey(exerciseId: Long): String = "workout:exercise:$exerciseId"

private fun workoutHistoryListKey(sessionId: Long): String = "workout:history:$sessionId"

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
    return groups.take(2).joinToString(" + ").ifBlank { "Hele lichaam" }
}

private fun String.normalizedDecimal(): String = trim().replace(',', '.')

internal fun filterDecimalInput(input: String, maxDecimals: Int): String {
    val decimals = maxDecimals.coerceAtLeast(0)
    val builder = StringBuilder()
    var hasSeparator = false
    var decimalCount = 0
    input.forEach { char ->
        when {
            char.isDigit() && !hasSeparator -> builder.append(char)
            char.isDigit() && decimalCount < decimals -> {
                builder.append(char)
                decimalCount++
            }
            (char == ',' || char == '.') && !hasSeparator -> {
                builder.append('.')
                hasSeparator = true
            }
        }
    }
    return builder.toString()
}

internal fun filterIntegerInput(input: String): String =
    input.filter { it.isDigit() }

internal fun normalizeActiveSetMetricInput(previousValue: String, filteredInput: String): String {
    if (previousValue != "0" || filteredInput.length <= 1 || filteredInput == "0") return filteredInput
    return when {
        filteredInput.startsWith("0.") -> filteredInput
        filteredInput.startsWith("0") -> filteredInput.dropWhile { it == '0' }.ifBlank { "0" }
        filteredInput.endsWith("0") -> filteredInput.dropLast(1).ifBlank { "0" }
        else -> filteredInput
    }
}

internal fun shouldDismissExercisePickerFromHandleDrag(
    verticalDragPx: Float,
    thresholdPx: Float,
): Boolean = verticalDragPx >= thresholdPx

internal fun shouldDismissSetEditorFromHandleDrag(
    verticalDragPx: Float,
    thresholdPx: Float,
): Boolean = verticalDragPx >= thresholdPx

internal sealed interface SetLogValidationResult {
    data class Valid(val weight: Double, val reps: Int, val restSeconds: Int, val rpe: Double) : SetLogValidationResult
    data class Invalid(
        val message: String,
        val fieldErrors: SetInputFieldErrors = SetInputFieldErrors(),
    ) : SetLogValidationResult
}

internal sealed interface SetLogStartResult {
    data class Started(val pendingExerciseIds: Set<Long>) : SetLogStartResult
    data object AlreadyPending : SetLogStartResult
}

internal fun tryStartSetLog(pendingExerciseIds: Set<Long>, exerciseId: Long): SetLogStartResult =
    if (exerciseId in pendingExerciseIds) {
        SetLogStartResult.AlreadyPending
    } else {
        SetLogStartResult.Started(pendingExerciseIds + exerciseId)
    }

internal fun finishSetLog(pendingExerciseIds: Set<Long>, exerciseId: Long): Set<Long> =
    pendingExerciseIds - exerciseId

internal fun visibleActiveSetRows(
    plannedSetCount: Int,
    loggedSetCount: Int,
    manualExtraSetRequested: Boolean,
): Int =
    maxOf(plannedSetCount, loggedSetCount + if (manualExtraSetRequested) 1 else 0)

internal fun shouldShowActiveSetLogger(
    isSessionFinished: Boolean,
    loggedSetCount: Int,
    activeSetTargetCount: Int,
    hasPendingCorrection: Boolean,
): Boolean =
    !isSessionFinished && (hasPendingCorrection || loggedSetCount < activeSetTargetCount)

internal fun activeSetLogButtonLabel(
    isLogPending: Boolean,
    hasPendingCorrection: Boolean,
    loggedSetCount: Int,
    plannedSetCount: Int,
): String = when {
    isLogPending -> "Opslaan..."
    hasPendingCorrection -> "Wijzig loggen"
    loggedSetCount >= plannedSetCount -> "Extra set loggen"
    else -> "Set loggen"
}

internal fun validateSetInput(draft: SetInputDraft): SetLogValidationResult {
    val parsedWeight = draft.weight.normalizedDecimal().toDoubleOrNull()
    val parsedReps = draft.reps.trim().toIntOrNull()
    val restInput = draft.restSeconds.trim()
    val parsedRestSeconds = if (restInput.isBlank()) 0 else restInput.toIntOrNull()
    val rpeInput = draft.rpe.normalizedDecimal()
    val parsedRpe = if (rpeInput.isBlank()) 0.0 else rpeInput.toDoubleOrNull()
    if (parsedWeight == null || !parsedWeight.isFinite() || parsedWeight < 0.0 || parsedWeight > MaxWeightKg) {
        val message = "Voer een gewicht tussen 0 en ${MaxWeightKg.toInt()} kg in."
        return SetLogValidationResult.Invalid(message, SetInputFieldErrors(weight = message))
    }
    if (parsedReps == null || parsedReps <= 0 || parsedReps > MaxReps) {
        val message = "Voer reps tussen 1 en $MaxReps in."
        return SetLogValidationResult.Invalid(message, SetInputFieldErrors(reps = message))
    }
    if (parsedRestSeconds == null || parsedRestSeconds !in 0..MaxRestSeconds) {
        val message = "Rust moet tussen 0 en ${MaxRestSeconds}s liggen."
        return SetLogValidationResult.Invalid(message, SetInputFieldErrors(restSeconds = message))
    }
    if (parsedRpe == null || !parsedRpe.isFinite() || parsedRpe !in 0.0..10.0) {
        val message = "RPE moet leeg zijn of tussen 0 en 10 liggen."
        return SetLogValidationResult.Invalid(message, SetInputFieldErrors(rpe = message))
    }
    return SetLogValidationResult.Valid(parsedWeight, parsedReps, parsedRestSeconds, parsedRpe)
}

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
    loggedSetsByPlanKey: Map<Long, List<LoggedSet>>,
    justLoggedPlanKey: Long,
): ActiveWorkoutFocusTarget? {
    val currentIndex = plans.indexOfFirst { it.activeKey == justLoggedPlanKey }
    if (currentIndex < 0) return null
    val current = plans[currentIndex]
    val currentGroupId = current.supersetGroupId
    if (currentGroupId != null) {
        val group = plans.filter { it.supersetGroupId == currentGroupId }
        val currentGroupIndex = group.indexOfFirst { it.activeKey == justLoggedPlanKey }
        val orderedCandidates = group.drop(currentGroupIndex + 1) + group.take(currentGroupIndex + 1)
        orderedCandidates.firstOrNull { plan ->
            loggedSetsByPlanKey[plan.activeKey].orEmpty().size < plan.plannedSetCount()
        }?.let { plan ->
            return ActiveWorkoutFocusTarget(
                exerciseId = plan.activeKey,
                setIndex = loggedSetsByPlanKey[plan.activeKey].orEmpty().size,
            )
        }
    }
    val currentLoggedCount = loggedSetsByPlanKey[justLoggedPlanKey].orEmpty().size
    if (currentLoggedCount < current.plannedSetCount()) {
        return ActiveWorkoutFocusTarget(justLoggedPlanKey, currentLoggedCount)
    }
    val next = plans.drop(currentIndex + 1).firstOrNull { plan ->
        loggedSetsByPlanKey[plan.activeKey].orEmpty().size < plan.plannedSetCount()
    }
    return next?.let {
        ActiveWorkoutFocusTarget(
            exerciseId = it.activeKey,
            setIndex = loggedSetsByPlanKey[it.activeKey].orEmpty().size,
        )
    }
}

private fun formatWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toInt().toString() else "%.1f".format(Locale.US, weight)

private fun formatPlateWeight(weight: Float): String =
    if (weight % 1f == 0f) weight.toInt().toString() else "%.2f".format(Locale.US, weight)

private fun formatTimer(seconds: Int): String = "%d:%02d".format(Locale.US, seconds / 60, seconds % 60)

private fun String.toDutchExperienceLabel(): String = when (this) {
    "beginner" -> "Beginner"
    "advanced" -> "Gevorderd"
    else -> "Gemiddeld"
}

private val historyDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.forLanguageTag("nl-NL"))

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
    restSeconds = restSeconds.takeIf { it > 0 }?.toString().orEmpty(),
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
    return exerciseSummaryMetaText(
        setCount = plan.plannedSetCount(),
        repRange = plan.repRange,
        restSeconds = plan.restSeconds,
        rpe = rpe,
        supersetGroupId = plan.supersetGroupId,
    )
}

private fun WorkoutExercisePlan.plannedSetCount(): Int = sets.size.takeIf { it > 0 } ?: targetSets

private val WorkoutExercisePlan.activeKey: Long
    get() = id

private val ActiveWorkoutSetEntry.activeKey: Long
    get() = sourceWorkoutExerciseId ?: exerciseId

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
    weight = activeSetDraftWeightText(targetWeightKg),
    reps = targetReps.takeIf { it > 0 }?.toString().orEmpty(),
    restSeconds = restSeconds.takeIf { it > 0 }?.toString().orEmpty(),
    rpe = targetRpe.takeIf { it > 0.0 }?.let(::formatWeight).orEmpty(),
    setType = setType,
)

private fun WorkoutExercisePlan.toPlannedDraft() = SetInputDraft(
    weight = activeSetDraftWeightText(targetWeightKg),
    reps = displayRepTarget(repRange).takeIf { it.isNotBlank() && it != repRange } ?: repRange.toIntOrNull()?.toString().orEmpty(),
    restSeconds = restSeconds.takeIf { it > 0 }?.toString().orEmpty(),
    rpe = targetRpe.takeIf { it > 0.0 }?.let(::formatWeight).orEmpty(),
    setType = setType,
)

internal fun activeSetDraftWeightText(targetWeightKg: Double): String =
    targetWeightKg.takeIf { it > 0.0 }?.let(::formatWeight) ?: "0"

internal fun activeSetUiDraft(
    savedDraft: SetInputDraft?,
    plan: WorkoutExercisePlan,
    loggedSetCount: Int,
    activeRestSeconds: Int = plan.plannedRestSeconds(loggedSetCount),
): SetInputDraft {
    val plannedDraft = plan.nextPlannedDraft(loggedSetCount).copy(
        restSeconds = activeRestSeconds.takeIf { it > 0 }?.toString().orEmpty(),
    )
    return savedDraft?.copy(
        weight = savedDraft.weight.ifBlank { plannedDraft.weight },
        reps = savedDraft.reps.ifBlank { plannedDraft.reps },
        restSeconds = savedDraft.restSeconds.ifBlank { plannedDraft.restSeconds },
        rpe = savedDraft.rpe.ifBlank { plannedDraft.rpe },
    ) ?: plannedDraft
}

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

private fun SetType.shortCode(): String = when (this) {
    SetType.NORMAL -> "N"
    SetType.WARM_UP -> "W"
    SetType.DROP_SET -> "D"
    SetType.FAILURE -> "F"
    SetType.BACK_OFF -> "B"
}

@Composable
private fun setTypeColor(setType: SetType): Color = when (setType) {
    SetType.NORMAL -> MaterialTheme.colorScheme.primary
    SetType.WARM_UP -> MaterialTheme.trainIqColors.amber
    SetType.DROP_SET -> MaterialTheme.colorScheme.tertiary
    SetType.FAILURE -> MaterialTheme.colorScheme.error
    SetType.BACK_OFF -> MaterialTheme.trainIqColors.mint
}

private fun SetType.next(): SetType = when (this) {
    SetType.NORMAL -> SetType.WARM_UP
    SetType.WARM_UP -> SetType.DROP_SET
    SetType.DROP_SET -> SetType.FAILURE
    SetType.FAILURE -> SetType.BACK_OFF
    SetType.BACK_OFF -> SetType.NORMAL
}


