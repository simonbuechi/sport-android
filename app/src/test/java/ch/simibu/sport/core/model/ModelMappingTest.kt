package ch.simibu.sport.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelMappingTest {

    @Test
    fun exerciseType_fromString_mapsCorrectlyOrDefaults() {
        assertEquals(ExerciseType.STRENGTH, ExerciseType.fromString("strength"))
        assertEquals(ExerciseType.STRENGTH, ExerciseType.fromString("STRENGTH"))
        assertEquals(ExerciseType.CARDIO, ExerciseType.fromString("cardio"))
        assertEquals(ExerciseType.FLEXIBILITY, ExerciseType.fromString("flexibility"))
        assertEquals(ExerciseType.OTHER, ExerciseType.fromString("other"))
        assertEquals(ExerciseType.STRENGTH, ExerciseType.fromString("unknown_type"))
        assertEquals(ExerciseType.STRENGTH, ExerciseType.fromString(null))
    }

    @Test
    fun bodyPart_fromString_mapsCorrectly() {
        assertEquals(BodyPart.CHEST, BodyPart.fromString("Chest"))
        assertEquals(BodyPart.LEGS, BodyPart.fromString("legs"))
        assertEquals(BodyPart.WHOLE_BODY, BodyPart.fromString("nonexistent"))
    }

    @Test
    fun exerciseCategory_fromString_mapsCorrectly() {
        assertEquals(ExerciseCategory.BARBELL, ExerciseCategory.fromString("Barbell"))
        assertEquals(ExerciseCategory.DUMBBELL, ExerciseCategory.fromString("dumbbell"))
        assertEquals(ExerciseCategory.BODYWEIGHT, ExerciseCategory.fromString(null))
    }

    @Test
    fun program_propertiesAndCounts_computeCorrectly() {
        val repetitiveProgram = Program(
            name = "PPL",
            type = Program.TYPE_REPETITIVE,
            days = listOf(
                ProgramDay(dayNumber = 1, templateId = "push_id"),
                ProgramDay(dayNumber = 2, templateId = "pull_id"),
                ProgramDay(dayNumber = 3, templateId = null) // rest
            )
        )
        org.junit.Assert.assertTrue(repetitiveProgram.isRepetitive)
        org.junit.Assert.assertFalse(repetitiveProgram.isOneTime)
        org.junit.Assert.assertEquals(2, repetitiveProgram.workoutDaysCount)
        org.junit.Assert.assertEquals(1, repetitiveProgram.restDaysCount)
        org.junit.Assert.assertFalse(repetitiveProgram.usesDates)

        val oneTimeDatedProgram = Program(
            name = "Peaking Block",
            type = Program.TYPE_ONE_TIME,
            days = listOf(
                ProgramDay(dayNumber = 1, date = "2026-05-01", templateId = "t1"),
                ProgramDay(dayNumber = 2, date = "2026-05-02", templateId = null)
            )
        )
        org.junit.Assert.assertTrue(oneTimeDatedProgram.isOneTime)
        org.junit.Assert.assertFalse(oneTimeDatedProgram.isRepetitive)
        org.junit.Assert.assertTrue(oneTimeDatedProgram.usesDates)
    }

    @Test
    fun programDay_isRestDay_evaluatesCorrectly() {
        val restDay1 = ProgramDay(dayNumber = 1, templateId = null)
        val restDay2 = ProgramDay(dayNumber = 2, templateId = "")
        val restDay3 = ProgramDay(dayNumber = 3, templateId = "   ")
        val workoutDay = ProgramDay(dayNumber = 4, templateId = "template_123")

        org.junit.Assert.assertTrue(restDay1.isRestDay)
        org.junit.Assert.assertTrue(restDay2.isRestDay)
        org.junit.Assert.assertTrue(restDay3.isRestDay)
        org.junit.Assert.assertFalse(workoutDay.isRestDay)
    }
}
