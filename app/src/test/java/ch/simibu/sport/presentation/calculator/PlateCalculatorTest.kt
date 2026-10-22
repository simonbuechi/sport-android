package ch.simibu.sport.presentation.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateCalculatorTest {

    @Test
    fun testEmptyBarWhenTargetEqualsBarWeight() {
        val result = PlateCalculatorUtil.calculate(
            targetWeight = 20.0,
            barWeight = 20.0,
            availablePlateWeights = DEFAULT_ACTIVE_PLATE_WEIGHTS
        )

        assertEquals(20.0, result.targetWeight, 0.001)
        assertEquals(20.0, result.achievedWeight, 0.001)
        assertEquals(0.0, result.weightPerSide, 0.001)
        assertTrue(result.loadedPlatesPerSide.isEmpty())
        assertTrue(result.isExact)
    }

    @Test
    fun testEmptyBarWhenTargetLessThanBarWeight() {
        val result = PlateCalculatorUtil.calculate(
            targetWeight = 15.0,
            barWeight = 20.0,
            availablePlateWeights = DEFAULT_ACTIVE_PLATE_WEIGHTS
        )

        assertEquals(20.0, result.achievedWeight, 0.001)
        assertEquals(0.0, result.weightPerSide, 0.001)
        assertTrue(result.loadedPlatesPerSide.isEmpty())
    }

    @Test
    fun testStandardBenchPress60kg() {
        // (60 - 20) / 2 = 20kg per side (one 20kg plate on each side)
        val result = PlateCalculatorUtil.calculate(
            targetWeight = 60.0,
            barWeight = 20.0,
            availablePlateWeights = DEFAULT_ACTIVE_PLATE_WEIGHTS
        )

        assertEquals(60.0, result.achievedWeight, 0.001)
        assertEquals(20.0, result.weightPerSide, 0.001)
        assertEquals(1, result.loadedPlatesPerSide.size)
        assertEquals(20.0, result.loadedPlatesPerSide.first().weight, 0.001)
        assertTrue(result.isExact)
    }

    @Test
    fun testStandard100kgLoading() {
        // (100 - 20) / 2 = 40kg per side -> 25kg + 15kg or 25 + 15 = 40
        val result = PlateCalculatorUtil.calculate(
            targetWeight = 100.0,
            barWeight = 20.0,
            availablePlateWeights = DEFAULT_ACTIVE_PLATE_WEIGHTS
        )

        assertEquals(100.0, result.achievedWeight, 0.001)
        assertEquals(40.0, result.weightPerSide, 0.001)
        assertEquals(listOf(25.0, 15.0), result.loadedPlatesPerSide.map { it.weight })
        assertTrue(result.isExact)
    }

    @Test
    fun testFractionalWeights82Point5kg() {
        // (82.5 - 20) / 2 = 31.25kg per side -> 25kg + 5kg + 1.25kg = 31.25kg
        val result = PlateCalculatorUtil.calculate(
            targetWeight = 82.5,
            barWeight = 20.0,
            availablePlateWeights = DEFAULT_ACTIVE_PLATE_WEIGHTS
        )

        assertEquals(82.5, result.achievedWeight, 0.001)
        assertEquals(31.25, result.weightPerSide, 0.001)
        assertEquals(listOf(25.0, 5.0, 1.25), result.loadedPlatesPerSide.map { it.weight })
        assertTrue(result.isExact)
        assertEquals(0.0, result.remainder, 0.001)
    }

    @Test
    fun testExcludedPlatesInventory() {
        // Gym without 25kg and 15kg plates: 100kg target
        // (100 - 20) / 2 = 40kg -> two 20kg plates per side
        val platesWithout25and15 = setOf(20.0, 10.0, 5.0, 2.5, 1.25)
        val result = PlateCalculatorUtil.calculate(
            targetWeight = 100.0,
            barWeight = 20.0,
            availablePlateWeights = platesWithout25and15
        )

        assertEquals(100.0, result.achievedWeight, 0.001)
        assertEquals(40.0, result.weightPerSide, 0.001)
        assertEquals(listOf(20.0, 20.0), result.loadedPlatesPerSide.map { it.weight })
        assertTrue(result.isExact)
    }

    @Test
    fun testRemainderWhenUnachievable() {
        // Target 83.0kg with plates min step 1.25kg (each side = 31.5kg, but 25+5+1.25 = 31.25, leaves 0.25 per side = 0.5 total)
        val result = PlateCalculatorUtil.calculate(
            targetWeight = 83.0,
            barWeight = 20.0,
            availablePlateWeights = setOf(25.0, 20.0, 10.0, 5.0, 2.5, 1.25)
        )

        assertEquals(82.5, result.achievedWeight, 0.001)
        assertEquals(0.5, result.remainder, 0.001)
        assertFalse(result.isExact)
    }

    @Test
    fun testWomensBar15kg() {
        // 50kg on 15kg bar: (50 - 15) / 2 = 17.5kg -> 15kg + 2.5kg
        val result = PlateCalculatorUtil.calculate(
            targetWeight = 50.0,
            barWeight = 15.0,
            availablePlateWeights = DEFAULT_ACTIVE_PLATE_WEIGHTS
        )

        assertEquals(50.0, result.achievedWeight, 0.001)
        assertEquals(17.5, result.weightPerSide, 0.001)
        assertEquals(listOf(15.0, 2.5), result.loadedPlatesPerSide.map { it.weight })
        assertTrue(result.isExact)
    }
}
