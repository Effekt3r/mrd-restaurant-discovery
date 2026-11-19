package com.effekt3r.mrd.core.model

@JvmInline
value class RestaurantId(val value: String)

/** An amount in rand, as integer cents: never floating point, so amounts stay exact. */
@JvmInline
value class Money(val cents: Long)

/** A rating from 0.0 to 5.0, kept in tenths (4.6 is 46) so it's exact and always has one decimal place. */
@JvmInline
value class Rating(val tenths: Int) {
    init {
        require(tenths in 0..50) { "A rating is from 0 to 50 tenths, not $tenths" }
    }
}

/** A restaurant in the list. Null values are missing from the API (spec §5.1): the UI shows its fallback. */
data class RestaurantSummary(
    val id: RestaurantId,
    val name: String,
    val cuisines: List<String>,
    val rating: Rating?,
    val deliveryFee: Money?,
    val etaMinutes: Int?,
    val imageUrl: String?,
    val openStatus: OpenStatus,
    val isFavourite: Boolean
)

/** A restaurant on Details: the list's fields plus its opening hours, which are null when unknown. */
data class Restaurant(val summary: RestaurantSummary, val openingHours: OpeningHours?)
