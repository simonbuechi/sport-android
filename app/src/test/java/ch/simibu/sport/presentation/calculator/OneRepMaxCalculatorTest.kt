package ch.simibu.sport.presentation.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OneRepMaxCalculatorTest {

    @Test
    fun testOneRep_returnsExactWeightForAllFormulas() {
        for (formula in OneRmFormula.entries) {
            val result = OneRepMaxCalculator.calculate1RM(100.0, 1, formula)
            assertEquals("Formula $formula should return exact weight on 1 rep", 100.0, result, 0.001)
        }
    }

    @Test
    fun testEpleyFormula() {
        // 100kg * (1 + 10 / 30) = 133.33 -> 133.3
        val result = OneRepMaxCalculator.calculate1RM(100.0, 10, OneRmFormula.EPLEY)
        assertEquals(133.3, result, 0.1)
    }

    @Test
    fun testBrzyckiFormula() {
        // 100kg * 36 / (37 - 10) = 100 * 36 / 27 = 133.33 -> 133.3
        val result = OneRepMaxCalculator.calculate1RM(100.0, 10, OneRmFormula.BRZYCKI)
        assertEquals(133.3, result, 0.1)
    }

    @Test
    fun testLombardiFormula() {
        // 100kg * 10^0.10 = 100 * 1.2589 = 125.9
        val result = OneRepMaxCalculator.calculate1RM(100.0, 10, OneRmFormula.LOMBARDI)
        assertEquals(125.9, result, 0.1)
    }

    @Test
    fun testWathanFormula() {
        // 100 * 100 / (48.8 + 53.8 * exp(-0.075 * 10)) = 10000 / (48.8 + 25.41) = 134.7
        val result = OneRepMaxCalculator.calculate1RM(100.0, 10, OneRmFormula.WATHAN)
        assertEquals(134.7, result, 0.5)
    }

    @Test
    fun testAverageFormula() {
        val epley = OneRepMaxCalculator.calculate1RM(100.0, 5, OneRmFormula.EPLEY)
        val brzycki = OneRepMaxCalculator.calculate1RM(100.0, 5, OneRmFormula.BRZYCKI)
        val lombardi = OneRepMaxCalculator.calculate1RM(100.0, 5, OneRmFormula.LOMBARDI)
        val wathan = OneRepMaxCalculator.calculate1RM(100.0, 5, OneRmFormula.WATHAN)
        val expectedAvg = Math.round(((epley + brzycki + lombardi + wathan) / 4.0) * 10.0) / 10.0

        val avgResult = OneRepMaxCalculator.calculate1RM(100.0, 5, OneRmFormula.AVERAGE)
        assertEquals(expectedAvg, avgResult, 0.2)
    }

    @Test
    fun testRoundToNearest() {
        assertEquals(82.5, OneRepMaxCalculator.roundToNearest(81.9, 2.5), 0.001)
        assertEquals(82.5, OneRepMaxCalculator.roundToNearest(83.1, 2.5), 0.001)
        assertEquals(85.0, OneRepMaxCalculator.roundToNearest(84.0, 2.5), 0.001)
        assertEquals(80.0, OneRepMaxCalculator.roundToNearest(80.4, 2.5), 0.001)
    }

    @Test
    fun testComputeFullBreakdown() {
        val breakdown = OneRepMaxCalculator.computeFullBreakdown(
            weight = 100.0,
            reps = 1,
            formula = OneRmFormula.EPLEY,
            roundStep = 2.5
        )

        assertEquals(100.0, breakdown.calculated1RM, 0.001)
        assertEquals(100.0, breakdown.rounded1RM, 0.001)
        assertEquals(11, breakdown.percentageRows.size)

        // 100% tier
        val top = breakdown.percentageRows.first()
        assertEquals(100, top.percentage)
        assertEquals(100.0, top.exactWeight, 0.001)
        assertEquals(100.0, top.roundedWeight, 0.001)
        assertEquals(TrainingZone.MAX_EFFORT, top.zone)

        // 50% tier
        val bottom = breakdown.percentageRows.last()
        assertEquals(50, bottom.percentage)
        assertEquals(50.0, bottom.exactWeight, 0.001)
        assertEquals(50.0, bottom.roundedWeight, 0.001)
        assertEquals(TrainingZone.WARMUP, bottom.zone)
    }
}
