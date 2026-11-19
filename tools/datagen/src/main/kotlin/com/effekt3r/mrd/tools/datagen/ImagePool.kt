package com.effekt3r.mrd.tools.datagen

import kotlin.random.Random
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * TheMealDB photo URLs by image key, from the committed image-pool.json. Generating reads this file rather than the
 * network, so it's repeatable and works offline. Being a resource on the classpath, it's also a Gradle input of both
 * the run task and the tests.
 */
internal class ImagePool(val photos: Map<String, List<String>>) {

    fun pick(key: String, random: Random): String = photosFor(key).random(random)

    fun first(key: String): String = photosFor(key).first()

    private fun photosFor(key: String) = photos[key] ?: error("$RESOURCE has no image key '$key'")

    companion object {
        const val RESOURCE = "image-pool.json"

        /** Where fetch-images writes the pool, relative to the repository root. */
        const val SOURCE_PATH = "tools/datagen/src/main/resources/$RESOURCE"

        fun load(): ImagePool = parse(resourceText())

        fun resourceText(): String = ImagePool::class.java.getResource("/$RESOURCE")?.readText()
            ?: error("$RESOURCE is missing. Run ./gradlew :tools:datagen:run --args=fetch-images")

        fun parse(json: String) = ImagePool(
            Json.parseToJsonElement(json).jsonObject.mapValues { (_, photos) ->
                photos.jsonArray.map { it.jsonPrimitive.content }
            }
        )
    }
}
