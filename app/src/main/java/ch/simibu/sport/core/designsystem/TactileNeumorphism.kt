package ch.simibu.sport.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class TactileElevation {
    FLAT,
    RAISED,
    PRESSED,
    CONVEX
}

fun Modifier.tactileShadow(
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: TactileElevation = TactileElevation.RAISED,
    highlightColor: Color = Color.White.copy(alpha = 0.8f),
    shadowColor: Color = Color.Black.copy(alpha = 0.08f),
    spread: Dp = 4.dp
): Modifier = drawWithCache {
    if (elevation == TactileElevation.FLAT) {
        return@drawWithCache onDrawBehind {}
    }

    val spreadPx = spread.toPx()
    val cornerRadiusPx = 16.dp.toPx()
    val isPressed = elevation == TactileElevation.PRESSED
    val offsetDistance = if (isPressed) spreadPx * 0.4f else spreadPx * 0.8f

    val shadowPaint = Paint().asFrameworkPaint().apply {
        color = shadowColor.toArgb()
        maskFilter = android.graphics.BlurMaskFilter(
            (spreadPx * if (isPressed) 0.5f else 1.2f).coerceAtLeast(0.5f),
            android.graphics.BlurMaskFilter.Blur.NORMAL
        )
    }
    val highlightPaint = Paint().asFrameworkPaint().apply {
        color = highlightColor.toArgb()
        maskFilter = android.graphics.BlurMaskFilter(
            (spreadPx * if (isPressed) 0.5f else 1.0f).coerceAtLeast(0.5f),
            android.graphics.BlurMaskFilter.Blur.NORMAL
        )
    }

    onDrawBehind {
        drawIntoCanvas { canvas ->
            // Bottom-right shadow
            canvas.nativeCanvas.drawRoundRect(
                offsetDistance,
                offsetDistance,
                size.width + offsetDistance,
                size.height + offsetDistance,
                cornerRadiusPx,
                cornerRadiusPx,
                shadowPaint
            )

            // Top-left highlight
            canvas.nativeCanvas.drawRoundRect(
                -offsetDistance * 0.7f,
                -offsetDistance * 0.7f,
                size.width - offsetDistance * 0.7f,
                size.height - offsetDistance * 0.7f,
                cornerRadiusPx,
                cornerRadiusPx,
                highlightPaint
            )
        }
    }
}

@Composable
fun Modifier.tactileClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tactileScale"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

@Composable
fun TactileCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: TactileElevation = TactileElevation.RAISED,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tactileColors = LocalTactileColors.current
    val borderColor = if (tactileColors.isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }

    val clickableModifier = if (onClick != null) {
        Modifier.tactileClickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(clickableModifier)
            .tactileShadow(
                shape = shape,
                elevation = elevation,
                highlightColor = tactileColors.highlight,
                shadowColor = tactileColors.shadow
            )
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape),
        content = content
    )
}

@Composable
fun TactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    gradient: Brush = Brush.linearGradient(
        colors = listOf(SportPurple, SportRuby)
    ),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val tactileColors = LocalTactileColors.current

    Box(
        modifier = modifier
            .tactileClickable(enabled = enabled, onClick = onClick)
            .tactileShadow(
                shape = shape,
                elevation = if (enabled) TactileElevation.RAISED else TactileElevation.FLAT,
                highlightColor = tactileColors.highlight,
                shadowColor = tactileColors.shadow,
                spread = 3.dp
            )
            .clip(shape)
            .background(gradient)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun TactileChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tactileColors = LocalTactileColors.current
    val shape = RoundedCornerShape(20.dp)

    val bg = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        if (tactileColors.isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
    }

    Box(
        modifier = modifier
            .tactileClickable(onClick = onClick)
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}
