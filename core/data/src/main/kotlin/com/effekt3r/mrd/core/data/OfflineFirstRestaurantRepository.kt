package com.effekt3r.mrd.core.data

import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.effekt3r.mrd.core.common.MinuteTicker
import com.effekt3r.mrd.core.common.NetworkMonitor
import com.effekt3r.mrd.core.data.database.RestaurantDao
import com.effekt3r.mrd.core.data.database.RestaurantRow
import com.effekt3r.mrd.core.data.database.likePattern
import com.effekt3r.mrd.core.data.database.toModel
import com.effekt3r.mrd.core.data.sync.RestaurantSync
import com.effekt3r.mrd.core.model.Restaurant
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.SyncStatus
import com.effekt3r.mrd.core.model.WeekMinute
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

internal class OfflineFirstRestaurantRepository @Inject constructor(
    private val dao: RestaurantDao,
    private val sync: RestaurantSync,
    private val ticker: MinuteTicker,
    private val networkMonitor: NetworkMonitor,
    private val clock: Clock
) : RestaurantRepository {

    override fun restaurants(
        query: String,
        openNow: Boolean,
        ticks: Flow<Instant>
    ): Flow<PagingData<RestaurantSummary>> = channelFlow {
        val pattern = likePattern(query)
        // The minute the current PagingSource worked its statuses out for. The factory runs on Paging's coroutine and
        // the ticks on another, so it's an atomic reference.
        val loadedFor = AtomicReference<Instant>()
        val sources = InvalidatingPagingSourceFactory {
            val now = clock.instant()
            loadedFor.set(now.truncatedTo(ChronoUnit.MINUTES))
            dao.restaurants(pattern, openNow, WeekMinute.at(now).value, now.toEpochMilli())
        }
        // A tick in a later minute invalidates the PagingSource instead of building a new Pager, so Paging's refresh key
        // keeps the scroll position when a status flips (spec §5.6). A tick in the minute the list was loaded for, such
        // as the one a screen sends as it becomes visible, changes nothing.
        launch {
            ticks.collect { tick -> if (tick.truncatedTo(ChronoUnit.MINUTES) != loadedFor.get()) sources.invalidate() }
        }
        Pager(PagingConfig(pageSize = PAGE_SIZE), pagingSourceFactory = sources).flow
            .collect { send(it.map(RestaurantRow::toModel)) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun count(query: String, openNow: Boolean): Flow<Int> {
        val pattern = likePattern(query)
        return ticker.ticks()
            .flatMapLatest { now -> dao.count(pattern, openNow, WeekMinute.at(now).value, now.toEpochMilli()) }
            .distinctUntilChanged()
    }

    override fun restaurant(id: RestaurantId): Flow<Restaurant?> =
        combine(dao.restaurant(id.value), dao.lastSyncedAt(), ticker.ticks()) { row, syncedAt, now ->
            row?.toModel(snapshotAt = syncedAt?.let(Instant::ofEpochMilli), now = now)
        }.distinctUntilChanged()

    override val syncStatus: Flow<SyncStatus> = channelFlow {
        launch {
            // Only a change back to online counts, never the first value.
            networkMonitor.isOnline.distinctUntilChanged().drop(1).filter { it }.collect {
                if (sync.state.value.lastError != null) sync.refresh()
            }
        }
        combine(dao.lastSyncedAt(), sync.state) { syncedAt, state ->
            SyncStatus(syncedAt?.let(Instant::ofEpochMilli), state.isRefreshing, state.lastError)
        }.collect { send(it) }
    }

    override suspend fun refresh() = sync.refresh()

    override suspend fun refreshIfStale() = sync.refreshIfStale()

    override suspend fun toggleFavourite(id: RestaurantId) = dao.toggleFavourite(id.value, clock.millis())

    private companion object {
        const val PAGE_SIZE = 30
    }
}
