@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.trainiq.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trainiq.core.theme.radii
import com.trainiq.core.theme.spacing
import com.trainiq.core.theme.trainIqColors
import com.trainiq.domain.model.ChartPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackgroundBrush()),
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .clearFocusOnTapOutside(),
            containerColor = Color.Transparent,
            bottomBar = bottomBar,
            content = content,
        )
    }
}

@Composable
fun Modifier.bringIntoViewOnFocus(delayMillis: Long = 250L): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val bringIntoViewJob = remember { mutableStateOf<Job?>(null) }
    return bringIntoViewRequester(requester)
        .onFocusChanged { focusState ->
            bringIntoViewJob.value?.cancel()
            if (focusState.isFocused) {
                bringIntoViewJob.value = scope.launch {
                    delay(delayMillis)
                    requester.bringIntoView()
                }
            }
        }
}

@Composable
fun Modifier.clearFocusOnTapOutside(): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return pointerInput(focusManager, keyboardController) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val drag = awaitTouchSlopOrCancellation(down.id) { _, _ -> }
            if (drag == null) {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            }
        }
    }
}

@Composable
fun Modifier.clearFocusOnScrollOrDrag(): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return pointerInput(focusManager, keyboardController) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            awaitTouchSlopOrCancellation(down.id) { _, _ ->
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            }
        }
    }
}

@Composable
fun AppBackgroundBrush(): Brush {
    val colors = MaterialTheme.trainIqColors
    return Brush.linearGradient(
        colors = listOf(
            colors.appBackground,
            colors.appBackground,
            colors.backgroundGlow.copy(alpha = 0.30f),
        ),
        start = Offset.Zero,
        end = Offset(900f, 1800f),
    )
}

@Composable
fun AppScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    actionContentDescription: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.trainIqColors.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (actionIcon != null && actionContentDescription != null && onActionClick != null) {
            SecondaryActionButton(onClick = onActionClick, contentPadding = PaddingValues(12.dp)) {
                Icon(actionIcon, contentDescription = actionContentDescription, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    elevated: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.spacing.medium),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.trainIqColors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.radii.card),
        border = BorderStroke(1.dp, colors.cardBorder),
        colors = CardDefaults.cardColors(
            containerColor = if (elevated) colors.cardElevated else colors.card,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.Transparent,
                            accent.copy(alpha = if (elevated) 0.08f else 0.035f),
                        ),
                    ),
                )
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            content = content,
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        trailing?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.trainIqColors.mutedText)
        }
    }
}

@Composable
fun AppChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick ?: {},
        modifier = modifier,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        shape = RoundedCornerShape(MaterialTheme.radii.chip),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.Transparent,
            selectedBorderColor = Color.Transparent,
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            labelColor = accent,
            selectedContainerColor = accent.copy(alpha = 0.18f),
            selectedLabelColor = accent,
        ),
    )
}

@Composable
fun PrimaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.primary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(MaterialTheme.radii.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.trainIqColors.track,
            disabledContentColor = MaterialTheme.trainIqColors.mutedText,
        ),
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
fun SecondaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.primary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(MaterialTheme.radii.button),
        border = BorderStroke(1.dp, MaterialTheme.trainIqColors.cardBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = accent,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
            disabledContentColor = MaterialTheme.trainIqColors.mutedText,
        ),
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.bringIntoViewOnFocus(),
        singleLine = singleLine,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.trainIqColors.cardBorder,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    )
}

@Composable
fun ProgressCard(
    title: String,
    subtitle: String,
    value: String? = null,
    progress: Float,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    AppCard(modifier = modifier, accent = accent) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            value?.let {
                Text(it, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = accent)
            }
        }
        AppLinearProgress(progress = progress, accent = accent, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    chips: List<String> = emptyList(),
) {
    AppCard(modifier = modifier, accent = accent) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = accent)
        }
        if (chips.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                chips.take(3).forEach { AppChip(label = it, accent = accent) }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    AppCard(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
        if (actionLabel != null && onAction != null) {
            SecondaryActionButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ChartCard(
    title: String,
    subtitle: String,
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    AppCard(modifier = modifier, accent = accent) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.trainIqColors.mutedText)
        if (points.isEmpty()) {
            EmptyMiniChart()
        } else {
            AppLineChart(points = points, accent = accent, modifier = Modifier.fillMaxWidth().height(140.dp))
        }
    }
}

@Composable
fun AppLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = accent,
        trackColor = MaterialTheme.trainIqColors.track,
        strokeCap = StrokeCap.Round,
    )
}

@Composable
fun AppLineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val grid = MaterialTheme.trainIqColors.cardBorder
    Canvas(modifier = modifier) {
        val maxValue = points.maxOfOrNull { it.value.toFloat() } ?: 1f
        val minValue = points.minOfOrNull { it.value.toFloat() } ?: 0f
        val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
        val horizontalPadding = 10.dp.toPx()
        repeat(4) { index ->
            val y = size.height * (index + 1) / 5f
            drawLine(grid, Offset(horizontalPadding, y), Offset(size.width - horizontalPadding, y), strokeWidth = 1.dp.toPx())
        }
        val stepX = if (points.size == 1) 0f else (size.width - horizontalPadding * 2) / (points.size - 1)
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = horizontalPadding + index * stepX
            val y = size.height - ((point.value.toFloat() - minValue) / range * (size.height * 0.74f)) - size.height * 0.13f
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, accent, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        points.forEachIndexed { index, point ->
            val x = horizontalPadding + index * stepX
            val y = size.height - ((point.value.toFloat() - minValue) / range * (size.height * 0.74f)) - size.height * 0.13f
            drawCircle(accent, radius = 5.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
private fun EmptyMiniChart() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .border(1.dp, MaterialTheme.trainIqColors.cardBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("No data yet", color = MaterialTheme.trainIqColors.mutedText)
    }
}

@Composable
fun AppDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancel",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.trainIqColors.card,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.trainIqColors.mutedText,
        shape = RoundedCornerShape(MaterialTheme.radii.sheet),
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(body) },
        confirmButton = { PrimaryActionButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.trainIqColors.card,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = MaterialTheme.radii.sheet, topEnd = MaterialTheme.radii.sheet),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            content = content,
        )
    }
}
