package ch.simibu.sport.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ExerciseSet(
    val id: String = UUID.randomUUID().toString(),
    val weight: Double? = null,
    val reps: Int? = null,
    val notes: String? = null
)

@Serializable
data class WorkoutExercise(
    val exerciseId: String = "",
    val sets: List<ExerciseSet> = emptyList(),
    val note: String? = null
)

@Serializable
data class Workout(
    val id: String = "",
    val userId: String = "",
    val date: String = "", // ISO date string: YYYY-MM-DD
    val time: String? = null, // e.g. HH:mm
    val length: Int? = null, // minutes
    val sessionType: ExerciseType = ExerciseType.STRENGTH,
    val intensity: Int? = null, // 1 to 5
    val maxPulse: Int? = null,
    val comment: String = "",
    val exerciseIds: List<String> = emptyList(),
    val exercises: List<WorkoutExercise> = emptyList()
)
