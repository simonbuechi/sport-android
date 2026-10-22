package ch.simibu.sport.presentation.calculator

import androidx.compose.ui.graphics.Color
import ch.simibu.sport.R

data class BarbellType(
    val titleRes: Int,
    val weight: Double
)

val STANDARD_BARBELL_TYPES = listOf(
    BarbellType(R.string.plate_calc_bar_standard, 20.0),
    BarbellType(R.string.plate_calc_bar_women, 15.0),
    BarbellType(R.string.plate_calc_bar_ez, 10.0),
    BarbellType(R.string.plate_calc_bar_zero, 0.0)
)

data class PlateDefinition(
    val weight: Double,
    val color: Color,
    val textColor: Color,
    val heightFraction: Float, // Relative visual height (1.0 = 450mm standard Olympic plate)
    val widthFraction: Float,  // Relative thickness for visual drawing
    val label: String
)

val ALL_STANDARD_PLATES = listOf(
    PlateDefinition(
        weight = 25.0,
        color = Color(0xFFD32F2F), // Red
        textColor = Color.White,
        heightFraction = 1.0f,
        widthFraction = 1.0f,
        label = "25"
    ),
    PlateDefinition(
        weight = 20.0,
        color = Color(0xFF1976D2), // Blue
        textColor = Color.White,
        heightFraction = 0.98f,
        widthFraction = 0.92f,
        label = "20"
    ),
    PlateDefinition(
        weight = 15.0,
        color = Color(0xFFFBC02D), // Yellow
        textColor = Color(0xFF212121),
        heightFraction = 0.88f,
        widthFraction = 0.82f,
        label = "15"
    ),
    PlateDefinition(
        weight = 10.0,
        color = Color(0xFF388E3C), // Green
        textColor = Color.White,
        heightFraction = 0.78f,
        widthFraction = 0.72f,
        label = "10"
    ),
    PlateDefinition(
        weight = 5.0,
        color = Color(0xFFECEFF1), // White
        textColor = Color(0xFF263238),
        heightFraction = 0.64f,
        widthFraction = 0.60f,
        label = "5"
    ),
    PlateDefinition(
        weight = 2.5,
        color = Color(0xFF37474F), // Black
        textColor = Color.White,
        heightFraction = 0.52f,
        widthFraction = 0.50f,
        label = "2.5"
    ),
    PlateDefinition(
        weight = 1.25,
        color = Color(0xFF78909C), // Silver
        textColor = Color.White,
        heightFraction = 0.44f,
        widthFraction = 0.42f,
        label = "1.25"
    ),
    PlateDefinition(
        weight = 0.5,
        color = Color(0xFF9E9E9E), // Micro
        textColor = Color.White,
        heightFraction = 0.36f,
        widthFraction = 0.35f,
        label = "0.5"
    )
)

val DEFAULT_ACTIVE_PLATE_WEIGHTS = setOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

data class PlateCountGroup(
    val plate: PlateDefinition,
    val countPerSide: Int
) {
    val totalCount: Int get() = countPerSide * 2
    val totalWeightPerSide: Double get() = countPerSide * plate.weight
}

data class PlateCalculationResult(
    val targetWeight: Double,
    val barWeight: Double,
    val weightPerSide: Double,
    val loadedPlatesPerSide: List<PlateDefinition>,
    val plateSummary: List<PlateCountGroup>,
    val achievedWeight: Double,
    val remainder: Double
) {
    val isExact: Boolean get() = remainder <= 0.001
}

object PlateCalculatorUtil {
    fun calculate(
        targetWeight: Double,
        barWeight: Double,
        availablePlateWeights: Set<Double>
    ): PlateCalculationResult {
        if (targetWeight <= barWeight) {
            return PlateCalculationResult(
                targetWeight = targetWeight,
                barWeight = barWeight,
                weightPerSide = 0.0,
                loadedPlatesPerSide = emptyList(),
                plateSummary = emptyList(),
                achievedWeight = barWeight,
                remainder = 0.0
            )
        }

        var weightNeededPerSide = (targetWeight - barWeight) / 2.0
        val sortedAvailable = ALL_STANDARD_PLATES
            .filter { availablePlateWeights.contains(it.weight) }
            .sortedByDescending { it.weight }

        val loadedPlates = mutableListOf<PlateDefinition>()
        val summary = mutableListOf<PlateCountGroup>()

        for (plateDef in sortedAvailable) {
            val count = (weightNeededPerSide / plateDef.weight + 0.0001).toInt()
            if (count > 0) {
                repeat(count) {
                    loadedPlates.add(plateDef)
                }
                summary.add(PlateCountGroup(plate = plateDef, countPerSide = count))
                weightNeededPerSide -= (count * plateDef.weight)
                weightNeededPerSide = Math.round(weightNeededPerSide * 1000.0) / 1000.0
            }
        }

        val totalPlatesWeightPerSide = loadedPlates.sumOf { it.weight }
        val achievedWeight = barWeight + (totalPlatesWeightPerSide * 2.0)
        val remainder = Math.max(0.0, Math.round((targetWeight - achievedWeight) * 100.0) / 100.0)

        return PlateCalculationResult(
            targetWeight = targetWeight,
            barWeight = barWeight,
            weightPerSide = totalPlatesWeightPerSide,
            loadedPlatesPerSide = loadedPlates,
            plateSummary = summary,
            achievedWeight = achievedWeight,
            remainder = remainder
        )
    }
}
