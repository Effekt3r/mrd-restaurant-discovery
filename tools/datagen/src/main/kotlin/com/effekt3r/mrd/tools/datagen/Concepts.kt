package com.effekt3r.mrd.tools.datagen

import com.effekt3r.mrd.tools.datagen.HoursStyle.ALL_DAY
import com.effekt3r.mrd.tools.datagen.HoursStyle.BREAKFAST
import com.effekt3r.mrd.tools.datagen.HoursStyle.LATE_NIGHT
import com.effekt3r.mrd.tools.datagen.HoursStyle.SPLIT
import com.effekt3r.mrd.tools.datagen.HoursStyle.STANDARD
import kotlin.random.Random

/** The cuisines a restaurant lists (1 to 3, written to `cuisines`), and the image key whose photos suit them. */
internal data class Menu(val cuisines: List<String>, val imageKey: String)

/**
 * A restaurant archetype: how common it is, what it's called and serves, and its usual hours, fees and delivery
 * time. A restaurant draws one entry from each list; listing an entry twice makes it twice as likely.
 */
internal data class Concept(
    val key: String,
    val weight: Int,
    val nouns: List<String>,
    val menus: List<Menu>,
    val hours: List<HoursStyle>,
    val feesCents: List<Int>,
    val etaMinutes: IntRange
)

internal object Concepts {

    /** Delivery fees in cents, R9 to R35. Free delivery (0) is drawn separately. */
    val FEE_LADDER = listOf(900, 1200, 1500, 1900, 2500, 2900, 3500)

    /** Weights are percentages of all restaurants. TheMealDB has no South African dishes, so Cape Malay borrows. */
    val all: List<Concept> = listOf(
        Concept(
            key = "pizza",
            weight = 12,
            nouns = listOf("Pizza Co.", "Pizzeria", "Pizza Bar", "Woodfire Pizza", "Pizza Kitchen"),
            menus = listOf(menu("pizza", "Pizza"), menu("pizza", "Pizza", "Italian")),
            hours = listOf(STANDARD, STANDARD, STANDARD, LATE_NIGHT, LATE_NIGHT),
            feesCents = listOf(900, 1200, 1500, 1900),
            etaMinutes = 20..40
        ),
        Concept(
            key = "burgers",
            weight = 11,
            nouns = listOf("Burger Bar", "Burger Co.", "Smash Burgers", "Burger Joint", "Patty Shack"),
            menus = listOf(
                menu("american", "Burgers"),
                menu("american", "Burgers", "American"),
                menu("beef", "Burgers", "Grill")
            ),
            hours = listOf(STANDARD, STANDARD, LATE_NIGHT, LATE_NIGHT, ALL_DAY),
            feesCents = listOf(900, 1200, 1500, 1900),
            etaMinutes = 15..35
        ),
        Concept(
            key = "chicken",
            weight = 10,
            nouns = listOf("Peri-Peri", "Chicken Shack", "Rotisserie", "Chicken Co.", "Flame Chicken"),
            menus = listOf(
                menu("chicken", "Chicken", "Peri-Peri"),
                menu("chicken", "Chicken"),
                menu("portuguese", "Peri-Peri", "Portuguese")
            ),
            hours = listOf(STANDARD, STANDARD, LATE_NIGHT, LATE_NIGHT, ALL_DAY),
            feesCents = listOf(900, 1200, 1500),
            etaMinutes = 15..30
        ),
        Concept(
            key = "sushi",
            weight = 7,
            nouns = listOf("Sushi Bar", "Sushi Co.", "Ramen Bar", "Izakaya", "Sushi & Grill"),
            menus = listOf(
                menu("japanese", "Sushi", "Japanese"),
                menu("japanese", "Japanese"),
                menu("seafood", "Sushi", "Seafood")
            ),
            hours = listOf(STANDARD, STANDARD, STANDARD, SPLIT),
            feesCents = listOf(1500, 1900, 2500, 2900),
            etaMinutes = 25..50
        ),
        Concept(
            key = "indian",
            weight = 7,
            nouns = listOf("Curry Kitchen", "Tandoor", "Masala", "Curry Co.", "Biryani Bar"),
            menus = listOf(
                menu("indian", "Indian", "Curry"),
                menu("indian", "Indian"),
                menu("indian", "Curry"),
                menu("indian", "Indian", "Curry", "Vegetarian")
            ),
            hours = listOf(SPLIT, SPLIT, SPLIT, STANDARD, STANDARD),
            feesCents = listOf(1200, 1500, 1900, 2500),
            etaMinutes = 30..55
        ),
        Concept(
            key = "chinese",
            weight = 6,
            nouns = listOf("Wok", "Dim Sum", "Noodle Bar", "Dumpling Bar", "Wok & Noodle"),
            menus = listOf(
                menu("chinese", "Chinese"),
                menu("chinese", "Chinese", "Asian"),
                menu("chinese", "Chinese", "Dim Sum", "Noodles")
            ),
            hours = listOf(STANDARD, STANDARD, LATE_NIGHT),
            feesCents = listOf(1200, 1500, 1900),
            etaMinutes = 25..45
        ),
        Concept(
            key = "italian",
            weight = 6,
            nouns = listOf("Trattoria", "Osteria", "Pasta Bar", "Cucina", "Pastificio"),
            menus = listOf(
                menu("italian", "Italian"),
                menu("pasta", "Italian", "Pasta"),
                menu("italian", "Italian", "Pizza", "Pasta")
            ),
            hours = listOf(SPLIT, SPLIT, SPLIT, STANDARD, STANDARD),
            feesCents = listOf(1500, 1900, 2500, 2900),
            etaMinutes = 25..45
        ),
        Concept(
            key = "steakhouse",
            weight = 6,
            nouns = listOf("Grill", "Steakhouse", "Braai", "Chophouse", "Grill Room"),
            menus = listOf(
                menu("beef", "Steakhouse", "Grill"),
                menu("beef", "Grill"),
                menu("beef", "Steakhouse"),
                menu("lamb", "Grill", "South African")
            ),
            hours = listOf(SPLIT, STANDARD, LATE_NIGHT),
            feesCents = listOf(1900, 2500, 2900, 3500),
            etaMinutes = 30..55
        ),
        Concept(
            key = "seafood",
            weight = 5,
            nouns = listOf("Fish & Chips", "Seafood", "Fish Shack", "Oyster Bar", "Catch"),
            menus = listOf(
                menu("seafood", "Seafood"),
                menu("seafood", "Fish & Chips"),
                menu("seafood", "Seafood", "Fish & Chips"),
                menu("seafood", "Seafood", "Sushi")
            ),
            hours = listOf(STANDARD),
            feesCents = listOf(1500, 1900, 2500, 2900),
            etaMinutes = 25..45
        ),
        Concept(
            key = "cafe",
            weight = 7,
            nouns = listOf("Café", "Coffee Bar", "Breakfast Club", "Roastery", "Kitchen"),
            menus = listOf(
                menu("breakfast", "Café", "Breakfast"),
                menu("breakfast", "Café"),
                menu("breakfast", "Breakfast", "Coffee"),
                menu("dessert", "Café", "Bakery", "Breakfast")
            ),
            hours = listOf(BREAKFAST, BREAKFAST, BREAKFAST, BREAKFAST, ALL_DAY),
            feesCents = listOf(900, 1200, 1500),
            etaMinutes = 15..30
        ),
        Concept(
            key = "healthy",
            weight = 5,
            nouns = listOf("Greens", "Salad Bar", "Juicery", "Plant Kitchen", "Bowls"),
            menus = listOf(
                menu("vegan", "Healthy", "Vegan"),
                menu("vegan", "Vegan"),
                menu("vegan", "Healthy", "Salads"),
                menu("vegan", "Vegetarian", "Healthy")
            ),
            hours = listOf(STANDARD, BREAKFAST),
            feesCents = listOf(1200, 1500, 1900),
            etaMinutes = 15..35
        ),
        Concept(
            key = "mexican",
            weight = 4,
            nouns = listOf("Taqueria", "Cantina", "Tacos", "Burrito Bar"),
            menus = listOf(
                menu("mexican", "Mexican"),
                menu("mexican", "Mexican", "Tacos"),
                menu("mexican", "Mexican", "Burritos", "Tacos")
            ),
            hours = listOf(STANDARD, LATE_NIGHT),
            feesCents = listOf(1200, 1500, 1900),
            etaMinutes = 20..40
        ),
        Concept(
            key = "thai",
            weight = 4,
            nouns = listOf("Thai Kitchen", "Thai", "Thai Garden", "Pad Thai Bar"),
            menus = listOf(menu("thai", "Thai"), menu("thai", "Thai", "Asian"), menu("thai", "Thai", "Noodles")),
            hours = listOf(STANDARD, STANDARD, SPLIT),
            feesCents = listOf(1500, 1900, 2500),
            etaMinutes = 25..45
        ),
        Concept(
            key = "middle-eastern",
            weight = 4,
            nouns = listOf("Mezze", "Taverna", "Shawarma", "Souvlaki", "Kebab Bar"),
            menus = listOf(
                menu("greek", "Greek"),
                menu("greek", "Greek", "Mediterranean"),
                menu("middle-eastern", "Middle Eastern"),
                menu("middle-eastern", "Middle Eastern", "Shawarma"),
                menu("middle-eastern", "Lebanese", "Middle Eastern", "Mediterranean")
            ),
            hours = listOf(STANDARD, LATE_NIGHT),
            feesCents = listOf(1200, 1500, 1900),
            etaMinutes = 20..40
        ),
        Concept(
            key = "cape-malay",
            weight = 3,
            nouns = listOf("Kitchen", "Gatsby Co.", "Roti Bar", "Bredie Pot", "Eatery"),
            menus = listOf(
                menu("indian", "Cape Malay"),
                menu("indian", "Cape Malay", "Curry"),
                menu("lamb", "South African"),
                menu("lamb", "South African", "Grill")
            ),
            hours = listOf(STANDARD),
            feesCents = listOf(1200, 1500, 1900),
            etaMinutes = 25..50
        ),
        Concept(
            key = "desserts",
            weight = 3,
            nouns = listOf("Bakery", "Pâtisserie", "Crêperie", "Doughnuts", "Gelato", "Waffle Bar"),
            menus = listOf(
                menu("dessert", "Desserts"),
                menu("dessert", "Bakery", "Café"),
                menu("dessert", "Desserts", "Ice Cream"),
                menu("dessert", "Bakery"),
                menu("dessert", "Crêpes", "Desserts")
            ),
            hours = listOf(BREAKFAST, STANDARD),
            feesCents = listOf(900, 1200, 1500),
            etaMinutes = 15..30
        )
    )

    // Each concept appears once per point of weight, so a uniform draw from the wheel follows the weights.
    private val wheel = all.flatMap { concept -> List(concept.weight) { concept } }

    fun pick(random: Random): Concept = wheel.random(random)

    fun named(key: String): Concept = all.single { it.key == key }
}

private fun menu(imageKey: String, vararg cuisines: String) = Menu(cuisines.toList(), imageKey)
