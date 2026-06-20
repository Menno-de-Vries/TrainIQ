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
    fun workoutOverviewSeparatesRoutinesLibraryAndHistoryTabs() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()

        assertTrue(workoutScreen.contains("WorkoutOverviewTab.Routines"))
        assertTrue(workoutScreen.contains("WorkoutOverviewTab.Library"))
        assertTrue(workoutScreen.contains("WorkoutOverviewTab.History"))
        assertTrue(workoutScreen.contains("filterNot { it.id == overview.activeRoutine?.id }"))
        assertTrue(workoutScreen.contains("RoutineOverlapProposalCard"))
    }

    @Test
    fun workoutLibrarySupportsScoredRecentAndUntrainedFilters() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val filterBody = workoutScreen.substringAfter("private enum class ExerciseLibraryFilter").substringBefore("private fun dayEstimatedMinutes")

        assertTrue(filterBody.contains("Scored(\"scored\", \"Met score\")"))
        assertTrue(filterBody.contains("Recent(\"recent\", \"Recent\")"))
        assertTrue(filterBody.contains("Untrained(\"untrained\", \"Nog niet getraind\")"))
        assertTrue(filterBody.contains("item.score > 0.0 && item.completedSessions > 0"))
        assertTrue(filterBody.contains("item.completedSessions == 0"))
    }

    @Test
    fun workoutHistoryCardShowsStoredDebriefFeedback() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val historyCard = workoutScreen.substringAfter("private fun HistoryCard(").substringBefore("@OptIn(ExperimentalMaterial3Api::class)")

        assertTrue(historyCard.contains("session.workoutName"))
        assertTrue(historyCard.contains("session.strongestSetLabel"))
        assertTrue(historyCard.contains("session.debriefSummary"))
        assertTrue(historyCard.contains("session.debriefRecommendation"))
        assertTrue(historyCard.contains("session.debriefNextSessionFocus"))
    }

    @Test
    fun activeRoutineCardOffersEditActionWhenRoutineCanStart() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val activeRoutineCard = workoutScreen.substringAfter("private fun ActiveRoutineCard(").substringBefore("@Composable\nprivate fun SectionHeader")
        val activeRoutineActionRow = workoutScreen.substringAfter("private fun ActiveRoutineActionRow(").substringBefore("@Composable\nprivate fun SectionHeader")
        val startableBranch = activeRoutineCard.substringAfter("} else {").substringBefore("}\n            }\n        }")

        assertTrue(startableBranch.contains("ActiveRoutineActionRow("))
        assertTrue(activeRoutineActionRow.contains("onStartWorkout(startableDay.id)"))
        assertTrue(activeRoutineActionRow.contains("onOpenDetails(activeRoutineId)"))
        assertTrue(activeRoutineActionRow.contains("Routine aanpassen") || activeRoutineActionRow.contains("Routine bewerken"))
    }

    @Test
    fun activeRoutineCardUsesStableActionRowForScrollPerformance() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val activeRoutineCard = workoutScreen.substringAfter("private fun ActiveRoutineCard(").substringBefore("@Composable\nprivate fun ActiveRoutineActionRow")
        val activeRoutineActionRow = workoutScreen.substringAfter("private fun ActiveRoutineActionRow(").substringBefore("@Composable\nprivate fun SectionHeader")

        assertTrue(activeRoutineCard.contains("ActiveRoutineActionRow("))
        assertFalse(activeRoutineCard.contains("WrappingActionRow("))
        assertTrue(activeRoutineActionRow.contains("Row("))
        assertTrue(activeRoutineActionRow.contains("Modifier.weight(1f).fillMaxWidth().heightIn(min = 48.dp)"))
        assertTrue(activeRoutineActionRow.contains("val startLabel = activeRoutineStartLabel(startableDay.name)"))
        assertTrue(activeRoutineActionRow.contains("onStartWorkout(startableDay.id)"))
        assertTrue(activeRoutineActionRow.contains("onOpenDetails(activeRoutineId)"))
    }

    @Test
    fun activeWorkoutStartConflictUsesExplicitDialogActions() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val conflictDialog = workoutScreen.substringAfter("private fun ActiveWorkoutStartConflictDialog(")
            .substringBefore("@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun ActiveWorkoutScreen")

        assertTrue(workoutScreen.contains("data class ActiveWorkoutStartConflict("))
        assertTrue(workoutScreen.contains("pendingStartConflict"))
        assertTrue(conflictDialog.contains("Oude training open"))
        assertTrue(conflictDialog.contains("Oude training hervatten"))
        assertTrue(conflictDialog.contains("Nieuwe training starten"))
        assertTrue(conflictDialog.contains("Annuleren"))
    }

    @Test
    fun routineActionsUseWrappingSharedButtons() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val routineCardOverview = workoutScreen
            .substringAfter("if (!detailMode) {")
            .substringBefore("return\n    }")
        val routineOverviewActionStrip = workoutScreen
            .substringAfter("private fun RoutineOverviewActionStrip(")
            .substringBefore("@Composable\nprivate fun RoutineDetailTabSwitcher")
        val routineDetailBody = workoutScreen
            .substringAfter("AppCard(modifier = Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.primary)")
            .substringBefore("HorizontalDivider()")

        assertTrue(routineCardOverview.contains("RoutineOverviewActionStrip("))
        assertTrue(routineOverviewActionStrip.contains("PrimaryActionButton(onClick = { onStartWorkout(startableDay.id)"))
        assertTrue(routineOverviewActionStrip.contains("SecondaryActionButton(onClick = onOpenDetails"))
        assertTrue(routineOverviewActionStrip.contains("weight(1f).fillMaxWidth()"))
        assertTrue(routineOverviewActionStrip.contains("heightIn(min = 48.dp)"))
        assertFalse(routineCardOverview.contains("WrappingActionRow(labels = listOf(\"Details\", \"Actief maken\", \"Start\"))"))
        assertTrue(routineDetailBody.contains("WrappingActionRow("))
        assertTrue(routineDetailBody.contains("PrimaryActionButton(onClick = { onStartWorkout(startableDay.id)"))
        assertTrue(routineDetailBody.contains("SecondaryActionButton(onClick = { onSetActiveRoutine(routine.id)"))
        assertTrue(routineDetailBody.contains("actionModifier ->"))
        assertTrue(routineDetailBody.contains("modifier = actionModifier"))
    }

    @Test
    fun workoutHistoryCardUsesReadableMetricTilesAndSeparateAdviceSections() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val historyCard = workoutScreen.substringAfter("private fun HistoryCard(").substringBefore("@OptIn(ExperimentalMaterial3Api::class)")

        assertTrue(historyCard.contains("HistoryMetricTile("))
        assertTrue(historyCard.contains("Duur"))
        assertTrue(historyCard.contains("Oefeningen"))
        assertTrue(historyCard.contains("Sets"))
        assertTrue(historyCard.contains("Volume"))
        assertTrue(historyCard.contains("Topset"))
        assertTrue(historyCard.contains("Herstel"))
        assertTrue(historyCard.contains("HistoryDebriefBlock("))
        assertFalse(historyCard.contains("AppChip(label = \"\${session.duration / 60} min\""))
    }

    @Test
    fun `critical headers and set labels allow wrapping except short screen header condensation`() {
        val appDesign = testSourceFile("core/ui/AppDesign.kt").readText()
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val appScreenHeaderBody = appDesign.substringAfter("fun AppScreenHeader(").substringBefore("fun AppCard(")
        val routineSetRowBody = workoutScreen.substringAfter("private fun RoutineSetRow(").substringBefore("BoxWithConstraints")

        assertTrue(appScreenHeaderBody.contains("compactShortScreen"))
        assertTrue(appScreenHeaderBody.contains("maxLines = if (compactShortScreen) 1 else Int.MAX_VALUE"))
        assertTrue(appScreenHeaderBody.contains("overflow = TextOverflow.Ellipsis"))
        assertFalse(routineSetRowBody.contains("softWrap = false"))
    }

    @Test
    fun `AI routine equipment field keeps accessibility label at large font scale`() {
        val appDesign = testSourceFile("core/ui/AppDesign.kt").readText()
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val fieldBody = appDesign.substringAfter("fun TapOnlyOutlinedTextField(")
            .substringBefore("internal fun shouldSuppressTextInputGesture(")
        val dialogBody = workoutScreen.substringAfter("private fun RoutineGeneratorDialog(")
            .substringBefore("private fun ExperienceLevelSelector(")

        assertTrue(fieldBody.contains("accessibilityLabel: String? = null"))
        assertTrue(fieldBody.contains("Modifier.semantics { contentDescription = accessibilityLabel }"))
        assertTrue(dialogBody.contains("accessibilityLabel = \"Beschikbaar materiaal\""))
    }

    @Test
    fun `AI routine dialog keeps Dutch labels for generator controls`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val dialogBody = workoutScreen.substringAfter("private fun RoutineGeneratorDialog(")
            .substringBefore("private fun RoutineDetailHeader(")

        assertTrue(dialogBody.contains("Text(\"Dagen per week\")"))
        assertTrue(dialogBody.contains("Text(\"Beschikbaar materiaal\")"))
        assertTrue(dialogBody.contains("Text(\"Ervaringsniveau\""))
        assertTrue(dialogBody.contains("Text(\"Sessieduur:"))
        assertTrue(dialogBody.contains("Text(\"Deload-richtlijn opnemen\""))
        assertTrue(dialogBody.contains("Text(\"Voegt hersteladvies toe voor lichtere weken.\""))
        assertFalse(dialogBody.contains("Days per week"))
        assertFalse(dialogBody.contains("Available equipment"))
        assertFalse(dialogBody.contains("Experience level"))
        assertFalse(dialogBody.contains("Session duration"))
        assertFalse(dialogBody.contains("Include deload guidance"))
    }

    @Test
    fun `AI routine dialog wraps compact chip and choice controls`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val dialogBody = workoutScreen.substringAfter("private fun RoutineGeneratorDialog(")
            .substringBefore("private fun ExperienceLevelSelector(")
        val levelBody = workoutScreen.substringAfter("private fun ExperienceLevelSelector(")
            .substringBefore("private fun SessionDurationSlider(")

        assertTrue(dialogBody.contains("FlowRow("))
        assertFalse(dialogBody.contains("maxLines = 1"))
        assertTrue(levelBody.contains("FlowRow("))
        assertTrue(levelBody.contains("FilterChip("))
        assertFalse(levelBody.contains("SingleChoiceSegmentedButtonRow"))
        assertFalse(levelBody.contains("SegmentedButton("))
    }

    @Test
    fun `AI routine preview save and cancel keep pending state explicit`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val savePendingBody = workoutScreen.substringAfter("fun savePendingGeneratedRoutine()")
            .substringBefore("fun dismissPendingGeneratedRoutine()")
        val dismissPendingBody = workoutScreen.substringAfter("fun dismissPendingGeneratedRoutine()")
            .substringBefore("fun retryGeneratedRoutine()")
        val previewDialogCall = workoutScreen.substringAfter("GeneratedRoutinePreviewDialog(")
            .substringBefore("if (showCreateDialog)")
        val generatorDialogCall = workoutScreen.substringAfter("RoutineGeneratorDialog(")
            .substringBefore("TrainingWithoutOverscroll")
        val dialogBody = workoutScreen.substringAfter("private fun RoutineGeneratorDialog(")
            .substringBefore("private fun ExperienceLevelSelector(")

        assertTrue(savePendingBody.contains("if (_isSavingGeneratedRoutine.value) return"))
        assertTrue(savePendingBody.contains("val routine = _pendingGeneratedRoutine.value ?: return"))
        assertTrue(savePendingBody.contains("_isSavingGeneratedRoutine.value = true"))
        assertTrue(savePendingBody.contains("saveGeneratedRoutineUseCase(routine)"))
        assertTrue(savePendingBody.contains("_pendingGeneratedRoutine.value = null"))
        assertTrue(savePendingBody.contains("_message.value = \"Routine opgeslagen.\""))
        assertTrue(savePendingBody.contains("_isSavingGeneratedRoutine.value = false"))
        assertTrue(dismissPendingBody.contains("_pendingGeneratedRoutine.value = null"))
        assertTrue(previewDialogCall.contains("onSave = onSaveGeneratedRoutine"))
        assertTrue(previewDialogCall.contains("onDismiss = onDismissGeneratedRoutine"))
        assertTrue(generatorDialogCall.contains("onDismiss = { if (!isGenerating) showAiDialog = false }"))
        assertTrue(dialogBody.contains("dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text(\"Annuleren\") } }"))
    }

    @Test
    fun `active workout logged set actions wire edit delete type change and undo`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val updateTypeBody = workoutScreen.substringAfter("fun updateLoggedSetType(")
            .substringBefore("fun editLoggedSet(")
        val editSetBody = workoutScreen.substringAfter("fun editLoggedSet(")
            .substringBefore("fun deleteLoggedSet(")
        val deleteSetBody = workoutScreen.substringAfter("fun deleteLoggedSet(")
            .substringBefore("fun relogSet(")
        val undoBody = workoutScreen.substringAfter("fun undoWorkoutLogEvent(")
            .substringBefore("fun logSameAgain(")
        val eventCollector = workoutScreen.substringAfter("is WorkoutUiEvent.SetLogged ->")
            .substringBefore("is WorkoutUiEvent.WorkoutCompleted")
        val activeRouteCall = workoutScreen.substringAfter("ActiveWorkoutScreen(")
            .substringBefore("onFinishWorkout =")
        val setRowsBody = workoutScreen.substringAfter("repeat(visibleSetRows) { index ->")
            .substringBefore("if (collapsed) return@Column")

        assertTrue(updateTypeBody.contains("updateActiveWorkoutSetTypeUseCase(setId, setType)"))
        assertTrue(editSetBody.contains("_pendingCorrectionSetIds.value"))
        assertTrue(editSetBody.contains("set.toDraft()"))
        assertTrue(editSetBody.contains("Set staat klaar voor correctie"))
        assertTrue(deleteSetBody.contains("_pendingCorrectionSetIds.value = _pendingCorrectionSetIds.value.filterValues { it != setId }"))
        assertTrue(deleteSetBody.contains("deleteActiveWorkoutSetUseCase(setId)"))
        assertTrue(deleteSetBody.contains("_message.value = \"Set verwijderd.\""))
        assertTrue(undoBody.contains("undoWorkoutLogEventUseCase(eventId)"))
        assertTrue(undoBody.contains("_message.value = \"Laatste set hersteld.\""))
        assertTrue(eventCollector.contains("actionLabel = event.undoEventId?.let { \"Ongedaan maken\" }"))
        assertTrue(eventCollector.contains("event.undoEventId?.let(viewModel::undoWorkoutLogEvent)"))
        assertTrue(activeRouteCall.contains("onSetTypeChange = viewModel::updateLoggedSetType"))
        assertTrue(activeRouteCall.contains("onEditSet = viewModel::editLoggedSet"))
        assertTrue(activeRouteCall.contains("onDeleteSet = { setId -> viewModel.deleteLoggedSet(setId) }"))
        assertTrue(activeRouteCall.contains("onRelogSet = viewModel::relogSet"))
        assertTrue(editSetBody.contains("updateActiveWorkoutDraftUseCase(exerciseId, draft.toDomainDraft())"))
        assertTrue(workoutScreen.contains("restSeconds = validInput.restSeconds"))
        assertTrue(setRowsBody.contains("onSetTypeChange(set.id, type)"))
        assertTrue(setRowsBody.contains("onEdit = { loggedSetForRow?.let { onEditSet(it.id) } }"))
        assertTrue(setRowsBody.contains("onDelete = { loggedSetForRow?.let { onDeleteSet(it.id) } }"))
        assertTrue(setRowsBody.contains("onRelog = { loggedSetForRow?.let { onRelogSet(it.id) } }"))
    }

    @Test
    fun `AI routine deload switch keeps accessibility label at large font scale`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val deloadBody = workoutScreen.substringAfter("private fun IncludeDeloadRow(")
            .substringBefore("private fun RoutineDetailHeader(")

        assertTrue(deloadBody.contains("Text(\"Deload-richtlijn opnemen\""))
        assertTrue(deloadBody.contains("contentDescription = \"Deload-richtlijn opnemen\""))
        assertTrue(deloadBody.contains("Column("))
        assertTrue(deloadBody.contains("modifier = Modifier.fillMaxWidth()"))
    }

    @Test
    fun `routine creation buttons keep accessibility labels when clipped at compact height`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val creationCardBody = workoutScreen.substringAfter("private fun RoutineCreationCard(")
            .substringBefore("@Composable\n@OptIn(ExperimentalFoundationApi::class)")

        assertTrue(creationCardBody.contains("contentDescription = \"Lege routine maken\""))
        assertTrue(creationCardBody.contains("contentDescription = \"Met AI genereren\""))
    }

    @Test
    fun `active workout header actions keep accessibility labels when clipped at compact height`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val activeWorkoutBody = workoutScreen.substringAfter("private fun ActiveWorkoutScreen(")
            .substringBefore("if (showFinishConfirm)")

        assertTrue(activeWorkoutBody.contains("contentDescription = \"Terug naar Training\""))
        assertTrue(activeWorkoutBody.contains("contentDescription = \"Actieve training weggooien\""))
    }

    @Test
    fun `active workout set type selector is integrated into set row pill`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val activeExerciseBody = workoutScreen.substringAfter("private fun ActiveExerciseCard(")
            .substringBefore("private fun ActiveExerciseRestControl(")
        val setRowBody = workoutScreen.substringAfter("private fun SetRow(")
            .substringBefore("private fun RoutineSetMetricValue(")

        assertFalse(activeExerciseBody.contains("compactShortScreen"))
        assertTrue(setRowBody.contains("SetTypePill("))
        assertTrue(setRowBody.contains("SetType.entries.forEach"))
        assertTrue(setRowBody.contains("enabled = loggedSet != null || isInputExpanded"))
    }

    @Test
    fun `routine session name field keeps accessibility label at compact font scale`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val detailBody = workoutScreen.substringAfter("private fun RoutineDetailView(")
            .substringBefore("private fun ExercisePickerSection(")

        assertTrue(detailBody.contains("accessibilityLabel = \"Sessienaam optioneel\""))
        assertTrue(detailBody.contains("label = { Text(\"Sessienaam (optioneel)\") }"))
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
    fun `active workout summary keeps rest status and session name visible`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val summaryBody = workoutScreen.substringAfter("private fun ActiveWorkoutSessionSummary(")
            .substringBefore("private fun ActiveWorkoutStickyStatus(")

        assertTrue(summaryBody.contains("displayWorkoutDayName"))
        assertTrue(summaryBody.contains("activeWorkoutBottomBarStatusText(restTimerSeconds)"))
        assertTrue(summaryBody.contains("StatusMetric(\"Rust\""))
        assertTrue(summaryBody.contains("AppLinearProgress(progress = progress"))
    }

    @Test
    fun `active set action row stacks on compact widths to avoid clipped Dutch labels`() {
        assertEquals(ActiveSetActionLayout.Stacked, activeSetActionLayoutForWidth(319.dp))
        assertEquals(ActiveSetActionLayout.Wrapped, activeSetActionLayoutForWidth(320.dp))

        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val activeExerciseBody = workoutScreen.substringAfter("if (showLogger) Column")
            .substringBefore("private fun ActiveExerciseRestControl(")

        assertTrue(activeExerciseBody.contains("BoxWithConstraints(modifier = Modifier.fillMaxWidth())"))
        assertTrue(activeExerciseBody.contains("activeSetActionLayoutForWidth(maxWidth)"))
        assertTrue(activeExerciseBody.contains("maxLines = 2"))
        assertTrue(activeExerciseBody.contains("TextAlign.Center"))
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
            "Tik op Routine inrichten en voeg eerst een oefening toe voordat je start.",
            activeRoutineNeedsExerciseText(),
        )
    }

    @Test
    fun `empty active routine exposes setup entry in active routine card`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val activeRoutineCard = workoutScreen
            .substringAfter("private fun ActiveRoutineCard(")
            .substringBefore("private fun SectionHeader")

        assertTrue(activeRoutineCard.contains("onOpenDetails: (Long) -> Unit"))
        assertTrue(activeRoutineCard.contains("activeRoutineSetupLabel()"))
        assertTrue(activeRoutineCard.contains("onOpenDetails(activeRoutine.id)"))
        assertTrue(activeRoutineCard.contains("Icon(Icons.Rounded.Add"))
    }

    @Test
    fun `routine setup label stays Dutch`() {
        assertEquals("Routine inrichten", activeRoutineSetupLabel())
    }

    @Test
    fun `routine detail opens sessions first when routine is not startable`() {
        val emptyRoutine = WorkoutRoutine(
            id = 1,
            name = "Routine",
            description = "",
            active = true,
            days = emptyList(),
        )
        val routineWithEmptyDay = emptyRoutine.copy(
            days = listOf(WorkoutDay(id = 1, routineId = 1, name = "Sessie", orderIndex = 0, exercises = emptyList())),
        )

        assertEquals("sessions", initialRoutineDetailTab(emptyRoutine))
        assertEquals("sessions", initialRoutineDetailTab(routineWithEmptyDay))
    }

    @Test
    fun `routine detail opens info first when routine can start`() {
        val exercise = Exercise(id = 1, name = "Bench press", muscleGroup = "Chest", equipment = "Barbell")
        val routine = WorkoutRoutine(
            id = 1,
            name = "Routine",
            description = "",
            active = true,
            days = listOf(
                WorkoutDay(
                    id = 1,
                    routineId = 1,
                    name = "Push",
                    orderIndex = 0,
                    exercises = listOf(
                        WorkoutExercisePlan(
                            id = 1,
                            exercise = exercise,
                            targetSets = 3,
                            repRange = "8-12",
                            restSeconds = 90,
                        ),
                    ),
                ),
            ),
        )

        assertEquals("info", initialRoutineDetailTab(routine))
    }

    @Test
    fun `routine detail only resets training list scroll when opened from overview`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val routeBody = workoutScreen
            .substringAfter("var selectedRoutineId by rememberSaveable")
            .substringBefore("@HiltViewModel\nclass WorkoutCompletionViewModel")

        assertTrue(routeBody.contains("val trainingListState = rememberLazyListState()"))
        assertTrue(routeBody.contains("var previousSelectedRoutineId by rememberSaveable"))
        assertTrue(routeBody.contains("selectedRoutineId != null && previousSelectedRoutineId == null"))
        assertTrue(routeBody.contains("previousSelectedRoutineId = selectedRoutineId"))
        assertTrue(routeBody.contains("state = trainingListState"))
    }

    @Test
    fun `active routine is prioritized before routine creation when present`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val overviewBody = workoutScreen
            .substringAfter("WorkoutOverviewTab.Routines.key -> {")
            .substringBefore("WorkoutOverviewTab.Library.key -> {")

        assertTrue(overviewBody.indexOf("ActiveRoutineCard(") < overviewBody.indexOf("RoutineCreationCard("))
        assertTrue(overviewBody.contains("filterNot { it.id == overview.activeRoutine?.id }"))
    }

    @Test
    fun `routine overview keeps details action`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val routineCardOverview = workoutScreen
            .substringAfter("if (!detailMode) {")
            .substringBefore("return\n    }")
        val routineOverviewActionStrip = workoutScreen
            .substringAfter("private fun RoutineOverviewActionStrip(")
            .substringBefore("@Composable\nprivate fun RoutineDetailTabSwitcher")

        assertTrue(routineCardOverview.contains("onOpenDetails"))
        assertTrue(routineOverviewActionStrip.contains("Text(\"Details\""))
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

        assertEquals("3 sets - 8-10 herh. - 90s rust - RPE 8 - Superset 12", text)
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
    fun `workout action controls sit below exercise headers and active messages use snackbar`() {
        val workoutScreen = testSourceFile("features/workout/WorkoutScreen.kt").readText()
        val routineExerciseCard = workoutScreen
            .substringAfter("private fun RoutineExerciseCard(")
            .substringBefore("private fun ExerciseSummaryMetaRow(")
        val activeExerciseCard = workoutScreen
            .substringAfter("private fun ActiveExerciseCard(")
            .substringBefore("private fun ActiveSetInputRow(")
        val setRow = workoutScreen
            .substringAfter("private fun SetRow(")
            .substringBefore("private fun RoutineSetMetricValue(")

        assertFalse(routineExerciseCard.contains("ActiveExerciseActionChip("))
        assertTrue(routineExerciseCard.contains("text = { Text(\"Set toevoegen\") }"))
        assertTrue(routineExerciseCard.contains("onAddSet()"))
        assertTrue(activeExerciseCard.contains("modifier = Modifier\n                        .fillMaxWidth()"))
        assertTrue(activeExerciseCard.contains("maxLines = 3"))
        assertTrue(activeExerciseCard.contains("horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)"))
        assertTrue(activeExerciseCard.contains("ActiveExerciseRestControl"))
        assertTrue(workoutScreen.contains("val activeWorkoutListState = rememberLazyListState()"))
        assertTrue(workoutScreen.contains("state = activeWorkoutListState"))
        assertTrue(activeExerciseCard.contains("DropdownMenuItem("))
        assertTrue(activeExerciseCard.contains("text = { Text(\"Set toevoegen\") }"))
        assertTrue(activeExerciseCard.contains("onActivate ="))
        assertTrue(activeExerciseCard.contains("activeInputIndex = index"))
        assertFalse(activeExerciseCard.contains("Card(\n                        modifier = Modifier.fillMaxWidth(),\n                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)"))
        assertTrue(setRow.contains("isInputExpanded: Boolean"))
        assertTrue(setRow.contains("ActiveSetInputMetrics("))
        assertFalse(setRow.contains("SetLoggerFields("))
        assertTrue(setRow.contains("enabled = loggedSet != null || isInputExpanded"))
        assertTrue(workoutScreen.contains("private fun ActiveSetInputMetricValue("))
        assertTrue(workoutScreen.contains("BasicTextField("))
        assertTrue(workoutScreen.contains("private fun SetTypePill("))
        assertTrue(workoutScreen.contains("snackbarHost = { SnackbarHost(snackbarHostState) }"))
        assertFalse(activeExerciseCard.contains("label = \"Set +\""))
        assertFalse(activeExerciseCard.contains("label = activeExerciseReplaceLabel()"))
        assertFalse(activeExerciseCard.contains("active-workout-message"))
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
    fun `active set metric input replaces default zero when typing around it`() {
        assertEquals("8", normalizeActiveSetMetricInput(previousValue = "0", filteredInput = "08"))
        assertEquals("8", normalizeActiveSetMetricInput(previousValue = "0", filteredInput = "80"))
        assertEquals("12", normalizeActiveSetMetricInput(previousValue = "0", filteredInput = "120"))
        assertEquals("0.5", normalizeActiveSetMetricInput(previousValue = "0", filteredInput = "0.5"))
    }

    @Test
    fun `set input accepts comma decimal and blank rpe`() {
        val result = validateSetInput(SetInputDraft(weight = "80,5", reps = "8", rpe = ""))

        assertTrue(result is SetLogValidationResult.Valid)
        result as SetLogValidationResult.Valid
        assertEquals(80.5, result.weight, 0.0)
        assertEquals(8, result.reps)
        assertEquals(0, result.restSeconds)
        assertEquals(0.0, result.rpe, 0.0)
    }

    @Test
    fun `set input accepts zero weight for bodyweight sets`() {
        val result = validateSetInput(SetInputDraft(weight = "0", reps = "12", rpe = "7"))

        assertTrue(result is SetLogValidationResult.Valid)
        result as SetLogValidationResult.Valid
        assertEquals(0.0, result.weight, 0.0)
        assertEquals(12, result.reps)
        assertEquals(0, result.restSeconds)
        assertEquals(7.0, result.rpe, 0.0)
    }

    @Test
    fun `set input accepts rest seconds within active workout bounds`() {
        val result = validateSetInput(SetInputDraft(weight = "80", reps = "8", restSeconds = "120", rpe = "7"))

        assertTrue(result is SetLogValidationResult.Valid)
        result as SetLogValidationResult.Valid
        assertEquals(120, result.restSeconds)
    }

    @Test
    fun `active logger defaults missing planned weight to zero for bodyweight logging`() {
        assertEquals("0", activeSetDraftWeightText(0.0))
        assertEquals("0", activeSetDraftWeightText(-1.0))
        assertEquals("80.5", activeSetDraftWeightText(80.5))
    }

    @Test
    fun `active logger ui draft falls back to planned bodyweight set`() {
        val exercise = Exercise(id = 1, name = "Ab Wheel Rollout", muscleGroup = "Core", equipment = "Lichaamsgewicht")
        val plan = WorkoutExercisePlan(
            id = 1,
            exercise = exercise,
            targetSets = 3,
            repRange = "8-12",
            restSeconds = 90,
            targetWeightKg = 0.0,
        )

        assertEquals(SetInputDraft(weight = "0", reps = "12", restSeconds = "90"), activeSetUiDraft(savedDraft = null, plan = plan, loggedSetCount = 0))
        assertEquals(SetInputDraft(weight = "0", reps = "12", restSeconds = "90"), activeSetUiDraft(savedDraft = SetInputDraft(weight = "", reps = "12"), plan = plan, loggedSetCount = 0))
        assertEquals(SetInputDraft(weight = "20", reps = "8", restSeconds = "150"), activeSetUiDraft(savedDraft = SetInputDraft(weight = "20", reps = "8", restSeconds = "150"), plan = plan, loggedSetCount = 0))
    }

    @Test
    fun `set input rejects rest outside active workout bounds`() {
        val result = validateSetInput(SetInputDraft(weight = "80", reps = "8", restSeconds = "1200", rpe = "7"))

        assertTrue(result is SetLogValidationResult.Invalid)
        result as SetLogValidationResult.Invalid
        assertEquals("Rust moet tussen 0 en 900s liggen.", result.fieldErrors.restSeconds)
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
    val userDir = File(System.getProperty("user.dir") ?: ".")
    return listOf(
        File(userDir, "src/main/java/com/trainiq/$relativePackagePath"),
        File(userDir, "app/src/main/java/com/trainiq/$relativePackagePath"),
    ).first { it.isFile }
}
