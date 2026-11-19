package com.effekt3r.mrd.feature.details

import com.effekt3r.mrd.core.model.DailyWindow
import com.effekt3r.mrd.core.model.OpeningHours
import com.effekt3r.mrd.core.model.WeekInterval
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import org.junit.Assert.assertEquals
import org.junit.Test

/** The week's hours as Details lists them: each day with the windows that open on it (spec §6.3). */
class WeekHoursTest {

    private fun hm(hour: Int, minute: Int = 0) = hour * 60 + minute

    private fun hours(vararg windows: DailyWindow) = OpeningHours.from(windows.toList())!!

    private fun window(opens: Int, closes: Int) = DayHours.Windows(listOf(Window(opens, closes)))

    @Test
    fun aWindowPastMidnightIsListedOnTheDayItOpens() {
        // Cape Town Grill on Friday: 11:00 to 02:00. Saturday's small hours belong to Friday's line.
        val week = weekHours(hours(DailyWindow(FRIDAY, hm(11), hm(2)), DailyWindow(SATURDAY, hm(11), hm(22))))
        assertEquals(window(hm(11), hm(26)), week.getValue(FRIDAY))
        assertEquals(window(hm(11), hm(22)), week.getValue(SATURDAY))
        assertEquals(DayHours.Closed, week.getValue(SUNDAY))
    }

    @Test
    fun splitShiftsAreListedInOrder() {
        val week = weekHours(hours(DailyWindow(MONDAY, hm(18), hm(22)), DailyWindow(MONDAY, hm(11), hm(15))))
        assertEquals(DayHours.Windows(listOf(Window(hm(11), hm(15)), Window(hm(18), hm(22)))), week.getValue(MONDAY))
    }

    @Test
    fun sundayNightRunsIntoMondayAsOneWindow() {
        // Split at the end of the week by the model, joined again for the list.
        val week = weekHours(hours(DailyWindow(SUNDAY, hm(18), hm(2)), DailyWindow(MONDAY, hm(11), hm(15))))
        assertEquals(window(hm(18), hm(26)), week.getValue(SUNDAY))
        assertEquals(window(hm(11), hm(15)), week.getValue(MONDAY))
    }

    @Test
    fun aRestaurantThatNeverClosesIsOpen24HoursEveryDay() {
        val week = weekHours(OpeningHours(listOf(WeekInterval(0, 10_080))))
        assertEquals(DayOfWeek.entries.associateWith { DayHours.AllDay }, week)
    }

    @Test
    fun aDayWithinALongWindowIsOpen24HoursAndItsLastDayShowsWhenItCloses() {
        // Open from Monday 11:00 right through to Wednesday 02:00.
        val week = weekHours(OpeningHours(listOf(WeekInterval(hm(11), 2 * 1440 + hm(2)))))
        assertEquals(window(hm(11), hm(24)), week.getValue(MONDAY))
        assertEquals(DayHours.AllDay, week.getValue(TUESDAY))
        assertEquals(window(0, hm(2)), week.getValue(WEDNESDAY))
        assertEquals(DayHours.Closed, week.getValue(FRIDAY))
    }

    @Test
    fun open24HoursExceptOneDay() {
        val week = weekHours(OpeningHours(listOf(WeekInterval(1440, 10_080))))
        assertEquals(DayHours.Closed, week.getValue(MONDAY))
        assertEquals(DayHours.AllDay, week.getValue(SUNDAY))
    }
}
