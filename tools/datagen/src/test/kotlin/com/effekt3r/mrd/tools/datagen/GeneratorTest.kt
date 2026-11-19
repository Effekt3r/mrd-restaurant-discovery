package com.effekt3r.mrd.tools.datagen

import java.time.DayOfWeek
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorTest {

    private companion object {
        val pool = ImagePool.load()
        val standard = Generator.restaurants(Dataset.STANDARD.size, pool)
        val stress = Generator.restaurants(Dataset.STRESS.size, pool)

        /** Every restaurant except the deliberately broken ones, which EdgeCasesTest covers. */
        val normal = stress.filterIndexed { index, _ -> index !in EdgeCases.indexes }

        fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content

        fun JsonObject.hours() = get("opening_hours")?.jsonArray?.map { entry ->
            val (day, opens, closes) = listOf("day", "opens", "closes").map { entry.jsonObject.text(it) }
            OpeningHours(DayOfWeek.valueOf(day), minutes(opens), minutes(closes))
        }

        fun minutes(clock: String) = at(clock.substringBefore(':').toInt(), clock.substringAfter(':').toInt())

        fun percent(of: List<JsonObject>, predicate: (JsonObject) -> Boolean) = 100.0 * of.count(predicate) / of.size
    }

    @Test
    fun generatingTwiceGivesTheSameRestaurants() {
        assertEquals(stress, Generator.restaurants(Dataset.STRESS.size, pool))
    }

    @Test
    fun theStandardDatasetIsTheStressDatasetsFirst220Restaurants() {
        assertEquals(220, standard.size)
        assertEquals(standard, stress.take(220))
    }

    @Test
    fun theFirstRestaurantIsTheBriefsExample() {
        val hours = listOf("22:00", "22:00", "22:00", "22:00", "02:00", "02:00", "21:00")
            .zip(DayOfWeek.entries) { closes, day -> """{"day":"$day","opens":"11:00","closes":"$closes"}""" }
        assertEquals(
            """{"id":"r-1001","name":"Cape Town Grill","cuisines":["Steakhouse","Grill"],"rating":4.6,""" +
                """"delivery_fee_cents":1500,"eta_minutes":32,"is_open":true,"image_url":"${pool.first("beef")}",""" +
                """"opening_hours":[${hours.joinToString(",")}]}""",
            standard.first().toString()
        )
    }

    @Test
    fun missingValuesAndFreeDeliveryFollowTheSpecsRates() {
        assertEquals("no rating", 6.0, percent(stress) { "rating" !in it }, 1.0)
        assertEquals("no fee", 4.0, percent(stress) { "delivery_fee_cents" !in it }, 1.0)
        assertEquals("no ETA", 4.0, percent(stress) { "eta_minutes" !in it }, 1.0)
        assertEquals("no image", 6.0, percent(stress) { "image_url" !in it }, 1.0)
        assertEquals("no opening_hours key", 4.0, percent(stress) { "opening_hours" !in it }, 1.0)
        assertEquals("opening_hours is []", 1.0, percent(stress) { it["opening_hours"] == JsonArray(emptyList()) }, 0.5)
        assertEquals("free delivery", 15.0, percent(stress) { it["delivery_fee_cents"]?.jsonPrimitive?.int == 0 }, 1.0)
        val withoutHours = normal.filter { it.hours().isNullOrEmpty() }
        val openWithoutHours = percent(withoutHours) { it.getValue("is_open").jsonPrimitive.boolean }
        assertEquals("open without hours", 70.0, openWithoutHours, 5.0)
    }

    @Test
    fun everyNormalRestaurantFollowsTheSpecsRules() {
        val photos = pool.photos.values.flatten().toSet()
        stress.forEachIndexed { index, record ->
            if (index in EdgeCases.indexes) return@forEachIndexed
            val where = "restaurant $index: $record"
            assertEquals(where, Restaurant.KEYS.filter { it in record }, record.keys.toList())
            assertTrue(where, record.text("name").isNotBlank())
            val cuisines = record.getValue("cuisines").jsonArray.map { it.jsonPrimitive.content }
            assertTrue(where, cuisines.size in 1..3 && cuisines.none { it.isBlank() })
            assertEquals(where, cuisines.distinct(), cuisines)
            record["rating"]?.jsonPrimitive?.let { rating ->
                assertTrue(where, !rating.isString && rating.content.matches(Regex("""[34]\.\d""")))
                assertTrue(where, rating.content.toDouble() in 3.2..4.9)
            }
            record["delivery_fee_cents"]?.let { assertTrue(where, it.jsonPrimitive.int in Concepts.FEE_LADDER + 0) }
            record["eta_minutes"]?.let { assertTrue(where, it.jsonPrimitive.int in 15..55) }
            record["image_url"]?.let { assertTrue(where, it.jsonPrimitive.content in photos) }
            record.hours()?.forEach { assertTrue(where, it.opens != it.closes && it.opens % 15 == 0) }
        }
    }

    @Test
    fun idsFollowThePositionAndOnlyTheEdgeCasesBreakThatOrUniqueness() {
        val ids = normal.map { it.text("id") }
        assertEquals(ids.size, ids.toSet().size)
        stress.forEachIndexed { index, record ->
            if (index != EdgeCases.MISSING_ID && index != EdgeCases.DUPLICATE_ID) {
                assertEquals(Generator.idFor(index), record.text("id"))
            }
        }
    }

    @Test
    fun isOpenIsTheStatusOnFridayAt1900WhenTheHoursAreKnown() {
        normal.filter { !it.hours().isNullOrEmpty() }.forEach { record ->
            val expected = record.hours()!!.isOpenAt(Generator.SNAPSHOT_WEEK_MINUTE)
            assertEquals("$record", expected, record.getValue("is_open").jsonPrimitive.boolean)
        }
    }

    @Test
    fun theSnapshotIsFridayAt1900() {
        // Monday 00:00 is week minute 0: Friday starts 4 × 1,440 minutes in, and 19:00 is 1,140 minutes into a day.
        assertEquals(6_900, Generator.SNAPSHOT_WEEK_MINUTE)
    }

    @Test
    fun namesAreUniqueInTheStandardDatasetSomeHaveAccentsAndNoneIsReal() {
        val names = standard.map { it.text("name") }
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.count { name -> name.any { it.code > 127 } } >= 3)
        val clashes = stress.map { it.text("name") }.distinct()
            .mapNotNull { name -> name.realNameInside()?.let { "$name contains $it" } }
        assertEquals(emptyList<String>(), clashes)
    }

    @Test
    fun theStandardDatasetHasEveryKindOfOpeningHours() {
        val hours = standard.filterIndexed { index, _ -> index !in EdgeCases.indexes }
            .mapNotNull { it.hours() }
            .filter { it.isNotEmpty() }
        assertTrue("overnight", hours.any { week -> week.any { it.closes < it.opens } })
        assertTrue("split shifts", hours.any { week -> week.groupBy { it.day }.values.any { it.size == 2 } })
        assertTrue("24 hours", hours.any { week -> week.all { it.opens == 0 && it.closes == MINUTES_PER_DAY } })
        assertTrue("closed one weekday", hours.any { week -> week.map { it.day }.distinct().size == 6 })
        val sundayIntoMonday = hours.any { week -> week.any { it.day == DayOfWeek.SUNDAY && it.closes < it.opens } }
        assertTrue("Sunday into Monday", sundayIntoMonday)
    }
}
