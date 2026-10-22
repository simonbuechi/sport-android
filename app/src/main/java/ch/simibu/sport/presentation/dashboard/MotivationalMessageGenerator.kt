package ch.simibu.sport.presentation.dashboard

import ch.simibu.sport.core.model.PersonalRecord
import java.time.LocalDate

object MotivationalMessageGenerator {

    fun generateMessage(
        workedOutToday: Boolean,
        daysSinceLastWorkout: Int?,
        lastPR: PersonalRecord?,
        seedDate: LocalDate = LocalDate.now()
    ): String {
        val seed = seedDate.dayOfYear + (lastPR?.exerciseId?.hashCode() ?: 0)

        fun pick(options: List<String>): String {
            val idx = Math.abs(seed) % options.size
            return options[idx]
        }

        // Scenario 1: User has no workouts yet
        if (daysSinceLastWorkout == null) {
            return pick(
                listOf(
                    "Welcome! Your fitness journey starts today — tap + to log workout #1.",
                    "Every legend started with Day 1. Start your first workout today!",
                    "The journey of a thousand reps begins with a single set. Ready to make history?"
                )
            )
        }

        // Scenario 2: User has worked out today
        if (workedOutToday) {
            val hasPrToday = lastPR != null && lastPR.daysAgo == 0L
            if (hasPrToday) {
                return pick(
                    listOf(
                        "🔥 Beast mode today! New PR on ${lastPR!!.exerciseName} (${lastPR.weight} kg). Rest up!",
                        "🏆 Record breaker! You hit a personal best on ${lastPR!!.exerciseName} today. Enjoy the gains!",
                        "⚡ What a session! Setting a new ${lastPR!!.exerciseName} record today proves the work pays off."
                    )
                )
            } else {
                return pick(
                    listOf(
                        "💪 Workout crushed today! Consistency is where champions are made.",
                        "✨ Today's session is in the books! Rehydrate, refuel, and let muscles rebuild.",
                        "🎯 You showed up and conquered today. Enjoy the post-workout endorphins!",
                        "🚀 Session locked in today. Great job keeping the momentum strong!"
                    )
                )
            }
        }

        // Scenario 3: Worked out yesterday (1 day ago)
        if (daysSinceLastWorkout == 1) {
            val hasPrYesterday = lastPR != null && lastPR.daysAgo == 1L
            if (hasPrYesterday) {
                return pick(
                    listOf(
                        "⚡ Riding high on yesterday's ${lastPR!!.exerciseName} PR! Ready to keep the momentum going?",
                        "🔥 Yesterday was huge with that ${lastPR!!.exerciseName} record. Let's keep that fire burning!"
                    )
                )
            } else {
                return pick(
                    listOf(
                        "💥 Strong effort yesterday! Taking a recovery day or ready for round two?",
                        "🌟 Back-to-back greatness? You crushed it yesterday — what's on the menu today?",
                        "🏋️ Yesterday's work pays today's dividends. Ready to conquer again?",
                        "⚡ Solid consistency! Ready to conquer today or enjoy a well-earned rest?"
                    )
                )
            }
        }

        // Scenario 4: Worked out 2 to 3 days ago
        if (daysSinceLastWorkout in 2..3) {
            val hasRecentPr = lastPR != null && lastPR.daysAgo in 2..7
            if (hasRecentPr) {
                return pick(
                    listOf(
                        "🏆 Channel that ${lastPR!!.exerciseName} PR energy from a few days ago. The iron awaits!",
                        "✨ Still hyped about your recent ${lastPR!!.exerciseName} record? Let's chase another one today!"
                    )
                )
            } else {
                return pick(
                    listOf(
                        "🔋 Well-rested and fully charged. The weights are calling your name today!",
                        "⚡ Your muscles are recovered and primed for growth. Time to get after it!",
                        "🎯 Perfect timing for a session! Ready to hit the gym today?",
                        "🔥 Fully recovered. Today is a great day to move some serious weight!"
                    )
                )
            }
        }

        // Scenario 5: Worked out 4+ days ago
        return pick(
            listOf(
                "⏳ It's been $daysSinceLastWorkout days since your last session. An iron comeback starts with rep one!",
                "🚀 Ready to shake off the rust? Even a short session today will feel incredible.",
                "💪 The weights miss you! Dust off your gear and make today count.",
                "⚡ No pressure, just progress. The best time to restart your streak is right now!"
            )
        )
    }
}
