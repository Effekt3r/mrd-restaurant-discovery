package com.effekt3r.mrd.tools.datagen

import java.io.File
import kotlin.random.Random
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Generates the menus: one per distinct restaurant id in the 220-restaurant file, from that id's first record, written
 * to mock-api/v1/menus/{id}.json. Stress-only restaurants have none, so the API answers 404 for them (spec §5.9).
 * Not to be confused with [Concept.menus], the cuisine lists a restaurant draws from.
 */
internal object MenuGenerator {

    const val DIRECTORY = "mock-api/v1/menus"

    /**
     * Menu i draws from its own Random, its restaurant's seed mixed with this constant ("menumenu" in ASCII). It
     * depends only on the seed, its restaurant's index and concept, and the restaurants' draws don't change.
     */
    private const val STREAM = 0x6D65_6E75_6D65_6E75L

    // Spec §5.9's rates, in percent, each drawn per item.
    private const val SOLD_OUT_PERCENT = 8
    private const val NO_DESCRIPTION_PERCENT = 25
    private const val OPTIONS_PERCENT = 30

    // Drinks have no photos, so dishes have one 65% of the time: about half of all items.
    private const val PHOTO_PERCENT = 65

    // Some options are free; the rest cost R5 to R35.
    private const val FREE_OPTION_PERCENT = 20
    private val OPTION_PRICES = (500..3_500 step 500).toList()

    /** Every menu by restaurant id, in restaurant order. */
    fun menus(pool: ImagePool): Map<String, JsonObject> {
        val menus = linkedMapOf<String, JsonObject>()
        Generator.generate(Dataset.STANDARD.size, pool).forEachIndexed { index, restaurant ->
            val id = restaurant.record["id"]?.jsonPrimitive?.content ?: return@forEachIndexed
            if (id !in menus) {
                menus[id] = MenuEdgeCases.apply(index, menu(index, id, restaurant.concept, restaurant.photoKey, pool))
            }
        }
        return menus
    }

    /** Every menu file's text by file name, `{id}.json`. */
    fun files(pool: ImagePool): Map<String, String> =
        menus(pool).entries.associate { (id, menu) -> "$id.json" to JsonWriter.pretty(menu) }

    /**
     * Writes [files] into [directory] and deletes every other `.json` file there: a menu whose restaurant has gone.
     * Returns the names it deleted.
     */
    fun write(directory: File, files: Map<String, String>): List<String> {
        directory.mkdirs()
        files.forEach { (name, text) -> File(directory, name).writeText(text) }
        val stale = directory.listFiles().orEmpty().filter { it.name.endsWith(".json") && it.name !in files }
        stale.forEach { check(it.delete()) { "Couldn't delete $it" } }
        return stale.map { it.name }.sorted()
    }

    /**
     * One restaurant's menu: its concept's courses in order, each listing 4 to 8 of its dishes in menu order. Ids
     * carry the restaurant's number: category `c-1001-1`, item `i-1001-101` (category 1, item 1) and option
     * `o-1001-101-1`. A restaurant prices either in round fives (R85) or just under (R89).
     */
    fun menu(index: Int, restaurantId: String, concept: Concept, photoKey: String, pool: ImagePool): JsonObject {
        val random = Random((Generator.SEED * 31 + index) xor STREAM)
        val number = restaurantId.removePrefix("r-")
        val endsInNine = random.nextBoolean()

        fun item(course: Course, dish: Dish, code: String) = buildJsonObject {
            put("id", "i-$code")
            put("name", dish.name)
            put("price_cents", price(course.rands, endsInNine, random))
            put("available", random.nextInt(100) >= SOLD_OUT_PERCENT)
            if (random.nextInt(100) >= NO_DESCRIPTION_PERCENT) put("description", dish.description)
            val photos = when (course.photos) {
                Photos.RESTAURANT -> photoKey
                Photos.DESSERT -> "dessert"
                Photos.NONE -> null
            }
            if (photos != null && random.nextInt(100) < PHOTO_PERCENT) put("image_url", pool.pick(photos, random))
            if (random.nextInt(100) < OPTIONS_PERCENT) {
                val names = course.options.shuffled(random).take((1..3).random(random))
                putJsonArray("options") {
                    names.forEachIndexed { option, name ->
                        addJsonObject {
                            put("id", "o-$code-${option + 1}")
                            put("name", name)
                            put("price_cents", optionPrice(random))
                        }
                    }
                }
            }
        }

        return buildJsonObject {
            put("restaurant_id", restaurantId)
            putJsonArray("categories") {
                Courses.of(concept).forEachIndexed { position, course ->
                    val category = position + 1
                    addJsonObject {
                        put("id", "c-$number-$category")
                        put("name", course.name)
                        putJsonArray("items") {
                            val count = (4..minOf(8, course.dishes.size)).random(random)
                            val dishes = course.dishes.indices.shuffled(random).take(count).sorted()
                            dishes.forEachIndexed { item, dish ->
                                add(item(course, course.dishes[dish], "$number-${category * 100 + item + 1}"))
                            }
                        }
                    }
                }
            }
        }
    }

    /** A price in [rands], in cents: a multiple of R5, or a rand amount ending in 9 when [endsInNine]. */
    fun price(rands: IntRange, endsInNine: Boolean, random: Random): Int {
        val prices = if (endsInNine) rands.filter { it % 10 == 9 } else rands.filter { it % 5 == 0 }
        return prices.random(random) * 100
    }

    private fun optionPrice(random: Random) =
        if (random.nextInt(100) < FREE_OPTION_PERCENT) 0 else OPTION_PRICES.random(random)
}
