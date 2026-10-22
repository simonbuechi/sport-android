package ch.simibu.sport.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PersonalRecord(
    val exerciseId: String,
    val exerciseName: String,
    val exerciseIconUrl: String? = null,
    val weight: Double,
    val reps: Int,
    val estimated1RM: Double,
    val date: String, // YYYY-MM-DD
    val daysAgo: Long = 0,
    val workoutId: String? = null
)
