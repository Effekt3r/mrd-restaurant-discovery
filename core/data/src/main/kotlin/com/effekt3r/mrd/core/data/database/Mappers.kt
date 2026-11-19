package com.effekt3r.mrd.core.data.database

import com.effekt3r.mrd.core.data.network.ParsedRestaurant
import com.effekt3r.mrd.core.model.Money
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.OpeningHours
import com.effekt3r.mrd.core.model.Rating
import com.effekt3r.mrd.core.model.Restaurant
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.WeekInterval
import com.effekt3r.mrd.core.model.WeekMinute
import com.effekt3r.mrd.core.model.openStatus
import java.time.Instant

internal fun ParsedRestaurant.toEntity(position: Int) = RestaurantEntity(
    id = id,
    name = name,
    nameSearch = searchKey(name),
    cuisines = cuisines,
    ratingTenths = rating?.tenths,
    deliveryFeeCents = deliveryFeeCents,
    etaMinutes = etaMinutes,
    isOpenSnapshot = isOpen,
    imageUrl = imageUrl,
    hasHours = hours != null,
    position = position
)

internal fun ParsedRestaurant.intervalEntities() =
    hours?.intervals.orEmpty().map { OpeningIntervalEntity(id, it.start, it.end) }

internal fun RestaurantRow.toModel() = RestaurantSummary(
    id = RestaurantId(id),
    name = name,
    cuisines = cuisines,
    rating = ratingTenths?.let(::Rating),
    deliveryFee = deliveryFeeCents?.let(::Money),
    etaMinutes = etaMinutes,
    imageUrl = imageUrl,
    openStatus = when (status) {
        "OPEN" -> OpenStatus.Open(closesAt = nextChange?.let(::WeekMinute))
        "CLOSED" -> OpenStatus.Closed(opensAt = nextChange?.let(::WeekMinute))
        else -> OpenStatus.Unknown
    },
    isFavourite = isFavourite
)

/** Details' status comes from the same rules in Kotlin (spec §5.6). */
internal fun RestaurantWithHours.toModel(snapshotAt: Instant?, now: Instant): Restaurant {
    val hours = intervals.sortedBy { it.startMinute }
        .map { WeekInterval(it.startMinute, it.endMinute) }
        .takeIf { it.isNotEmpty() }
        ?.let(::OpeningHours)
    return Restaurant(
        summary = RestaurantSummary(
            id = RestaurantId(restaurant.id),
            name = restaurant.name,
            cuisines = restaurant.cuisines,
            rating = restaurant.ratingTenths?.let(::Rating),
            deliveryFee = restaurant.deliveryFeeCents?.let(::Money),
            etaMinutes = restaurant.etaMinutes,
            imageUrl = restaurant.imageUrl,
            openStatus = openStatus(hours, restaurant.isOpenSnapshot, snapshotAt, now),
            isFavourite = isFavourite
        ),
        openingHours = hours
    )
}
