package com.trainiq.features.workout

import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.RoutineSet
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.WorkoutDebriefSource
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutExercisePlan
import com.trainiq.domain.model.WorkoutRoutine
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutInputValidationTest {
    @Test
    fun `workout processing uses shimmer loading instead of spinner`() {
        assertEquals(true, workoutProcessingUsesShimmerLoading())
    }

    @Test
    fun `active workout keeps bottom space for snackbar feedback above action bar`() {
        assertTrue(activeWorkoutBottomContentPaddingForFeedback() >= 144.dp)
    }

    @Test
    fun `active set header keeps enough height for set label and type`() {
        assertTrue(activeSetHeaderMinHeightForLabels() >= 52.dp)
    }

    @Test
    fun `critical headers and set labels allow wrapping at large font scale`() {
        val appDesign = testSourceFile("core/ui/AppDesign.kt").readText()
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val appScreenHeaderBody = appDesign.substringAfter("fun AppScreenHeader(").substringBefore("fun AppCard(")
        val routineSetRowBody = workoutScreen.substringAfter("private fun RoutineSetRow(").substringBefore("BoxWithConstraints")

        assertFalse(appScreenHeaderBody.contains("maxLines = 1"))
        assertFalse(appScreenHeaderBody.contains("TextOverflow.Ellipsis"))
        assertFalse(routineSetRowBody.contains("softWrap = false"))
    }

    @Test
    fun `active logged set count text uses Dutch singular and plural`() {
        assertEquals("1 set gelogd", activeLoggedSetCountText(1))
        assertEquals("2 sets gelogd", activeLoggedSetCountText(2))
    }

    @Test
    fun `active workout bottom bar exposes merged accessibility summary`() {
        val state = ActiveWorkoutUiState(
            workout = WorkoutDay(id = 1, routineId = 1, name = "Push", orderIndex = 0, exercises = emptyList()),
            loggedSetsThisSession = mapOf(42L to listOf(sampleLoggedSet(id = 7L))),
        )

        assertEquals("Rust 1:30", activeWorkoutBottomBarStatusText(restTimerSeconds = 90))
        assertEquals("Klaar voor volgende set", activeWorkoutBottomBarStatusText(restTimerSeconds = 0))
        assertEquals("Training afronden", activeWorkoutFinishContentDescription(enabled = true))
        assertEquals("Training afronden niet beschikbaar", activeWorkoutFinishContentDescription(enabled = false))
        assertEquals(
            "Rust 1:30. 1 set gelogd. Training afronden",
            activeWorkoutBottomBarContentDescription(state, restTimerSeconds = 90),
        )
    }

    @Test
    fun `active workout bottom bar counts visible sets during correction`() {
        val state = ActiveWorkoutUiState(
            completedSets = 0,
            loggedSetsThisSession = mapOf(
                42L to listOf(sampleLoggedSet(id = 7L)),
            ),
            pendingCorrectionSetIds = mapOf(42L to 7L),
        )

        assertEquals(1, state.visibleLoggedSetCount)
        assertEquals("1 set gelogd", activeLoggedSetCountText(state.visibleLoggedSetCount))
    }

    @Test
    fun `active workout sticky status exposes merged accessibility summary`() {
        val state = ActiveWorkoutUiState(
            elapsedSeconds = 125L,
            completedSets = 3,
            targetSets = 5,
            totalVolume = 1200.0,
        )

        assertEquals(
            "Actieve training: tijd 2:05, sets 3 van 5, volume 1200 kg, rust 1:15.",
            activeWorkoutStickyStatusContentDescription(state, restTimerSeconds = 75),
        )
        assertEquals(
            "Actieve training: tijd 2:05, sets 3 van 5, volume 1200 kg, rust klaar.",
            activeWorkoutStickyStatusContentDescription(state, restTimerSeconds = 0),
        )
    }

    @Test
    fun `active set title combines number and type in one readable label`() {
        assertEquals("Set 1 - Normaal", activeSetTitleText(1, SetType.NORMAL))
        assertEquals("Set 2", activeSetTitleText(2, null))
    }

    @Test
    fun `routine metadata localizes focus and uses ascii separators to avoid mojibake`() {
        val text = routineMetadataText(focus = "Shoulders", exerciseCount = 1, setCount = 3, estimatedMinutes = 10)

        assertEquals("Focus: Schouders - 1 oefening - 3 sets - ca. 10 min", text)
        assertEquals(false, text.contains("Â"))
    }

    @Test
    fun `active routine start label stays Dutch without echoing raw day name`() {
        assertEquals("Training starten", activeRoutineStartLabel("Session 1"))
    }

    @Test
    fun `active routine without exercises explains the editor action`() {
        assertEquals(
            "Open deze routine hieronder en voeg eerst een trainingsdag met oefening toe voordat je start.",
            activeRoutineNeedsExerciseText(),
        )
    }

    @Test
    fun `empty active routine exposes setup entry in active routine card`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val activeRoutineCard = workoutScreen
            .substringAfter("private fun ActiveRoutineCard(")
            .substringBefore("@Composable\nprivate fun SectionHeader")

        assertTrue(activeRoutineCard.contains("onOpenDetails: (Long) -> Unit"))
        assertTrue(activeRoutineCard.contains("activeRoutineSetupLabel()"))
        assertTrue(activeRoutineCard.contains("onOpenDetails(activeRoutine.id)"))
    }

    @Test
    fun `routine setup label stays Dutch`() {
        assertEquals("Routine inrichten", activeRoutineSetupLabel())
    }

    @Test
    fun `routine overview keeps details action`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val routineCardOverview = workoutScreen
            .substringAfter("if (!detailMode) {")
            .substringBefore("return\n    }")

        assertTrue(routineCardOverview.contains("onOpenDetails"))
        assertTrue(routineCardOverview.contains("Text(\"Details\")"))
    }

    @Test
    fun `blank workout day fallback stays Dutch`() {
        assertEquals("Sessie", defaultWorkoutDayName())
    }

    @Test
    fun `routine empty description copy is consistent`() {
        assertEquals("Nog geen beschrijving.", routineEmptyDescriptionText())
    }

    @Test
    fun `active exercise menu uses Dutch action labels`() {
        assertEquals("Oefening vervangen", activeExerciseReplaceLabel())
        assertEquals("Oefening verwijderen", activeExerciseDeleteLabel())
    }

    @Test
    fun `rest timer finished message is fully Dutch`() {
        assertEquals("Rusttijd klaar - volgende set klaar", restTimerFinishedMessage())
    }

    @Test
    fun `active workout rest status label stays Dutch`() {
        assertEquals("Rust", activeWorkoutRestStatusLabel())
    }

    @Test
    fun `status metrics expose merged accessibility labels`() {
        assertEquals("Rust: 1:30", statusMetricContentDescription("Rust", "1:30"))
        assertEquals("Volume: 1200 kg", statusMetricContentDescription("Volume", "1200 kg"))
    }

    @Test
    fun `rest timer icon controls expose contextual Dutch accessibility labels`() {
        assertEquals("Rusttimer 30 seconden korter", restTimerAdjustContentDescription(-30))
        assertEquals("Rusttimer 30 seconden langer", restTimerAdjustContentDescription(30))
        assertEquals("Rusttimer opnieuw starten", restTimerRestartContentDescription())
        assertEquals("Rusttimer overslaan", restTimerSkipContentDescription())
        assertEquals("Overslaan", restTimerSkipLabel())
    }

    @Test
    fun `logged set relog action exposes compact accessibility label`() {
        assertEquals("Set opnieuw loggen", relogSetContentDescription())
    }

    @Test
    fun `active exercise rest control exposes compact accessibility label`() {
        assertEquals("Rust voor deze oefening: 120s", activeExerciseRestControlDescription(120))
    }

    @Test
    fun `rest timer card exposes merged accessibility summary`() {
        assertEquals(
            "Rusttimer: 1:15 resterend, herstel, totaal 2:30.",
            restTimerCardContentDescription(restTimerSeconds = 75, totalSeconds = 150),
        )
        assertEquals(
            "Rusttimer: 0:10 resterend, bijna klaar.",
            restTimerCardContentDescription(restTimerSeconds = 10, totalSeconds = 0),
        )
    }

    @Test
    fun `active workout elapsed time is capped for stale restored sessions`() {
        assertEquals(120L, activeWorkoutElapsedSeconds(startedAt = 1_000L, now = 121_000L))
        assertEquals(14_400L, activeWorkoutElapsedSeconds(startedAt = 1_000L, now = 90_000_000L))
        assertEquals(0L, activeWorkoutElapsedSeconds(startedAt = 0L, now = 90_000_000L))
    }

    @Test
    fun `completion source chip distinguishes local fallback from gemini`() {
        assertEquals("Gemini 2.5 Flash", workoutDebriefSourceChipLabel(WorkoutDebriefSource.GEMINI_2_5_FLASH))
        assertEquals("Lokale fallback", workoutDebriefSourceChipLabel(WorkoutDebriefSource.LOCAL_FALLBACK))
    }

    @Test
    fun `history metadata uses a real bullet separator`() {
        assertEquals("Borst • Halterstang", exerciseHistorySubtitleText("Chest", "Barbell"))
        assertEquals("12:30 • 4 sets", exerciseHistorySessionMetaText("12:30", 4))
    }

    @Test
    fun `copy previous set accessibility label is readable Dutch`() {
        assertEquals("Vorige set kopiëren", copyPreviousSetContentDescription())
    }

    @Test
    fun `completion bullets strip raw markdown markers`() {
        assertEquals("Volume bleef stabiel.", cleanCompletionBulletText("- Volume bleef stabiel."))
        assertEquals("Herstel bewaken.", cleanCompletionBulletText("• Herstel bewaken."))
    }

    @Test
    fun `planned performance target uses safe separator text`() {
        assertEquals("80 kg - RPE 8", plannedPerformanceTargetText(80.0, 8.0))
        assertEquals(false, plannedPerformanceTargetText(80.0, 8.0).contains("Â"))
    }

    @Test
    fun `exercise summary metadata avoids mojibake separators`() {
        val text = exerciseSummaryMetaText(
            setCount = 3,
            repRange = "8-10",
            restSeconds = 90,
            rpe = "RPE 8",
            supersetGroupId = 12,
        )

        assertEquals("3 sets - 8-10 reps - 90s rust - RPE 8 - Superset 12", text)
        assertEquals(false, text.contains("Â"))
    }

    @Test
    fun `active workout header localizes generated session names`() {
        assertEquals("Sessie 1", displayWorkoutDayName("Session 1"))
        assertEquals("Push", displayWorkoutDayName("Push"))
    }

    @Test
    fun `routine session metadata localizes common English muscle groups`() {
        val text = routineSessionMetadataText(focus = "Chest", exerciseCount = 1, estimatedMinutes = 15)

        assertEquals("Focus: Borst - 1 oefening - ca. 15 min", text)
        assertEquals(false, text.contains("±"))
    }

    @Test
    fun `plate bar accessibility text is Dutch`() {
        assertEquals("Geen schijven geladen", plateBarContentDescription(emptyList()))
        assertEquals("Schijven per kant: 20 kg, 2.50 kg", plateBarContentDescription(listOf(20f, 2.5f)))
    }

    @Test
    fun `discard active workout copy is spelled correctly`() {
        assertEquals(
            "Gelogde sets en ingevulde waarden voor deze actieve sessie worden verwijderd.",
            discardActiveWorkoutBodyText(),
        )
    }

    @Test
    fun `routine set metric cells keep fixed scan columns with placeholders`() {
        val set = RoutineSet(
            id = 1,
            workoutExerciseId = 10,
            orderIndex = 0,
            setType = SetType.NORMAL,
            targetReps = 8,
            targetWeightKg = 80.0,
            restSeconds = -1,
            targetRpe = 0.0,
        )

        assertEquals(
            listOf("8", "80 kg", "-", "-"),
            routineSetMetricCells(set).map { it.value },
        )
    }

    @Test
    fun `routine set metric labels stay Dutch and compact`() {
        val set = RoutineSet(
            id = 1,
            workoutExerciseId = 10,
            orderIndex = 0,
            targetReps = 8,
            targetWeightKg = 80.0,
            restSeconds = 90,
            targetRpe = 7.0,
        )

        assertEquals(
            listOf("Herh.", "Kg", "Rust", "RPE"),
            routineSetMetricCells(set).map { it.label },
        )
    }

    @Test
    fun `routine set metric layout uses one row when enough width is available`() {
        assertEquals(RoutineSetMetricLayout.OneRow, routineSetMetricLayoutForWidth(260.dp))
    }

    @Test
    fun `routine set metric layout falls back to balanced grid on narrow widths`() {
        assertEquals(RoutineSetMetricLayout.BalancedGrid, routineSetMetricLayoutForWidth(220.dp))
    }

    @Test
    fun `active set metric cells prefer logged values and keep routine editor labels`() {
        val loggedSet = LoggedSet(
            exerciseId = 1,
            weight = 50.0,
            reps = 12,
            rpe = 6.5,
            restSeconds = 90,
        )

        assertEquals(
            listOf("Herh." to "12", "Kg" to "50 kg", "Rust" to "90s", "RPE" to "6.5"),
            activeSetMetricCells(repRange = "8-12", plannedSet = null, loggedSet = loggedSet, activeRestSeconds = 120)
                .map { it.label to it.value },
        )
    }

    @Test
    fun `active set metric cells use planned values with placeholders`() {
        val plannedSet = RoutineSet(
            id = 1,
            workoutExerciseId = 10,
            orderIndex = 0,
            targetReps = 12,
            targetWeightKg = 0.0,
            restSeconds = 90,
            targetRpe = 0.0,
        )

        assertEquals(
            listOf("12", "-", "120s", "-"),
            activeSetMetricCells(repRange = "8-12", plannedSet = plannedSet, loggedSet = null, activeRestSeconds = 120)
                .map { it.value },
        )
    }

    @Test
    fun `active set rows keep manually reduced target without hiding logged sets`() {
        assertEquals(2, visibleActiveSetRows(plannedSetCount = 2, loggedSetCount = 0, manualExtraSetRequested = false))
        assertEquals(3, visibleActiveSetRows(plannedSetCount = 2, loggedSetCount = 3, manualExtraSetRequested = false))
        assertEquals(4, visibleActiveSetRows(plannedSetCount = 3, loggedSetCount = 3, manualExtraSetRequested = true))
    }

    @Test
    fun `active replacement picker allows custom exercise creation`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val activeReplacementPicker = workoutScreen
            .substringAfter("replacingActivePlan?.let { plan ->")
            .substringBefore("creatingActiveReplacement?.let")

        assertTrue(activeReplacementPicker.contains("allowCustomExercise = true"))
        assertTrue(activeReplacementPicker.contains("creatingActiveReplacement = plan"))
    }

    @Test
    fun `active exercise rest override is per exercise and clamped`() {
        assertEquals(90, activeExerciseRestSeconds(baseRestSeconds = 90, overrideRestSeconds = null))
        assertEquals(120, activeExerciseRestSeconds(baseRestSeconds = 90, overrideRestSeconds = 120))
        assertEquals(0, activeExerciseRestSeconds(baseRestSeconds = 90, overrideRestSeconds = -30))
        assertEquals(900, activeExerciseRestSeconds(baseRestSeconds = 90, overrideRestSeconds = 1200))
    }

    @Test
    fun `set log start rejects duplicate pending submit`() {
        val started = tryStartSetLog(emptySet(), exerciseId = 10L)

        assertTrue(started is SetLogStartResult.Started)
        started as SetLogStartResult.Started
        assertEquals(SetLogStartResult.AlreadyPending, tryStartSetLog(started.pendingExerciseIds, exerciseId = 10L))
    }

    @Test
    fun `set log start allows submit after completion`() {
        val started = tryStartSetLog(emptySet(), exerciseId = 10L) as SetLogStartResult.Started
        val finished = finishSetLog(started.pendingExerciseIds, exerciseId = 10L)

        assertTrue(tryStartSetLog(finished, exerciseId = 10L) is SetLogStartResult.Started)
    }

    @Test
    fun `set log start allows different exercise while another is pending`() {
        val started = tryStartSetLog(emptySet(), exerciseId = 10L) as SetLogStartResult.Started
        val next = tryStartSetLog(started.pendingExerciseIds, exerciseId = 20L)

        assertTrue(next is SetLogStartResult.Started)
        next as SetLogStartResult.Started
        assertEquals(setOf(10L, 20L), next.pendingExerciseIds)
    }

    @Test
    fun `pending correction keeps logger visible and changes primary action label`() {
        assertTrue(shouldShowActiveSetLogger(isSessionFinished = false, loggedSetCount = 3, activeSetTargetCount = 3, hasPendingCorrection = true))
        assertEquals("Wijzig loggen", activeSetLogButtonLabel(isLogPending = false, hasPendingCorrection = true, loggedSetCount = 3, plannedSetCount = 3))
    }

    @Test
    fun `decimal filter keeps digits and a single separator with limited decimals`() {
        assertEquals("12.34", filterDecimalInput("1a2,3.456", maxDecimals = 2))
    }

    @Test
    fun `decimal filter supports leading separator and rpe precision`() {
        assertEquals(".5", filterDecimalInput(",56", maxDecimals = 1))
    }

    @Test
    fun `integer filter keeps digits only`() {
        assertEquals("123", filterIntegerInput("1a2,3"))
    }

    @Test
    fun `set input accepts comma decimal and blank rpe`() {
        val result = validateSetInput(SetInputDraft(weight = "80,5", reps = "8", rpe = ""))

        assertTrue(result is SetLogValidationResult.Valid)
        result as SetLogValidationResult.Valid
        assertEquals(80.5, result.weight, 0.0)
        assertEquals(8, result.reps)
        assertEquals(0.0, result.rpe, 0.0)
    }

    @Test
    fun `set input accepts zero weight for bodyweight sets`() {
        val result = validateSetInput(SetInputDraft(weight = "0", reps = "12", rpe = "7"))

        assertTrue(result is SetLogValidationResult.Valid)
        result as SetLogValidationResult.Valid
        assertEquals(0.0, result.weight, 0.0)
        assertEquals(12, result.reps)
        assertEquals(7.0, result.rpe, 0.0)
    }

    @Test
    fun `set input rejects non numeric rpe`() {
        val result = validateSetInput(SetInputDraft(weight = "80", reps = "8", rpe = "abc"))

        assertTrue(result is SetLogValidationResult.Invalid)
    }

    @Test
    fun `set input rejects rpe above ten`() {
        val result = validateSetInput(SetInputDraft(weight = "80", reps = "8", rpe = "10,5"))

        assertTrue(result is SetLogValidationResult.Invalid)
    }

    @Test
    fun `invalid rpe maps to field specific error feedback`() {
        val result = validateSetInput(SetInputDraft(weight = "80", reps = "8", rpe = "11.5"))

        assertTrue(result is SetLogValidationResult.Invalid)
        result as SetLogValidationResult.Invalid
        assertEquals("RPE moet leeg zijn of tussen 0 en 10 liggen.", result.fieldErrors.rpe)
        assertEquals(null, result.fieldErrors.weight)
        assertEquals(null, result.fieldErrors.reps)
    }

    @Test
    fun `first startable day skips empty days`() {
        val emptyDay = WorkoutDay(id = 1, routineId = 1, name = "Empty", orderIndex = 0, exercises = emptyList())
        val exercise = Exercise(id = 1, name = "Bench press", muscleGroup = "Chest", equipment = "Barbell")
        val startableDay = WorkoutDay(
            id = 2,
            routineId = 1,
            name = "Push",
            orderIndex = 1,
            exercises = listOf(
                WorkoutExercisePlan(
                    id = 10,
                    exercise = exercise,
                    targetSets = 3,
                    repRange = "8-12",
                    restSeconds = 90,
                ),
            ),
        )
        val routine = WorkoutRoutine(
            id = 1,
            name = "Routine",
            description = "",
            active = true,
            days = listOf(emptyDay, startableDay),
        )

        assertEquals(startableDay, routine.firstStartableDay())
    }

    @Test
    fun `selected routine id is cleared when restored routine no longer exists`() {
        val routine = WorkoutRoutine(
            id = 1,
            name = "Routine",
            description = "",
            active = true,
            days = emptyList(),
        )

        assertEquals(1L, resolveSelectedRoutineId(1L, listOf(routine)))
        assertEquals(null, resolveSelectedRoutineId(2L, listOf(routine)))
        assertEquals(null, resolveSelectedRoutineId(null, listOf(routine)))
    }

    @Test
    fun `workout list keys do not collide with saved routine ids`() {
        val routine = WorkoutRoutine(
            id = 1,
            name = "Routine",
            description = "",
            active = true,
            days = emptyList(),
        )
        val keys = workoutOverviewListKeys(
            routines = listOf(routine),
            exercises = listOf(Exercise(id = 1, name = "Bench press", muscleGroup = "Chest", equipment = "Barbell")),
            historySessionIds = listOf(1),
            hasMessage = true,
        )

        assertEquals(keys.distinct(), keys)
    }

    @Test
    fun `exercise picker handle dismiss requires clear downward drag threshold`() {
        val thresholdPx = 96f

        assertEquals(false, shouldDismissExercisePickerFromHandleDrag(verticalDragPx = 24f, thresholdPx = thresholdPx))
        assertEquals(false, shouldDismissExercisePickerFromHandleDrag(verticalDragPx = 95f, thresholdPx = thresholdPx))
        assertEquals(true, shouldDismissExercisePickerFromHandleDrag(verticalDragPx = 96f, thresholdPx = thresholdPx))
        assertEquals(true, shouldDismissExercisePickerFromHandleDrag(verticalDragPx = 140f, thresholdPx = thresholdPx))
    }

    @Test
    fun `exercise edit trailing scroll padding includes ime in dialog set editor`() {
        val padding = exerciseEditTrailingScrollPadding(
            imeBottomPadding = 280.dp,
            minimumTrailingPadding = 24.dp,
        )

        assertEquals(304.dp, padding)
    }

    @Test
    fun `set editor uses dialog backed surface instead of material bottom sheet`() {
        assertEquals(true, setEditorUsesDialogBackedSurface())
    }

    @Test
    fun `set editor handle dismiss requires clear downward drag threshold`() {
        val thresholdPx = 96f

        assertEquals(false, shouldDismissSetEditorFromHandleDrag(verticalDragPx = 24f, thresholdPx = thresholdPx))
        assertEquals(false, shouldDismissSetEditorFromHandleDrag(verticalDragPx = 95f, thresholdPx = thresholdPx))
        assertEquals(true, shouldDismissSetEditorFromHandleDrag(verticalDragPx = 96f, thresholdPx = thresholdPx))
        assertEquals(true, shouldDismissSetEditorFromHandleDrag(verticalDragPx = 140f, thresholdPx = thresholdPx))
    }

    @Test
    fun `set editor focused input reveal scrolls only enough to clear visible viewport`() {
        assertEquals(
            148f,
            focusedInputRevealScrollDelta(
                fieldTop = 1223f,
                fieldBottom = 1391f,
                viewportTop = 444f,
                viewportBottom = 1291f,
                marginPx = 48f,
            ),
        )
        assertEquals(
            0f,
            focusedInputRevealScrollDelta(
                fieldTop = 900f,
                fieldBottom = 1050f,
                viewportTop = 444f,
                viewportBottom = 1291f,
                marginPx = 48f,
            ),
        )
    }

    @Test
    fun `set editor visible viewport bottom accounts for ime overlay`() {
        assertEquals(
            1291f,
            setEditorVisibleViewportBottom(
                viewportBottom = 1849f,
                rootHeight = 2400f,
                imeBottomPx = 1109f,
            ),
        )
        assertEquals(
            1849f,
            setEditorVisibleViewportBottom(
                viewportBottom = 1849f,
                rootHeight = 2400f,
                imeBottomPx = 0f,
            ),
        )
    }

    @Test
    fun `set editor reveal verifies focused field against ime-reduced viewport`() {
        val viewport = Rect(left = 0f, top = 444f, right = 1079f, bottom = 1849f)
        val hiddenByIme = Rect(left = 555f, top = 1223f, right = 1016f, bottom = 1391f)
        val visibleAfterScroll = Rect(left = 555f, top = 1000f, right = 1016f, bottom = 1168f)

        assertEquals(
            false,
            isFocusedInputVisibleInSetEditor(
                field = hiddenByIme,
                viewport = viewport,
                visibleViewportBottom = 1291f,
                marginPx = 48f,
            ),
        )
        assertEquals(
            true,
            isFocusedInputVisibleInSetEditor(
                field = visibleAfterScroll,
                viewport = viewport,
                visibleViewportBottom = 1291f,
                marginPx = 48f,
            ),
        )
    }

    @Test
    fun `set log guard rejects second tap while exercise is pending`() {
        val firstTap = tryStartSetLog(pendingExerciseIds = emptySet(), exerciseId = 42L)
        assertTrue(firstTap is SetLogStartResult.Started)

        val secondTap = tryStartSetLog(
            pendingExerciseIds = (firstTap as SetLogStartResult.Started).pendingExerciseIds,
            exerciseId = 42L,
        )

        assertTrue(secondTap is SetLogStartResult.AlreadyPending)
    }

    @Test
    fun `set log guard can clear pending exercise after save finishes`() {
        val firstTap = tryStartSetLog(pendingExerciseIds = emptySet(), exerciseId = 42L) as SetLogStartResult.Started

        assertEquals(emptySet<Long>(), finishSetLog(firstTap.pendingExerciseIds, exerciseId = 42L))
    }
}

private fun sampleLoggedSet(id: Long): LoggedSet =
    LoggedSet(
        id = id,
        exerciseId = 42L,
        weight = 77.5,
        reps = 8,
        rpe = 8.0,
        restSeconds = 90,
    )

private fun testSourceFile(relativePackagePath: String): File {
    val userDir = File(System.getProperty("user.dir"))
    return listOf(
        File(userDir, "src/main/java/com/trainiq/$relativePackagePath"),
        File(userDir, "app/src/main/java/com/trainiq/$relativePackagePath"),
    ).first(File::isFile)
}
