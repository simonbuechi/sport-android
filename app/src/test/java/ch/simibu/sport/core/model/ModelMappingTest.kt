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
}
