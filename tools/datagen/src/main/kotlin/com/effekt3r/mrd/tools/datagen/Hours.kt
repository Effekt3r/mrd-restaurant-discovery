package com.effekt3r.mrd.tools.datagen

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.TUESDAY
import kotlin.random.Random
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal const val MINUTES_PER_DAY = 24 * 60
internal const val MINUTES_PER_WEEK = 7 * MINUTES_PER_DAY

/** Minutes after midnight: `at(11, 30)` is 11:30. */
internal fun at(hour: Int, minute: Int = 0) = hour * 60 + minute

/**
 * One opening window in the restaurant's local time (Africa/Johannesburg), in minutes after midnight. [closes] may be
 * 1440 ("24:00"), or earlier than [opens] when the window runs past midnight into the next day.
 */
internal data class OpeningHours(val day: DayOfWeek, val opens: Int, val closes: Int) {

    fun toJson(): JsonObject = buildJsonObject {
        put("day", day.name)
        put("opens", clock(opens))
        put("closes", clock(closes))
    }

    // "HH:mm", padded by hand: String.format would use the machine's locale, which could change the digits.
    private fun clock(minutes: Int) =
        (minutes / 60).toString().padStart(2, '0') + ":" + (minutes % 60).toString().padStart(2, '0')
}

/** The weekly patterns a restaurant's opening hours follow (spec §5.2). Every time is on a 15-minute grid. */
internal enum class HoursStyle {
    /** One window a day, opening 10:00 to 12:00 and closing 21:00 to 23:00. About one in five closes one weekday. */
    STANDARD,

    /** Lunch and dinner shifts every day. About one in five closes on Mondays. */
    SPLIT,

    /** Open late, and until 02:00 on Friday and Saturday nights. Half close at 01:00 on Sunday night (Monday). */
    LATE_NIGHT,

    /** Open around the clock: 00:00 to 24:00 every day. */
    ALL_DAY,

    /** A breakfast café, open from about 07:00 to 15:00. */
    BREAKFAST
}

internal object Hours {

    fun generate(style: HoursStyle, random: Random): List<OpeningHours> = when (style) {
        HoursStyle.STANDARD -> standard(random)
        HoursStyle.SPLIT -> split(random)
        HoursStyle.LATE_NIGHT -> lateNight(random)
        HoursStyle.ALL_DAY -> DayOfWeek.entries.map { OpeningHours(it, 0, MINUTES_PER_DAY) }
        HoursStyle.BREAKFAST -> breakfast(random)
    }

    private fun standard(random: Random): List<OpeningHours> {
        val opens = random.oneOf(at(10), at(10, 30), at(11), at(11, 30), at(12))
        val closes = random.oneOf(at(21), at(21, 30), at(21, 45), at(22), at(22, 30), at(23))
        val closedDay = if (random.nextInt(100) < 20) random.oneOf(MONDAY, MONDAY, TUESDAY) else null
        return DayOfWeek.entries.filter { it != closedDay }.map { OpeningHours(it, opens, closes) }
    }

    private fun split(random: Random): List<OpeningHours> {
        val lunch = random.oneOf(at(11, 30), at(12)) to random.oneOf(at(14, 30), at(15))
        val dinner = random.oneOf(at(17, 30), at(18)) to random.oneOf(at(21, 30), at(22), at(22, 30))
        val closedDay = if (random.nextInt(100) < 20) MONDAY else null
        return DayOfWeek.entries.filter { it != closedDay }.flatMap { day ->
            listOf(OpeningHours(day, lunch.first, lunch.second), OpeningHours(day, dinner.first, dinner.second))
        }
    }

    private fun lateNight(random: Random): List<OpeningHours> {
        val opens = random.oneOf(at(11), at(12))
        val closes = random.oneOf(at(23), at(23, 30), at(24))
        // Closing at 01:00 on Sunday night is the Sunday-to-Monday wrap: it ends in the first hour of the week.
        val sundayCloses = if (random.nextBoolean()) at(1) else closes
        return DayOfWeek.entries.map { day ->
            when (day) {
                FRIDAY, SATURDAY -> OpeningHours(day, opens, at(2))
                SUNDAY -> OpeningHours(day, opens, sundayCloses)
                else -> OpeningHours(day, opens, closes)
            }
        }
    }

    private fun breakfast(random: Random): List<OpeningHours> {
        val opens = random.oneOf(at(6, 30), at(7), at(7), at(7, 30))
        val closes = random.oneOf(at(14), at(15), at(15), at(16))
        return DayOfWeek.entries.map { OpeningHours(it, opens, closes) }
    }
}

/**
 * Whether a window covers [weekMinute], where 0 is Monday 00:00 (spec §5.6's model). A window that runs past
 * midnight continues into the next day, and one that runs past Sunday midnight continues at the start of the week.
 * An entry that opens and closes at the same time is invalid (spec §5.1), so it's never open.
 */
internal fun List<OpeningHours>.isOpenAt(weekMinute: Int): Boolean = any { (day, opens, closes) ->
    val dayStart = day.ordinal * MINUTES_PER_DAY
    val start = dayStart + opens
    val end = dayStart + if (closes > opens) closes else closes + MINUTES_PER_DAY
    opens != closes && (weekMinute in start until end || weekMinute + MINUTES_PER_WEEK in start until end)
}

/** One of [options], each equally likely. List an option twice to make it twice as likely. */
internal fun <T> Random.oneOf(vararg options: T): T = options[nextInt(options.size)]
