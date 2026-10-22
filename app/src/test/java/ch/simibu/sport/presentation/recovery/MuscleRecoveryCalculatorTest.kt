package ch.simibu.sport.presentation.recovery

import ch.simibu.sport.core.model.BodyPart
import ch.simibu.sport.core.model.Exercise
import ch.simibu.sport.core.model.ExerciseSet
import ch.simibu.sport.core.model.Workout
import ch.simibu.sport.core.model.WorkoutExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class MuscleRecoveryCalculatorTest {

    private val benchPress = Exercise(id = "bench", name = "Bench Press", bodypart = BodyPart.CHEST)
    private val squat = Exercise(id = "squat", name = "Squat", bodypart = BodyPart.LEGS)
    private val exerciseMap = mapOf("bench" to benchPress, "squat" to squat)

    @Test
    fun testNoWorkouts_returnsAllReady() {
        val summary = MuscleRecoveryCalculator.calculateRecovery(
            workouts = emptyList(),
            exerciseMap = exerciseMap
        )

        assertEquals(100, summary.overallRecoveryPercent)
        assertEquals(RecoveryStatus.READY, summary.status)
        assertTrue(summary.fatiguedMuscles.isEmpty())
        assertTrue(summary.readyMuscles.size >= 7)
    }

    @Test
    fun testRecentWorkout2HoursAgo_chestIsFatigued() {
        val now = LocalDateTime.of(2026, 10, 22, 12, 0)
        val workoutTime = now.minusHours(2)

        val workout = Workout(
            id = "w1",
            date = workoutTime.toLocalDate().toString(),
            time = workoutTime.toLocalTime().toString(),
            intensity = 4,
            exercises = listOf(
                WorkoutExercise(
                    exerciseId = "bench",
                    sets = listOf(
                        ExerciseSet(weight = 80.0, reps = 8),
                        ExerciseSet(weight = 80.0, reps = 8),
                        ExerciseSet(weight = 80.0, reps = 8),
                        ExerciseSet(weight = 80.0, reps = 8)
                    )
                )
            )
        )

        val summary = MuscleRecoveryCalculator.calculateRecovery(
            workouts = listOf(workout),
            exerciseMap = exerciseMap,
            now = now
        )

        val chest = summary.muscles[BodyPart.CHEST]
        assertNotNull(chest)
        assertEquals(RecoveryStatus.FATIGUED, chest!!.status)
        assertTrue(chest.recoveryPercent <= 40)
        assertEquals(4, chest.setsInLast72h)

        // Legs was not trained, should remain READY
        val legs = summary.muscles[BodyPart.LEGS]
        assertNotNull(legs)
        assertEquals(RecoveryStatus.READY, legs!!.status)
        assertEquals(100, legs.recoveryPercent)
    }

    @Test
    fun testWorkout36HoursAgo_chestIsRecovering() {
        val now = LocalDateTime.of(2026, 10, 22, 12, 0)
        val workoutTime = now.minusHours(36)

        val workout = Workout(
            id = "w1",
            date = workoutTime.toLocalDate().toString(),
            time = workoutTime.toLocalTime().toString(),
            intensity = 3,
            exercises = listOf(
                WorkoutExercise(
                    exerciseId = "bench",
                    sets = listOf(
                        ExerciseSet(weight = 80.0, reps = 8),
                        ExerciseSet(weight = 80.0, reps = 8),
                        ExerciseSet(weight = 80.0, reps = 8)
                    )
                )
            )
        )

        val summary = MuscleRecoveryCalculator.calculateRecovery(
            workouts = listOf(workout),
            exerciseMap = exerciseMap,
            now = now
        )

        val chest = summary.muscles[BodyPart.CHEST]
        assertNotNull(chest)
        assertEquals(RecoveryStatus.RECOVERING, chest!!.status)
        assertTrue(chest.recoveryPercent in 41..85)
    }

    @Test
    fun testWorkout80HoursAgo_chestIsFullyRecovered() {
        val now = LocalDateTime.of(2026, 10, 22, 12, 0)
        val workoutTime = now.minusHours(80)

        val workout = Workout(
            id = "w1",
            date = workoutTime.toLocalDate().toString(),
            time = workoutTime.toLocalTime().toString(),
            intensity = 3,
            exercises = listOf(
                WorkoutExercise(
                    exerciseId = "bench",
                    sets = listOf(
                        ExerciseSet(weight = 80.0, reps = 8),
                        ExerciseSet(weight = 80.0, reps = 8)
                    )
                )
            )
        )

        val summary = MuscleRecoveryCalculator.calculateRecovery(
            workouts = listOf(workout),
            exerciseMap = exerciseMap,
            now = now
        )

        val chest = summary.muscles[BodyPart.CHEST]
        assertNotNull(chest)
        assertEquals(RecoveryStatus.READY, chest!!.status)
        assertEquals(100, chest.recoveryPercent)
    }
}
