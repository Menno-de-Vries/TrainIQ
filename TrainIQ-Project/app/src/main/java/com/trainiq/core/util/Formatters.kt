package com.trainiq.core.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trainiq.core.theme.spacing
import com.trainiq.core.theme.trainIqColors
import com.trainiq.core.ui.AppCard
import com.trainiq.core.ui.AppChip
import com.trainiq.core.ui.AppLinearProgress
import com.trainiq.core.ui.ChartCard
import com.trainiq.domain.model.ChartPoint
import com.trainiq.domain.model.EnergyBalanceSnapshot
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.WorkoutExercisePlan
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")

fun Long.toReadableDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

fun todayEpochMillis(): Long =
    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

internal fun energyBalanceValueText(balance: Int): String {
    val status = when {
        balance < 0 -> "tekort"
        balance > 0 -> "overschot"
        else -> "in balans"
    }
    return "${abs(balance)} kcal $status"
}

internal fun energyBalanceMetaText(caloriesIn: Int, caloriesOut: Int, calorieTarget: Int): String =
    "In $caloriesIn kcal - Uit $caloriesOut kcal - Doel $calorieTarget kcal"

internal data class EnergyOutBreakdownRow(
    val label: String,
    val value: Int,
    val source: String,
)

internal fun energyOutBreakdownRows(energyBalance: EnergyBalanceSnapshot): List<EnergyOutBreakdownRow> =
    listOf(
        EnergyOutBreakdownRow("BMR", energyBalance.bmr, "Profielschatting"),
        EnergyOutBreakdownRow("TEF", energyBalance.tefCalories, "Uit gelogde intake"),
        EnergyOutBreakdownRow("Stappen", energyBalance.neatCalories, "Health Connect"),
        EnergyOutBreakdownRow("Training", energyBalance.workoutCalories, "Krachttraining"),
    )

@Composable
fun MetricCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
    }
}

@Composable
fun ProgressCard(title: String, progress: Float, current: String, target: String, modifier: Modifier = Modifier) {
    com.trainiq.core.ui.ProgressCard(
        title = title,
        subtitle = current,
        value = target,
        progress = progress,
        modifier = modifier,
    )
}

@Composable
fun EnergyBalanceCard(
    energyBalance: EnergyBalanceSnapshot?,
    calorieTarget: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (calorieTarget > 0 && energyBalance != null) {
        (energyBalance.caloriesIn / calorieTarget.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
            Text("Energiebalans", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(
                energyBalance?.let { "Netto status met zicht op de bronnen achter calorieën uit." }
                    ?: "Vul je profiel in voor rustverbranding, vertering, beweging en trainingsverbruik.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.trainIqColors.mutedText,
            )
            energyBalance?.let {
                Text(
                    energyBalanceValueText(it.balance),
                    modifier = Modifier.align(Alignment.End),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        AppLinearProgress(progress = progress)
        energyBalance?.let {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Calorieën in", style = MaterialTheme.typography.labelLarge)
                Text("${it.caloriesIn} kcal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.mutedText)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Calorieën uit", style = MaterialTheme.typography.labelLarge)
                Text("${it.caloriesOut} kcal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.mutedText)
            }
            Text(
                energyBalanceMetaText(it.caloriesIn, it.caloriesOut, calorieTarget),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.trainIqColors.mutedText,
            )
            Text(
                "Waar komt calorieën uit vandaan?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            energyOutBreakdownRows(it).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.label, style = MaterialTheme.typography.labelLarge)
                        Text(row.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.trainIqColors.mutedText)
                    }
                    Text("${row.value} kcal", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
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
    AppCard(modifier = modifier) {
        Text("Macrodoelen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(
            "Eiwit $protein/$proteinTarget g - Kh $carbs/$carbsTarget g - Vet $fat/$fatTarget g",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.trainIqColors.mutedText,
        )
        MacroProgressRow("Eiwit", protein, proteinTarget, MaterialTheme.colorScheme.primary)
        MacroProgressRow("Koolhydraten", carbs, carbsTarget, MaterialTheme.colorScheme.tertiary)
        MacroProgressRow("Vet", fat, fatTarget, MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun MacroProgressRow(
    label: String,
    current: Int,
    target: Int,
    color: androidx.compose.ui.graphics.Color,
) {
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("$current / ${target.coerceAtLeast(0)} g", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.mutedText)
        }
        AppLinearProgress(
            progress = if (target <= 0) 0f else (current / target.toFloat()).coerceIn(0f, 1f),
            accent = color,
        )
    }
}

@Composable
fun WorkoutExerciseItem(plan: WorkoutExercisePlan, loggedSetCount: Int, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Text(plan.exercise.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text("${plan.targetSets} sets - ${plan.repRange} reps - ${plan.restSeconds}s rust", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            AppChip(label = plan.exercise.muscleGroup)
            AppChip(label = "$loggedSetCount gelogd")
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
        OutlinedTextField(value = weight, onValueChange = onWeightChange, label = { Text("Gewicht") }, modifier = Modifier.weight(1f))
        OutlinedTextField(value = reps, onValueChange = onRepsChange, label = { Text("Herhalingen") }, modifier = Modifier.weight(1f))
        OutlinedTextField(value = rpe, onValueChange = onRpeChange, label = { Text("RPE") }, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ChartComposable(title: String, points: List<ChartPoint>, modifier: Modifier = Modifier) {
    ChartCard(title = title, subtitle = "${points.size} metingen", points = points, modifier = modifier)
}

fun LoggedSet.volume(): Double = weight * reps

