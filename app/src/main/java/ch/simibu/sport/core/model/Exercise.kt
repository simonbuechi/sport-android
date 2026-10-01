package ch.simibu.sport.core.model

import androidx.annotation.StringRes
import ch.simibu.sport.R
import kotlinx.serialization.Serializable

@Serializable
enum class ExerciseType(val value: String, @StringRes val labelRes: Int) {
    STRENGTH("strength", R.string.type_strength),
    CARDIO("cardio", R.string.type_cardio),
    FLEXIBILITY("flexibility", R.string.type_flexibility),
    OTHER("other", R.string.type_other);

    companion object {
        fun fromString(value: String?): ExerciseType {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: STRENGTH
        }
    }
}

@Serializable
enum class BodyPart(val displayName: String, @StringRes val labelRes: Int) {
    WHOLE_BODY("Whole Body", R.string.bodypart_whole_body),
    LEGS("Legs", R.string.bodypart_legs),
    BACK("Back", R.string.bodypart_back),
    SHOULDERS("Shoulders", R.string.bodypart_shoulders),
    CHEST("Chest", R.string.bodypart_chest),
    BICEPS("Biceps", R.string.bodypart_biceps),
    TRICEPS("Triceps", R.string.bodypart_triceps),
    CORE("Core", R.string.bodypart_core),
    FOREARMS("Forearms", R.string.bodypart_forearms);

    companion object {
        fun fromString(value: String?): BodyPart {
            return entries.find { it.displayName.equals(value, ignoreCase = true) } ?: WHOLE_BODY
        }
    }
}

@Serializable
enum class ExerciseCategory(val displayName: String, @StringRes val labelRes: Int) {
    BODYWEIGHT("Bodyweight", R.string.category_bodyweight),
    BARBELL("Barbell", R.string.category_barbell),
    DUMBBELL("Dumbbell", R.string.category_dumbbell),
    MACHINE("Machine", R.string.category_machine),
    CABLE("Cable", R.string.category_cable),
    KETTLEBELL("Kettlebell", R.string.category_kettlebell);

    companion object {
        fun fromString(value: String?): ExerciseCategory {
            return entries.find { it.displayName.equals(value, ignoreCase = true) } ?: BODYWEIGHT
        }
    }
}

@Serializable
data class ExerciseLink(
    val url: String = "",
    val label: String? = null
)

@Serializable
data class Exercise(
    val id: String = "",
    val name: String = "",
    val type: ExerciseType = ExerciseType.STRENGTH,
    val bodypart: BodyPart = BodyPart.WHOLE_BODY,
    val category: ExerciseCategory = ExerciseCategory.BODYWEIGHT,
    val aliases: List<String> = emptyList(),
    val description: String? = null,
    val iconUrl: String? = null,
    val show: Boolean = true,
    val popular: Boolean = false,
    val links: List<ExerciseLink> = emptyList()
)
