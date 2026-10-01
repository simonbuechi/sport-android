package ch.simibu.sport.domain.repository

import ch.simibu.sport.core.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getExercises(): Flow<List<Exercise>>
    fun getExercise(id: String): Flow<Exercise?>
    suspend fun createExercise(exercise: Exercise): Result<String>
    suspend fun updateExercise(exercise: Exercise): Result<Unit>
    suspend fun deleteExercise(id: String): Result<Unit>
}
