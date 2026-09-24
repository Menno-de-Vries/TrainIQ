package com.trainiq.features.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.trainiq.core.ui.TapOnlyOutlinedTextField
import com.trainiq.core.ui.clearFocusOnScrollOrDrag
import com.trainiq.domain.model.GeneratedRoutine
import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.GeneratedRoutineSource

@Composable
fun CreateRoutineDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = "",
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val trimmedName = name.trim()

    AlertDialog(
        modifier = modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text("Routine maken") },
        text = {
            TapOnlyOutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth(),
                label = { Text("Routinenaam") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (trimmedName.isNotEmpty()) {
                            onConfirm(trimmedName)
                        }
                    },
                ),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(trimmedName) },
                enabled = trimmedName.isNotEmpty(),
            ) {
                Text("Maken")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuleren")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratedRoutinePreviewDialog(
    routine: GeneratedRoutine,
    availableExercises: List<Exercise>,
    isSaving: Boolean,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onReplaceExercise: (Int, Int, Exercise) -> Unit,
    onRemoveExercise: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var exerciseQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = routine.routineName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                GeneratedRoutineInfoPill(label = routine.source.label(), contentDescription = "Bron: ${routine.source.label()}")
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .clearFocusOnScrollOrDrag()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = routine.routineDescription.ifBlank { "Geen beschrijving ingevuld." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GeneratedRoutineMetaRow(routine)
                if (routine.periodizationNote.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Opbouw", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = routine.periodizationNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
                routine.days.forEachIndexed { dayIndex, day ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = day.dayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${day.exercises.size} oefeningen",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "${day.estimatedDurationMinutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            day.exercises.forEachIndexed { exerciseIndex, exercise ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "${exercise.exerciseName} - ${exercise.targetSets} x ${exercise.repRange}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    if (exercise.coachingCue.isNotBlank()) {
                                        Text(
                                            text = exercise.coachingCue,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    TextButton(
                                        onClick = { editTarget = dayIndex to exerciseIndex; exerciseQuery = "" },
                                        enabled = !isSaving,
                                    ) { Text("Oefening aanpassen") }
                                }
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isSaving) "Opslaan..." else "Opslaan")
                }
                TextButton(onClick = onRetry, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text("Opnieuw proberen")
                }
                TextButton(onClick = onDismiss, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text("Annuleren")
                }
            }
        }
    }
    editTarget?.let { (dayIndex, exerciseIndex) ->
        val exercise = routine.days.getOrNull(dayIndex)?.exercises?.getOrNull(exerciseIndex)
        if (exercise != null) AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("${exercise.exerciseName} vervangen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TapOnlyOutlinedTextField(
                        value = exerciseQuery,
                        onValueChange = { exerciseQuery = it },
                        label = { Text("Zoek bestaande oefening") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(availableExercises.filter { exerciseQuery.isBlank() || it.name.contains(exerciseQuery, ignoreCase = true) }.take(40), key = { it.id }) { replacement ->
                            TextButton(
                                onClick = { onReplaceExercise(dayIndex, exerciseIndex, replacement); editTarget = null },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("${replacement.name} · ${replacement.equipment}") }
                        }
                    }
                }
            },
            confirmButton = {
                if (routine.days[dayIndex].exercises.size > 1) TextButton(onClick = {
                    onRemoveExercise(dayIndex, exerciseIndex)
                    editTarget = null
                }) { Text("Verwijderen") }
            },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text("Sluiten") } },
        )
    }
}

private fun GeneratedRoutineSource.label(): String = when (this) {
    GeneratedRoutineSource.GEMINI_2_5_FLASH -> "Gemini 2.5 Flash"
    GeneratedRoutineSource.OPENAI -> "OpenAI"
    GeneratedRoutineSource.LOCAL_FALLBACK -> "Lokale analyse"
}

@Composable
private fun GeneratedRoutineInfoPill(
    label: String,
    modifier: Modifier = Modifier,
    contentDescription: String = label,
) {
    Card(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeneratedRoutineMetaRow(routine: GeneratedRoutine) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GeneratedRoutineInfoPill(label = "${routine.days.size} dagen")
        if (routine.estimatedDurationMinutes > 0) {
            GeneratedRoutineInfoPill(label = "${routine.estimatedDurationMinutes} min/sessie")
        }
        GeneratedRoutineInfoPill(label = "${routine.days.sumOf { it.exercises.size }} oefeningen")
    }
}
