package com.effekt3r.mrd.tools.datagen

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeCasesTest {

    private companion object {
        val records = Generator.restaurants(Dataset.STANDARD.size, ImagePool.load())

        fun record(index: Int): JsonObject = records[index]

        fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content
    }

    @Test
    fun everyCaseHasItsOwnIndexBelow220AndTheFirstRestaurantIsLeftAlone() {
        assertEquals(17, EdgeCases.indexes.size)
        assertTrue(EdgeCases.indexes.all { it in 1 until Dataset.STANDARD.size })
        assertTrue(EdgeCases.DUPLICATE_ID_SOURCE < EdgeCases.DUPLICATE_ID)
        assertFalse(EdgeCases.DUPLICATE_ID_SOURCE in EdgeCases.indexes)
    }

    @Test
    fun exactlyOneRestaurantHasNoId() {
        assertEquals(listOf(EdgeCases.MISSING_ID), records.indices.filter { "id" !in record(it) })
    }

    @Test
    fun exactlyOneRestaurantHasABlankName() {
        assertEquals(listOf(EdgeCases.BLANK_NAME), records.indices.filter { record(it).text("name").isBlank() })
        assertEquals("   ", record(EdgeCases.BLANK_NAME).text("name"))
    }

    @Test
    fun exactlyTwoRestaurantsShareAnId() {
        val repeated = records.mapNotNull { it["id"]?.jsonPrimitive?.content }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
        assertEquals(mapOf(Generator.idFor(EdgeCases.DUPLICATE_ID_SOURCE) to 2), repeated)
        assertEquals(Generator.idFor(EdgeCases.DUPLICATE_ID_SOURCE), record(EdgeCases.DUPLICATE_ID).text("id"))
    }

    @Test
    fun ratingsAreOutOfRangeAnExplicitNullAndAString() {
        assertEquals(JsonPrimitive(5.4), record(EdgeCases.RATING_OUT_OF_RANGE)["rating"])
        assertEquals(JsonNull, record(EdgeCases.RATING_NULL)["rating"])
        assertEquals(JsonPrimitive("4.5"), record(EdgeCases.RATING_AS_STRING)["rating"])
    }

    @Test
    fun aNegativeFeeAZeroEtaAndAnHttpImage() {
        assertEquals(JsonPrimitive(-500), record(EdgeCases.NEGATIVE_FEE)["delivery_fee_cents"])
        assertEquals(JsonPrimitive(0), record(EdgeCases.ZERO_ETA)["eta_minutes"])
        assertTrue(record(EdgeCases.HTTP_IMAGE).text("image_url").startsWith("http://www.themealdb.com/"))
    }

    @Test
    fun emptyHoursAndInvalidEntriesNextToValidOnes() {
        assertEquals(JsonArray(emptyList()), record(EdgeCases.EMPTY_HOURS)["opening_hours"])
        mapOf(
            EdgeCases.HOURS_OPEN_EQUALS_CLOSE to """{"day":"WEDNESDAY","opens":"15:00","closes":"15:00"}""",
            EdgeCases.HOURS_UNKNOWN_DAY to """{"day":"FUNDAY","opens":"10:00","closes":"22:00"}""",
            EdgeCases.HOURS_INVALID_TIME to """{"day":"SATURDAY","opens":"10:00","closes":"25:00"}"""
        ).forEach { (index, invalid) ->
            val hours = record(index).getValue("opening_hours").jsonArray
            assertEquals(invalid, hours.last().toString())
            assertTrue(hours.size >= 7)
        }
    }

    @Test
    fun cuisinesWithBlankAndRepeatedEntries() {
        val cuisines = record(EdgeCases.MESSY_CUISINES).getValue("cuisines").jsonArray.map { it.jsonPrimitive.content }
        assertTrue("$cuisines", cuisines.count { it.isBlank() } == 2 && cuisines.size - cuisines.distinct().size == 1)
    }

    @Test
    fun exactlyOneRestaurantHasNoIsOpenAndItHasNoHoursEither() {
        assertEquals(listOf(EdgeCases.UNKNOWN_STATUS), records.indices.filter { "is_open" !in record(it) })
        assertFalse("opening_hours" in record(EdgeCases.UNKNOWN_STATUS))
    }

    @Test
    fun searchCasesHaveAPercentSignAndAnAccent() {
        assertEquals("100% Plant Kitchen", record(EdgeCases.PERCENT_IN_NAME).text("name"))
        assertEquals("Café Fynbos", record(EdgeCases.ACCENTED_NAME).text("name"))
    }

    @Test
    fun apartFromItsOneChangeEachCaseHasEveryFieldInOrder() {
        val removesKeys = setOf(EdgeCases.MISSING_ID, EdgeCases.UNKNOWN_STATUS)
        (EdgeCases.indexes - removesKeys).forEach { index ->
            assertEquals("$index", Restaurant.KEYS, record(index).keys.toList())
        }
    }
}
