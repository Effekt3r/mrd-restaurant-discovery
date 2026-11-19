package com.effekt3r.mrd.tools.datagen

import java.net.URLEncoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Builds image-pool.json from TheMealDB's free API. The only part of the generator that uses the network. */
internal object ImagePoolFetcher {

    /**
     * The queries behind each image key: `c=` a category, `a=` an area, `s=` a search by meal name. Some areas need
     * the country's name (India, United States): list.php?a=list returns demonyms, but meals filed under those
     * countries carry the country's name, and the demonym finds nothing. TheMealDB has no South African dishes, so
     * there's no key for them.
     */
    val QUERIES: Map<String, List<String>> = mapOf(
        "american" to listOf("a=United States"),
        "beef" to listOf("c=Beef"),
        "breakfast" to listOf("c=Breakfast"),
        "chicken" to listOf("c=Chicken"),
        "chinese" to listOf("a=Chinese"),
        "dessert" to listOf("c=Dessert"),
        "greek" to listOf("a=Greek"),
        "indian" to listOf("a=India"),
        "italian" to listOf("a=Italian"),
        "japanese" to listOf("a=Japanese"),
        "lamb" to listOf("c=Lamb"),
        "mexican" to listOf("a=Mexican"),
        "middle-eastern" to listOf("a=Turkish", "a=Moroccan", "a=Egyptian"),
        "pasta" to listOf("c=Pasta"),
        "pizza" to listOf("s=pizza", "s=flatbread", "s=lahmacun"),
        "portuguese" to listOf("a=Portuguese"),
        "seafood" to listOf("c=Seafood"),
        "thai" to listOf("a=Thai"),
        "vegan" to listOf("c=Vegan", "c=Vegetarian")
    )

    const val MIN_PHOTOS = 5

    /** A photo URL as the API returns it: HTTPS, with no size suffix such as /medium (the app adds that). */
    val PHOTO_URL = Regex("""^https://www\.themealdb\.com/images/media/meals/[^/]+\.jpg$""")

    // The free API's public test key, "1".
    private const val API = "https://www.themealdb.com/api/json/v1/1"

    /**
     * Runs every query through [get] (a URL in, the response body out) and returns image-pool.json's text: keys
     * sorted, each key's photos sorted and distinct. URLs that aren't canonical are skipped, and any key left with
     * fewer than [MIN_PHOTOS] photos fails the fetch, naming every such key.
     */
    fun fetch(get: (String) -> String): String {
        val pool = QUERIES.toSortedMap().mapValues { (_, queries) ->
            val (canonical, skipped) = queries.flatMap { photos(get(url(it))) }.distinct().partition(PHOTO_URL::matches)
            skipped.forEach { println("Skipped a photo URL that isn't canonical: $it") }
            canonical.sorted()
        }
        val short = pool.filterValues { it.size < MIN_PHOTOS }
        check(short.isEmpty()) {
            "Every image key needs at least $MIN_PHOTOS photos. Add a query for " +
                short.entries.joinToString { (key, photos) -> "'$key' (${photos.size})" }
        }
        return JsonWriter.pretty(
            buildJsonObject { pool.forEach { (key, photos) -> put(key, JsonArray(photos.map(::JsonPrimitive))) } }
        )
    }

    /** The request URL for a query such as "a=United States". */
    fun url(query: String): String {
        val (parameter, value) = query.split('=', limit = 2)
        val endpoint = when (parameter) {
            "a", "c" -> "filter.php"
            "s" -> "search.php"
            else -> error("Unknown TheMealDB query '$query'")
        }
        return "$API/$endpoint?$parameter=" + URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")
    }

    /** The photo URLs in a filter.php or search.php response. `{"meals":null}` means no results. */
    fun photos(response: String): List<String> {
        val meals = Json.parseToJsonElement(response).jsonObject["meals"]
        if (meals == null || meals is JsonNull) return emptyList()
        return meals.jsonArray.map { it.jsonObject.getValue("strMealThumb").jsonPrimitive.content }
    }
}
