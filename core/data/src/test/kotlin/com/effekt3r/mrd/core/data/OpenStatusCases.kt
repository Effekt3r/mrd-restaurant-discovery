package com.effekt3r.mrd.core.data

import com.effekt3r.mrd.core.model.DailyWindow
import com.effekt3r.mrd.core.model.MINUTES_PER_DAY
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.WeekMinute
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Duration

/** One of spec §5.6's boundary cases: a restaurant's hours (none: unknown) and server snapshot, and a moment. */
internal data class OpenStatusCase(
    val name: String,
    val windows: List<DailyWindow>,
    val at: WeekMinute,
    val expected: OpenStatus,
    val openSnapshot: Boolean? = true,
    val snapshotAge: Duration = Duration.ZERO
)

/** Spec §5.6's boundary cases, run against both the Kotlin rules (Details) and the SQL query (the list). */
internal val openStatusCases: List<OpenStatusCase> = run {
    val elevenToTen = daily(hm(11), hm(22))
    val fridayNight = listOf(DailyWindow(FRIDAY, hm(18), hm(2)))
    val sundayNight = listOf(DailyWindow(SUNDAY, hm(20), hm(1)))
    val split = daily(hm(12), hm(14, 30)) + daily(hm(18), hm(22, 30))
    val allDay = daily(0, MINUTES_PER_DAY)
    val noHours = emptyList<DailyWindow>()
    listOf(
        OpenStatusCase("opening time is included", elevenToTen, at(MONDAY, 11), OpenStatus.Open(at(MONDAY, 22))),
        OpenStatusCase("closing time is excluded", elevenToTen, at(MONDAY, 22), OpenStatus.Closed(at(TUESDAY, 11))),
        OpenStatusCase("a minute before opening", elevenToTen, at(MONDAY, 10, 59), OpenStatus.Closed(at(MONDAY, 11))),
        OpenStatusCase("overnight, after midnight", fridayNight, at(SATURDAY, 1, 59), OpenStatus.Open(at(SATURDAY, 2))),
        OpenStatusCase("overnight, at closing", fridayNight, at(SATURDAY, 2), OpenStatus.Closed(at(FRIDAY, 18))),
        OpenStatusCase(
            "Sunday into Monday, before midnight",
            sundayNight,
            at(SUNDAY, 23),
            OpenStatus.Open(at(MONDAY, 1))
        ),
        OpenStatusCase(
            "Sunday into Monday, after midnight",
            sundayNight,
            at(MONDAY, 0, 30),
            OpenStatus.Open(at(MONDAY, 1))
        ),
        OpenStatusCase("Sunday into Monday, at closing", sundayNight, at(MONDAY, 1), OpenStatus.Closed(at(SUNDAY, 20))),
        OpenStatusCase(
            "split shifts, last lunch minute",
            split,
            at(MONDAY, 14, 29),
            OpenStatus.Open(at(MONDAY, 14, 30))
        ),
        OpenStatusCase("split shifts, between them", split, at(MONDAY, 15), OpenStatus.Closed(at(MONDAY, 18))),
        OpenStatusCase("24 hours, end of the week", allDay, at(SUNDAY, 23, 59), OpenStatus.Open(closesAt = null)),
        OpenStatusCase("24 hours, start of the week", allDay, at(MONDAY, 0), OpenStatus.Open(closesAt = null)),
        OpenStatusCase(
            "closed day",
            daily(hm(11), hm(22), closedOn = MONDAY),
            at(MONDAY, 12),
            OpenStatus.Closed(at(TUESDAY, 11))
        ),
        OpenStatusCase(
            "touching windows merge",
            listOf(DailyWindow(MONDAY, hm(11), hm(15)), DailyWindow(MONDAY, hm(15), hm(22))),
            at(MONDAY, 14, 59),
            OpenStatus.Open(at(MONDAY, 22))
        ),
        OpenStatusCase(
            "no hours, fresh snapshot, open",
            noHours,
            at(MONDAY, 12),
            OpenStatus.Open(closesAt = null),
            snapshotAge = Duration.ofMinutes(29)
        ),
        OpenStatusCase(
            "no hours, fresh snapshot, closed",
            noHours,
            at(MONDAY, 12),
            OpenStatus.Closed(opensAt = null),
            openSnapshot = false
        ),
        OpenStatusCase(
            "no hours, snapshot 30 minutes old",
            noHours,
            at(MONDAY, 12),
            OpenStatus.Unknown,
            snapshotAge = Duration.ofMinutes(30)
        ),
        OpenStatusCase(
            "no hours, stale snapshot",
            noHours,
            at(MONDAY, 12),
            OpenStatus.Unknown,
            snapshotAge = Duration.ofHours(6)
        ),
        OpenStatusCase("no hours and no is_open", noHours, at(MONDAY, 12), OpenStatus.Unknown, openSnapshot = null),
        // The time passed in is the latest minute tick, so a sync that lands between two ticks is newer than it.
        OpenStatusCase(
            "no hours, synced seconds after the minute tick",
            noHours,
            at(MONDAY, 12),
            OpenStatus.Open(closesAt = null),
            snapshotAge = Duration.ofSeconds(-6)
        ),
        OpenStatusCase(
            "no hours, synced 59 seconds after the minute tick",
            noHours,
            at(MONDAY, 12),
            OpenStatus.Open(closesAt = null),
            snapshotAge = Duration.ofSeconds(-59)
        ),
        OpenStatusCase(
            "no hours, snapshot a minute ahead of the tick (the clock was set back)",
            noHours,
            at(MONDAY, 12),
            OpenStatus.Unknown,
            snapshotAge = Duration.ofMinutes(-1)
        )
    )
}
