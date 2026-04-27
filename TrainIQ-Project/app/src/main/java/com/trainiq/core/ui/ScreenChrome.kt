package com.trainiq.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trainiq.core.theme.spacing
import com.trainiq.core.theme.trainIqColors
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    actionIcon: ImageVector? = null,
    actionContentDescription: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    AppScreenHeader(
        title = title,
        subtitle = subtitle,
        actionIcon = actionIcon,
        actionContentDescription = actionContentDescription,
        onActionClick = onActionClick,
    )
}

@Composable
fun MessageCard(message: String, onDismiss: (() -> Unit)? = null) {
    AppCard(accent = MaterialTheme.colorScheme.tertiary) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.trainIqColors.mutedText)
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) { Text("Sluiten") }
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    AppCard {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        content()
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
    val surface = MaterialTheme.trainIqColors.cardElevated
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
    AppCard(modifier = modifier) {
        Column(
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

@Composable
fun AnimatedScreenState(
    targetState: Any?,
    modifier: Modifier = Modifier,
    content: @Composable (Any?) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = { fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180)) },
        label = "screen-state",
    ) { state ->
        content(state)
    }
}

@Composable
fun PermissionManagerCard(
    status: HealthConnectStatus,
    onRequestPermission: () -> Unit,
    onOpenInstall: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text("Health Connect", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(status.message, style = MaterialTheme.typography.bodyMedium)
            when (status.state) {
                HealthConnectState.PROVIDER_MISSING -> FilledTonalButton(onClick = onOpenInstall) { Text("Installeren of bijwerken") }
                HealthConnectState.PERMISSION_REQUIRED -> FilledTonalButton(onClick = onRequestPermission) { Text("Opnieuw verbinden") }
                HealthConnectState.CONNECTED, HealthConnectState.NO_DATA -> OutlinedButton(onClick = onOpenSettings) { Text("Health Connect openen") }
                HealthConnectState.ERROR -> OutlinedButton(onClick = onRefresh) { Text("Opnieuw proberen") }
                HealthConnectState.UNSUPPORTED -> Text("Dit apparaat ondersteunt Health Connect niet.", style = MaterialTheme.typography.bodyMedium)
            }
            status.lastSyncedAt?.let {
                Text("Laatst gesynchroniseerd: $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
