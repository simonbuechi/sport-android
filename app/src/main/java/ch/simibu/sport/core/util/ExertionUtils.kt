package ch.simibu.sport.core.util

import kotlin.math.round

object ExertionUtils {

    const val MODE_RPE = "rpe"
    const val MODE_RIR = "rir"

    /**
     * Converts RPE (Rate of Perceived Exertion) to RIR (Reps In Reserve).
     * Formula: RIR = max(0, 10 - RPE)
     */
    fun rpeToRir(rpe: Double): Double {
        val raw = 10.0 - rpe
        return maxOf(0.0, round(raw * 10.0) / 10.0)
    }

    /**
     * Converts RIR (Reps In Reserve) to RPE (Rate of Perceived Exertion).
     * Formula: RPE = max(0, 10 - RIR)
     */
    fun rirToRpe(rir: Double): Double {
        val raw = 10.0 - rir
        return maxOf(0.0, round(raw * 10.0) / 10.0)
    }

    /**
     * Calculates both rpe and rir from a user-entered value given the active mode.
     * Returns Pair(rpe, rir).
     */
    fun calculateExertionValues(value: Double?, mode: String): Pair<Double?, Double?> {
        if (value == null) return Pair(null, null)

        return if (mode == MODE_RPE) {
            val clampedRpe = minOf(10.0, maxOf(0.0, round(value * 2.0) / 2.0))
            Pair(clampedRpe, rpeToRir(clampedRpe))
        } else {
            val clampedRir = maxOf(0.0, round(value * 2.0) / 2.0)
            Pair(rirToRpe(clampedRir), clampedRir)
        }
    }

    /**
     * Returns the exertion value to display in the user's preferred mode ('rpe' or 'rir').
     */
    fun getDisplayedExertion(rpe: Double?, rir: Double?, mode: String): Double? {
        return if (mode == MODE_RPE) {
            when {
                rpe != null -> rpe
                rir != null -> rirToRpe(rir)
                else -> null
            }
        } else {
            when {
                rir != null -> rir
                rpe != null -> rpeToRir(rpe)
                else -> null
            }
        }
    }

    /**
     * Formats an exertion string for labels (e.g. "@ RPE 8.5" or "@ 2 RIR").
     */
    fun formatExertionLabel(rpe: Double?, rir: Double?, mode: String): String {
        val value = getDisplayedExertion(rpe, rir, mode) ?: return ""
        val formatted = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
        return if (mode == MODE_RPE) {
            "@ RPE $formatted"
        } else {
            "@ $formatted RIR"
        }
    }
}
