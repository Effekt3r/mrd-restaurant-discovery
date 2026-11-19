package com.effekt3r.mrd.core.data.sync

import com.effekt3r.mrd.core.common.ApplicationScope
import com.effekt3r.mrd.core.common.IoDispatcher
import com.effekt3r.mrd.core.data.database.RestaurantDao
import com.effekt3r.mrd.core.data.database.intervalEntities
import com.effekt3r.mrd.core.data.database.toEntity
import com.effekt3r.mrd.core.data.network.DiscoveryApi
import com.effekt3r.mrd.core.data.network.RestaurantsPayloadReader
import com.effekt3r.mrd.core.model.SyncError
import java.io.IOException
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

/** Whether a refresh is running, and how the last one failed (null: it worked, or none has run yet). */
internal data class RefreshState(val isRefreshing: Boolean = false, val lastError: SyncError? = null)

/** Fetches the restaurant snapshot into Room (spec §5.3). */
@Singleton
internal class RestaurantSync @Inject constructor(
    private val api: DiscoveryApi,
    private val dao: RestaurantDao,
    private val json: Json,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val mutableState = MutableStateFlow(RefreshState())
    val state: StateFlow<RefreshState> = mutableState.asStateFlow()

    private val lock = Mutex()
    private var running: Deferred<Unit>? = null

    /**
     * Fetches a new snapshot. A call while a refresh runs joins it, so overlapping refreshes make one request. The
     * refresh runs in the application scope, so a caller that goes away doesn't cancel it for the others.
     */
    suspend fun refresh() {
        val refresh = lock.withLock {
            running?.takeIf { it.isActive } ?: scope.async { run() }.also { running = it }
        }
        refresh.await()
    }

    /** Refreshes when there's no snapshot yet, or it's at least 5 minutes old. */
    suspend fun refreshIfStale() {
        val age = dao.lastSyncedAt().first()?.let { clock.millis() - it }
        if (age == null || age !in 0 until STALE_AFTER_MILLIS) refresh()
    }

    private suspend fun run() {
        mutableState.update { it.copy(isRefreshing = true) }
        try {
            // Every failure becomes a SyncError here, exactly once (spec §5.4).
            val error = try {
                download()
            } catch (e: CancellationException) {
                throw e // not a failure: the application scope is shutting down
            } catch (e: IOException) {
                SyncError.Network
            } catch (e: IllegalArgumentException) {
                SyncError.BadData // malformed JSON: SerializationException is an IllegalArgumentException
            } catch (e: Exception) {
                // The network fails with IOException and the parser with IllegalArgumentException, so what's left is
                // the database: a full disk, an I/O error or a corrupt file (SQLiteException), or a closed database.
                SyncError.Storage
            }
            mutableState.value = RefreshState(isRefreshing = false, lastError = error)
        } finally {
            // Cancelled, the refresh is still over; the last error stands.
            mutableState.update { it.copy(isRefreshing = false) }
        }
    }

    // decodeFromStream parses the body record by record, never as a JSON tree; Retrofit has already read the body into
    // memory (spec §5.3). decodeFromStream has been experimental since 1.3 but is stable in practice.
    @OptIn(ExperimentalSerializationApi::class)
    /** Null when the snapshot was stored or confirmed unchanged; otherwise why it wasn't. */
    private suspend fun download(): SyncError? {
        val response = api.restaurants()
        if (!response.isSuccessful) {
            response.errorBody()?.close()
            return SyncError.Server(response.code())
        }
        val body = response.body() ?: return SyncError.BadData
        return body.use {
            val now = clock.millis()
            val etag = response.headers()["ETag"]
            // OkHttp answers a 304 with its cached copy; the network response keeps the real code. A 304 proves only
            // that the copy is current, not that the database stored it: a refresh can fail after OkHttp caches the
            // body (bad data, a failed write). So the write is skipped only when the database holds the snapshot with
            // that ETag and isn't empty; otherwise the cached copy is processed like a 200 (spec §5.3).
            val notModified = response.raw().networkResponse?.code == HTTP_NOT_MODIFIED
            if (notModified && etag != null && etag == dao.snapshotEtag() && dao.hasRestaurants()) {
                dao.setLastSyncedAt(now)
                return@use null
            }
            val payload = withContext(ioDispatcher) { json.decodeFromStream(RestaurantsPayloadReader, it.byteStream()) }
            // Records that all failed validation are bad data, and never replace the snapshot.
            if (payload.recordCount > 0 && payload.restaurants.isEmpty()) return@use SyncError.BadData
            dao.replaceSnapshot(
                restaurants = payload.restaurants.mapIndexed { position, restaurant -> restaurant.toEntity(position) },
                intervals = payload.restaurants.flatMap { it.intervalEntities() },
                syncedAt = now,
                etag = etag
            )
            null
        }
    }

    private companion object {
        const val STALE_AFTER_MILLIS = 5 * 60 * 1000L
    }
}
