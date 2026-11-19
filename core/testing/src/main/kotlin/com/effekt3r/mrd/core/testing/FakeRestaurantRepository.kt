package com.effekt3r.mrd.core.testing

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.effekt3r.mrd.core.data.RestaurantRepository
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.Restaurant
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.SyncStatus
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * An in-memory repository for ViewModel tests: set [restaurants] and [status], then read what was asked of it: the
 * paged lists' filters, the ticks they received, and the call counts.
 */
class FakeRestaurantRepository : RestaurantRepository {
    val restaurants = MutableStateFlow<List<Restaurant>>(emptyList())
    val status = MutableStateFlow(SyncStatus(lastSyncedAt = null, isRefreshing = false, lastError = null))

    /** The query and Open now of each paged list collected, in order. */
    val listRequests = mutableListOf<Pair<String, Boolean>>()

    /** Every tick a paged list received while it was collected: what would reload it in place. */
    val listTicks = mutableListOf<Instant>()
    var refreshCalls = 0
        private set
    var refreshIfStaleCalls = 0
        private set

    override fun restaurants(
        query: String,
        openNow: Boolean,
        ticks: Flow<Instant>
    ): Flow<PagingData<RestaurantSummary>> = channelFlow {
        listRequests += query to openNow
        launch { ticks.collect { listTicks += it } }
        // Each page says it has finished loading, as a Pager's does, so paging-testing's asSnapshot() can read it.
        matching(query, openNow).collect { send(PagingData.from(it, LOADED)) }
    }

    override fun count(query: String, openNow: Boolean): Flow<Int> = matching(query, openNow).map { it.size }

    override fun restaurant(id: RestaurantId): Flow<Restaurant?> =
        restaurants.map { all -> all.firstOrNull { it.summary.id == id } }

    override val syncStatus: Flow<SyncStatus> = status

    override suspend fun refresh() {
        refreshCalls++
    }

    override suspend fun refreshIfStale() {
        refreshIfStaleCalls++
    }

    override suspend fun toggleFavourite(id: RestaurantId) = restaurants.update { all ->
        all.map {
            if (it.summary.id != id) it else it.copy(summary = it.summary.copy(isFavourite = !it.summary.isFavourite))
        }
    }

    private companion object {
        val LOADED = LoadStates(
            refresh = LoadState.NotLoading(endOfPaginationReached = false),
            prepend = LoadState.NotLoading(endOfPaginationReached = true),
            append = LoadState.NotLoading(endOfPaginationReached = true)
        )
    }

    private fun matching(query: String, openNow: Boolean) = restaurants.map { all ->
        all.map { it.summary }.filter {
            it.name.contains(query.trim(), ignoreCase = true) && (!openNow || it.openStatus is OpenStatus.Open)
        }
    }
}
