package com.effekt3r.mrd.core.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.effekt3r.mrd.core.model.Money
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.OpeningHours
import com.effekt3r.mrd.core.model.Rating
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.WeekInterval
import com.effekt3r.mrd.core.model.WeekMinute
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Spec §6.6's formats, in en-ZA. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rZA")
class RestaurantFormatterTest {

    private val format = RestaurantFormatter(ApplicationProvider.getApplicationContext<Context>().resources)

    private fun at(day: java.time.DayOfWeek, hour: Int, minute: Int = 0) = WeekMinute.of(day, hour, minute)

    @Test
    fun moneyIsWholeRandsOrRandsAndCents() {
        assertEquals("R15", format.money(Money(1500)))
        assertEquals("R12.50", format.money(Money(1250)))
        assertEquals("R0.05", format.money(Money(5)))
        assertEquals("R0", format.money(Money(0)))
        assertEquals("R1 250", format.money(Money(125_000)))
        assertEquals("R12 345.60", format.money(Money(1_234_560)))
    }

    @Test
    fun aZeroDeliveryFeeIsFreeDelivery() {
        assertEquals("R15 delivery", format.deliveryFee(Money(1500)))
        assertEquals("R12.50 delivery", format.deliveryFee(Money(1250)))
        assertEquals("Free delivery", format.deliveryFee(Money(0)))
    }

    @Test
    fun etaIsInMins() {
        assertEquals("25 mins", format.eta(25))
        assertEquals("1 min", format.eta(1))
        assertEquals("32 minutes", format.etaSpoken(32))
    }

    @Test
    fun ratingHasOneDecimalPlace() {
        assertEquals("4.6", format.rating(Rating(46)))
        assertEquals("5.0", format.rating(Rating(50)))
        assertEquals("0.0", format.rating(Rating(0)))
    }

    @Test
    fun timesAreOnA24HourClock() {
        assertEquals("09:05", format.time(9 * 60 + 5))
        assertEquals("22:00", format.time(22 * 60))
        assertEquals("00:00", format.time(24 * 60)) // midnight, as a closing time
    }

    @Test
    fun theCardSaysOpenOrWhenAClosedPlaceOpens() {
        val now = at(MONDAY, 9)
        assertEquals("Open", format.status(OpenStatus.Open(closesAt = at(MONDAY, 22)), now))
        assertEquals("Closed · Opens 11:00", format.status(OpenStatus.Closed(opensAt = at(MONDAY, 11)), now))
        assertEquals("Closed · Opens tomorrow 11:00", format.status(OpenStatus.Closed(at(TUESDAY, 11)), now))
        assertEquals("Closed · Opens Thu 11:00", format.status(OpenStatus.Closed(at(THURSDAY, 11)), now))
        assertEquals("Closed", format.status(OpenStatus.Closed(opensAt = null), now))
        assertEquals("Hours unknown", format.status(OpenStatus.Unknown, now))
    }

    @Test
    fun theNextChangeNamesTheDayWhenItIsNotToday() {
        val friday = at(FRIDAY, 23)
        assertEquals("Opens 11:00", format.nextChange(OpenStatus.Closed(at(FRIDAY, 11)), at(FRIDAY, 9)))
        assertEquals("Opens tomorrow 11:00", format.nextChange(OpenStatus.Closed(at(SATURDAY, 11)), friday))
        assertEquals("Opens Mon 11:00", format.nextChange(OpenStatus.Closed(at(MONDAY, 11)), friday))
        // The week wraps: from Sunday night, Monday morning is tomorrow.
        assertEquals("Opens tomorrow 07:30", format.nextChange(OpenStatus.Closed(at(MONDAY, 7, 30)), at(SUNDAY, 22)))
        // The same weekday a week later isn't today.
        assertEquals("Opens Mon 11:00", format.nextChange(OpenStatus.Closed(at(MONDAY, 11)), at(MONDAY, 12)))
        // Closing within 24 hours needs no day, even past midnight.
        assertEquals("Closes 22:00", format.nextChange(OpenStatus.Open(at(FRIDAY, 22)), at(FRIDAY, 12)))
        assertEquals("Closes 02:00", format.nextChange(OpenStatus.Open(at(SATURDAY, 2)), friday))
        assertEquals("Closes Sun 21:00", format.nextChange(OpenStatus.Open(at(SUNDAY, 21)), friday))
        assertNull(format.nextChange(OpenStatus.Open(closesAt = null), friday))
        assertNull(format.nextChange(OpenStatus.Unknown, friday))
    }

    @Test
    fun detailsStatusLineIncludesTheNextChange() {
        val now = at(MONDAY, 12)
        val hours = OpeningHours(listOf(WeekInterval(0, 10_080)))
        assertEquals("Open · Closes 22:00", format.statusLine(OpenStatus.Open(at(MONDAY, 22)), now, hours = null))
        assertEquals("Open 24 hours", format.statusLine(OpenStatus.Open(closesAt = null), now, hours))
        assertEquals("Open", format.statusLine(OpenStatus.Open(closesAt = null), now, hours = null))
        assertEquals("Closed · Opens tomorrow 11:00", format.statusLine(OpenStatus.Closed(at(TUESDAY, 11)), now, null))
        assertEquals("Hours unknown", format.statusLine(OpenStatus.Unknown, now, null))
    }

    @Test
    fun cuisinesAreJoinedWithADot() {
        assertEquals("Steakhouse · Grill", format.cuisines(listOf("Steakhouse", "Grill")))
        assertEquals("", format.cuisines(emptyList()))
    }

    @Test
    fun talkBackReadsTheWholeCardInSpecOrder() {
        val grill = summary(rating = Rating(46), fee = Money(1500), eta = 32, status = OpenStatus.Open(at(MONDAY, 22)))
        assertEquals(
            "Cape Town Grill. Steakhouse, Grill. Rated 4.6. 32 minutes. R15 delivery. Open, closes 22:00.",
            format.description(grill, at(MONDAY, 12))
        )
        val sparse = summary(rating = null, fee = null, eta = null, status = OpenStatus.Closed(at(TUESDAY, 7, 30)))
        assertEquals(
            "Cape Town Grill. Steakhouse, Grill. New, not rated yet. Closed, opens tomorrow 07:30.",
            format.description(sparse, at(MONDAY, 23))
        )
    }

    @Test
    fun aFetchTimeNamesItsDayOnceItIsNotToday() {
        val zone = ZoneId.of("Africa/Johannesburg")
        val now = Instant.parse("2026-11-18T12:00:00Z") // Wednesday 14:00 in Cape Town
        assertEquals("10:42", format.fetchedAt(Instant.parse("2026-11-18T08:42:00Z"), now, zone))
        assertEquals("Mon 10:42", format.fetchedAt(Instant.parse("2026-11-16T08:42:00Z"), now, zone))
        assertEquals("8 Nov 10:42", format.fetchedAt(Instant.parse("2026-11-08T08:42:00Z"), now, zone))
        // A fetch dated tomorrow means the clock was set back: it's shown as a time, not a date.
        assertEquals("10:00", format.fetchedAt(Instant.parse("2026-11-19T08:00:00Z"), now, zone))
    }

    private fun summary(rating: Rating?, fee: Money?, eta: Int?, status: OpenStatus) = RestaurantSummary(
        id = RestaurantId("r-1001"),
        name = "Cape Town Grill",
        cuisines = listOf("Steakhouse", "Grill"),
        rating = rating,
        deliveryFee = fee,
        etaMinutes = eta,
        imageUrl = null,
        openStatus = status,
        isFavourite = false
    )
}
