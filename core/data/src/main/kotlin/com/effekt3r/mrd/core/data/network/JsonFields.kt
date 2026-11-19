package com.effekt3r.mrd.core.data.network

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

// Lenient field readers: a missing, null or wrong-typed value reads as null, so one bad field never fails a record.
// A number sent as a string ("4.5") is wrong-typed.

internal fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

internal fun JsonObject.long(key: String): Long? = (get(key) as? JsonPrimitive)?.takeUnless { it.isString }?.longOrNull

internal fun JsonObject.double(key: String): Double? =
    (get(key) as? JsonPrimitive)?.takeUnless { it.isString }?.doubleOrNull

internal fun JsonObject.boolean(key: String): Boolean? =
    (get(key) as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull

internal fun JsonObject.array(key: String): List<JsonElement> = get(key) as? JsonArray ?: emptyList()

/** A trimmed string that isn't blank. */
internal fun JsonObject.text(key: String): String? = string(key)?.trim()?.takeIf { it.isNotEmpty() }

/** An HTTPS URL; anything else, such as http://, reads as missing (spec §5.1). */
internal fun JsonObject.httpsUrl(key: String): String? = string(key)?.takeIf { it.startsWith("https://") }
