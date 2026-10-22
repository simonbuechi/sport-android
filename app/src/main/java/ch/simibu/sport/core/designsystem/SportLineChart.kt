package ch.simibu.sport.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.simibu.sport.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

enum class ChartTimeFrame(val id: String, val labelRes: Int) {
    ONE_MONTH("1m", R.string.chart_timeframe_1m),
    THREE_MONTHS("3m", R.string.chart_timeframe_3m),
    SIX_MONTHS("6m", R.string.chart_timeframe_6m),
    ONE_YEAR("1y", R.string.chart_timeframe_1y),
    ALL("all", R.string.chart_timeframe_all);

    fun getCutoffDate(now: LocalDate = LocalDate.now()): LocalDate? {
        return when (this) {
            ONE_MONTH -> now.minusMonths(1)
            THREE_MONTHS -> now.minusMonths(3)
            SIX_MONTHS -> now.minusMonths(6)
            ONE_YEAR -> now.minusYears(1)
            ALL -> null
        }
    }
}

data class ChartDataPoint(
    val timestamp: Long,
    val value: Float,
    val formattedDate: String = ""
)

@Composable
fun SportLineChart(
    title: String,
    dataPoints: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    unit: String = "kg",
    lineColor: Color = SportPurple,
    timeFrames: List<ChartTimeFrame> = ChartTimeFrame.entries,
    selectedTimeFrame: ChartTimeFrame = ChartTimeFrame.SIX_MONTHS,
    onTimeFrameSelected: (ChartTimeFrame) -> Unit = {},
    metricTabs: List<String> = emptyList(),
    selectedMetricIndex: Int = 0,
    onMetricTabSelected: (Int) -> Unit = {}
) {
    // Sort chronologically ascending
    val sortedPoints = remember(dataPoints) {
        dataPoints.sortedBy { it.timestamp }
    }

    TactileCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title and Metric switcher / Timeframes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Timeframe Selector Chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    timeFrames.forEach { tf ->
                        val isSelected = tf == selectedTimeFrame
                        TactileChip(
                            text = stringResource(tf.labelRes),
                            selected = isSelected,
                            onClick = { onTimeFrameSelected(tf) }
                        )
                    }
                }
            }

            // Optional Metric Switcher Tabs (e.g. Weight vs BMI)
            if (metricTabs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    metricTabs.forEachIndexed { index, label ->
                        TactileChip(
                            text = label,
                            selected = index == selectedMetricIndex,
                            onClick = { onMetricTabSelected(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (sortedPoints.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.chart_not_enough_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                ChartCanvas(
                    dataPoints = sortedPoints,
                    unit = unit,
                    lineColor = lineColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )
            }
        }
    }
}

@Composable
private fun ChartCanvas(
    dataPoints: List<ChartDataPoint>,
    unit: String,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    // Interactive scrub state
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Animation progress
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(dataPoints) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 650))
    }

    val pointValues = remember(dataPoints) { dataPoints.map { it.value } }
    val rawMinY = remember(pointValues) { pointValues.minOrNull() ?: 0f }
    val rawMaxY = remember(pointValues) { pointValues.maxOrNull() ?: 100f }

    // Add 8% vertical padding so curve doesn't clip
    val ySpread = max(rawMaxY - rawMinY, 2f)
    val minY = floor(rawMinY - ySpread * 0.08f)
    val maxY = ceil(rawMaxY + ySpread * 0.08f)

    val minTime = remember(dataPoints) { dataPoints.first().timestamp }
    val maxTime = remember(dataPoints) { dataPoints.last().timestamp }
    val timeSpread = max(maxTime - minTime, 1L)

    val labelStyle = remember(onSurfaceVariant) {
        TextStyle(
            color = onSurfaceVariant.copy(alpha = 0.75f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }

    val tooltipValueStyle = remember(onSurfaceColor) {
        TextStyle(
            color = onSurfaceColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }

    val tooltipDateStyle = remember(onSurfaceVariant) {
        TextStyle(
            color = onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal
        )
    }

    Box(
        modifier = modifier
            .pointerInput(dataPoints) {
                detectTapGestures(
                    onPress = { offset ->
                        val paddingLeft = 36.dp.toPx()
                        val paddingRight = 16.dp.toPx()
                        val chartWidth = size.width - paddingLeft - paddingRight
                        if (chartWidth > 0) {
                            val touchFraction = ((offset.x - paddingLeft) / chartWidth).coerceIn(0f, 1f)
                            val targetTime = minTime + (touchFraction * timeSpread).toLong()
                            val closest = dataPoints.indices.minByOrNull { abs(dataPoints[it].timestamp - targetTime) }
                            selectedIndex = closest
                        }
                    }
                )
            }
            .pointerInput(dataPoints) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val paddingLeft = 36.dp.toPx()
                        val paddingRight = 16.dp.toPx()
                        val chartWidth = size.width - paddingLeft - paddingRight
                        if (chartWidth > 0) {
                            val touchFraction = ((offset.x - paddingLeft) / chartWidth).coerceIn(0f, 1f)
                            val targetTime = minTime + (touchFraction * timeSpread).toLong()
                            val closest = dataPoints.indices.minByOrNull { abs(dataPoints[it].timestamp - targetTime) }
                            selectedIndex = closest
                        }
                    },
                    onDragEnd = { selectedIndex = null },
                    onDragCancel = { selectedIndex = null },
                    onDrag = { change, _ ->
                        change.consume()
                        val paddingLeft = 36.dp.toPx()
                        val paddingRight = 16.dp.toPx()
                        val chartWidth = size.width - paddingLeft - paddingRight
                        if (chartWidth > 0) {
                            val touchFraction = ((change.position.x - paddingLeft) / chartWidth).coerceIn(0f, 1f)
                            val targetTime = minTime + (touchFraction * timeSpread).toLong()
                            val closest = dataPoints.indices.minByOrNull { abs(dataPoints[it].timestamp - targetTime) }
                            selectedIndex = closest
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(210.dp)) {
            val paddingLeft = 38.dp.toPx()
            val paddingRight = 16.dp.toPx()
            val paddingTop = 20.dp.toPx()
            val paddingBottom = 28.dp.toPx()

            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom

            if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

            // 1. Draw horizontal gridlines & Y labels
            val gridSteps = 3
            val yRange = maxY - minY
            for (step in 0..gridSteps) {
                val fraction = step.toFloat() / gridSteps
                val gridY = paddingTop + (1f - fraction) * chartHeight
                val gridVal = minY + fraction * yRange

                // Dashed horizontal line
                drawLine(
                    color = onSurfaceVariant.copy(alpha = 0.12f),
                    start = Offset(paddingLeft, gridY),
                    end = Offset(size.width - paddingRight, gridY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )

                // Label
                val formattedY = if (gridVal >= 100) "${gridVal.toInt()}" else String.format(Locale.US, "%.1f", gridVal)
                val textLayout = textMeasurer.measure(
                    text = formattedY,
                    style = labelStyle
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(paddingLeft - textLayout.size.width - 6.dp.toPx(), gridY - textLayout.size.height / 2f)
                )
            }

            // 2. Map coordinates
            val coords = dataPoints.map { dp ->
                val xFrac = if (timeSpread > 0) ((dp.timestamp - minTime).toFloat() / timeSpread).coerceIn(0f, 1f) else 0f
                val yFrac = ((dp.value - minY) / yRange).coerceIn(0f, 1f)
                val targetY = paddingTop + (1f - yFrac) * chartHeight
                // Animate Y position from baseline up to targetY
                val animatedY = (paddingTop + chartHeight) - ((paddingTop + chartHeight) - targetY) * animationProgress.value
                Offset(paddingLeft + xFrac * chartWidth, animatedY)
            }

            // 3. Draw smooth cubic Bézier line and area gradient
            if (coords.isNotEmpty()) {
                val linePath = Path()
                val areaPath = Path()

                linePath.moveTo(coords[0].x, coords[0].y)
                areaPath.moveTo(coords[0].x, paddingTop + chartHeight)
                areaPath.lineTo(coords[0].x, coords[0].y)

                for (i in 1 until coords.size) {
                    val prev = coords[i - 1]
                    val curr = coords[i]
                    val controlX1 = prev.x + (curr.x - prev.x) / 2f
                    val controlY1 = prev.y
                    val controlX2 = prev.x + (curr.x - prev.x) / 2f
                    val controlY2 = curr.y

                    linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                    areaPath.cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                }

                areaPath.lineTo(coords.last().x, paddingTop + chartHeight)
                areaPath.close()

                // Draw filled area gradient
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.32f),
                            lineColor.copy(alpha = 0.02f)
                        ),
                        startY = paddingTop,
                        endY = paddingTop + chartHeight
                    )
                )

                // Draw curve stroke
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw small dots at data points if <= 25 points
                if (dataPoints.size <= 25) {
                    coords.forEach { pt ->
                        drawCircle(
                            color = lineColor,
                            radius = 3.5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = surfaceColor,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            // 4. Draw X-axis date labels
            val xLabelCount = minOf(4, dataPoints.size)
            if (xLabelCount >= 2) {
                val stepIndex = (dataPoints.size - 1).toFloat() / (xLabelCount - 1)
                for (k in 0 until xLabelCount) {
                    val idx = (k * stepIndex).toInt().coerceIn(0, dataPoints.size - 1)
                    val dp = dataPoints[idx]
                    val pt = coords[idx]
                    val dateText = dp.formattedDate.ifEmpty {
                        formatDate(dp.timestamp)
                    }
                    val dateLayout = textMeasurer.measure(
                        text = dateText,
                        style = labelStyle
                    )
                    val labelX = (pt.x - dateLayout.size.width / 2f).coerceIn(
                        paddingLeft,
                        size.width - paddingRight - dateLayout.size.width
                    )
                    drawText(
                        textLayoutResult = dateLayout,
                        topLeft = Offset(labelX, size.height - paddingBottom + 6.dp.toPx())
                    )
                }
            }

            // 5. Draw highlighted point and tooltip if user is scrubbing
            selectedIndex?.let { idx ->
                if (idx in coords.indices) {
                    val pt = coords[idx]
                    val dp = dataPoints[idx]

                    // Vertical guideline
                    drawLine(
                        color = lineColor.copy(alpha = 0.7f),
                        start = Offset(pt.x, paddingTop),
                        end = Offset(pt.x, paddingTop + chartHeight),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )

                    // Glow circle
                    drawCircle(
                        color = lineColor.copy(alpha = 0.25f),
                        radius = 9.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = pt
                    )

                    // Tooltip box
                    val valText = if (dp.value % 1f == 0f) "${dp.value.toInt()} $unit" else String.format(Locale.US, "%.1f %s", dp.value, unit)
                    val dtText = dp.formattedDate.ifEmpty { formatDate(dp.timestamp) }

                    val valLayout = textMeasurer.measure(valText, tooltipValueStyle)
                    val dtLayout = textMeasurer.measure(dtText, tooltipDateStyle)

                    val tooltipW = max(valLayout.size.width, dtLayout.size.width) + 16.dp.toPx()
                    val tooltipH = valLayout.size.height + dtLayout.size.height + 12.dp.toPx()

                    var tooltipX = pt.x - tooltipW / 2f
                    tooltipX = tooltipX.coerceIn(paddingLeft, size.width - paddingRight - tooltipW)

                    var tooltipY = pt.y - tooltipH - 12.dp.toPx()
                    if (tooltipY < paddingTop) {
                        tooltipY = pt.y + 12.dp.toPx()
                    }

                    // Background bubble
                    drawRoundRect(
                        color = surfaceColor,
                        topLeft = Offset(tooltipX, tooltipY),
                        size = Size(tooltipW, tooltipH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                    drawRoundRect(
                        color = lineColor.copy(alpha = 0.4f),
                        topLeft = Offset(tooltipX, tooltipY),
                        size = Size(tooltipW, tooltipH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Text
                    drawText(
                        textLayoutResult = valLayout,
                        topLeft = Offset(tooltipX + 8.dp.toPx(), tooltipY + 4.dp.toPx())
                    )
                    drawText(
                        textLayoutResult = dtLayout,
                        topLeft = Offset(tooltipX + 8.dp.toPx(), tooltipY + 6.dp.toPx() + valLayout.size.height)
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return try {
        val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        date.format(DateTimeFormatter.ofPattern("MMM d"))
    } catch (_: Exception) {
        ""
    }
}
