package com.effekt3r.mrd.tools.datagen

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NamesTest {

    /** Names drawn the way the generator draws them: each restaurant's own Random picks a concept, then a name. */
    private fun names(count: Int): List<String> {
        val book = NameBook(uniqueCount = 220)
        return List(count) { index ->
            val random = Random(index.toLong())
            book.next(index, Concepts.pick(random), random)
        }
    }

    @Test
    fun namesAreDeterministic() {
        assertEquals(names(1_000), names(1_000))
    }

    @Test
    fun theFirst220AreUniqueAndLaterNamesRepeatAsChainsDo() {
        val names = names(10_000)
        assertEquals(220, names.take(220).toSet().size)
        assertTrue(names.size > names.toSet().size)
    }

    @Test
    fun noPossibleNameContainsARealBusinessName() {
        val possible = Concepts.all.flatMap { it.nouns }.flatMap { noun -> PLACES.map { place -> "$place $noun" } }
        val clashes = possible.mapNotNull { name -> name.realNameInside()?.let { "$name contains $it" } }
        assertEquals(emptyList<String>(), clashes)
    }

    @Test
    fun someNamesCarryAccents() {
        val accented = names(220).filter { name -> name.any { it.code > 127 } }
        assertTrue("$accented", accented.size >= 3)
    }
}
