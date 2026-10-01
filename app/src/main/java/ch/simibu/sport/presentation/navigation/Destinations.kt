package ch.simibu.sport.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object AuthDestination

@Serializable
object DashboardDestination

@Serializable
object ExercisesDestination

@Serializable
data class ExerciseDetailsDestination(val id: String)

@Serializable
data class ExerciseHistoryDestination(val id: String)

@Serializable
data class ExerciseFormDestination(val id: String? = null)

@Serializable
object JournalDestination

@Serializable
data class WorkoutDetailsDestination(val id: String)

@Serializable
data class WorkoutFormDestination(
    val id: String? = null,
    val templateId: String? = null
)

@Serializable
object TemplatesDestination

@Serializable
data class TemplateFormDestination(val id: String? = null)

@Serializable
object ProfileDestination

@Serializable
object BodyHistoryDestination
