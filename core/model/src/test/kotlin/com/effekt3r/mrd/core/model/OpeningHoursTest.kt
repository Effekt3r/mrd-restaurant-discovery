package com.effekt3r.mrd.core.model

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpeningHoursTest {

    private fun at(hour: Int, minute: Int = 0) = hour * 60 + minute

    private fun minute(day: DayOfWeek, hour: Int, minute: Int = 0) = WeekMinute.of(day, hour, minute).value

    @Test
    fun aWindowPastMidnightEndsTheNextDay() {
        val hours = OpeningHours.from(listOf(DailyWindow(FRIDAY, at(18), at(2))))
        assertEquals(listOf(WeekInterval(minute(FRIDAY, 18), minute(SATURDAY, 2))), hours?.intervals)
    }

    @Test
    fun aWindowPastSundayMidnightWrapsToTheStartOfTheWeek() {
        val hours = OpeningHours.from(listOf(DailyWindow(SUNDAY, at(20), at(1))))
        assertEquals(
            listOf(WeekInterval(0, minute(MONDAY, 1)), WeekInterval(minute(SUNDAY, 20), MINUTES_PER_WEEK)),
            hours?.intervals
        )
    }

    @Test
    fun overlappingAndTouchingWindowsMerge() {
        val windows = listOf(
            DailyWindow(MONDAY, at(15), at(22)),
            DailyWindow(MONDAY, at(11), at(15)),
            DailyWindow(MONDAY, at(12), at(14)),
            DailyWindow(TUESDAY, at(0), at(1))
        )
        assertEquals(
            listOf(
                WeekInterval(minute(MONDAY, 11), minute(MONDAY, 22)),
                WeekInterval(minute(TUESDAY, 0), minute(TUESDAY, 1))
            ),
            OpeningHours.from(windows)?.intervals
        )
    }

    @Test
    fun twentyFourHoursEveryDayIsOneIntervalThatNeverCloses() {
        val hours = OpeningHours.from(DayOfWeek.entries.map { DailyWindow(it, 0, MINUTES_PER_DAY) })
        assertEquals(listOf(WeekInterval(0, MINUTES_PER_WEEK)), hours?.intervals)
        assertEquals(OpenStatus.Open(closesAt = null), hours?.statusAt(WeekMinute.of(SUNDAY, 23, 59)))
    }

    @Test
    fun noWindowsMeansUnknownHours() {
        assertNull(OpeningHours.from(emptyList()))
    }

    @Test
    fun weekMinutesAreCapeTownTime() {
        // 22:30 UTC on Sunday is 00:30 on Monday in Cape Town (UTC+2).
        assertEquals(WeekMinute.of(MONDAY, 0, 30), WeekMinute.at(Instant.parse("2026-11-15T22:30:00Z")))
    }
}
