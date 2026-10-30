package ch.simibu.sport.core.model

import kotlinx.serialization.Serializable

@Serializable
data class MarkedStatus(
    val favorite: Boolean = false,
    val notes: String? = null
)

@Serializable
data class WeightEntry(
    val id: String = "",
    val date: String = "", // YYYY-MM-DD
    val weightKg: Double = 0.0,
    val bodyFatPercent: Double? = null
)

@Serializable
data class MeasurementEntry(
    val id: String = "",
    val date: String = "", // YYYY-MM-DD
    val waist: Double? = null,
    val hips: Double? = null,
    val neck: Double? = null,
    val chest: Double? = null,
    val shoulders: Double? = null,
    val rightBicep: Double? = null,
    val leftBicep: Double? = null,
    val rightForearm: Double? = null,
    val leftForearm: Double? = null,
    val rightThigh: Double? = null,
    val leftThigh: Double? = null,
    val rightCalf: Double? = null,
    val leftCalf: Double? = null
)

@Serializable
data class UserSettings(
    val theme: String = "system", // light, dark, system
    val autoFillSets: Boolean = true,
    val showTimer: Boolean = true,
    val takeWorkoutTime: Boolean = true,
    val enableSupersets: Boolean = false,
    val enableExertionTracking: Boolean = false,
    val exertionMode: String = "rpe", // "rpe" or "rir"
    val enableHealthConnect: Boolean = false
)

@Serializable
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val birthYear: Int? = null,
    val height: Double? = null, // cm
    val notes: String = "",
    val markedExercises: Map<String, MarkedStatus> = emptyMap(),
    val weights: List<WeightEntry> = emptyList(),
    val measurements: List<MeasurementEntry> = emptyList(),
    val dashboardWidgets: List<String> = emptyList(),
    val dashboardOrder: List<String> = emptyList(),
    val settings: UserSettings = UserSettings()
)
