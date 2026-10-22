package ch.simibu.sport.presentation.recovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.simibu.sport.R
import ch.simibu.sport.core.model.BodyPart

@Composable
fun AnatomicalDualBodyView(
    recoveryMap: Map<BodyPart, MuscleRecoveryData>,
    selectedMuscle: BodyPart?,
    onMuscleSelected: (BodyPart?) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.recovery_front_view).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            AnatomicalBodySilhouette(
                viewType = BodyViewType.FRONT,
                recoveryMap = recoveryMap,
                selectedMuscle = selectedMuscle,
                onMuscleSelected = onMuscleSelected,
                height = height
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.recovery_back_view).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            AnatomicalBodySilhouette(
                viewType = BodyViewType.BACK,
                recoveryMap = recoveryMap,
                selectedMuscle = selectedMuscle,
                onMuscleSelected = onMuscleSelected,
                height = height
            )
        }
    }
}

@Composable
fun AnatomicalBodySilhouette(
    viewType: BodyViewType,
    recoveryMap: Map<BodyPart, MuscleRecoveryData>,
    selectedMuscle: BodyPart?,
    onMuscleSelected: (BodyPart?) -> Unit,
    height: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val width = height * 0.52f
    val neutralBodyTone = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val outlineTone = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Canvas(
        modifier = modifier
            .size(width = width, height = height)
            .pointerInput(viewType) {
                detectTapGestures { offset ->
                    val normX = offset.x / size.width
                    val normY = offset.y / size.height
                    val tappedPart = findTappedMuscle(viewType, normX, normY)
                    if (tappedPart == selectedMuscle) {
                        onMuscleSelected(null)
                    } else {
                        onMuscleSelected(tappedPart)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw Neutral Head & Neck Base
        drawHeadAndNeck(w, h, neutralBodyTone, outlineTone)

        // 2. Draw view-specific muscle groups
        if (viewType == BodyViewType.FRONT) {
            drawFrontMuscles(w, h, recoveryMap, selectedMuscle, outlineTone)
        } else {
            drawBackMuscles(w, h, recoveryMap, selectedMuscle, outlineTone)
        }
    }
}

private fun DrawScope.drawHeadAndNeck(w: Float, h: Float, bodyTone: Color, outlineTone: Color) {
    // Head
    drawCircle(
        color = bodyTone,
        radius = 0.055f * w,
        center = Offset(0.5f * w, 0.07f * h)
    )
    drawCircle(
        color = outlineTone,
        radius = 0.055f * w,
        center = Offset(0.5f * w, 0.07f * h),
        style = Stroke(width = 1.dp.toPx())
    )

    // Neck
    drawRoundRect(
        color = bodyTone,
        topLeft = Offset(0.44f * w, 0.11f * h),
        size = Size(0.12f * w, 0.045f * h),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
}

private fun DrawScope.drawFrontMuscles(
    w: Float,
    h: Float,
    recoveryMap: Map<BodyPart, MuscleRecoveryData>,
    selectedMuscle: BodyPart?,
    outlineColor: Color
) {
    // Chest (Pectorals)
    drawMusclePath(
        path = createChestPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.CHEST),
        isSelected = selectedMuscle == BodyPart.CHEST,
        outlineColor = outlineColor
    )

    // Shoulders (Front Deltoids)
    drawMusclePath(
        path = createFrontShouldersPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.SHOULDERS),
        isSelected = selectedMuscle == BodyPart.SHOULDERS,
        outlineColor = outlineColor
    )

    // Biceps
    drawMusclePath(
        path = createBicepsPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.BICEPS),
        isSelected = selectedMuscle == BodyPart.BICEPS,
        outlineColor = outlineColor
    )

    // Forearms
    drawMusclePath(
        path = createFrontForearmsPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.FOREARMS),
        isSelected = selectedMuscle == BodyPart.FOREARMS,
        outlineColor = outlineColor
    )

    // Core / Abs
    drawMusclePath(
        path = createCorePath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.CORE),
        isSelected = selectedMuscle == BodyPart.CORE,
        outlineColor = outlineColor
    )

    // Legs (Front - Quads & Calves)
    drawMusclePath(
        path = createFrontLegsPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.LEGS),
        isSelected = selectedMuscle == BodyPart.LEGS,
        outlineColor = outlineColor
    )
}

private fun DrawScope.drawBackMuscles(
    w: Float,
    h: Float,
    recoveryMap: Map<BodyPart, MuscleRecoveryData>,
    selectedMuscle: BodyPart?,
    outlineColor: Color
) {
    // Shoulders / Trapezius (Back)
    drawMusclePath(
        path = createTrapsAndRearDeltsPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.SHOULDERS),
        isSelected = selectedMuscle == BodyPart.SHOULDERS,
        outlineColor = outlineColor
    )

    // Back (Lats & Lower Back)
    drawMusclePath(
        path = createBackLatsPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.BACK),
        isSelected = selectedMuscle == BodyPart.BACK,
        outlineColor = outlineColor
    )

    // Triceps
    drawMusclePath(
        path = createTricepsPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.TRICEPS),
        isSelected = selectedMuscle == BodyPart.TRICEPS,
        outlineColor = outlineColor
    )

    // Forearms (Back)
    drawMusclePath(
        path = createFrontForearmsPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.FOREARMS),
        isSelected = selectedMuscle == BodyPart.FOREARMS,
        outlineColor = outlineColor
    )

    // Legs (Back - Glutes, Hamstrings, Calves)
    drawMusclePath(
        path = createBackLegsPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.LEGS),
        isSelected = selectedMuscle == BodyPart.LEGS,
        outlineColor = outlineColor
    )
}

private fun DrawScope.drawMusclePath(
    path: Path,
    color: Color,
    isSelected: Boolean,
    outlineColor: Color
) {
    // Fill with recovery heat tint
    drawPath(
        path = path,
        color = color.copy(alpha = if (isSelected) 0.95f else 0.80f),
        style = Fill
    )

    // Stroke definition
    drawPath(
        path = path,
        color = if (isSelected) Color.White else outlineColor,
        style = Stroke(width = if (isSelected) 2.5f.dp.toPx() else 1.2f.dp.toPx())
    )
}

private fun getMuscleColor(
    recoveryMap: Map<BodyPart, MuscleRecoveryData>,
    bodyPart: BodyPart
): Color {
    val data = recoveryMap[bodyPart] ?: return Color(0xFF10B981)
    return data.color
}

// ----------------- PATH GENERATORS -----------------

private fun createChestPath(w: Float, h: Float): Path = Path().apply {
    // Left Pec
    addRoundRect(
        RoundRect(
            rect = Rect(0.35f * w, 0.16f * h, 0.485f * w, 0.26f * h),
            cornerRadius = CornerRadius(4.dp.value, 4.dp.value)
        )
    )
    // Right Pec
    addRoundRect(
        RoundRect(
            rect = Rect(0.515f * w, 0.16f * h, 0.65f * w, 0.26f * h),
            cornerRadius = CornerRadius(4.dp.value, 4.dp.value)
        )
    )
}

private fun createFrontShouldersPath(w: Float, h: Float): Path = Path().apply {
    // Left Shoulder
    addRoundRect(
        RoundRect(
            rect = Rect(0.24f * w, 0.15f * h, 0.34f * w, 0.23f * h),
            cornerRadius = CornerRadius(5.dp.value, 5.dp.value)
        )
    )
    // Right Shoulder
    addRoundRect(
        RoundRect(
            rect = Rect(0.66f * w, 0.15f * h, 0.76f * w, 0.23f * h),
            cornerRadius = CornerRadius(5.dp.value, 5.dp.value)
        )
    )
}

private fun createBicepsPath(w: Float, h: Float): Path = Path().apply {
    // Left Bicep
    addRoundRect(
        RoundRect(
            rect = Rect(0.23f * w, 0.24f * h, 0.32f * w, 0.35f * h),
            cornerRadius = CornerRadius(4.dp.value, 4.dp.value)
        )
    )
    // Right Bicep
    addRoundRect(
        RoundRect(
            rect = Rect(0.68f * w, 0.24f * h, 0.77f * w, 0.35f * h),
            cornerRadius = CornerRadius(4.dp.value, 4.dp.value)
        )
    )
}

private fun createFrontForearmsPath(w: Float, h: Float): Path = Path().apply {
    // Left Forearm
    addRoundRect(
        RoundRect(
            rect = Rect(0.18f * w, 0.36f * h, 0.28f * w, 0.50f * h),
            cornerRadius = CornerRadius(4.dp.value, 4.dp.value)
        )
    )
    // Right Forearm
    addRoundRect(
        RoundRect(
            rect = Rect(0.72f * w, 0.36f * h, 0.82f * w, 0.50f * h),
            cornerRadius = CornerRadius(4.dp.value, 4.dp.value)
        )
    )
}

private fun createCorePath(w: Float, h: Float): Path = Path().apply {
    // Abdominals & Obliques
    addRoundRect(
        RoundRect(
            rect = Rect(0.38f * w, 0.27f * h, 0.62f * w, 0.47f * h),
            cornerRadius = CornerRadius(6.dp.value, 6.dp.value)
        )
    )
}

private fun createFrontLegsPath(w: Float, h: Float): Path = Path().apply {
    // Left Quad
    addRoundRect(
        RoundRect(
            rect = Rect(0.33f * w, 0.49f * h, 0.485f * w, 0.71f * h),
            cornerRadius = CornerRadius(6.dp.value, 6.dp.value)
        )
    )
    // Right Quad
    addRoundRect(
        RoundRect(
            rect = Rect(0.515f * w, 0.49f * h, 0.67f * w, 0.71f * h),
            cornerRadius = CornerRadius(6.dp.value, 6.dp.value)
        )
    )
    // Left Shin/Calf
    addRoundRect(
        RoundRect(
            rect = Rect(0.34f * w, 0.74f * h, 0.46f * w, 0.93f * h),
            cornerRadius = CornerRadius(5.dp.value, 5.dp.value)
        )
    )
    // Right Shin/Calf
    addRoundRect(
        RoundRect(
            rect = Rect(0.54f * w, 0.74f * h, 0.66f * w, 0.93f * h),
            cornerRadius = CornerRadius(5.dp.value, 5.dp.value)
        )
    )
}

private fun createTrapsAndRearDeltsPath(w: Float, h: Float): Path = Path().apply {
    // Upper Trapezius
    moveTo(0.50f * w, 0.12f * h)
    lineTo(0.64f * w, 0.16f * h)
    lineTo(0.50f * w, 0.22f * h)
    lineTo(0.36f * w, 0.16f * h)
    close()

    // Left Rear Delt
    addRoundRect(
        RoundRect(
            rect = Rect(0.24f * w, 0.15f * h, 0.35f * w, 0.23f * h),
            cornerRadius = CornerRadius(4.dp.value, 4.dp.value)
        )
    )
    // Right Rear Delt
    addRoundRect(
        RoundRect(
            rect = Rect(0.65f * w, 0.15f * h, 0.76f * w, 0.23f * h),
            cornerRadius = CornerRadius(4.dp.value, 4.dp.value)
        )
    )
}

private fun createBackLatsPath(w: Float, h: Float): Path = Path().apply {
    // Middle & Lower Back / Lats
    addRoundRect(
        RoundRect(
            rect = Rect(0.34f * w, 0.22f * h, 0.66f * w, 0.45f * h),
            cornerRadius = CornerRadius(6.dp.value, 6.dp.value)
        )
    )
}

private fun createTricepsPath(w: Float, h: Float): Path = Path().apply {
    // Left Tricep
    addRoundRect(
        RoundRect(
            rect = Rect(0.23f * w, 0.24f * h, 0.32f * w, 0.35f * h),
            cornerRadius = CornerRadius(4.dp.value, 4.dp.value)
        )
    )
    // Right Tricep
    addRoundRect(
        RoundRect(
            rect = Rect(0.68f * w, 0.24f * h, 0.77f * w, 0.35f * h),
            cornerRadius = CornerRadius(4.dp.value, 4.dp.value)
        )
    )
}

private fun createBackLegsPath(w: Float, h: Float): Path = Path().apply {
    // Left Glute & Hamstring
    addRoundRect(
        RoundRect(
            rect = Rect(0.33f * w, 0.47f * h, 0.485f * w, 0.71f * h),
            cornerRadius = CornerRadius(6.dp.value, 6.dp.value)
        )
    )
    // Right Glute & Hamstring
    addRoundRect(
        RoundRect(
            rect = Rect(0.515f * w, 0.47f * h, 0.67f * w, 0.71f * h),
            cornerRadius = CornerRadius(6.dp.value, 6.dp.value)
        )
    )
    // Left Calf (Gastrocnemius)
    addRoundRect(
        RoundRect(
            rect = Rect(0.34f * w, 0.74f * h, 0.46f * w, 0.93f * h),
            cornerRadius = CornerRadius(5.dp.value, 5.dp.value)
        )
    )
    // Right Calf
    addRoundRect(
        RoundRect(
            rect = Rect(0.54f * w, 0.74f * h, 0.66f * w, 0.93f * h),
            cornerRadius = CornerRadius(5.dp.value, 5.dp.value)
        )
    )
}

private fun findTappedMuscle(viewType: BodyViewType, normX: Float, normY: Float): BodyPart? {
    if (viewType == BodyViewType.FRONT) {
        return when {
            normY in 0.15f..0.24f && (normX in 0.22f..0.36f || normX in 0.64f..0.78f) -> BodyPart.SHOULDERS
            normY in 0.16f..0.27f && normX in 0.34f..0.66f -> BodyPart.CHEST
            normY in 0.24f..0.36f && (normX in 0.22f..0.34f || normX in 0.66f..0.78f) -> BodyPart.BICEPS
            normY in 0.36f..0.52f && (normX in 0.16f..0.30f || normX in 0.70f..0.84f) -> BodyPart.FOREARMS
            normY in 0.27f..0.48f && normX in 0.36f..0.64f -> BodyPart.CORE
            normY in 0.49f..0.95f && normX in 0.30f..0.70f -> BodyPart.LEGS
            else -> null
        }
    } else {
        return when {
            normY in 0.12f..0.23f && normX in 0.36f..0.64f -> BodyPart.SHOULDERS // Traps
            normY in 0.15f..0.24f && (normX in 0.22f..0.36f || normX in 0.64f..0.78f) -> BodyPart.SHOULDERS
            normY in 0.22f..0.46f && normX in 0.33f..0.67f -> BodyPart.BACK
            normY in 0.24f..0.36f && (normX in 0.22f..0.34f || normX in 0.66f..0.78f) -> BodyPart.TRICEPS
            normY in 0.36f..0.52f && (normX in 0.16f..0.30f || normX in 0.70f..0.84f) -> BodyPart.FOREARMS
            normY in 0.47f..0.95f && normX in 0.30f..0.70f -> BodyPart.LEGS
            else -> null
        }
    }
}
