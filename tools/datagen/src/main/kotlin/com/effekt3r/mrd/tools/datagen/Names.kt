package com.effekt3r.mrd.tools.datagen

import kotlin.random.Random

/** Cape Town suburbs, landmarks and local words. A generated name is "{place} {noun}"; none is a real business. */
internal val PLACES = listOf(
    "Bakoven", "Bloubergstrand", "Bo-Kaap", "Bree Street", "Camps Bay", "Cape Point", "Chapman's Peak", "Clifton",
    "Constantia", "Dassie", "De Waterkant", "Devil's Peak", "False Bay", "Fish Hoek", "Fynbos", "Gardens",
    "Green Point", "Hout Bay", "Kalk Bay", "Kirstenbosch", "Kloof Street", "Kommetjie", "Lion's Head", "Llandudno",
    "Long Street", "Mouille Point", "Muizenberg", "Newlands", "Noordhoek", "Observatory", "Oranjezicht", "Protea",
    "Rondebosch", "Salt River", "Scarborough", "Sea Point", "Signal Hill", "Simon's Town", "Southeaster", "Tafelberg",
    "Tamboerskloof", "Two Oceans", "Woodstock", "Wynberg"
)

/**
 * Hands out names in restaurant order. The first [uniqueCount] are unique: a name already taken is drawn again, so
 * each depends only on its restaurant's Random and the names before it. Later names may repeat, as chains' do.
 */
internal class NameBook(private val uniqueCount: Int) {

    private val taken = mutableSetOf<String>()

    fun next(index: Int, concept: Concept, random: Random): String {
        repeat(MAX_ATTEMPTS) {
            val name = "${PLACES.random(random)} ${concept.nouns.random(random)}"
            if (index >= uniqueCount || taken.add(name)) return name
        }
        error("No unique name for restaurant $index after $MAX_ATTEMPTS attempts. Add places or nouns.")
    }

    private companion object {
        const val MAX_ATTEMPTS = 100
    }
}
