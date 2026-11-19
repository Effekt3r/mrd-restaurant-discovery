package com.effekt3r.mrd.tools.datagen

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConceptsTest {

    @Test
    fun weightsAddUpToOneHundredAndKeysAreUnique() {
        assertEquals(100, Concepts.all.sumOf { it.weight })
        assertEquals(Concepts.all.size, Concepts.all.map { it.key }.distinct().size)
    }

    @Test
    fun conceptsArePickedInProportionToTheirWeight() {
        val random = Random(0)
        val counts = List(100_000) { Concepts.pick(random).key }.groupingBy { it }.eachCount()
        Concepts.all.forEach { concept ->
            val percent = counts.getValue(concept.key) / 1_000.0
            assertEquals(concept.key, concept.weight.toDouble(), percent, 0.5)
        }
    }

    @Test
    fun everyMenuListsOneToThreeDistinctCuisines() {
        Concepts.all.flatMap { it.menus }.forEach { menu ->
            assertTrue("$menu", menu.cuisines.size in 1..3)
            assertEquals("$menu", menu.cuisines.distinct(), menu.cuisines)
            assertTrue("$menu", menu.cuisines.none { it.isBlank() })
        }
    }

    @Test
    fun someCuisinesCarryAccentsForAccentInsensitiveSearch() {
        val cuisines = Concepts.all.flatMap { it.menus }.flatMap { it.cuisines }
        assertTrue(cuisines.containsAll(listOf("Café", "Crêpes")))
    }

    @Test
    fun feesComeFromTheLadderAndEveryStepIsUsed() {
        assertEquals(FEE_LADDER.toSet(), Concepts.all.flatMap { it.feesCents }.toSet())
    }

    @Test
    fun deliveryTimesStayBetweenFifteenAndFiftyFiveMinutes() {
        Concepts.all.forEach { assertTrue(it.key, it.etaMinutes.first >= 15 && it.etaMinutes.last <= 55) }
    }

    @Test
    fun everyImageKeyIsInThePoolAndEveryPoolKeyIsUsed() {
        val used = Concepts.all.flatMap { it.menus }.map { it.imageKey }.toSet()
        assertEquals(ImagePool.load().photos.keys, used)
    }
}
