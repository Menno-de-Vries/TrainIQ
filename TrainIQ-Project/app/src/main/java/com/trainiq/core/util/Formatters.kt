package com.trainiq.core.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trainiq.domain.model.ChartPoint
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.WorkoutExercisePlan
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")

fun Long.toReadableDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

fun todayEpochMillis(): Long =
    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

@Composable
fun MetricCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProgressCard(title: String, progress: Float, current: String, target: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(24.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.size(56.dp),
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(current, style = MaterialTheme.typography.bodyLarge)
                Text("Target: $target", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun WorkoutExerciseItem(plan: WorkoutExercisePlan, loggedSetCount: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(plan.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${plan.targetSets} sets • ${plan.repRange} reps • ${plan.restSeconds}s rest")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(plan.exercise.muscleGroup) })
                AssistChip(onClick = {}, label = { Text("$loggedSetCount logged") })
            }
        }
    }
}

@Composable
fun SetLogger(
    weight: String,
    reps: String,
    rpe: String,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onRpeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = weight, onValueChange = onWeightChange, label = { Text("Weight") }, modifier = Modifier.weight(1f))
        OutlinedTextField(value = reps, onValueChange = onRepsChange, label = { Text("Reps") }, modifier = Modifier.weight(1f))
        OutlinedTextField(value = rpe, onValueChange = onRpeChange, label = { Text("RPE") }, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ChartComposable(title: String, points: List<ChartPoint>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(modifier = modifier, shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (points.isEmpty()) {
                Text("No data yet")
            } else {
                Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    val maxValue = points.maxOf { it.value.toFloat() }.coerceAtLeast(1f)
                    val stepX = if (points.size == 1) size.width else size.width / (points.size - 1)
                    val path = Path()
                    points.forEachIndexed { index, point ->
                        val x = index * stepX
                        val y = size.height - ((point.value.toFloat() / maxValue) * size.height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(primaryColor, radius = 6f, center = Offset(x, y))
                    }
                    drawPath(path = path, color = primaryColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    points.take(4).forEach { Text(it.label, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

fun LoggedSet.volume(): Double = weight * reps
