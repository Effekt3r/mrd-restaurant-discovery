package com.effekt3r.mrd.core.data

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.effekt3r.mrd.core.data.database.DiscoveryDatabase
import com.effekt3r.mrd.core.data.database.RestaurantDao
import com.effekt3r.mrd.core.data.network.ParsedRestaurant
import com.effekt3r.mrd.core.model.DailyWindow
import com.effekt3r.mrd.core.model.OpeningHours
import com.effekt3r.mrd.core.model.WeekMinute
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant

/** Room on Robolectric's SQLite, which runs the same framework code path as a device. */
internal fun inMemoryDatabase(): DiscoveryDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DiscoveryDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/** The stored restaurants' ids, in the list's order. */
internal suspend fun RestaurantDao.storedIds(): List<String> {
    val page = restaurants("", openNow = false, weekMinute = 0, now = 0)
        .load(PagingSource.LoadParams.Refresh(key = null, loadSize = 100, placeholdersEnabled = false))
    return (page as PagingSource.LoadResult.Page).data.map { it.id }
}

/** Monday 16 November 2026, 00:00 in Cape Town. */
internal val MONDAY_START: Instant = Instant.parse("2026-11-15T22:00:00Z")

internal fun WeekMinute.instant(): Instant = MONDAY_START + Duration.ofMinutes(value.toLong())

internal fun at(day: DayOfWeek, hour: Int, minute: Int = 0) = WeekMinute.of(day, hour, minute)

/** Every day of the week from [opens] to [closes] (minutes after midnight), except [closedOn]. */
internal fun daily(opens: Int, closes: Int, closedOn: DayOfWeek? = null) =
    DayOfWeek.entries.filter { it != closedOn }.map { DailyWindow(it, opens, closes) }

internal fun hm(hour: Int, minute: Int = 0) = hour * 60 + minute

internal fun parsedRestaurant(
    id: String,
    name: String = "Restaurant $id",
    windows: List<DailyWindow> = daily(hm(11), hm(22)),
    isOpen: Boolean? = true
) = ParsedRestaurant(
    id = id,
    name = name,
    cuisines = listOf("Grill"),
    rating = null,
    deliveryFeeCents = 1500,
    etaMinutes = 30,
    isOpen = isOpen,
    imageUrl = null,
    hours = OpeningHours.from(windows)
)
