package ch.simibu.sport.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ProgramDay(
    val id: String = UUID.randomUUID().toString(),
    val dayNumber: Int = 1,
    val date: String? = null, // "YYYY-MM-DD" if fixed date
    val templateId: String? = null, // null/blank for Rest Day
    val note: String? = null
) {
    val isRestDay: Boolean get() = templateId.isNullOrBlank()
}

@Serializable
data class Program(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String? = null,
    val type: String = TYPE_REPETITIVE, // "repetitive" | "one-time"
    val isActive: Boolean = false,
    val startDate: String? = null, // "YYYY-MM-DD"
    val currentDayIndex: Int = 0,
    val days: List<ProgramDay> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    val isOneTime: Boolean get() = type == TYPE_ONE_TIME
    val isRepetitive: Boolean get() = type == TYPE_REPETITIVE
    val usesDates: Boolean get() = isOneTime && days.any { !it.date.isNullOrBlank() }

    val workoutDaysCount: Int get() = days.count { !it.isRestDay }
    val restDaysCount: Int get() = days.count { it.isRestDay }

    companion object {
        const val TYPE_REPETITIVE = "repetitive"
        const val TYPE_ONE_TIME = "one-time"
    }
}
