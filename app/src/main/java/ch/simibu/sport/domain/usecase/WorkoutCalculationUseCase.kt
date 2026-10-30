package ch.simibu.sport.domain.usecase

import ch.simibu.sport.core.model.Workout
import ch.simibu.sport.presentation.calculator.OneRepMaxCalculator
import ch.simibu.sport.presentation.calculator.OneRmFormula
import javax.inject.Inject

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
        return OneRepMaxCalculator.calculate1RM(weight, reps, OneRmFormula.EPLEY)
    }
}
