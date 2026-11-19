package com.effekt3r.mrd.core.testing

import androidx.paging.PagingData
import com.effekt3r.mrd.core.data.RestaurantRepository
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.Restaurant
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** An in-memory repository for ViewModel tests: set [restaurants] and [status], then read the call counts. */
class FakeRestaurantRepository : RestaurantRepository {
    val restaurants = MutableStateFlow<List<Restaurant>>(emptyList())
    val status = MutableStateFlow(SyncStatus(lastSyncedAt = null, isRefreshing = false, lastError = null))
    var refreshCalls = 0
        private set
    var refreshIfStaleCalls = 0
        private set

    override fun restaurants(query: String, openNow: Boolean): Flow<PagingData<RestaurantSummary>> =
        matching(query, openNow).map { PagingData.from(it) }

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

    private fun matching(query: String, openNow: Boolean) = restaurants.map { all ->
        all.map { it.summary }.filter {
            it.name.contains(query.trim(), ignoreCase = true) && (!openNow || it.openStatus is OpenStatus.Open)
        }
    }
}
