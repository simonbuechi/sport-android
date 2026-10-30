package ch.simibu.sport.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExertionUtilsTest {

    @Test
    fun testRpeToRir() {
        assertEquals(0.0, ExertionUtils.rpeToRir(10.0), 0.001)
        assertEquals(1.0, ExertionUtils.rpeToRir(9.0), 0.001)
        assertEquals(2.0, ExertionUtils.rpeToRir(8.0), 0.001)
        assertEquals(1.5, ExertionUtils.rpeToRir(8.5), 0.001)
        assertEquals(0.0, ExertionUtils.rpeToRir(11.0), 0.001)
    }

    @Test
    fun testRirToRpe() {
        assertEquals(10.0, ExertionUtils.rirToRpe(0.0), 0.001)
        assertEquals(9.0, ExertionUtils.rirToRpe(1.0), 0.001)
        assertEquals(8.0, ExertionUtils.rirToRpe(2.0), 0.001)
        assertEquals(8.5, ExertionUtils.rirToRpe(1.5), 0.001)
    }

    @Test
    fun testCalculateExertionValues() {
        val (rpe1, rir1) = ExertionUtils.calculateExertionValues(8.5, ExertionUtils.MODE_RPE)
        assertEquals(8.5, rpe1!!, 0.001)
        assertEquals(1.5, rir1!!, 0.001)

        val (rpe2, rir2) = ExertionUtils.calculateExertionValues(2.0, ExertionUtils.MODE_RIR)
        assertEquals(8.0, rpe2!!, 0.001)
        assertEquals(2.0, rir2!!, 0.001)

        val (rpeNull, rirNull) = ExertionUtils.calculateExertionValues(null, ExertionUtils.MODE_RPE)
        assertNull(rpeNull)
        assertNull(rirNull)
    }

    @Test
    fun testGetDisplayedExertion() {
        assertEquals(8.5, ExertionUtils.getDisplayedExertion(8.5, 1.5, ExertionUtils.MODE_RPE)!!, 0.001)
        assertEquals(1.5, ExertionUtils.getDisplayedExertion(8.5, 1.5, ExertionUtils.MODE_RIR)!!, 0.001)
        assertEquals(8.0, ExertionUtils.getDisplayedExertion(null, 2.0, ExertionUtils.MODE_RPE)!!, 0.001)
        assertEquals(2.0, ExertionUtils.getDisplayedExertion(8.0, null, ExertionUtils.MODE_RIR)!!, 0.001)
    }

    @Test
    fun testFormatExertionLabel() {
        assertEquals("@ RPE 8.5", ExertionUtils.formatExertionLabel(8.5, 1.5, ExertionUtils.MODE_RPE))
        assertEquals("@ RPE 8", ExertionUtils.formatExertionLabel(8.0, 2.0, ExertionUtils.MODE_RPE))
        assertEquals("@ 1.5 RIR", ExertionUtils.formatExertionLabel(8.5, 1.5, ExertionUtils.MODE_RIR))
        assertEquals("@ 2 RIR", ExertionUtils.formatExertionLabel(8.0, 2.0, ExertionUtils.MODE_RIR))
        assertEquals("", ExertionUtils.formatExertionLabel(null, null, ExertionUtils.MODE_RPE))
    }
}
