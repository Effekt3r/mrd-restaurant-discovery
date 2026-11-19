package com.effekt3r.mrd.tools.datagen

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The files the mock API serves: where each one lives, how many restaurants it holds, and how it's written. */
internal enum class Dataset(val path: String, val size: Int, private val pretty: Boolean) {

    /** The app's data. Pretty-printed, so people can read it and review changes to it. */
    STANDARD("mock-api/v1/restaurants.json", 220, pretty = true),

    /** The stress test. Compact, because at 10,000 restaurants size matters more than reading it. */
    STRESS("mock-api/v1/restaurants-10k.json", 10_000, pretty = false);

    fun render(pool: ImagePool): String {
        val json = buildJsonObject { put("restaurants", JsonArray(Generator.restaurants(size, pool))) }
        return if (pretty) JsonWriter.pretty(json) else JsonWriter.compact(json)
    }
}
