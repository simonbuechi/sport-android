package ch.simibu.sport.presentation.dashboard

import ch.simibu.sport.core.model.PersonalRecord
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MotivationalMessageGeneratorTest {

    private val testDate = LocalDate.of(2026, 10, 22)

    @Test
    fun testNoWorkoutsYet_returnsWelcomeMessage() {
        val message = MotivationalMessageGenerator.generateMessage(
            workedOutToday = false,
            daysSinceLastWorkout = null,
            lastPR = null,
            seedDate = testDate
        )

        assertNotNull(message)
        assertTrue(message.isNotBlank())
        assertTrue(message.contains("Welcome") || message.contains("journey") || message.contains("Day 1"))
    }

    @Test
    fun testWorkedOutToday_withPrToday_mentionsPrAndExercise() {
        val pr = PersonalRecord(
            exerciseId = "bench_press",
            exerciseName = "Bench Press",
            weight = 100.0,
            reps = 5,
            estimated1RM = 116.7,
            date = "2026-10-22",
            daysAgo = 0L
        )

        val message = MotivationalMessageGenerator.generateMessage(
            workedOutToday = true,
            daysSinceLastWorkout = 0,
            lastPR = pr,
            seedDate = testDate
        )

        assertNotNull(message)
        assertTrue(message.contains("Bench Press"))
        assertTrue(message.contains("PR") || message.contains("record") || message.contains("best"))
    }

    @Test
    fun testWorkedOutToday_withoutPrToday_returnsCompletedSessionMessage() {
        val oldPr = PersonalRecord(
            exerciseId = "squat",
            exerciseName = "Barbell Squat",
            weight = 140.0,
            reps = 3,
            estimated1RM = 154.0,
            date = "2026-10-10",
            daysAgo = 12L
        )

        val message = MotivationalMessageGenerator.generateMessage(
            workedOutToday = true,
            daysSinceLastWorkout = 0,
            lastPR = oldPr,
            seedDate = testDate
        )

        assertNotNull(message)
        assertTrue(message.contains("today") || message.contains("Today"))
    }

    @Test
    fun testWorkedOutYesterday_withPrYesterday_mentionsYesterdayPr() {
        val yesterdayPr = PersonalRecord(
            exerciseId = "deadlift",
            exerciseName = "Deadlift",
            weight = 180.0,
            reps = 2,
            estimated1RM = 192.0,
            date = "2026-10-21",
            daysAgo = 1L
        )

        val message = MotivationalMessageGenerator.generateMessage(
            workedOutToday = false,
            daysSinceLastWorkout = 1,
            lastPR = yesterdayPr,
            seedDate = testDate
        )

        assertNotNull(message)
        assertTrue(message.contains("Deadlift"))
        assertTrue(message.contains("yesterday") || message.contains("Yesterday"))
    }

    @Test
    fun testWorkedOutYesterday_general_returnsConsistencyMessage() {
        val message = MotivationalMessageGenerator.generateMessage(
            workedOutToday = false,
            daysSinceLastWorkout = 1,
            lastPR = null,
            seedDate = testDate
        )

        assertNotNull(message)
        assertTrue(message.contains("yesterday") || message.contains("Yesterday") || message.contains("consistency"))
    }

    @Test
    fun testWorkedOut2DaysAgo_returnsRecoveredMessage() {
        val message = MotivationalMessageGenerator.generateMessage(
            workedOutToday = false,
            daysSinceLastWorkout = 2,
            lastPR = null,
            seedDate = testDate
        )

        assertNotNull(message)
        assertTrue(message.isNotBlank())
    }

    @Test
    fun testWorkedOut5DaysAgo_returnsComebackMessage() {
        val message = MotivationalMessageGenerator.generateMessage(
            workedOutToday = false,
            daysSinceLastWorkout = 5,
            lastPR = null,
            seedDate = testDate
        )

        assertNotNull(message)
        assertTrue(message.contains("5 days") || message.contains("miss") || message.contains("rust") || message.contains("streak"))
    }
}
