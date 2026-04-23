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
    WARMUP,
    WORKING,
    TOP_SET,
    BACKOFF,
    DROP,
    FAILURE,
}

fun WorkoutSetType.label(): String = when (this) {
    WorkoutSetType.WARMUP -> "Warmup"
    WorkoutSetType.WORKING -> "Working"
    WorkoutSetType.TOP_SET -> "Top set"
    WorkoutSetType.BACKOFF -> "Backoff"
    WorkoutSetType.DROP -> "Drop"
    WorkoutSetType.FAILURE -> "Failure"
}

@Composable
fun setTypeContainerColor(type: WorkoutSetType): Color = when (type) {
    WorkoutSetType.WARMUP -> MaterialTheme.colorScheme.tertiaryContainer
    WorkoutSetType.WORKING -> MaterialTheme.colorScheme.primaryContainer
    WorkoutSetType.TOP_SET -> MaterialTheme.colorScheme.secondaryContainer
    WorkoutSetType.BACKOFF -> MaterialTheme.colorScheme.surfaceVariant
    WorkoutSetType.DROP -> MaterialTheme.colorScheme.errorContainer
    WorkoutSetType.FAILURE -> MaterialTheme.colorScheme.errorContainer
}

@Composable
fun setTypeContentColor(type: WorkoutSetType): Color = when (type) {
    WorkoutSetType.WARMUP -> MaterialTheme.colorScheme.onTertiaryContainer
    WorkoutSetType.WORKING -> MaterialTheme.colorScheme.onPrimaryContainer
    WorkoutSetType.TOP_SET -> MaterialTheme.colorScheme.onSecondaryContainer
    WorkoutSetType.BACKOFF -> MaterialTheme.colorScheme.onSurfaceVariant
    WorkoutSetType.DROP -> MaterialTheme.colorScheme.onErrorContainer
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
