package com.effekt3r.mrd.tools.datagen

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonWriterTest {

    // eta_minutes is missing, so it's left out; rating is an explicit null.
    private val record = buildJsonObject {
        put("id", "r-1001")
        put("name", "Café \"Fynbos\" \\ Grill")
        putJsonArray("cuisines") {
            add("Steakhouse")
            add("Grill")
        }
        put("rating", JsonNull)
        put("delivery_fee_cents", 1500)
        putJsonArray("opening_hours") {
            addJsonObject {
                put("day", "MONDAY")
                put("opens", "11:00")
                put("closes", "22:00")
            }
        }
    }

    @Test
    fun compactIsOneLineInInsertionOrder() {
        assertEquals(
            """{"id":"r-1001","name":"Café \"Fynbos\" \\ Grill","cuisines":["Steakhouse","Grill"],"rating":null,""" +
                """"delivery_fee_cents":1500,"opening_hours":[{"day":"MONDAY","opens":"11:00","closes":"22:00"}]}""" +
                "\n",
            JsonWriter.compact(record)
        )
    }

    @Test
    fun prettyIndentsTwoSpacesAndKeepsShortListsOfValuesOnOneLine() {
        val expected = """
            {
              "id": "r-1001",
              "name": "Café \"Fynbos\" \\ Grill",
              "cuisines": ["Steakhouse", "Grill"],
              "rating": null,
              "delivery_fee_cents": 1500,
              "opening_hours": [
                { "day": "MONDAY", "opens": "11:00", "closes": "22:00" }
              ]
            }
        """.trimIndent() + "\n"
        assertEquals(expected, JsonWriter.pretty(record))
    }

    @Test
    fun prettyWritesLongListsOneItemPerLine() {
        val urls = (1..3).map { "https://www.themealdb.com/images/media/meals/photo-number-$it.jpg" }
        val pool = buildJsonObject { putJsonArray("beef") { urls.forEach { add(it) } } }
        val expected = """
            {
              "beef": [
                "${urls[0]}",
                "${urls[1]}",
                "${urls[2]}"
              ]
            }
        """.trimIndent() + "\n"
        assertEquals(expected, JsonWriter.pretty(pool))
    }

    @Test
    fun aMissingValueIsLeftOutAndAnExplicitNullIsWritten() {
        val compact = JsonWriter.compact(record)
        assertTrue(compact.contains("\"rating\":null"))
        assertFalse(compact.contains("eta_minutes"))
    }

    @Test
    fun bothFormsEndWithExactlyOneNewlineAndUseNoCarriageReturns() {
        listOf(JsonWriter.compact(record), JsonWriter.pretty(record)).forEach { text ->
            assertTrue(text.endsWith("}\n"))
            assertFalse(text.contains('\r'))
        }
    }

    @Test
    fun bothFormsParseBackToTheSameJson() {
        assertEquals(record, Json.parseToJsonElement(JsonWriter.compact(record)))
        assertEquals(record, Json.parseToJsonElement(JsonWriter.pretty(record)))
    }
}
