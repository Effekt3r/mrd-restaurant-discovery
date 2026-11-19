package com.effekt3r.mrd.tools.datagen

/**
 * Real South African chains and well-known Cape Town restaurants. No generated name may contain one, ignoring case,
 * so the fictional data can't be mistaken for a real business.
 */
internal val REAL_NAMES = listOf(
    // South African chains
    "Nando", "Ocean Basket", "Steers", "Spur", "Debonairs", "Wimpy", "KFC", "McDonald", "Burger King", "Roman's",
    "Col'Cacchio", "Panarottis", "Mugg & Bean", "Kauai", "Galito", "RocoMamas", "Simply Asia", "John Dory",
    "Fishaways", "Chicken Licken", "Pizza Hut", "Domino", "Starbucks", "Vida e", "Seattle Coffee", "Wakaberry",
    "Mythos", "Barcelos", "Hungry Lion", "Mr D",
    // Well-known Cape Town restaurants
    "Kloof Street House", "Bo-Kaap Kombuis", "Mama Africa", "Test Kitchen", "La Colombe", "Harbour House", "Kalky",
    "Codfather", "Massimo", "Truth Coffee", "Jason Bakery", "Clarke's", "Biesmiellah", "Marco's African Place",
    "Cape Town Fish Market"
)

/** The real name this name contains, if any. */
internal fun String.realNameInside(): String? = REAL_NAMES.firstOrNull { contains(it, ignoreCase = true) }
