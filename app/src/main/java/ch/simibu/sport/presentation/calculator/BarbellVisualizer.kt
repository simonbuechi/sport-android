package ch.simibu.sport.presentation.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.simibu.sport.R

@Composable
fun BarbellVisualizer(
    calculationResult: PlateCalculationResult,
    modifier: Modifier = Modifier
) {
    val maxPlateHeight = 120.dp
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Barbell Loading Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxPlateHeight + 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Grip Shaft (Inner barbell)
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(18.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF5A6268),
                                    Color(0xFF868E96),
                                    Color(0xFF495057)
                                )
                            ),
                            RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                        )
                )

                // 2. Inner Barbell Flange / Collar Stop (Thick metallic ring)
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(84.dp)
                        .shadow(4.dp, RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF2B303A),
                                    Color(0xFF495057),
                                    Color(0xFF212529)
                                )
                            ),
                            RoundedCornerShape(2.dp)
                        )
                )

                // 3. Sleeve Section with Loaded Plates
                Box(
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Steel Barbell Sleeve Core (behind plates)
                    val sleeveWidth = maxOf(
                        160.dp,
                        (calculationResult.loadedPlatesPerSide.size * 26 + 48).dp
                    )
                    Box(
                        modifier = Modifier
                            .width(sleeveWidth)
                            .height(28.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFCED4DA),
                                        Color(0xFFF8F9FA),
                                        Color(0xFFADB5BD)
                                    )
                                )
                            )
                    )

                    // Plates stacked sequentially
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        if (calculationResult.loadedPlatesPerSide.isEmpty()) {
                            // Empty sleeve indicator
                            Box(
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .height(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.plate_calc_empty_bar),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            calculationResult.loadedPlatesPerSide.forEach { plate ->
                                BarbellPlateItem(plate = plate, maxHeight = maxPlateHeight)
                                Spacer(modifier = Modifier.width(2.dp))
                            }

                            // 4. Barbell Spring Clamp / Collar
                            BarbellClampItem()
                        }
                    }
                }

                // 5. Sleeve End Cap
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(28.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFADB5BD), Color(0xFF6C757D))
                            ),
                            RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Weight Breakdown Subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(
                        R.string.plate_calc_weight_per_side,
                        calculationResult.weightPerSide
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${stringResource(R.string.plate_calc_bar_weight)}: ${calculationResult.barWeight} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Total weight achieved badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${calculationResult.achievedWeight} kg",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BarbellPlateItem(
    plate: PlateDefinition,
    maxHeight: androidx.compose.ui.unit.Dp
) {
    val plateHeight = maxHeight * plate.heightFraction
    val plateWidth = (24.dp * plate.widthFraction).coerceAtLeast(14.dp)

    Box(
        modifier = Modifier
            .width(plateWidth)
            .height(plateHeight)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(3.dp)
            )
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        plate.color.copy(alpha = 0.85f),
                        plate.color,
                        plate.color.copy(alpha = 0.9f)
                    )
                ),
                shape = RoundedCornerShape(3.dp)
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.35f),
                shape = RoundedCornerShape(3.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Label on the plate
        Text(
            text = plate.label,
            color = plate.textColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (plate.label.length > 3) 8.sp else 10.sp,
                fontWeight = FontWeight.Black
            ),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun BarbellClampItem() {
    Box(
        modifier = Modifier
            .width(10.dp)
            .height(42.dp)
            .shadow(2.dp, RoundedCornerShape(3.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF212529),
                        Color(0xFF495057),
                        Color(0xFF212529)
                    )
                ),
                shape = RoundedCornerShape(3.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFFFFC107).copy(alpha = 0.8f),
                shape = RoundedCornerShape(3.dp)
            )
    )
}
