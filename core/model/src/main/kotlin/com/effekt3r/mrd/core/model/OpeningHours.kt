package com.effekt3r.mrd.core.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Restaurants keep Cape Town time. South Africa has no daylight saving, so every week is 10,080 minutes. */
val RESTAURANT_TIME_ZONE: ZoneId = ZoneId.of("Africa/Johannesburg")

const val MINUTES_PER_DAY = 24 * 60
const val MINUTES_PER_WEEK = 7 * MINUTES_PER_DAY

/** How long the server's `is_open` is trusted for a restaurant without hours (spec §5.6). */
val SNAPSHOT_TRUSTED_FOR: Duration = Duration.ofMinutes(30)

/** A minute of the week in Cape Town time: 0 is Monday 00:00 and 10,079 is Sunday 23:59. */
@JvmInline
value class WeekMinute(val value: Int) {
    init {
        require(value in 0 until MINUTES_PER_WEEK) { "A week minute is from 0 to 10,079, not $value" }
    }

    val day: DayOfWeek get() = DayOfWeek.of(value / MINUTES_PER_DAY + 1)

    /** Minutes after midnight: 1,320 is 22:00. */
    val minuteOfDay: Int get() = value % MINUTES_PER_DAY

    companion object {
        fun of(day: DayOfWeek, hour: Int, minute: Int = 0) = WeekMinute(
            day.ordinal * MINUTES_PER_DAY + hour * 60 + minute
        )

        fun at(instant: Instant): WeekMinute =
            instant.atZone(RESTAURANT_TIME_ZONE).let { of(it.dayOfWeek, it.hour, it.minute) }
    }
}

/**
 * One opening window as the API sends it: [opens] and [closes] are minutes after midnight on [day]. [closes] may be
 * 1,440 (24:00), or earlier than [opens] when the window ends the next day. A window can't open and close at once.
 */
data class DailyWindow(val day: DayOfWeek, val opens: Int, val closes: Int) {
    init {
        require(opens in 0 until MINUTES_PER_DAY && closes in 0..MINUTES_PER_DAY && opens != closes) {
            "Invalid window: $day $opens-$closes"
        }
    }
}

/** A half-open span of the week, [start, end) in week minutes. [end] is at most 10,080, the end of Sunday. */
data class WeekInterval(val start: Int, val end: Int) {
    init {
        require(start in 0 until end && end <= MINUTES_PER_WEEK) { "Invalid interval: $start-$end" }
    }

    operator fun contains(minute: WeekMinute) = minute.value >= start && minute.value < end
}

/**
 * A restaurant's weekly opening hours (spec §5.6): sorted intervals that neither overlap nor touch. A window that
 * runs past Sunday midnight is split, and its second part starts the week.
 */
data class OpeningHours(val intervals: List<WeekInterval>) {
    init {
        require(intervals.isNotEmpty()) { "Unknown hours are null, not empty" }
        require(intervals.zipWithNext().all { (a, b) -> a.end < b.start }) { "Unsorted or touching: $intervals" }
    }

    /** Open or Closed at [now], with the next change. */
    fun statusAt(now: WeekMinute): OpenStatus {
        val current = intervals.firstOrNull { now in it }
        if (current != null) return OpenStatus.Open(closesAt = closingOf(current))
        val next = intervals.firstOrNull { it.start > now.value } ?: intervals.first()
        return OpenStatus.Closed(opensAt = WeekMinute(next.start))
    }

    // An interval that runs to the end of Sunday carries on into the week's first interval when that starts at 00:00.
    private fun closingOf(interval: WeekInterval): WeekMinute? = when {
        interval.end < MINUTES_PER_WEEK -> WeekMinute(interval.end)
        interval.start == 0 -> null
        else -> WeekMinute(intervals.first().let { if (it.start == 0) it.end else 0 })
    }

    companion object {
        /**
         * The hours from the API's windows: a window past midnight ends the next day, one past Sunday midnight wraps to
         * the start of the week, and overlapping or touching windows merge. No windows means the hours are unknown.
         */
        fun from(windows: List<DailyWindow>): OpeningHours? {
            val spans = windows.flatMap { window ->
                val dayStart = window.day.ordinal * MINUTES_PER_DAY
                val start = dayStart + window.opens
                val end = dayStart + window.closes + if (window.closes > window.opens) 0 else MINUTES_PER_DAY
                if (end <= MINUTES_PER_WEEK) {
                    listOf(WeekInterval(start, end))
                } else {
                    listOf(WeekInterval(start, MINUTES_PER_WEEK), WeekInterval(0, end - MINUTES_PER_WEEK))
                }
            }
            val merged = spans.sortedBy { it.start }.fold(emptyList<WeekInterval>()) { acc, span ->
                val last = acc.lastOrNull()
                if (last != null && span.start <= last.end) {
                    acc.dropLast(1) + WeekInterval(last.start, maxOf(last.end, span.end))
                } else {
                    acc + span
                }
            }
            return merged.takeIf { it.isNotEmpty() }?.let(::OpeningHours)
        }
    }
}

/** Whether a restaurant is open, and when that next changes. */
sealed interface OpenStatus {
    /** [closesAt] is null when it never closes (open around the clock) or the hours are unknown. */
    data class Open(val closesAt: WeekMinute?) : OpenStatus

    /** [opensAt] is null when the hours are unknown. */
    data class Closed(val opensAt: WeekMinute?) : OpenStatus

    /** No hours, and the server's snapshot is too old to trust. */
    data object Unknown : OpenStatus
}

/**
 * A restaurant's status at [now] (spec §5.6). Known hours decide. Without them, the server's [openSnapshot] counts while
 * the snapshot, taken at [snapshotAt], is fresh; after that the app doesn't guess.
 */
fun openStatus(hours: OpeningHours?, openSnapshot: Boolean?, snapshotAt: Instant?, now: Instant): OpenStatus = when {
    hours != null -> hours.statusAt(WeekMinute.at(now))
    openSnapshot == null || !isSnapshotFresh(snapshotAt, now) -> OpenStatus.Unknown
    openSnapshot -> OpenStatus.Open(closesAt = null)
    else -> OpenStatus.Closed(opensAt = null)
}

/** Whether a snapshot is less than 30 minutes old. One from the future (the clock was set back) isn't trusted. */
fun isSnapshotFresh(snapshotAt: Instant?, now: Instant): Boolean =
    snapshotAt != null && !now.isBefore(snapshotAt) && Duration.between(snapshotAt, now) < SNAPSHOT_TRUSTED_FOR
