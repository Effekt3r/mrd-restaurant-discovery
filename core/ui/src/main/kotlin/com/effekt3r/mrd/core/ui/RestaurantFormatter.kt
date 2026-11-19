package com.effekt3r.mrd.core.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import com.effekt3r.mrd.core.designsystem.component.MrdStatusTone
import com.effekt3r.mrd.core.model.MINUTES_PER_DAY
import com.effekt3r.mrd.core.model.MINUTES_PER_WEEK
import com.effekt3r.mrd.core.model.Money
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.OpeningHours
import com.effekt3r.mrd.core.model.Rating
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.WeekMinute
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Display text for restaurants and menus, in en-ZA (spec §6.6): rand from integer cents, "25 mins", "4.6", times on a
 * 24-hour clock, and the open status with its next change. The words come from string resources and the day names
 * from [locale]. Times of day are the restaurant's own (Cape Town, as [WeekMinute] keeps them).
 */
class RestaurantFormatter(
    private val resources: Resources,
    private val locale: Locale = resources.configuration.locales[0]
) {

    /** "R15", "R12.50", or "R1 250" with a non-breaking space between thousands. Whole rands show no cents. */
    fun money(amount: Money): String {
        val rands = amount.cents / 100
        val cents = amount.cents % 100
        val grouped = rands.toString().reversed().chunked(3).joinToString(NO_BREAK_SPACE).reversed()
        val number = if (cents == 0L) grouped else "$grouped.${cents.toString().padStart(2, '0')}"
        return resources.getString(R.string.ui_rand, number)
    }

    /** A card's fee: "R15 delivery", or "Free delivery" when it's zero. */
    fun deliveryFee(fee: Money): String = if (fee.cents == 0L) {
        resources.getString(R.string.ui_free_delivery)
    } else {
        resources.getString(R.string.ui_delivery_fee, money(fee))
    }

    /** "25 mins", Mr D's format. */
    fun eta(minutes: Int): String = resources.getQuantityString(R.plurals.ui_eta, minutes, minutes)

    /** "25 minutes", for TalkBack. */
    fun etaSpoken(minutes: Int): String = resources.getQuantityString(R.plurals.ui_eta_spoken, minutes, minutes)

    /** One decimal place: "4.6", "5.0". */
    fun rating(rating: Rating): String = "${rating.tenths / 10}.${rating.tenths % 10}"

    /** "22:00", from minutes after midnight. 1,440, a closing time of midnight, is "00:00". */
    fun time(minuteOfDay: Int): String {
        val minute = minuteOfDay % MINUTES_PER_DAY
        return "%02d:%02d".format(Locale.ROOT, minute / 60, minute % 60)
    }

    /** "Steakhouse · Grill". */
    fun cuisines(cuisines: List<String>): String =
        cuisines.joinToString(resources.getString(R.string.ui_cuisine_separator))

    /** A card's status: "Open", "Closed · Opens 11:00", "Closed" (no hours) or "Hours unknown". */
    fun status(status: OpenStatus, now: WeekMinute): String = when (status) {
        is OpenStatus.Open -> resources.getString(R.string.ui_status_open)
        is OpenStatus.Closed -> withChange(R.string.ui_status_closed, nextChange(status, now))
        OpenStatus.Unknown -> resources.getString(R.string.ui_status_unknown)
    }

    /**
     * Details' status line, with the next change either way: "Open · Closes 22:00", "Closed · Opens tomorrow 11:00".
     * A restaurant open around the clock, whose [hours] are known, is "Open 24 hours".
     */
    fun statusLine(status: OpenStatus, now: WeekMinute, hours: OpeningHours?): String = when {
        status is OpenStatus.Open && status.closesAt == null && hours != null ->
            resources.getString(R.string.ui_status_open_24_hours)

        status is OpenStatus.Open -> withChange(R.string.ui_status_open, nextChange(status, now))

        else -> status(status, now)
    }

    /**
     * When [status] next changes, from [now]: "Opens 11:00" later today, "Opens tomorrow 11:00", "Opens Mon 11:00",
     * and "Closes 22:00" within a day, past midnight too, or "Closes Sun 21:00". Null when it never changes, or the
     * hours aren't known.
     */
    fun nextChange(status: OpenStatus, now: WeekMinute): String? = when (status) {
        is OpenStatus.Open -> status.closesAt?.let { closesAt ->
            if (minutesFrom(now, closesAt) < MINUTES_PER_DAY) {
                resources.getString(R.string.ui_closes_today, time(closesAt.minuteOfDay))
            } else {
                resources.getString(R.string.ui_closes_on, dayName(closesAt.day), time(closesAt.minuteOfDay))
            }
        }

        is OpenStatus.Closed -> status.opensAt?.let { opensAt ->
            val days = (opensAt.day.ordinal - now.day.ordinal).mod(7)
            val opens = time(opensAt.minuteOfDay)
            when {
                days == 0 && minutesFrom(now, opensAt) < MINUTES_PER_DAY ->
                    resources.getString(R.string.ui_opens_today, opens)

                days == 1 -> resources.getString(R.string.ui_opens_tomorrow, opens)

                else -> resources.getString(R.string.ui_opens_on, dayName(opensAt.day), opens)
            }
        }

        OpenStatus.Unknown -> null
    }

    /**
     * What TalkBack reads for a card (spec §6.5): "Cape Town Grill. Steakhouse, Grill. Rated 4.6. 32 minutes.
     * R15 delivery. Open, closes 22:00." Missing values are left out, except the rating, which is "New".
     */
    fun description(restaurant: RestaurantSummary, now: WeekMinute): String = listOfNotNull(
        restaurant.name,
        restaurant.cuisines.takeIf { it.isNotEmpty() }
            ?.joinToString(resources.getString(R.string.ui_spoken_list_separator)),
        restaurant.rating?.let { resources.getString(R.string.ui_spoken_rated, rating(it)) }
            ?: resources.getString(R.string.ui_spoken_not_rated),
        restaurant.etaMinutes?.let(::etaSpoken),
        restaurant.deliveryFee?.let(::deliveryFee),
        spokenStatus(restaurant.openStatus, now)
    ).joinToString(
        separator = resources.getString(R.string.ui_spoken_sentence_separator),
        postfix = resources.getString(R.string.ui_spoken_sentence_end)
    )

    /**
     * When a snapshot or a menu was fetched, in [zone]: "10:42" today, "Mon 10:42" within the last week, else
     * "8 Nov 10:42". A time stamped after today (the clock was set back) shows as a time.
     */
    fun fetchedAt(at: Instant, now: Instant, zone: ZoneId): String {
        val fetched = at.atZone(zone)
        val time = time(fetched.hour * 60 + fetched.minute)
        val days = ChronoUnit.DAYS.between(fetched.toLocalDate(), now.atZone(zone).toLocalDate())
        return when {
            days <= 0 -> time

            days < 7 -> resources.getString(R.string.ui_day_and_time, dayName(fetched.dayOfWeek), time)

            else -> resources.getString(
                R.string.ui_day_and_time,
                DateTimeFormatter.ofPattern("d MMM", locale).format(fetched),
                time
            )
        }
    }

    private fun spokenStatus(status: OpenStatus, now: WeekMinute): String {
        val state = when (status) {
            is OpenStatus.Open -> resources.getString(R.string.ui_status_open)
            is OpenStatus.Closed -> resources.getString(R.string.ui_status_closed)
            OpenStatus.Unknown -> return resources.getString(R.string.ui_status_unknown)
        }
        val change = nextChange(status, now)?.replaceFirstChar { it.lowercase(locale) } ?: return state
        return resources.getString(R.string.ui_spoken_status_with_change, state, change)
    }

    private fun withChange(state: Int, change: String?): String =
        change?.let { resources.getString(R.string.ui_status_with_change, resources.getString(state), it) }
            ?: resources.getString(state)

    private fun dayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, locale)

    // How far ahead [to] is, wrapping round the end of the week.
    private fun minutesFrom(now: WeekMinute, to: WeekMinute): Int = (to.value - now.value).mod(MINUTES_PER_WEEK)

    private companion object {
        // Grouping thousands with a space that never breaks, so "R1 250" stays on one line.
        const val NO_BREAK_SPACE = " "
    }
}

/** How a status reads: green when open, red when closed, muted when unknown. */
val OpenStatus.tone: MrdStatusTone
    get() = when (this) {
        is OpenStatus.Open -> MrdStatusTone.Open
        is OpenStatus.Closed -> MrdStatusTone.Closed
        OpenStatus.Unknown -> MrdStatusTone.Unknown
    }

/** A formatter for the current configuration's resources and locale. */
@Composable
fun rememberRestaurantFormatter(): RestaurantFormatter {
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current
    return remember(resources, configuration) { RestaurantFormatter(resources) }
}
