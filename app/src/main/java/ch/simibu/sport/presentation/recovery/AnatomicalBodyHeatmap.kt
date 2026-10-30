package ch.simibu.sport.presentation.recovery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    // Standard human anatomical proportion ratio ~ 1 : 2.1 (width = height * 0.48)
    val width = height * 0.48f
    val neutralBodyTone = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val outlineTone = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f)
    val anatomicalLineTone = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

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

        // 1. Draw anatomical human base silhouette (head, neck, hands, feet, torso underlying contour)
        drawHumanBaseSilhouette(w, h, viewType, neutralBodyTone, outlineTone, anatomicalLineTone)

        // 2. Draw view-specific muscle groups with textbook contours
        if (viewType == BodyViewType.FRONT) {
            drawFrontMuscles(w, h, recoveryMap, selectedMuscle, outlineTone, anatomicalLineTone)
        } else {
            drawBackMuscles(w, h, recoveryMap, selectedMuscle, outlineTone, anatomicalLineTone)
        }
    }
}

/**
 * Draws a schoolbook-style human base silhouette (body background, natural head, hands, feet,
 * and delicate anatomical skeletal reference lines).
 */
private fun DrawScope.drawHumanBaseSilhouette(
    w: Float,
    h: Float,
    viewType: BodyViewType,
    bodyTone: Color,
    outlineTone: Color,
    lineTone: Color
) {
    // 1. Head (anatomical oval with cranial dome and defined jawline)
    val headPath = Path().apply {
        moveTo(0.50f * w, 0.022f * h)
        // Cranial dome
        cubicTo(0.42f * w, 0.022f * h, 0.41f * w, 0.055f * h, 0.41f * w, 0.075f * h)
        // Jawline tapering down to chin
        cubicTo(0.41f * w, 0.095f * h, 0.46f * w, 0.120f * h, 0.50f * w, 0.122f * h)
        // Symmetrical right side
        cubicTo(0.54f * w, 0.120f * h, 0.59f * w, 0.095f * h, 0.59f * w, 0.075f * h)
        cubicTo(0.59f * w, 0.055f * h, 0.58f * w, 0.022f * h, 0.50f * w, 0.022f * h)
        close()
    }
    drawPath(headPath, color = bodyTone, style = Fill)
    drawPath(headPath, color = outlineTone, style = Stroke(width = 1.dp.toPx()))

    // Ear notches
    val earLeft = Path().apply {
        moveTo(0.41f * w, 0.065f * h)
        cubicTo(0.395f * w, 0.070f * h, 0.395f * w, 0.082f * h, 0.415f * w, 0.086f * h)
    }
    val earRight = Path().apply {
        moveTo(0.59f * w, 0.065f * h)
        cubicTo(0.605f * w, 0.070f * h, 0.605f * w, 0.082f * h, 0.585f * w, 0.086f * h)
    }
    drawPath(earLeft, color = outlineTone, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
    drawPath(earRight, color = outlineTone, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))

    // 2. Neck
    val neckPath = Path().apply {
        moveTo(0.45f * w, 0.115f * h)
        lineTo(0.435f * w, 0.150f * h)
        lineTo(0.565f * w, 0.150f * h)
        lineTo(0.55f * w, 0.115f * h)
        close()
    }
    drawPath(neckPath, color = bodyTone, style = Fill)

    // 3. Hands (anatomical silhouette at thighs)
    drawHandSilhouette(w, h, isLeft = true, bodyTone, outlineTone)
    drawHandSilhouette(w, h, isLeft = false, bodyTone, outlineTone)

    // 4. Feet (anatomical grounded stance)
    drawFootSilhouette(w, h, isLeft = true, viewType, bodyTone, outlineTone)
    drawFootSilhouette(w, h, isLeft = false, viewType, bodyTone, outlineTone)

    // 5. Delicate Skeletal reference linework (Clavicles, sternum notch, spine)
    if (viewType == BodyViewType.FRONT) {
        // Clavicles (S-curves across the collar)
        val clavicleLeft = Path().apply {
            moveTo(0.49f * w, 0.152f * h)
            cubicTo(0.45f * w, 0.150f * h, 0.40f * w, 0.155f * h, 0.35f * w, 0.158f * h)
        }
        val clavicleRight = Path().apply {
            moveTo(0.51f * w, 0.152f * h)
            cubicTo(0.55f * w, 0.150f * h, 0.60f * w, 0.155f * h, 0.65f * w, 0.158f * h)
        }
        drawPath(clavicleLeft, color = lineTone, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
        drawPath(clavicleRight, color = lineTone, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))

        // Patellar knee caps
        drawCircle(
            color = lineTone,
            radius = 0.024f * w,
            center = Offset(0.385f * w, 0.705f * h),
            style = Stroke(width = 0.8.dp.toPx())
        )
        drawCircle(
            color = lineTone,
            radius = 0.024f * w,
            center = Offset(0.615f * w, 0.705f * h),
            style = Stroke(width = 0.8.dp.toPx())
        )
    } else {
        // Spine column groove
        val spinePath = Path().apply {
            moveTo(0.50f * w, 0.145f * h)
            lineTo(0.50f * w, 0.460f * h)
        }
        drawPath(spinePath, color = lineTone, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))

        // Achilles tendon hints
        val achillesLeft = Path().apply {
            moveTo(0.385f * w, 0.860f * h)
            lineTo(0.385f * w, 0.930f * h)
        }
        val achillesRight = Path().apply {
            moveTo(0.615f * w, 0.860f * h)
            lineTo(0.615f * w, 0.930f * h)
        }
        drawPath(achillesLeft, color = lineTone, style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round))
        drawPath(achillesRight, color = lineTone, style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawHandSilhouette(
    w: Float,
    h: Float,
    isLeft: Boolean,
    bodyTone: Color,
    outlineTone: Color
) {
    val sign = if (isLeft) 1f else -1f
    val baseCenterX = if (isLeft) 0.190f * w else 0.810f * w

    val handPath = Path().apply {
        moveTo(baseCenterX - sign * 0.020f * w, 0.495f * h)
        // Outer thumb curve
        cubicTo(
            baseCenterX - sign * 0.040f * w, 0.520f * h,
            baseCenterX - sign * 0.035f * w, 0.540f * h,
            baseCenterX - sign * 0.020f * w, 0.550f * h
        )
        // Fingers tip
        cubicTo(
            baseCenterX - sign * 0.010f * w, 0.565f * h,
            baseCenterX + sign * 0.015f * w, 0.565f * h,
            baseCenterX + sign * 0.020f * w, 0.545f * h
        )
        // Inner palm back to wrist
        cubicTo(
            baseCenterX + sign * 0.025f * w, 0.525f * h,
            baseCenterX + sign * 0.020f * w, 0.510f * h,
            baseCenterX + sign * 0.015f * w, 0.495f * h
        )
        close()
    }
    drawPath(handPath, color = bodyTone, style = Fill)
    drawPath(handPath, color = outlineTone, style = Stroke(width = 0.8.dp.toPx()))
}

private fun DrawScope.drawFootSilhouette(
    w: Float,
    h: Float,
    isLeft: Boolean,
    viewType: BodyViewType,
    bodyTone: Color,
    outlineTone: Color
) {
    val sign = if (isLeft) 1f else -1f
    val ankleX = if (isLeft) 0.385f * w else 0.615f * w

    val footPath = Path().apply {
        moveTo(ankleX - sign * 0.022f * w, 0.930f * h)
        if (viewType == BodyViewType.FRONT) {
            // Front-facing foot (slight outward V angle)
            cubicTo(
                ankleX - sign * 0.030f * w, 0.955f * h,
                ankleX - sign * 0.028f * w, 0.980f * h,
                ankleX - sign * 0.010f * w, 0.985f * h
            )
            cubicTo(
                ankleX + sign * 0.010f * w, 0.985f * h,
                ankleX + sign * 0.025f * w, 0.970f * h,
                ankleX + sign * 0.020f * w, 0.930f * h
            )
        } else {
            // Back-facing heel
            cubicTo(
                ankleX - sign * 0.025f * w, 0.955f * h,
                ankleX - sign * 0.015f * w, 0.980f * h,
                ankleX, 0.982f * h
            )
            cubicTo(
                ankleX + sign * 0.015f * w, 0.980f * h,
                ankleX + sign * 0.025f * w, 0.955f * h,
                ankleX + sign * 0.020f * w, 0.930f * h
            )
        }
        close()
    }
    drawPath(footPath, color = bodyTone, style = Fill)
    drawPath(footPath, color = outlineTone, style = Stroke(width = 0.8.dp.toPx()))
}

// ----------------- FRONT MUSCLES -----------------

private fun DrawScope.drawFrontMuscles(
    w: Float,
    h: Float,
    recoveryMap: Map<BodyPart, MuscleRecoveryData>,
    selectedMuscle: BodyPart?,
    outlineColor: Color,
    lineTone: Color
) {
    // 1. Shoulders (Deltoids - Left & Right caps)
    drawMusclePath(
        path = createFrontDeltoidsPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.SHOULDERS),
        isSelected = selectedMuscle == BodyPart.SHOULDERS,
        outlineColor = outlineColor
    )

    // 2. Chest (Pectoralis Major - Left & Right contoured plates)
    drawMusclePath(
        path = createPectoralsPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.CHEST),
        isSelected = selectedMuscle == BodyPart.CHEST,
        outlineColor = outlineColor
    )

    // 3. Biceps (Biceps Brachii - Fusiform muscle bellies)
    drawMusclePath(
        path = createBicepsAnatomicalPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.BICEPS),
        isSelected = selectedMuscle == BodyPart.BICEPS,
        outlineColor = outlineColor
    )

    // 4. Forearms (Brachioradialis & Flexors)
    drawMusclePath(
        path = createForearmsAnatomicalPath(w, h, isBack = false),
        color = getMuscleColor(recoveryMap, BodyPart.FOREARMS),
        isSelected = selectedMuscle == BodyPart.FOREARMS,
        outlineColor = outlineColor
    )

    // 5. Core (Rectus Abdominis 6-pack & External Obliques)
    drawMusclePath(
        path = createCoreAnatomicalPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.CORE),
        isSelected = selectedMuscle == BodyPart.CORE,
        outlineColor = outlineColor
    )

    // 6. Legs (Quadriceps & Front Calves)
    drawMusclePath(
        path = createFrontLegsAnatomicalPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.LEGS),
        isSelected = selectedMuscle == BodyPart.LEGS,
        outlineColor = outlineColor
    )
}

// ----------------- BACK MUSCLES -----------------

private fun DrawScope.drawBackMuscles(
    w: Float,
    h: Float,
    recoveryMap: Map<BodyPart, MuscleRecoveryData>,
    selectedMuscle: BodyPart?,
    outlineColor: Color,
    lineTone: Color
) {
    // 1. Shoulders & Trapezius (Iconic Diamond Trapezius & Rear Deltoids)
    drawMusclePath(
        path = createTrapeziusAndRearDeltsPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.SHOULDERS),
        isSelected = selectedMuscle == BodyPart.SHOULDERS,
        outlineColor = outlineColor
    )

    // 2. Back (Latissimus Dorsi & Rhomboids / Lower Back)
    drawMusclePath(
        path = createLatsAndLowerBackPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.BACK),
        isSelected = selectedMuscle == BodyPart.BACK,
        outlineColor = outlineColor
    )

    // 3. Triceps (Horseshoe lateral and long heads)
    drawMusclePath(
        path = createTricepsAnatomicalPath(w, h),
        color = getMuscleColor(recoveryMap, BodyPart.TRICEPS),
        isSelected = selectedMuscle == BodyPart.TRICEPS,
        outlineColor = outlineColor
    )

    // 4. Forearms (Extensors posterior)
    drawMusclePath(
        path = createForearmsAnatomicalPath(w, h, isBack = true),
        color = getMuscleColor(recoveryMap, BodyPart.FOREARMS),
        isSelected = selectedMuscle == BodyPart.FOREARMS,
        outlineColor = outlineColor
    )

    // 5. Legs (Gluteus Maximus, Hamstrings & Dual-head Gastrocnemius)
    drawMusclePath(
        path = createBackLegsAnatomicalPath(w, h),
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
    // Fill with soft anatomical recovery tint
    drawPath(
        path = path,
        color = color.copy(alpha = if (isSelected) 0.95f else 0.82f),
        style = Fill
    )

    // Diagrammatic ink outline stroke
    drawPath(
        path = path,
        color = if (isSelected) Color.White else outlineColor,
        style = Stroke(
            width = if (isSelected) 2.2f.dp.toPx() else 1.0f.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun getMuscleColor(
    recoveryMap: Map<BodyPart, MuscleRecoveryData>,
    bodyPart: BodyPart
): Color {
    val data = recoveryMap[bodyPart] ?: return Color(0xFF10B981)
    return data.color
}

// ----------------- ANATOMICAL PATH BUILDERS -----------------

/**
 * Pectoralis Major: Two realistic, anatomically contoured chest plates meeting at the sternum.
 */
private fun createPectoralsPath(w: Float, h: Float): Path = Path().apply {
    // Left Pec (viewer's left)
    moveTo(0.485f * w, 0.158f * h)
    // Sternal clavicular margin towards shoulder
    cubicTo(0.420f * w, 0.155f * h, 0.360f * w, 0.160f * h, 0.335f * w, 0.170f * h)
    // Axillary border curving under deltoid insertion
    cubicTo(0.320f * w, 0.190f * h, 0.315f * w, 0.220f * h, 0.335f * w, 0.245f * h)
    // Lower pectoral sweep (inframammary fold) curving to sternum
    cubicTo(0.365f * w, 0.268f * h, 0.430f * w, 0.268f * h, 0.485f * w, 0.258f * h)
    // Sternal midline
    lineTo(0.485f * w, 0.158f * h)
    close()

    // Right Pec (viewer's right)
    moveTo(0.515f * w, 0.158f * h)
    cubicTo(0.580f * w, 0.155f * h, 0.640f * w, 0.160f * h, 0.665f * w, 0.170f * h)
    cubicTo(0.680f * w, 0.190f * h, 0.685f * w, 0.220f * h, 0.665f * w, 0.245f * h)
    cubicTo(0.635f * w, 0.268f * h, 0.570f * w, 0.268f * h, 0.515f * w, 0.258f * h)
    lineTo(0.515f * w, 0.158f * h)
    close()
}

/**
 * Front Deltoids (Anterior & Lateral heads crowning the shoulder joint).
 */
private fun createFrontDeltoidsPath(w: Float, h: Float): Path = Path().apply {
    // Left Deltoid
    moveTo(0.355f * w, 0.158f * h)
    // Acromion curvature
    cubicTo(0.300f * w, 0.155f * h, 0.230f * w, 0.165f * h, 0.210f * w, 0.190f * h)
    // Outer sweep down to deltoid tuberosity insertion
    cubicTo(0.195f * w, 0.215f * h, 0.215f * w, 0.242f * h, 0.245f * w, 0.250f * h)
    // Inner contour meeting bicep sulcus & pec
    cubicTo(0.270f * w, 0.240f * h, 0.315f * w, 0.210f * h, 0.335f * w, 0.170f * h)
    close()

    // Right Deltoid
    moveTo(0.645f * w, 0.158f * h)
    cubicTo(0.700f * w, 0.155f * h, 0.770f * w, 0.165f * h, 0.790f * w, 0.190f * h)
    cubicTo(0.805f * w, 0.215f * h, 0.785f * w, 0.242f * h, 0.755f * w, 0.250f * h)
    cubicTo(0.730f * w, 0.240f * h, 0.685f * w, 0.210f * h, 0.665f * w, 0.170f * h)
    close()
}

/**
 * Biceps Brachii: Anatomical spindle muscle bellies with natural arm curves.
 */
private fun createBicepsAnatomicalPath(w: Float, h: Float): Path = Path().apply {
    // Left Biceps
    moveTo(0.248f * w, 0.248f * h)
    cubicTo(0.200f * w, 0.270f * h, 0.190f * w, 0.315f * h, 0.225f * w, 0.348f * h)
    cubicTo(0.245f * w, 0.355f * h, 0.265f * w, 0.350f * h, 0.280f * w, 0.335f * h)
    cubicTo(0.295f * w, 0.300f * h, 0.285f * w, 0.260f * h, 0.248f * w, 0.248f * h)
    close()

    // Right Biceps
    moveTo(0.752f * w, 0.248f * h)
    cubicTo(0.800f * w, 0.270f * h, 0.810f * w, 0.315f * h, 0.775f * w, 0.348f * h)
    cubicTo(0.755f * w, 0.355f * h, 0.735f * w, 0.350f * h, 0.720f * w, 0.335f * h)
    cubicTo(0.705f * w, 0.300f * h, 0.715f * w, 0.260f * h, 0.752f * w, 0.248f * h)
    close()
}

/**
 * Forearms: Natural brachioradialis sweep and wrist taper.
 */
private fun createForearmsAnatomicalPath(w: Float, h: Float, isBack: Boolean): Path = Path().apply {
    // Left Forearm
    moveTo(0.230f * w, 0.352f * h)
    // Lateral brachioradialis bulge
    cubicTo(0.165f * w, 0.380f * h, 0.155f * w, 0.435f * h, 0.180f * w, 0.495f * h)
    // Wrist band
    lineTo(0.215f * w, 0.495f * h)
    // Medial flexor/extensor curve back to elbow
    cubicTo(0.245f * w, 0.440f * h, 0.270f * w, 0.395f * h, 0.275f * w, 0.352f * h)
    close()

    // Right Forearm
    moveTo(0.770f * w, 0.352f * h)
    cubicTo(0.835f * w, 0.380f * h, 0.845f * w, 0.435f * h, 0.820f * w, 0.495f * h)
    lineTo(0.785f * w, 0.495f * h)
    cubicTo(0.755f * w, 0.440f * h, 0.730f * w, 0.395f * h, 0.725f * w, 0.352f * h)
    close()
}

/**
 * Core: Rectus Abdominis 6-pack segments and external obliques flanking the waist.
 */
private fun createCoreAnatomicalPath(w: Float, h: Float): Path = Path().apply {
    val cr = 3.dp.value

    // Upper Abs (tier 1)
    addRect(Rect(0.420f * w, 0.268f * h, 0.485f * w, 0.315f * h))
    addRect(Rect(0.515f * w, 0.268f * h, 0.580f * w, 0.315f * h))

    // Mid Abs (tier 2)
    addRect(Rect(0.420f * w, 0.323f * h, 0.485f * w, 0.370f * h))
    addRect(Rect(0.515f * w, 0.323f * h, 0.580f * w, 0.370f * h))

    // Lower Abs (tier 3)
    addRect(Rect(0.425f * w, 0.378f * h, 0.485f * w, 0.438f * h))
    addRect(Rect(0.515f * w, 0.378f * h, 0.575f * w, 0.438f * h))

    // Left External Oblique (waist curve)
    moveTo(0.360f * w, 0.270f * h)
    cubicTo(0.345f * w, 0.320f * h, 0.355f * w, 0.390f * h, 0.335f * w, 0.450f * h)
    lineTo(0.410f * w, 0.450f * h)
    cubicTo(0.410f * w, 0.380f * h, 0.405f * w, 0.320f * h, 0.410f * w, 0.270f * h)
    close()

    // Right External Oblique
    moveTo(0.640f * w, 0.270f * h)
    cubicTo(0.655f * w, 0.320f * h, 0.645f * w, 0.390f * h, 0.665f * w, 0.450f * h)
    lineTo(0.590f * w, 0.450f * h)
    cubicTo(0.590f * w, 0.380f * h, 0.595f * w, 0.320f * h, 0.590f * w, 0.270f * h)
    close()
}

/**
 * Front Legs: Quadriceps (Vastus Lateralis, Medialis, Rectus Femoris) and anterior calves.
 */
private fun createFrontLegsAnatomicalPath(w: Float, h: Float): Path = Path().apply {
    // Left Quad
    moveTo(0.340f * w, 0.465f * h)
    // Lateral vastus sweep (outer thigh curve)
    cubicTo(0.285f * w, 0.520f * h, 0.285f * w, 0.600f * h, 0.335f * w, 0.685f * h)
    lineTo(0.430f * w, 0.685f * h)
    // Medial vastus teardrop curve towards groin
    cubicTo(0.445f * w, 0.630f * h, 0.465f * w, 0.560f * h, 0.485f * w, 0.490f * h)
    close()

    // Right Quad
    moveTo(0.660f * w, 0.465f * h)
    cubicTo(0.715f * w, 0.520f * h, 0.715f * w, 0.600f * h, 0.665f * w, 0.685f * h)
    lineTo(0.570f * w, 0.685f * h)
    cubicTo(0.555f * w, 0.630f * h, 0.535f * w, 0.560f * h, 0.515f * w, 0.490f * h)
    close()

    // Left Shin & Front Calf
    moveTo(0.345f * w, 0.725f * h)
    // Lateral calf curve
    cubicTo(0.305f * w, 0.780f * h, 0.315f * w, 0.860f * h, 0.360f * w, 0.925f * h)
    lineTo(0.410f * w, 0.925f * h)
    // Inner shin line
    cubicTo(0.425f * w, 0.860f * h, 0.425f * w, 0.780f * h, 0.415f * w, 0.725f * h)
    close()

    // Right Shin & Front Calf
    moveTo(0.655f * w, 0.725f * h)
    cubicTo(0.695f * w, 0.780f * h, 0.685f * w, 0.860f * h, 0.640f * w, 0.925f * h)
    lineTo(0.590f * w, 0.925f * h)
    cubicTo(0.575f * w, 0.860f * h, 0.575f * w, 0.780f * h, 0.585f * w, 0.725f * h)
    close()
}

/**
 * Trapezius and Rear Deltoids: Textbook diamond trapezius from neck to mid-spine plus posterior delts.
 */
private fun createTrapeziusAndRearDeltsPath(w: Float, h: Float): Path = Path().apply {
    // Iconic Trapezius Diamond (Kite shape)
    moveTo(0.500f * w, 0.115f * h) // Superior nuchal line
    cubicTo(0.440f * w, 0.130f * h, 0.370f * w, 0.145f * h, 0.345f * w, 0.160f * h) // Left acromion
    cubicTo(0.410f * w, 0.200f * h, 0.470f * w, 0.260f * h, 0.500f * w, 0.315f * h) // T12 thoracic spine apex
    cubicTo(0.530f * w, 0.260f * h, 0.590f * w, 0.200f * h, 0.655f * w, 0.160f * h) // Right acromion
    cubicTo(0.630f * w, 0.145f * h, 0.560f * w, 0.130f * h, 0.500f * w, 0.115f * h)
    close()

    // Left Posterior Deltoid
    moveTo(0.345f * w, 0.160f * h)
    cubicTo(0.290f * w, 0.160f * h, 0.220f * w, 0.170f * h, 0.205f * w, 0.195f * h)
    cubicTo(0.195f * w, 0.220f * h, 0.215f * w, 0.245f * h, 0.245f * w, 0.252f * h)
    cubicTo(0.270f * w, 0.240f * h, 0.310f * w, 0.210f * h, 0.330f * w, 0.175f * h)
    close()

    // Right Posterior Deltoid
    moveTo(0.655f * w, 0.160f * h)
    cubicTo(0.710f * w, 0.160f * h, 0.780f * w, 0.170f * h, 0.795f * w, 0.195f * h)
    cubicTo(0.805f * w, 0.220f * h, 0.785f * w, 0.245f * h, 0.755f * w, 0.252f * h)
    cubicTo(0.730f * w, 0.240f * h, 0.690f * w, 0.210f * h, 0.670f * w, 0.175f * h)
    close()
}

/**
 * Latissimus Dorsi & Rhomboids / Lower back (V-Taper wings).
 */
private fun createLatsAndLowerBackPath(w: Float, h: Float): Path = Path().apply {
    // Left Lat Wing
    moveTo(0.485f * w, 0.225f * h)
    cubicTo(0.420f * w, 0.225f * h, 0.350f * w, 0.230f * h, 0.325f * w, 0.245f * h)
    // Lateral sweep down to waist
    cubicTo(0.315f * w, 0.290f * h, 0.335f * w, 0.380f * h, 0.355f * w, 0.445f * h)
    // Thoracolumbar fascia border to lower spine
    lineTo(0.485f * w, 0.455f * h)
    close()

    // Right Lat Wing
    moveTo(0.515f * w, 0.225f * h)
    cubicTo(0.580f * w, 0.225f * h, 0.650f * w, 0.230f * h, 0.675f * w, 0.245f * h)
    cubicTo(0.685f * w, 0.290f * h, 0.665f * w, 0.380f * h, 0.645f * w, 0.445f * h)
    lineTo(0.515f * w, 0.455f * h)
    close()
}

/**
 * Triceps Brachii: Classic horseshoe long & lateral heads on posterior arm.
 */
private fun createTricepsAnatomicalPath(w: Float, h: Float): Path = Path().apply {
    // Left Triceps
    moveTo(0.248f * w, 0.250f * h)
    cubicTo(0.195f * w, 0.270f * h, 0.185f * w, 0.315f * h, 0.220f * w, 0.348f * h)
    cubicTo(0.240f * w, 0.352f * h, 0.260f * w, 0.350f * h, 0.275f * w, 0.335f * h)
    cubicTo(0.290f * w, 0.300f * h, 0.280f * w, 0.265f * h, 0.248f * w, 0.250f * h)
    close()

    // Right Triceps
    moveTo(0.752f * w, 0.250f * h)
    cubicTo(0.805f * w, 0.270f * h, 0.815f * w, 0.315f * h, 0.780f * w, 0.348f * h)
    cubicTo(0.760f * w, 0.352f * h, 0.740f * w, 0.350f * h, 0.725f * w, 0.335f * h)
    cubicTo(0.710f * w, 0.300f * h, 0.720f * w, 0.265f * h, 0.752f * w, 0.250f * h)
    close()
}

/**
 * Back Legs: Gluteus Maximus, Hamstrings, and the characteristic double-headed Gastrocnemius calves.
 */
private fun createBackLegsAnatomicalPath(w: Float, h: Float): Path = Path().apply {
    // Left Gluteus Maximus
    moveTo(0.485f * w, 0.458f * h)
    // Iliac crest & outer hip curve
    cubicTo(0.410f * w, 0.455f * h, 0.320f * w, 0.480f * h, 0.300f * w, 0.525f * h)
    // Gluteal fold (crease under buttocks)
    cubicTo(0.315f * w, 0.570f * h, 0.420f * w, 0.575f * h, 0.485f * w, 0.555f * h)
    close()

    // Right Gluteus Maximus
    moveTo(0.515f * w, 0.458f * h)
    cubicTo(0.590f * w, 0.455f * h, 0.680f * w, 0.480f * h, 0.700f * w, 0.525f * h)
    cubicTo(0.685f * w, 0.570f * h, 0.580f * w, 0.575f * h, 0.515f * w, 0.555f * h)
    close()

    // Left Hamstring
    moveTo(0.335f * w, 0.575f * h)
    cubicTo(0.295f * w, 0.615f * h, 0.300f * w, 0.655f * h, 0.340f * w, 0.690f * h)
    lineTo(0.425f * w, 0.690f * h)
    cubicTo(0.450f * w, 0.655f * h, 0.465f * w, 0.615f * h, 0.465f * w, 0.575f * h)
    close()

    // Right Hamstring
    moveTo(0.665f * w, 0.575f * h)
    cubicTo(0.705f * w, 0.615f * h, 0.700f * w, 0.655f * h, 0.660f * w, 0.690f * h)
    lineTo(0.575f * w, 0.690f * h)
    cubicTo(0.550f * w, 0.655f * h, 0.535f * w, 0.615f * h, 0.535f * w, 0.575f * h)
    close()

    // Left Posterior Calf (Double-headed Gastrocnemius tapering into Achilles tendon)
    moveTo(0.345f * w, 0.725f * h)
    // Lateral head bulge
    cubicTo(0.295f * w, 0.760f * h, 0.305f * w, 0.825f * h, 0.360f * w, 0.865f * h)
    // Achilles tendon narrowing down to heel
    lineTo(0.360f * w, 0.925f * h)
    lineTo(0.410f * w, 0.925f * h)
    lineTo(0.410f * w, 0.865f * h)
    // Medial head bulge (classic lower anatomical insertion)
    cubicTo(0.455f * w, 0.825f * h, 0.450f * w, 0.760f * h, 0.415f * w, 0.725f * h)
    close()

    // Right Posterior Calf
    moveTo(0.655f * w, 0.725f * h)
    cubicTo(0.705f * w, 0.760f * h, 0.695f * w, 0.825f * h, 0.640f * w, 0.865f * h)
    lineTo(0.640f * w, 0.925f * h)
    lineTo(0.590f * w, 0.925f * h)
    lineTo(0.590f * w, 0.865f * h)
    cubicTo(0.545f * w, 0.825f * h, 0.550f * w, 0.760f * h, 0.585f * w, 0.725f * h)
    close()
}

// ----------------- INTERACTIVE TAP DETECTION -----------------

private fun findTappedMuscle(viewType: BodyViewType, normX: Float, normY: Float): BodyPart? {
    if (viewType == BodyViewType.FRONT) {
        return when {
            // Shoulders (Anterior deltoids)
            normY in 0.145f..0.255f && (normX in 0.18f..0.36f || normX in 0.64f..0.82f) -> BodyPart.SHOULDERS
            // Chest (Pectorals)
            normY in 0.155f..0.275f && normX in 0.32f..0.68f -> BodyPart.CHEST
            // Biceps
            normY in 0.245f..0.355f && (normX in 0.18f..0.31f || normX in 0.69f..0.82f) -> BodyPart.BICEPS
            // Forearms
            normY in 0.350f..0.505f && (normX in 0.14f..0.29f || normX in 0.71f..0.86f) -> BodyPart.FOREARMS
            // Core (Abs & Obliques)
            normY in 0.265f..0.470f && normX in 0.33f..0.67f -> BodyPart.CORE
            // Legs (Quads & Calves)
            normY in 0.465f..0.940f && normX in 0.28f..0.72f -> BodyPart.LEGS
            else -> null
        }
    } else {
        return when {
            // Shoulders & Trapezius
            normY in 0.110f..0.220f && normX in 0.34f..0.66f -> BodyPart.SHOULDERS // Upper Traps
            normY in 0.145f..0.255f && (normX in 0.18f..0.36f || normX in 0.64f..0.82f) -> BodyPart.SHOULDERS // Rear Delts
            // Back (Lats & Lower Back)
            normY in 0.220f..0.465f && normX in 0.31f..0.69f -> BodyPart.BACK
            // Triceps
            normY in 0.245f..0.355f && (normX in 0.18f..0.31f || normX in 0.69f..0.82f) -> BodyPart.TRICEPS
            // Forearms
            normY in 0.350f..0.505f && (normX in 0.14f..0.29f || normX in 0.71f..0.86f) -> BodyPart.FOREARMS
            // Legs (Glutes, Hamstrings & Posterior Calves)
            normY in 0.455f..0.940f && normX in 0.28f..0.72f -> BodyPart.LEGS
            else -> null
        }
    }
}
