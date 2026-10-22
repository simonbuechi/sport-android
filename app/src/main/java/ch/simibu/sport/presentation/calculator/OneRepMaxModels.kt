package ch.simibu.sport.presentation.calculator

import androidx.compose.ui.graphics.Color
import ch.simibu.sport.R
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

enum class OneRmFormula(val titleRes: Int) {
    EPLEY(R.string.one_rm_formula_epley),
    BRZYCKI(R.string.one_rm_formula_brzycki),
    LOMBARDI(R.string.one_rm_formula_lombardi),
    WATHAN(R.string.one_rm_formula_wathan),
    AVERAGE(R.string.one_rm_formula_average)
}

enum class TrainingZone(
    val titleRes: Int,
    val color: Color
) {
    MAX_EFFORT(R.string.one_rm_zone_max, Color(0xFFEF4444)),
    STRENGTH(R.string.one_rm_zone_strength, Color(0xFFF97316)),
    HYPERTROPHY(R.string.one_rm_zone_hypertrophy, Color(0xFF10B981)),
    ENDURANCE(R.string.one_rm_zone_endurance, Color(0xFF06B6D4)),
    WARMUP(R.string.one_rm_zone_warmup, Color(0xFF6366F1))
}

data class PercentageRow(
    val percentage: Int,
    val exactWeight: Double,
    val roundedWeight: Double,
    val estReps: String,
    val zone: TrainingZone
)

data class OneRmResult(
    val weight: Double,
    val reps: Int,
    val formula: OneRmFormula,
    val calculated1RM: Double,
    val rounded1RM: Double,
    val percentageRows: List<PercentageRow>
)

object OneRepMaxCalculator {

    fun calculate1RM(weight: Double, reps: Int, formula: OneRmFormula): Double {
        if (weight <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weight

        val raw = when (formula) {
            OneRmFormula.EPLEY -> weight * (1.0 + reps / 30.0)
            OneRmFormula.BRZYCKI -> {
                if (reps >= 37) weight * (1.0 + reps / 30.0)
                else weight * (36.0 / (37.0 - reps))
            }
            OneRmFormula.LOMBARDI -> weight * reps.toDouble().pow(0.10)
            OneRmFormula.WATHAN -> (100.0 * weight) / (48.8 + 53.8 * exp(-0.075 * reps))
            OneRmFormula.AVERAGE -> {
                val epley = weight * (1.0 + reps / 30.0)
                val brzycki = if (reps >= 37) epley else weight * (36.0 / (37.0 - reps))
                val lombardi = weight * reps.toDouble().pow(0.10)
                val wathan = (100.0 * weight) / (48.8 + 53.8 * exp(-0.075 * reps))
                (epley + brzycki + lombardi + wathan) / 4.0
            }
        }

        return (raw * 10.0).roundToInt() / 10.0
    }

    fun roundToNearest(weight: Double, step: Double): Double {
        if (step <= 0.0) return (weight * 10.0).roundToInt() / 10.0
        return ((weight / step).roundToInt() * step * 100.0).roundToInt() / 100.0
    }

    fun computeFullBreakdown(
        weight: Double,
        reps: Int,
        formula: OneRmFormula = OneRmFormula.EPLEY,
        roundStep: Double = 2.5
    ): OneRmResult {
        val oneRm = calculate1RM(weight, reps, formula)
        val roundedOneRm = roundToNearest(oneRm, roundStep)

        val tiers = listOf(
            Triple(100, "1", TrainingZone.MAX_EFFORT),
            Triple(95, "2", TrainingZone.MAX_EFFORT),
            Triple(90, "3–4", TrainingZone.STRENGTH),
            Triple(85, "5–6", TrainingZone.STRENGTH),
            Triple(80, "7–8", TrainingZone.HYPERTROPHY),
            Triple(75, "9–10", TrainingZone.HYPERTROPHY),
            Triple(70, "11–12", TrainingZone.HYPERTROPHY),
            Triple(65, "13–15", TrainingZone.ENDURANCE),
            Triple(60, "16–18", TrainingZone.ENDURANCE),
            Triple(55, "19–20", TrainingZone.WARMUP),
            Triple(50, "21–25", TrainingZone.WARMUP)
        )

        val rows = tiers.map { (pct, repStr, zone) ->
            val exact = (oneRm * (pct / 100.0) * 10.0).roundToInt() / 10.0
            val rounded = roundToNearest(exact, roundStep)
            PercentageRow(
                percentage = pct,
                exactWeight = exact,
                roundedWeight = rounded,
                estReps = repStr,
                zone = zone
            )
        }

        return OneRmResult(
            weight = weight,
            reps = reps,
            formula = formula,
            calculated1RM = oneRm,
            rounded1RM = roundedOneRm,
            percentageRows = rows
        )
    }
}
