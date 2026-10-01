package ch.simibu.sport.domain.usecase

import ch.simibu.sport.core.model.ExerciseSet
import ch.simibu.sport.core.model.Workout
import ch.simibu.sport.core.model.WorkoutExercise
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WorkoutCalculationUseCaseTest {

    private lateinit var useCase: WorkoutCalculationUseCase

    @Before
    fun setUp() {
        useCase = WorkoutCalculationUseCase()
    }

    @Test
    fun calculateTotalVolume_sumsAllSetsCorrectly() {
        val workout = Workout(
            id = "w1",
            exercises = listOf(
                WorkoutExercise(
                    exerciseId = "bench-press",
                    sets = listOf(
                        ExerciseSet(weight = 100.0, reps = 10), // 1000
                        ExerciseSet(weight = 100.0, reps = 8)   // 800
                    )
                ),
                WorkoutExercise(
                    exerciseId = "pull-ups",
                    sets = listOf(
                        ExerciseSet(weight = 0.0, reps = 12),   // 0
                        ExerciseSet(weight = 10.0, reps = 5)    // 50
                    )
                )
            )
        )

        val volume = useCase.calculateTotalVolume(workout)
        assertEquals(1850.0, volume, 0.001)
    }

    @Test
    fun calculate1RM_calculatesEpleyFormulaCorrectly() {
        // 100kg for 1 rep = 100kg
        assertEquals(100.0, useCase.calculate1RM(100.0, 1), 0.01)

        // 100kg for 10 reps: 100 * (1 + 10/30) = 133.3 kg
        assertEquals(133.3, useCase.calculate1RM(100.0, 10), 0.01)

        // 0 weight or 0 reps
        assertEquals(0.0, useCase.calculate1RM(0.0, 10), 0.01)
        assertEquals(0.0, useCase.calculate1RM(100.0, 0), 0.01)
    }
}
