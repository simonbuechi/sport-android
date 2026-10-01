package ch.simibu.sport.domain.usecase

import ch.simibu.sport.core.model.Workout
import javax.inject.Inject
import kotlin.math.roundToInt

class WorkoutCalculationUseCase @Inject constructor() {

    fun calculateTotalVolume(workout: Workout): Double {
        var total = 0.0
        for (exercise in workout.exercises) {
            for (set in exercise.sets) {
                val weight = set.weight ?: 0.0
                val reps = set.reps ?: 0
                total += (weight * reps)
            }
        }
        return total
    }

    fun calculate1RM(weight: Double, reps: Int): Double {
        if (reps <= 0 || weight <= 0) return 0.0
        if (reps == 1) return weight
        // Epley formula
        val epley = weight * (1.0 + reps / 30.0)
        return (epley * 10.0).roundToInt() / 10.0
    }
}
