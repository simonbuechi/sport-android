package ch.simibu.sport.presentation.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.simibu.sport.R
import ch.simibu.sport.core.designsystem.ChartDataPoint
import ch.simibu.sport.core.designsystem.ChartTimeFrame
import ch.simibu.sport.core.designsystem.SportLineChart
import ch.simibu.sport.core.model.WeightEntry
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BodyMetricsChart(
    weights: List<WeightEntry>,
    heightCm: Double?,
    modifier: Modifier = Modifier
) {
    var selectedTimeFrame by remember { mutableStateOf(ChartTimeFrame.SIX_MONTHS) }
    var selectedMetricIndex by remember { mutableIntStateOf(0) }

    val hasHeight = heightCm != null && heightCm > 0
    val weightLabel = stringResource(R.string.chart_metric_weight)
    val bmiLabel = stringResource(R.string.chart_metric_bmi)
    val bodyFatLabel = stringResource(R.string.chart_metric_body_fat)

    val metricTabs = remember(hasHeight, weightLabel, bmiLabel, bodyFatLabel) {
        if (hasHeight) {
            listOf(weightLabel, bmiLabel, bodyFatLabel)
        } else {
            listOf(weightLabel, bodyFatLabel)
        }
    }

    val currentUnit = when (selectedMetricIndex) {
        0 -> stringResource(R.string.unit_kg)
        1 -> if (hasHeight) "" else "%"
        2 -> "%"
        else -> stringResource(R.string.unit_kg)
    }

    val chartDataPoints = remember(weights, selectedTimeFrame, selectedMetricIndex, heightCm) {
        val cutoff = selectedTimeFrame.getCutoffDate()

        weights.mapNotNull { entry ->
            val localDate = try {
                LocalDate.parse(entry.date.substringBefore('T'))
            } catch (_: Exception) {
                null
            } ?: return@mapNotNull null

            if (cutoff != null && localDate.isBefore(cutoff)) {
                return@mapNotNull null
            }

            val value: Float? = when (selectedMetricIndex) {
                0 -> entry.weightKg.toFloat()
                1 -> {
                    if (hasHeight) {
                        val heightM = (heightCm!! / 100.0).toFloat()
                        if (heightM > 0f) {
                            entry.weightKg.toFloat() / (heightM * heightM)
                        } else null
                    } else {
                        entry.bodyFatPercent?.toFloat()
                    }
                }
                2 -> entry.bodyFatPercent?.toFloat()
                else -> entry.weightKg.toFloat()
            }

            if (value == null || value <= 0f) return@mapNotNull null

            val timestamp = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val formattedDate = localDate.format(DateTimeFormatter.ofPattern("MMM d"))
            ChartDataPoint(
                timestamp = timestamp,
                value = value,
                formattedDate = formattedDate
            )
        }.sortedBy { it.timestamp }
    }

    SportLineChart(
        title = stringResource(R.string.chart_weight_progress),
        dataPoints = chartDataPoints,
        unit = currentUnit,
        lineColor = MaterialTheme.colorScheme.secondary,
        selectedTimeFrame = selectedTimeFrame,
        onTimeFrameSelected = { selectedTimeFrame = it },
        metricTabs = metricTabs,
        selectedMetricIndex = selectedMetricIndex,
        onMetricTabSelected = { selectedMetricIndex = it },
        modifier = modifier.fillMaxWidth()
    )
}
