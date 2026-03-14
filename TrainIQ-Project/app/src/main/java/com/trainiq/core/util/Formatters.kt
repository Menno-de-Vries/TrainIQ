package com.trainiq.core.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trainiq.core.theme.spacing
import com.trainiq.domain.model.ChartPoint
import com.trainiq.domain.model.EnergyBalanceSnapshot
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

private val cardShape = RoundedCornerShape(24.dp)

@Composable
fun MetricCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = cardShape,
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProgressCard(title: String, progress: Float, current: String, target: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = cardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(current, style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                strokeCap = StrokeCap.Round,
            )
            Text("Target: $target", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EnergyBalanceCard(
    energyBalance: EnergyBalanceSnapshot?,
    calorieTarget: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = cardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text("Energy balance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            if (energyBalance == null) {
                Text("Complete your profile to unlock BMR, TEF, NEAT, and training energy math.", style = MaterialTheme.typography.bodyMedium)
            } else {
                val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                val progressColor = MaterialTheme.colorScheme.primary
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Canvas(modifier = Modifier.size(156.dp)) {
                        val stroke = 18.dp.toPx()
                        val startAngle = 150f
                        val sweep = 240f
                        val targetProgress = if (calorieTarget <= 0) 0f else (energyBalance.caloriesIn / calorieTarget.toFloat()).coerceIn(0f, 1.2f)
                        drawArc(
                            color = trackColor,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                        drawArc(
                            color = progressColor,
                            startAngle = startAngle,
                            sweepAngle = sweep * targetProgress.coerceAtMost(1f),
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("${energyBalance.balance} kcal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("Balance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("In ${energyBalance.caloriesIn} • Out ${energyBalance.caloriesOut} • Target $calorieTarget", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "BMR ${energyBalance.bmr} • TEF ${energyBalance.tefCalories} • NEAT ${energyBalance.neatCalories} • EAT ${energyBalance.workoutCalories}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun MacroBreakdownCard(
    protein: Int,
    proteinTarget: Int,
    carbs: Int,
    carbsTarget: Int,
    fat: Int,
    fatTarget: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = cardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text("Macro breakdown", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            MacroProgressRow("Protein", protein, proteinTarget, MaterialTheme.colorScheme.primary)
            MacroProgressRow("Carbs", carbs, carbsTarget, MaterialTheme.colorScheme.secondary)
            MacroProgressRow("Fat", fat, fatTarget, MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun MacroProgressRow(
    label: String,
    current: Int,
    target: Int,
    color: androidx.compose.ui.graphics.Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("$current / ${target.coerceAtLeast(0)} g", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { if (target <= 0) 0f else (current / target.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
fun WorkoutExerciseItem(plan: WorkoutExercisePlan, loggedSetCount: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = cardShape,
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            Text(plan.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${plan.targetSets} sets • ${plan.repRange} reps • ${plan.restSeconds}s rest", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
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
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        OutlinedTextField(value = weight, onValueChange = onWeightChange, label = { Text("Weight") }, modifier = Modifier.weight(1f))
        OutlinedTextField(value = reps, onValueChange = onRepsChange, label = { Text("Reps") }, modifier = Modifier.weight(1f))
        OutlinedTextField(value = rpe, onValueChange = onRpeChange, label = { Text("RPE") }, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ChartComposable(title: String, points: List<ChartPoint>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = cardShape,
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (points.isEmpty()) {
                Text("No data yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
