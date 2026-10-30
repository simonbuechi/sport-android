package ch.simibu.sport.core.util

import ch.simibu.sport.core.model.WorkoutExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupersetUtilsTest {

    @Test
    fun testGetAlphabeticalLabel() {
        assertEquals("A", SupersetUtils.getAlphabeticalLabel(0))
        assertEquals("B", SupersetUtils.getAlphabeticalLabel(1))
        assertEquals("Z", SupersetUtils.getAlphabeticalLabel(25))
        assertEquals("AA", SupersetUtils.getAlphabeticalLabel(26))
    }

    @Test
    fun testGroupExercisesIntoBlocks() {
        val exercises = listOf(
            WorkoutExercise(exerciseId = "ex1", supersetId = "ss1"),
            WorkoutExercise(exerciseId = "ex2", supersetId = "ss1"),
            WorkoutExercise(exerciseId = "ex3")
        )

        val groups = SupersetUtils.groupWorkoutExercises(exercises)
        assertEquals(2, groups.size)

        // First group should be Superset A
        assertTrue(groups[0].isSuperset)
        assertEquals("A", groups[0].letter)
        assertEquals(2, groups[0].items.size)
        assertEquals("A1", groups[0].items[0].tag)
        assertEquals("A2", groups[0].items[1].tag)

        // Second group should be standalone
        assertFalse(groups[1].isSuperset)
        assertEquals(1, groups[1].items.size)
        assertEquals(2, groups[1].items[0].originalIndex)
    }

    @Test
    fun testLinkWithNext() {
        val exercises = listOf(
            WorkoutExercise(exerciseId = "ex1"),
            WorkoutExercise(exerciseId = "ex2"),
            WorkoutExercise(exerciseId = "ex3")
        )

        val linked = SupersetUtils.linkWithNext(exercises, 0)
        assertNotNull(linked[0].supersetId)
        assertEquals(linked[0].supersetId, linked[1].supersetId)
        assertNull(linked[2].supersetId)
    }

    @Test
    fun testUnlinkFromSuperset() {
        val exercises = listOf(
            WorkoutExercise(exerciseId = "ex1", supersetId = "ss1"),
            WorkoutExercise(exerciseId = "ex2", supersetId = "ss1"),
            WorkoutExercise(exerciseId = "ex3")
        )

        // If one is unlinked and only 1 remains, both become standalone
        val unlinked = SupersetUtils.unlinkFromSuperset(exercises, 0)
        assertNull(unlinked[0].supersetId)
        assertNull(unlinked[1].supersetId)
    }

    @Test
    fun testUpdateSupersetNote() {
        val exercises = listOf(
            WorkoutExercise(exerciseId = "ex1", supersetId = "ss1"),
            WorkoutExercise(exerciseId = "ex2", supersetId = "ss1")
        )

        val updated = SupersetUtils.updateSupersetNote(exercises, "ss1", "Rest 60s between")
        assertEquals("Rest 60s between", updated[0].supersetNote)
        assertEquals("Rest 60s between", updated[1].supersetNote)
    }
}
