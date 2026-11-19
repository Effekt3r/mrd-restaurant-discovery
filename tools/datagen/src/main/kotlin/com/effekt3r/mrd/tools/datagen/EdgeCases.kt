package com.effekt3r.mrd.tools.datagen

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Deliberately broken restaurants at fixed indexes below 220, so both datasets carry them. Each keeps every other
 * field valid, so milestone 3's validation tests know exactly why a record is dropped or what's repaired.
 * mock-api/README.md lists them with their ids.
 */
internal object EdgeCases {

    // Required by spec §5.2. The app drops these records.
    const val MISSING_ID = 10
    const val BLANK_NAME = 20
    const val DUPLICATE_ID = 30

    /** The earlier restaurant whose id [DUPLICATE_ID] repeats. */
    const val DUPLICATE_ID_SOURCE = 29

    // For milestone 3's validation tests (AC-14). The app keeps these records and treats the bad value as missing.
    const val RATING_OUT_OF_RANGE = 40
    const val RATING_NULL = 50
    const val RATING_AS_STRING = 60
    const val NEGATIVE_FEE = 70
    const val ZERO_ETA = 80
    const val HTTP_IMAGE = 90
    const val EMPTY_HOURS = 100
    const val HOURS_OPEN_EQUALS_CLOSE = 110
    const val HOURS_UNKNOWN_DAY = 120
    const val HOURS_INVALID_TIME = 130
    const val MESSY_CUISINES = 140
    const val UNKNOWN_STATUS = 150

    // Valid restaurants whose names test search: a LIKE wildcard to escape, and an accent to ignore.
    const val PERCENT_IN_NAME = 160
    const val ACCENTED_NAME = 170

    val indexes = setOf(
        MISSING_ID, BLANK_NAME, DUPLICATE_ID, RATING_OUT_OF_RANGE, RATING_NULL, RATING_AS_STRING, NEGATIVE_FEE,
        ZERO_ETA, HTTP_IMAGE, EMPTY_HOURS, HOURS_OPEN_EQUALS_CLOSE, HOURS_UNKNOWN_DAY, HOURS_INVALID_TIME,
        MESSY_CUISINES, UNKNOWN_STATUS, PERCENT_IN_NAME, ACCENTED_NAME
    )

    /** A name an edge case fixes, and the concept whose cuisines, hours and photo suit it. */
    class PinnedName(val name: String, val conceptKey: String)

    val pinnedNames = mapOf(
        PERCENT_IN_NAME to PinnedName("100% Plant Kitchen", "healthy"),
        ACCENTED_NAME to PinnedName("Café Fynbos", "cafe")
    )

    fun apply(index: Int, record: JsonObject): JsonObject = when (index) {
        MISSING_ID -> record.withField("id", null)
        BLANK_NAME -> record.withField("name", JsonPrimitive("   "))
        DUPLICATE_ID -> record.withField("id", JsonPrimitive(Generator.idFor(DUPLICATE_ID_SOURCE)))
        RATING_OUT_OF_RANGE -> record.withField("rating", JsonPrimitive(5.4))
        RATING_NULL -> record.withField("rating", JsonNull)
        RATING_AS_STRING -> record.withField("rating", JsonPrimitive("4.5"))
        NEGATIVE_FEE -> record.withField("delivery_fee_cents", JsonPrimitive(-500))
        ZERO_ETA -> record.withField("eta_minutes", JsonPrimitive(0))
        HTTP_IMAGE -> record.withField("image_url", JsonPrimitive(record.text("image_url").replace("https:", "http:")))
        EMPTY_HOURS -> record.withField("opening_hours", JsonArray(emptyList()))
        HOURS_OPEN_EQUALS_CLOSE -> record.withHoursEntry("WEDNESDAY", "15:00", "15:00")
        HOURS_UNKNOWN_DAY -> record.withHoursEntry("FUNDAY", "10:00", "22:00")
        HOURS_INVALID_TIME -> record.withHoursEntry("SATURDAY", "10:00", "25:00")
        MESSY_CUISINES -> {
            val cuisines = record.getValue("cuisines").jsonArray
            val blank = JsonPrimitive(" ")
            val empty = JsonPrimitive("")
            record.withField("cuisines", JsonArray(listOf(cuisines[0], blank, cuisines[0], empty) + cuisines.drop(1)))
        }
        UNKNOWN_STATUS -> record.withField("is_open", null).withField("opening_hours", null)
        else -> record
    }

    /** This record with [key] set to [value], or left out when [value] is null, in spec §5.1's key order. */
    private fun JsonObject.withField(key: String, value: JsonElement?): JsonObject {
        val fields = toMutableMap()
        if (value == null) fields.remove(key) else fields[key] = value
        return JsonObject(Restaurant.KEYS.mapNotNull { name -> fields[name]?.let { name to it } }.toMap())
    }

    /** This record with one more opening-hours entry after its valid ones. */
    private fun JsonObject.withHoursEntry(day: String, opens: String, closes: String): JsonObject {
        val entry = buildJsonObject {
            put("day", day)
            put("opens", opens)
            put("closes", closes)
        }
        return withField("opening_hours", JsonArray(getValue("opening_hours").jsonArray + entry))
    }

    private fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content
}
