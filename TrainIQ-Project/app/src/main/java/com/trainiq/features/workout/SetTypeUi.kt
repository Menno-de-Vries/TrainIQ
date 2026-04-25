package com.trainiq.features.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class WorkoutSetType {
    WARM_UP,
    NORMAL,
    BACK_OFF,
    DROP_SET,
    FAILURE,
}

fun WorkoutSetType.label(): String = when (this) {
    WorkoutSetType.NORMAL -> "Normaal"
    WorkoutSetType.WARM_UP -> "Warm-up"
    WorkoutSetType.DROP_SET -> "Drop set"
    WorkoutSetType.FAILURE -> "Failure"
    WorkoutSetType.BACK_OFF -> "Back-off"
}

@Composable
fun setTypeContainerColor(type: WorkoutSetType): Color = when (type) {
    WorkoutSetType.WARM_UP -> MaterialTheme.colorScheme.tertiaryContainer
    WorkoutSetType.NORMAL -> MaterialTheme.colorScheme.primaryContainer
    WorkoutSetType.BACK_OFF -> MaterialTheme.colorScheme.surfaceVariant
    WorkoutSetType.DROP_SET -> MaterialTheme.colorScheme.errorContainer
    WorkoutSetType.FAILURE -> MaterialTheme.colorScheme.errorContainer
}

@Composable
fun setTypeContentColor(type: WorkoutSetType): Color = when (type) {
    WorkoutSetType.WARM_UP -> MaterialTheme.colorScheme.onTertiaryContainer
    WorkoutSetType.NORMAL -> MaterialTheme.colorScheme.onPrimaryContainer
    WorkoutSetType.BACK_OFF -> MaterialTheme.colorScheme.onSurfaceVariant
    WorkoutSetType.DROP_SET -> MaterialTheme.colorScheme.onErrorContainer
    WorkoutSetType.FAILURE -> MaterialTheme.colorScheme.onErrorContainer
}

@Composable
fun SetTypeLabel(
    type: WorkoutSetType,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = setTypeContainerColor(type),
        contentColor = setTypeContentColor(type),
    ) {
        Text(
            text = type.label(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun SetTypeLabelRow(
    selectedType: WorkoutSetType,
    modifier: Modifier = Modifier,
    availableTypes: List<WorkoutSetType> = WorkoutSetType.entries,
    onSelectedTypeChange: (WorkoutSetType) -> Unit = {},
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        availableTypes.forEach { type ->
            Surface(
                onClick = { onSelectedTypeChange(type) },
                shape = RoundedCornerShape(50),
                color = if (type == selectedType) {
                    setTypeContainerColor(type)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (type == selectedType) {
                    setTypeContentColor(type)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Text(
                    text = type.label(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
