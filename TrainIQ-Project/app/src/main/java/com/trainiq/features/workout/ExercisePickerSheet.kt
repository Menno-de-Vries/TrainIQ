package com.trainiq.features.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.trainiq.domain.model.Exercise
import com.trainiq.core.ui.bringIntoViewOnFocus
import com.trainiq.core.ui.clearFocusOnScrollOrDrag

data class CustomExerciseDraft(
    val name: String,
    val muscleGroup: String,
    val equipment: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    exercises: List<Exercise>,
    onSelect: (Exercise) -> Unit,
    onCreateCustom: (CustomExerciseDraft) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var showCustomForm by remember { mutableStateOf(false) }
    val filteredExercises by remember(exercises, query) {
        derivedStateOf {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isEmpty()) {
                exercises
            } else {
                exercises.filter { exercise ->
                    exercise.name.contains(normalizedQuery, ignoreCase = true) ||
                        exercise.muscleGroup.contains(normalizedQuery, ignoreCase = true) ||
                        exercise.equipment.contains(normalizedQuery, ignoreCase = true)
                }
            }
        }
    }

    ModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Choose exercise", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus(),
                label = { Text("Search exercises") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            OutlinedButton(
                onClick = { showCustomForm = !showCustomForm },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(if (showCustomForm) "Hide custom exercise" else "Add custom exercise")
            }
            if (showCustomForm) {
                CustomExerciseForm(
                    initialName = query,
                    onCreateCustom = onCreateCustom,
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearFocusOnScrollOrDrag(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (filteredExercises.isEmpty()) {
                    item {
                        Text(
                            text = "No matching exercises.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                } else {
                    items(filteredExercises, key = { it.id }) { exercise ->
                        ExercisePickerRow(
                            exercise = exercise,
                            onSelect = { onSelect(exercise) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerRow(
    exercise: Exercise,
    onSelect: () -> Unit,
) {
    Card(onClick = onSelect) {
        ListItem(
            headlineContent = { Text(exercise.name) },
            supportingContent = { Text("${exercise.muscleGroup} - ${exercise.equipment}") },
        )
    }
}

@Composable
private fun CustomExerciseForm(
    initialName: String,
    onCreateCustom: (CustomExerciseDraft) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var muscleGroup by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }
    val canCreate = name.isNotBlank() && muscleGroup.isNotBlank() && equipment.isNotBlank()

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Custom exercise", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Exercise name") },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = muscleGroup,
                    onValueChange = { muscleGroup = it },
                    modifier = Modifier.weight(1f).bringIntoViewOnFocus(),
                    label = { Text("Muscle") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    modifier = Modifier.weight(1f).bringIntoViewOnFocus(),
                    label = { Text("Equipment") },
                    singleLine = true,
                )
            }
            Button(
                onClick = {
                    onCreateCustom(
                        CustomExerciseDraft(
                            name = name.trim(),
                            muscleGroup = muscleGroup.trim(),
                            equipment = equipment.trim(),
                        ),
                    )
                },
                enabled = canCreate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Use custom exercise")
            }
        }
    }
}
