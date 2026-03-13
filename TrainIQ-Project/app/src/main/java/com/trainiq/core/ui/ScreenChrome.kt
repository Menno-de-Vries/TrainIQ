package com.trainiq.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ScreenHeader(
    title: String,
    actionIcon: ImageVector? = null,
    actionContentDescription: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (actionIcon != null && onActionClick != null && actionContentDescription != null) {
            IconButton(onClick = onActionClick) {
                Icon(imageVector = actionIcon, contentDescription = actionContentDescription)
            }
        }
    }
}

@Composable
fun MessageCard(message: String, onDismiss: (() -> Unit)? = null) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(message, modifier = Modifier.weight(1f))
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-progress",
    )
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val surface = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    return background(
        brush = Brush.linearGradient(
            colors = listOf(surface, highlight, surface),
            start = Offset(x = -400f + (800f * progress), y = 0f),
            end = Offset(x = 0f + (800f * progress), y = 400f),
        ),
    )
}

@Composable
fun ShimmerCardPlaceholder(
    modifier: Modifier = Modifier,
    lineCount: Int = 3,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(lineCount) { index ->
                val widthFraction = when (index) {
                    0 -> 0.5f
                    lineCount - 1 -> 0.85f
                    else -> 1f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(widthFraction)
                        .height(if (index == 0) 24.dp else 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmer(),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
