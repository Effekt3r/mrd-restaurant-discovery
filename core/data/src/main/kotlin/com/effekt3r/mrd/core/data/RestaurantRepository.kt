package com.effekt3r.mrd.core.data

import androidx.paging.PagingData
import com.effekt3r.mrd.core.model.Restaurant
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.SyncStatus
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** The restaurants of one delivery area, from the Room cache: the single source of truth (spec §5.3). */
interface RestaurantRepository {
    /**
     * The restaurants whose names contain [query] (ignoring case and accents), only those open now when [openNow], in
     * the server's order. The list reloads in place on each of [ticks] that falls in a later minute than the one it was
     * loaded for, so statuses follow the clock (spec §5.6).
     *
     * The caller supplies the ticks because a screen caches this flow with `cachedIn`, which keeps it collected for as
     * long as the ViewModel lives, in the background too. Pass ticks that flow only while the screen shows the list.
     */
    fun restaurants(query: String, openNow: Boolean, ticks: Flow<Instant>): Flow<PagingData<RestaurantSummary>>

    /** How many restaurants [restaurants] lists, updated every minute. */
    fun count(query: String, openNow: Boolean): Flow<Int>

    /** The restaurant with [id], with its status updated every minute; null while it isn't cached. */
    fun restaurant(id: RestaurantId): Flow<Restaurant?>

    /**
     * The snapshot's time, whether a refresh is running and how the last one failed. While it's collected, a failed
     * refresh is retried when the connection comes back (spec §5.7).
     */
    val syncStatus: Flow<SyncStatus>

    /** Fetches a new snapshot; overlapping calls make one request. Failures arrive through [syncStatus], not as exceptions. */
    suspend fun refresh()

    /** Refreshes when there's no snapshot yet or it's at least 5 minutes old, as when Browse becomes visible. */
    suspend fun refreshIfStale()

    suspend fun toggleFavourite(id: RestaurantId)
}
