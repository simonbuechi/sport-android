package ch.simibu.sport.domain.repository

import ch.simibu.sport.core.model.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getWorkouts(userId: String): Flow<List<Workout>>
    fun getWorkout(userId: String, workoutId: String): Flow<Workout?>
    suspend fun createWorkout(workout: Workout): Result<String>
    suspend fun updateWorkout(workout: Workout): Result<Unit>
    suspend fun deleteWorkout(userId: String, workoutId: String): Result<Unit>
}
