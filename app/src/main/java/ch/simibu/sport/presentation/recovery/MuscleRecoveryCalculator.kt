package ch.simibu.sport.presentation.recovery

import androidx.compose.ui.graphics.Color
import ch.simibu.sport.core.model.BodyPart
import ch.simibu.sport.core.model.Exercise
import ch.simibu.sport.core.model.Workout
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

object MuscleRecoveryCalculator {

    private val TRACKED_BODY_PARTS = listOf(
        BodyPart.CHEST,
        BodyPart.BACK,
        BodyPart.LEGS,
        BodyPart.SHOULDERS,
        BodyPart.BICEPS,
        BodyPart.TRICEPS,
        BodyPart.CORE,
        BodyPart.FOREARMS
    )

    fun calculateRecovery(
        workouts: List<Workout>,
        exerciseMap: Map<String, Exercise>,
        now: LocalDateTime = LocalDateTime.now()
    ): BodyRecoverySummary {
        if (workouts.isEmpty()) {
            return BodyRecoverySummary.defaultFullyRecovered()
        }

        val cutoffDateTime = now.minusHours(120) // Consider sessions in the last 5 days

        // Parse workouts into date-time pairs
        val recentSessions = workouts.mapNotNull { w ->
            val dt = parseWorkoutDateTime(w.date, w.time) ?: return@mapNotNull null
            if (dt.isAfter(cutoffDateTime)) Pair(dt, w) else null
        }.sortedByDescending { it.first }

        val muscleRecoveryMap = mutableMapOf<BodyPart, MuscleRecoveryData>()

        for (bodyPart in TRACKED_BODY_PARTS) {
            var totalFatigue = 0.0
            var setsInLast72h = 0
            var lastTrainedDateTime: LocalDateTime? = null
            var lastTrainedDateStr: String? = null

            for ((sessionTime, workout) in recentSessions) {
                // Count effective sets for this body part
                var sessionSetsForMuscle = 0
                for (we in workout.exercises) {
                    val ex = exerciseMap[we.exerciseId]
                    val exBodyPart = ex?.bodypart ?: BodyPart.WHOLE_BODY
                    val setCount = we.sets.size.coerceAtLeast(1)

                    if (exBodyPart == bodyPart) {
                        sessionSetsForMuscle += setCount
                    } else if (exBodyPart == BodyPart.WHOLE_BODY) {
                        // Whole body exercises lightly fatigue major groups
                        if (bodyPart in listOf(BodyPart.LEGS, BodyPart.BACK, BodyPart.SHOULDERS, BodyPart.CORE)) {
                            sessionSetsForMuscle += (setCount * 0.5).toInt().coerceAtLeast(1)
                        }
                    }
                }

                if (sessionSetsForMuscle > 0) {
                    if (lastTrainedDateTime == null) {
                        lastTrainedDateTime = sessionTime
                        lastTrainedDateStr = workout.date.substringBefore('T')
                    }

                    val hoursAgo = ChronoUnit.HOURS.between(sessionTime, now).coerceAtLeast(0)
                    if (hoursAgo <= 72) {
                        setsInLast72h += sessionSetsForMuscle
                    }

                    // Volume-weighted time decay:
                    // 1 to 5 star intensity factor (default 3)
                    val intensity = workout.intensity ?: 3
                    val intensityMultiplier = 0.8 + (intensity * 0.1)
                    val trainingStress = sessionSetsForMuscle * intensityMultiplier

                    // Recovery hours scale between 48h to 72h
                    val requiredHours = (40.0 + (trainingStress * 3.5)).coerceIn(48.0, 72.0)
                    val recoveryFraction = (hoursAgo.toDouble() / requiredHours).coerceIn(0.0, 1.0)
                    val sessionFatigue = (1.0 - recoveryFraction)

                    // Accumulate fatigue with decay for older sessions
                    totalFatigue += sessionFatigue
                }
            }

            val cappedFatigue = totalFatigue.coerceIn(0.0, 1.0)
            val recoveryPercent = ((1.0 - cappedFatigue) * 100.0).toInt().coerceIn(0, 100)

            val status = when {
                recoveryPercent <= 40 -> RecoveryStatus.FATIGUED
                recoveryPercent <= 85 -> RecoveryStatus.RECOVERING
                else -> RecoveryStatus.READY
            }

            val color = when (status) {
                RecoveryStatus.FATIGUED -> Color(0xFFEF4444)
                RecoveryStatus.RECOVERING -> Color(0xFFF59E0B)
                RecoveryStatus.READY -> Color(0xFF10B981)
            }

            val hoursSince = lastTrainedDateTime?.let {
                ChronoUnit.HOURS.between(it, now).coerceAtLeast(0)
            }

            muscleRecoveryMap[bodyPart] = MuscleRecoveryData(
                bodyPart = bodyPart,
                recoveryPercent = recoveryPercent,
                status = status,
                lastTrainedDate = lastTrainedDateStr,
                hoursSinceWorkout = hoursSince,
                setsInLast72h = setsInLast72h,
                color = color
            )
        }

        val allValues = muscleRecoveryMap.values.toList()
        val avgRecovery = if (allValues.isNotEmpty()) {
            allValues.map { it.recoveryPercent }.average().toInt()
        } else 100

        val overallStatus = when {
            avgRecovery <= 40 -> RecoveryStatus.FATIGUED
            avgRecovery <= 85 -> RecoveryStatus.RECOVERING
            else -> RecoveryStatus.READY
        }

        return BodyRecoverySummary(
            overallRecoveryPercent = avgRecovery,
            status = overallStatus,
            muscles = muscleRecoveryMap,
            fatiguedMuscles = allValues.filter { it.status == RecoveryStatus.FATIGUED },
            recoveringMuscles = allValues.filter { it.status == RecoveryStatus.RECOVERING },
            readyMuscles = allValues.filter { it.status == RecoveryStatus.READY }
        )
    }

    private fun parseWorkoutDateTime(dateStr: String, timeStr: String?): LocalDateTime? {
        val cleanDate = dateStr.substringBefore('T').trim()
        val localDate = try {
            LocalDate.parse(cleanDate)
        } catch (_: Exception) {
            return null
        }

        val localTime = if (!timeStr.isNullOrBlank()) {
            try {
                LocalTime.parse(timeStr.trim())
            } catch (_: Exception) {
                LocalTime.NOON
            }
        } else {
            LocalTime.NOON
        }

        return LocalDateTime.of(localDate, localTime)
    }
}
