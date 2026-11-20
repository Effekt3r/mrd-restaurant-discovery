package com.effekt3r.mrd.core.data.network

import com.effekt3r.mrd.core.data.database.searchKey
import com.effekt3r.mrd.core.model.DailyWindow
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.OpeningHours
import com.effekt3r.mrd.core.model.Rating
import com.effekt3r.mrd.core.model.WeekMinute
import java.io.File
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalSerializationApi::class)
class RestaurantParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(text: String) = json.decodeFromString(RestaurantsPayloadReader, text)

    // The committed mock API, read with the production reader. build.gradle.kts declares it as the tests' input.
    private val committed by lazy {
        File("../../mock-api/v1/restaurants.json").inputStream().use {
            json.decodeFromStream(RestaurantsPayloadReader, it)
        }
    }

    private fun committed(id: String) = committed.restaurants.single { it.id == id }

    @Test
    fun aValidRecordKeepsEveryField() {
        val payload = parse(
            """
            {"restaurants": [{"id": "r-1", "name": " Cape Town Grill ", "cuisines": ["Steakhouse", "Grill"],
              "rating": 4.6, "delivery_fee_cents": 1500, "eta_minutes": 32, "is_open": true,
              "image_url": "https://example.com/a.jpg", "unknown_key": 1,
              "opening_hours": [{"day": "MONDAY", "opens": "11:00", "closes": "22:00"},
                                {"day": "FRIDAY", "opens": "18:00", "closes": "02:00"}]}]}
            """
        )
        assertEquals(
            ParsedRestaurant(
                id = "r-1",
                name = "Cape Town Grill",
                cuisines = listOf("Steakhouse", "Grill"),
                rating = Rating(46),
                deliveryFeeCents = 1500,
                etaMinutes = 32,
                isOpen = true,
                imageUrl = "https://example.com/a.jpg",
                hours = OpeningHours.from(listOf(DailyWindow(MONDAY, 660, 1320), DailyWindow(FRIDAY, 1080, 120)))
            ),
            payload.restaurants.single()
        )
    }

    @Test
    fun wrongTypedValuesReadAsMissing() {
        val restaurant = parse(
            """
            {"restaurants": [{"id": "r-1", "name": "Grill", "cuisines": "Grill", "rating": "4.5",
              "delivery_fee_cents": 15.5, "eta_minutes": "30", "is_open": "true", "image_url": 7,
              "opening_hours": {"day": "MONDAY"}}]}
            """
        ).restaurants.single()
        assertEquals(ParsedRestaurant("r-1", "Grill", emptyList(), null, null, null, null, null, null), restaurant)
    }

    @Test
    fun recordsWithoutAnIdOrNameAreDroppedAndARepeatedIdKeepsTheFirst() {
        val payload = parse(
            """
            {"restaurants": [{"name": "No id"}, {"id": " ", "name": "Blank id"}, {"id": "r-1", "name": "  "},
              {"id": "r-2", "name": "First"}, {"id": "r-2", "name": "Second"}, 42, null]}
            """
        )
        assertEquals(7, payload.recordCount)
        assertEquals(listOf("First"), payload.restaurants.map { it.name })
    }

    @Test
    fun malformedJsonOrAMissingArrayIsBadData() {
        assertThrows(SerializationException::class.java) { parse("""{"restaurants": [{"id": "r-1"""") }
        assertThrows(SerializationException::class.java) { parse("""{"data": []}""") }
        assertThrows(SerializationException::class.java) { parse("""{"restaurants": {}}""") }
    }

    @Test
    fun theCommittedFileKeeps217Of220Records() {
        assertEquals(220, committed.recordCount)
        assertEquals(217, committed.restaurants.size)
        assertEquals("Fynbos Pizza Bar", committed("r-1030").name) // index 29; index 30 repeats its id
        assertEquals(null, committed.restaurants.firstOrNull { it.name == "De Waterkant Pizza Kitchen" }) // no id
        assertEquals(null, committed.restaurants.firstOrNull { it.id == "r-1021" }) // blank name
    }

    @Test
    fun theCommittedEdgeCasesReadAsSpecSection51Says() {
        assertNull(committed("r-1041").rating) // 5.4
        assertNull(committed("r-1051").rating) // null
        assertNull(committed("r-1061").rating) // "4.5"
        assertNull(committed("r-1071").deliveryFeeCents) // -500
        assertNull(committed("r-1081").etaMinutes) // 0
        assertNull(committed("r-1091").imageUrl) // http://
        assertNull(committed("r-1101").hours) // []
        assertEquals(listOf("Pizza", "Italian"), committed("r-1141").cuisines)
        assertNull(committed("r-1151").hours)
        assertNull(committed("r-1151").isOpen)
        assertEquals("100% Plant Kitchen", committed("r-1161").name)
        assertEquals("cafe fynbos", searchKey(committed("r-1171").name))
        // Only the broken entry is dropped; the record keeps its other hours.
        assertFalse(committed("r-1111").isOpenAt(WEDNESDAY, 3)) // 15:00-15:00 isn't open all day
        assertTrue(committed("r-1121").isOpenAt(TUESDAY, 12)) // FUNDAY
        assertFalse(committed("r-1131").isOpenAt(SATURDAY, 10, 30)) // 10:00-25:00
        assertTrue(committed("r-1131").isOpenAt(SATURDAY, 12))
    }

    private fun ParsedRestaurant.isOpenAt(day: DayOfWeek, hour: Int, minute: Int = 0) =
        hours?.statusAt(WeekMinute.of(day, hour, minute)) is OpenStatus.Open
}
