package com.trainiq.features.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun PlateBarDiagram(
    plates: List<Float>,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.onSurfaceVariant
    val sleeveColor = MaterialTheme.colorScheme.outline
    val density = LocalDensity.current
    val contentDescription = rememberPlateDescription(plates)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val barHeight = with(density) { 4.dp.toPx() }
            val collarWidth = with(density) { 5.dp.toPx() }
            val gap = with(density) { 2.dp.toPx() }
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val sleeveLength = size.width * 0.42f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(centerX - sleeveLength, centerY - barHeight / 2f),
                size = Size(sleeveLength * 2f, barHeight),
                cornerRadius = CornerRadius(barHeight, barHeight),
            )
            drawRoundRect(
                color = sleeveColor,
                topLeft = Offset(centerX - collarWidth / 2f, centerY - size.height * 0.3f),
                size = Size(collarWidth, size.height * 0.6f),
                cornerRadius = CornerRadius(collarWidth, collarWidth),
            )

            drawPlateStack(
                plates = plates,
                centerX = centerX,
                centerY = centerY,
                startOffset = collarWidth + gap,
                direction = -1f,
                gap = gap,
            )
            drawPlateStack(
                plates = plates,
                centerX = centerX,
                centerY = centerY,
                startOffset = collarWidth + gap,
                direction = 1f,
                gap = gap,
            )
        }
    }
}

@Composable
private fun rememberPlateDescription(plates: List<Float>): String =
    if (plates.isEmpty()) {
        "No plates loaded"
    } else {
        "Plates per side: ${plates.joinToString(", ") { it.formatPlateKg() }}"
    }

private fun DrawScope.drawPlateStack(
    plates: List<Float>,
    centerX: Float,
    centerY: Float,
    startOffset: Float,
    direction: Float,
    gap: Float,
) {
    var offset = startOffset
    plates.forEach { plate ->
        val width = max(size.width * 0.018f, size.width * (0.012f + plate / 1800f))
        val height = size.height * (0.38f + (plate.coerceAtMost(25f) / 25f) * 0.52f)
        val left = if (direction < 0f) {
            centerX - offset - width
        } else {
            centerX + offset
        }
        drawRoundRect(
            color = plateColor(plate),
            topLeft = Offset(left, centerY - height / 2f),
            size = Size(width, height),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
        offset += width + gap
    }
}

private fun plateColor(weightKg: Float): Color = when {
    weightKg >= 25f -> Color(0xFFD32F2F)
    weightKg >= 20f -> Color(0xFF1976D2)
    weightKg >= 15f -> Color(0xFFFBC02D)
    weightKg >= 10f -> Color(0xFF388E3C)
    weightKg >= 5f -> Color(0xFFF5F5F5)
    weightKg >= 2.5f -> Color(0xFF212121)
    else -> Color(0xFF9E9E9E)
}

private fun Float.formatPlateKg(): String =
    if (this % 1f == 0f) "${toInt()} kg" else "%.2f kg".format(this)
