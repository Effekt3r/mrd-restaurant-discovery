package com.effekt3r.mrd.tools.datagen

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.TUESDAY
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HoursTest {

    private fun samples(style: HoursStyle) = (0L until 500L).map { Hours.generate(style, Random(it)) }

    private fun weekMinute(day: DayOfWeek, hour: Int, minute: Int = 0) =
        day.ordinal * MINUTES_PER_DAY + at(hour, minute)

    @Test
    fun everyPatternUsesTheQuarterHourGridAndNeverOpensAndClosesAtOnce() {
        HoursStyle.entries.flatMap(::samples).flatten().forEach { entry ->
            assertTrue("$entry", entry.opens % 15 == 0 && entry.closes % 15 == 0)
            assertTrue("$entry", entry.opens in 0 until MINUTES_PER_DAY && entry.closes in 1..MINUTES_PER_DAY)
            assertTrue("$entry", entry.opens != entry.closes)
        }
    }

    @Test
    fun standardIsTheSameWindowEveryDayAndAboutOneInFiveClosesOneWeekday() {
        val samples = samples(HoursStyle.STANDARD)
        samples.forEach { hours ->
            assertEquals(1, hours.map { it.opens to it.closes }.distinct().size)
            assertTrue(hours.all { it.closes > it.opens })
            assertEquals(hours.map { it.day }.distinct(), hours.map { it.day })
        }
        val closedOneDay = samples.filter { it.size == 6 }
        assertTrue("${closedOneDay.size} of 500", closedOneDay.size in 75..125)
        assertEquals(samples.size, samples.count { it.size == 7 } + closedOneDay.size)
        closedOneDay.forEach { hours ->
            assertTrue(hours.none { it.day == MONDAY } || hours.none { it.day == TUESDAY })
        }
    }

    @Test
    fun splitHasALunchAndADinnerShiftEachDayAndSomeCloseOnMondays() {
        val samples = samples(HoursStyle.SPLIT)
        samples.forEach { hours ->
            hours.groupBy { it.day }.values.forEach { shifts ->
                val (lunch, dinner) = shifts
                assertEquals(2, shifts.size)
                assertTrue("$shifts", lunch.closes <= at(15) && dinner.opens >= at(17, 30))
            }
            assertFalse(hours.isOpenAt(weekMinute(FRIDAY, 16)))
        }
        assertTrue(samples.any { hours -> hours.none { it.day == MONDAY } })
    }

    @Test
    fun lateNightRunsPastMidnightOnFridayAndSaturday() {
        samples(HoursStyle.LATE_NIGHT).forEach { hours ->
            hours.filter { it.day == FRIDAY || it.day == SATURDAY }.forEach { assertEquals(at(2), it.closes) }
            assertTrue(hours.isOpenAt(weekMinute(SATURDAY, 1, 45)))
            assertFalse(hours.isOpenAt(weekMinute(SATURDAY, 2)))
        }
    }

    @Test
    fun someLateNightPlacesCloseAtOneOnSundayWhichWrapsIntoMonday() {
        val (wrapping, notWrapping) = samples(HoursStyle.LATE_NIGHT).partition { hours ->
            hours.single { it.day == SUNDAY }.closes == at(1)
        }
        assertTrue(wrapping.isNotEmpty() && notWrapping.isNotEmpty())
        wrapping.forEach { assertTrue(it.isOpenAt(weekMinute(MONDAY, 0, 30))) }
        notWrapping.forEach { assertFalse(it.isOpenAt(weekMinute(MONDAY, 0, 30))) }
    }

    @Test
    fun allDayIsMidnightToMidnightEveryDay() {
        val hours = Hours.generate(HoursStyle.ALL_DAY, Random(0))
        assertEquals(DayOfWeek.entries.map { OpeningHours(it, 0, MINUTES_PER_DAY) }, hours)
        assertTrue((0 until MINUTES_PER_WEEK step 15).all(hours::isOpenAt))
    }

    @Test
    fun breakfastCafesOpenEarlyAndCloseInTheAfternoon() {
        samples(HoursStyle.BREAKFAST).flatten().forEach { entry ->
            assertTrue("$entry", entry.opens in at(6, 30)..at(7, 30) && entry.closes in at(14)..at(16))
        }
    }

    @Test
    fun isOpenAtIncludesTheOpeningMinuteAndExcludesTheClosingMinute() {
        val hours = listOf(OpeningHours(MONDAY, at(11), at(22)))
        assertFalse(hours.isOpenAt(weekMinute(MONDAY, 10, 45)))
        assertTrue(hours.isOpenAt(weekMinute(MONDAY, 11)))
        assertTrue(hours.isOpenAt(weekMinute(MONDAY, 21, 45)))
        assertFalse(hours.isOpenAt(weekMinute(MONDAY, 22)))
    }

    @Test
    fun anEntryThatOpensAndClosesAtTheSameTimeIsNeverOpen() {
        // Spec §5.1 drops such an entry, so it must not read as a window that stays open for a whole day.
        val sameTime = listOf(OpeningHours(DayOfWeek.WEDNESDAY, at(15), at(15)))
        assertFalse(sameTime.isOpenAt(weekMinute(DayOfWeek.WEDNESDAY, 16)))
        assertFalse(sameTime.isOpenAt(weekMinute(DayOfWeek.THURSDAY, 9)))
    }

    @Test
    fun entriesBecomeJsonWithDayNamesAndTwentyFourHourTimes() {
        assertEquals(
            """{"day":"FRIDAY","opens":"11:00","closes":"02:00"}""",
            OpeningHours(FRIDAY, at(11), at(2)).toJson().toString()
        )
        assertEquals(
            """{"day":"SUNDAY","opens":"00:00","closes":"24:00"}""",
            OpeningHours(SUNDAY, 0, MINUTES_PER_DAY).toJson().toString()
        )
    }
}
