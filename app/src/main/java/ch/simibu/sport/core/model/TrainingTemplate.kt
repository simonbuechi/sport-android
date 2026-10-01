package ch.simibu.sport.core.model

import kotlinx.serialization.Serializable

@Serializable
data class TemplateExercise(
    val exerciseId: String = "",
    val note: String? = null,
    val sets: List<ExerciseSet> = emptyList()
)

@Serializable
data class TrainingTemplate(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val exercises: List<TemplateExercise> = emptyList()
)
