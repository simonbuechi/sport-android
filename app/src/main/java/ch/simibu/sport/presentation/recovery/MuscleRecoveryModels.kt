package ch.simibu.sport.presentation.recovery

import androidx.compose.ui.graphics.Color
import ch.simibu.sport.R
import ch.simibu.sport.core.model.BodyPart

enum class RecoveryStatus(
    val labelRes: Int,
    val defaultColor: Color
) {
    FATIGUED(R.string.recovery_fatigued, Color(0xFFEF4444)), // Red/Coral 0-40%
    RECOVERING(R.string.recovery_recovering, Color(0xFFF59E0B)), // Amber 41-85%
    READY(R.string.recovery_ready, Color(0xFF10B981)) // Green/Teal 86-100%
}

data class MuscleRecoveryData(
    val bodyPart: BodyPart,
    val recoveryPercent: Int, // 0 to 100
    val status: RecoveryStatus,
    val lastTrainedDate: String? = null,
    val hoursSinceWorkout: Long? = null,
    val setsInLast72h: Int = 0,
    val color: Color
)

data class BodyRecoverySummary(
    val overallRecoveryPercent: Int,
    val status: RecoveryStatus,
    val muscles: Map<BodyPart, MuscleRecoveryData>,
    val fatiguedMuscles: List<MuscleRecoveryData>,
    val recoveringMuscles: List<MuscleRecoveryData>,
    val readyMuscles: List<MuscleRecoveryData>
) {
    companion object {
        fun defaultFullyRecovered(): BodyRecoverySummary {
            val defaultMap = BodyPart.entries
                .filter { it != BodyPart.WHOLE_BODY }
                .associateWith { bp ->
                    MuscleRecoveryData(
                        bodyPart = bp,
                        recoveryPercent = 100,
                        status = RecoveryStatus.READY,
                        lastTrainedDate = null,
                        hoursSinceWorkout = null,
                        setsInLast72h = 0,
                        color = RecoveryStatus.READY.defaultColor
                    )
                }
            return BodyRecoverySummary(
                overallRecoveryPercent = 100,
                status = RecoveryStatus.READY,
                muscles = defaultMap,
                fatiguedMuscles = emptyList(),
                recoveringMuscles = emptyList(),
                readyMuscles = defaultMap.values.toList()
            )
        }
    }
}

enum class BodyViewType {
    FRONT,
    BACK
}
