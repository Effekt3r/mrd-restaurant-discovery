package com.effekt3r.mrd.core.data

import com.effekt3r.mrd.core.data.network.RestaurantsPayloadReader
import com.effekt3r.mrd.core.model.MINUTES_PER_WEEK
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.OpeningHours
import com.effekt3r.mrd.core.model.WeekMinute
import java.io.File
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spec §5.6's measurement for SOLUTION.md: how many open statuses a cached `is_open` gets wrong after hours offline.
 * A refresh at each quarter hour of the week stores the true status as `is_open`; after 1, 3, 6 and 12 hours offline,
 * the flag is wrong wherever the hours say the status has changed since. Only restaurants with hours count: for the
 * others there's no truth to compare, and the app shows them as Unknown once the snapshot is 30 minutes old. The
 * status the app derives from the hours is right at every one of those moments, because it's computed for that moment.
 */
class StalenessMeasurementTest {

    @Test
    fun cachedIsOpenOver220Restaurants() =
        assertEquals("205 with hours; wrong after 1h 8.8%, 3h 26.1%, 6h 44.5%, 12h 77.9%", measure("restaurants.json"))

    @Test
    fun cachedIsOpenOver10000Restaurants() = assertEquals(
        "9481 with hours; wrong after 1h 8.8%, 3h 26.1%, 6h 44.5%, 12h 77.4%",
        measure("restaurants-10k.json")
    )

    @OptIn(ExperimentalSerializationApi::class)
    private fun measure(file: String): String {
        val payload = File("../../mock-api/v1/$file").inputStream().use {
            testJson.decodeFromStream(RestaurantsPayloadReader, it)
        }
        val hours = payload.restaurants.mapNotNull { it.hours }
        // Whether each restaurant is open at each minute of the week, worked out once.
        val open = hours.map { it.openMinutes() }
        val refreshes = 0 until MINUTES_PER_WEEK step 15
        val results = listOf(1, 3, 6, 12).joinToString { offline ->
            val wrong = open.sumOf { minutes ->
                refreshes.count { at -> minutes[at] != minutes[(at + offline * 60) % MINUTES_PER_WEEK] }
            }
            "${offline}h ${percent(wrong, open.size * refreshes.count())}"
        }
        return "${hours.size} with hours; wrong after $results"
    }

    private fun OpeningHours.openMinutes() = List(MINUTES_PER_WEEK) { statusAt(WeekMinute(it)) is OpenStatus.Open }

    // One decimal place, rounded half up, without floating point.
    private fun percent(part: Int, whole: Int): String {
        val tenths = (part * 1000L + whole / 2) / whole
        return "${tenths / 10}.${tenths % 10}%"
    }
}
