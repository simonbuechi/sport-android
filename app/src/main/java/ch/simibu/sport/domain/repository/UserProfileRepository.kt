package ch.simibu.sport.domain.repository

import ch.simibu.sport.core.model.MeasurementEntry
import ch.simibu.sport.core.model.UserProfile
import ch.simibu.sport.core.model.WeightEntry
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun getUserProfile(userId: String): Flow<UserProfile?>
    suspend fun createUserProfile(profile: UserProfile): Result<Unit>
    suspend fun updateUserProfile(profile: UserProfile): Result<Unit>
    suspend fun addWeightEntry(userId: String, entry: WeightEntry): Result<Unit>
    suspend fun addMeasurementEntry(userId: String, entry: MeasurementEntry): Result<Unit>
    suspend fun updateExerciseStatus(
        userId: String,
        exerciseId: String,
        favorite: Boolean?,
        notes: String?
    ): Result<Unit>
}
