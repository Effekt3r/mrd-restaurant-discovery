package com.effekt3r.mrd.tools.datagen

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import kotlin.random.Random
import kotlinx.serialization.json.JsonObject

/**
 * Generates the restaurants. Restaurant i draws from its own Random(SEED * 31 + i), so it depends only on the seed,
 * its index and the names before it: generating twice gives the same restaurants, and the first 220 of 10,000 are
 * exactly the 220 of the standard dataset. kotlin.random only promises the same sequence within one Kotlin version,
 * so a Kotlin upgrade may change the data. GoldenFilesTest then fails, and regenerating is the fix.
 */
internal object Generator {

    const val SEED = 20_261_118L

    /** Friday 19:00 in Cape Town: the moment `is_open` describes, like a server snapshot that's never refreshed. */
    val SNAPSHOT_WEEK_MINUTE = FRIDAY.ordinal * MINUTES_PER_DAY + at(19)

    // Spec §5.2's rates, in percent, each drawn per restaurant.
    private const val NO_RATING_PERCENT = 6
    private const val NO_FEE_PERCENT = 4
    private const val FREE_DELIVERY_PERCENT = 15
    private const val NO_ETA_PERCENT = 4
    private const val NO_IMAGE_PERCENT = 6

    // 5% have no hours: 4% leave the key out and 1% send [].
    private const val NO_HOURS_PERCENT = 5
    private const val EMPTY_HOURS_PERCENT = 1

    // Without hours, `is_open` is all there is.
    private const val OPEN_WITHOUT_HOURS_PERCENT = 70

    fun restaurants(count: Int, pool: ImagePool): List<JsonObject> {
        val names = NameBook(uniqueCount = Dataset.STANDARD.size)
        return List(count) { index -> EdgeCases.apply(index, restaurant(index, names, pool).toJson()) }
    }

    fun idFor(index: Int) = "r-${1001 + index}"

    private fun restaurant(index: Int, names: NameBook, pool: ImagePool): Restaurant {
        if (index == 0) return capeTownGrill(pool)
        val random = Random(SEED * 31 + index)

        // Edge cases keep every field, so each one has only the defect EdgeCases gives it.
        val complete = index in EdgeCases.indexes
        fun missing(percent: Int) = !complete && random.nextInt(100) < percent

        val pinned = EdgeCases.pinnedNames[index]
        val concept = pinned?.let { Concepts.named(it.conceptKey) } ?: Concepts.pick(random)
        val name = pinned?.name ?: names.next(index, concept, random)
        val menu = concept.menus.random(random)
        val rating = if (missing(NO_RATING_PERCENT)) null else (32..49).random(random) / 10.0
        // One draw sets the fee, so free delivery is 15% of all restaurants, not of those with a fee.
        val feeDraw = random.nextInt(100)
        val deliveryFeeCents = when {
            feeDraw < NO_FEE_PERCENT && !complete -> null
            feeDraw >= 100 - FREE_DELIVERY_PERCENT -> 0
            else -> concept.feesCents.random(random)
        }
        val etaMinutes = if (missing(NO_ETA_PERCENT)) null else concept.etaMinutes.random(random)
        val imageUrl = if (missing(NO_IMAGE_PERCENT)) null else pool.pick(menu.imageKey, random)
        val openingHours = when {
            !missing(NO_HOURS_PERCENT) -> Hours.generate(concept.hours.random(random), random)
            random.nextInt(NO_HOURS_PERCENT) < EMPTY_HOURS_PERCENT -> emptyList()
            else -> null
        }
        val isOpen = if (openingHours.isNullOrEmpty()) {
            random.nextInt(100) < OPEN_WITHOUT_HOURS_PERCENT
        } else {
            openingHours.isOpenAt(SNAPSHOT_WEEK_MINUTE)
        }
        return Restaurant(
            idFor(index), name, menu.cuisines, rating, deliveryFeeCents, etaMinutes, isOpen, imageUrl, openingHours
        )
    }

    /** r-1001 is the brief's example, which the app's placeholder already shows. */
    private fun capeTownGrill(pool: ImagePool): Restaurant {
        val hours = DayOfWeek.entries.map { day ->
            when (day) {
                FRIDAY, SATURDAY -> OpeningHours(day, at(11), at(2))
                SUNDAY -> OpeningHours(day, at(11), at(21))
                else -> OpeningHours(day, at(11), at(22))
            }
        }
        return Restaurant(
            id = idFor(0),
            name = "Cape Town Grill",
            cuisines = listOf("Steakhouse", "Grill"),
            rating = 4.6,
            deliveryFeeCents = 1500,
            etaMinutes = 32,
            isOpen = hours.isOpenAt(SNAPSHOT_WEEK_MINUTE),
            imageUrl = pool.first("beef"),
            openingHours = hours
        )
    }
}
