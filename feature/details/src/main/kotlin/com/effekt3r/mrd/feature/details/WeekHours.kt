package com.effekt3r.mrd.feature.details

import com.effekt3r.mrd.core.model.MINUTES_PER_DAY
import com.effekt3r.mrd.core.model.MINUTES_PER_WEEK
import com.effekt3r.mrd.core.model.OpeningHours
import java.time.DayOfWeek

/** What one day shows in Details' list of opening hours (spec §6.3). */
internal sealed interface DayHours {
    data object Closed : DayHours

    data object AllDay : DayHours

    /** The windows that open this day, in order. */
    data class Windows(val windows: List<Window>) : DayHours
}

/**
 * A window as minutes after the day's midnight. [closes] is more than 1,440 when it runs past midnight: Friday's
 * 11:00 to 02:00 is (660, 1560), listed on Friday.
 */
internal data class Window(val opens: Int, val closes: Int)

/**
 * Each day of the week with its hours, as a list reads them: a window belongs to the day it opens on, with its closing
 * time even when that's the next morning. A day the restaurant is open throughout is "open 24 hours", and the day a
 * longer window ends on shows it closing from midnight.
 */
internal fun weekHours(hours: OpeningHours): Map<DayOfWeek, DayHours> {
    // The model splits a window that runs past Sunday midnight; join it back, so Sunday reads 18:00 to 02:00.
    val spans = hours.intervals.map { it.start to it.end }.toMutableList()
    if (spans.size > 1 && spans.first().first == 0 && spans.last().second == MINUTES_PER_WEEK) {
        val head = spans.removeAt(0)
        val tail = spans.removeAt(spans.lastIndex)
        spans += tail.first to head.second + MINUTES_PER_WEEK
    }
    // Each span is also seen a week earlier, so a span that ends early on Monday reaches Monday.
    val all = spans.flatMap { (start, end) -> listOf(start to end, start - MINUTES_PER_WEEK to end - MINUTES_PER_WEEK) }
    return DayOfWeek.entries.associateWith { day ->
        val dayStart = day.ordinal * MINUTES_PER_DAY
        val dayEnd = dayStart + MINUTES_PER_DAY
        if (all.any { (start, end) -> start <= dayStart && end >= dayEnd }) return@associateWith DayHours.AllDay
        val windows = all.mapNotNull { (start, end) ->
            when {
                // Opens today. A window that also covers the whole of tomorrow is shown closing at midnight, since
                // tomorrow says "open 24 hours".
                start in dayStart until dayEnd -> {
                    val closes = if (end >= dayEnd + MINUTES_PER_DAY) dayEnd else end
                    Window(start - dayStart, closes - dayStart)
                }

                // Opened before yesterday and ends today: yesterday said "open 24 hours", so today shows the end.
                start <= dayStart - MINUTES_PER_DAY && end > dayStart -> Window(0, end - dayStart)

                else -> null
            }
        }.sortedBy { it.opens }
        if (windows.isEmpty()) DayHours.Closed else DayHours.Windows(windows)
    }
}
