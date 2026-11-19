package com.effekt3r.mrd.core.data.sync

import android.database.sqlite.SQLiteFullException
import com.effekt3r.mrd.core.data.database.OpeningIntervalEntity
import com.effekt3r.mrd.core.data.database.RestaurantDao
import com.effekt3r.mrd.core.data.database.RestaurantEntity
import com.effekt3r.mrd.core.data.database.intervalEntities
import com.effekt3r.mrd.core.data.database.toEntity
import com.effekt3r.mrd.core.data.inMemoryDatabase
import com.effekt3r.mrd.core.data.notModified
import com.effekt3r.mrd.core.data.ok
import com.effekt3r.mrd.core.data.parsedRestaurant
import com.effekt3r.mrd.core.data.payload
import com.effekt3r.mrd.core.data.status
import com.effekt3r.mrd.core.data.storedIds
import com.effekt3r.mrd.core.data.testApi
import com.effekt3r.mrd.core.data.testJson
import com.effekt3r.mrd.core.model.SyncError
import com.effekt3r.mrd.core.testing.TestClock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RestaurantSyncTest {

    @get:Rule
    val cacheDir = TemporaryFolder()

    private val server = MockWebServer().apply { start() }
    private val database = inMemoryDatabase()
    private val dao = database.restaurantDao()
    private val clock = TestClock(Instant.parse("2026-11-16T10:00:00Z"))

    @After
    fun tearDown() {
        server.close()
        database.close()
    }

    private fun TestScope.sync(
        readTimeoutMillis: Long = 5_000,
        dao: RestaurantDao = this@RestaurantSyncTest.dao,
        scope: CoroutineScope = backgroundScope
    ) = RestaurantSync(
        api = testApi(server, cacheDir.root, readTimeoutMillis),
        dao = dao,
        json = testJson,
        clock = clock,
        ioDispatcher = Dispatchers.IO,
        scope = scope
    )

    // The real DAO, except that the snapshot swap runs [swap] instead.
    private fun swapping(swap: suspend () -> Unit): RestaurantDao = object : RestaurantDao by dao {
        override suspend fun replaceSnapshot(
            restaurants: List<RestaurantEntity>,
            intervals: List<OpeningIntervalEntity>,
            syncedAt: Long,
            etag: String?
        ) = swap()
    }

    // A snapshot written straight to the database, as if an earlier refresh had stored it from the response tagged
    // [etag]. Null: a snapshot of unknown version.
    private suspend fun seed(vararg ids: String, etag: String? = null) {
        val restaurants = ids.map { parsedRestaurant(it) }
        dao.replaceSnapshot(
            restaurants.mapIndexed { position, it -> it.toEntity(position) },
            restaurants.flatMap { it.intervalEntities() },
            syncedAt = 1,
            etag = etag
        )
    }

    @Test
    fun a200StoresTheSnapshotInTheServersOrder() = runTest {
        server.enqueue(ok(payload("r-2", "r-1")))
        val sync = sync()
        sync.refresh()
        assertEquals(listOf("r-2", "r-1"), dao.storedIds())
        assertEquals(clock.millis(), dao.lastSyncedAt().first())
        assertEquals(RefreshState(isRefreshing = false, lastError = null), sync.state.value)
    }

    @Test
    fun a304SkipsTheDatabaseWriteAndOnlyRecordsTheTime() = runTest {
        server.enqueue(ok(payload("r-1")))
        server.enqueue(notModified())
        val sync = sync()
        sync.refresh()
        seed("r-local", etag = "\"v1\"") // the database holds v1; anything the 304 rewrote would replace this
        clock.advanceBy(Duration.ofMinutes(10))
        sync.refresh()
        server.takeRequest()
        assertEquals("\"v1\"", server.takeRequest().headers["If-None-Match"])
        assertEquals(listOf("r-local"), dao.storedIds())
        assertEquals(clock.millis(), dao.lastSyncedAt().first())
        assertNull(sync.state.value.lastError)
    }

    @Test
    fun a304ForAVersionTheDatabaseDoesntHoldStoresTheCachedBody() = runTest {
        server.enqueue(ok(payload("r-1")))
        server.enqueue(notModified())
        val sync = sync()
        sync.refresh()
        seed("r-local") // an older snapshot, as if the swap of r-1 had never committed
        clock.advanceBy(Duration.ofMinutes(10))
        sync.refresh()
        assertEquals(listOf("r-1"), dao.storedIds())
        assertEquals(clock.millis(), dao.lastSyncedAt().first())
        assertNull(sync.state.value.lastError)
    }

    @Test
    fun a304WithoutAnETagStoresTheCachedBody() = runTest {
        // Without an ETag, OkHttp revalidates with Last-Modified, and the 304 can't name the version it confirms.
        server.enqueue(
            ok(payload("r-1")).newBuilder()
                .removeHeader("ETag")
                .addHeader("Last-Modified", "Mon, 16 Nov 2026 09:00:00 GMT")
                .build()
        )
        server.enqueue(status(304))
        val sync = sync()
        sync.refresh()
        seed("r-local") // also without an ETag
        clock.advanceBy(Duration.ofMinutes(10))
        sync.refresh()
        server.takeRequest()
        assertEquals("Mon, 16 Nov 2026 09:00:00 GMT", server.takeRequest().headers["If-Modified-Since"])
        assertEquals(listOf("r-1"), dao.storedIds())
        assertNull(sync.state.value.lastError)
    }

    @Test
    fun a304AfterBadDataNeitherClearsTheErrorNorMarksTheOldSnapshotFresh() = runTest {
        seed("r-1")
        server.enqueue(ok("""{"restaurants": [{"name": "No id"}]}""", etag = "\"v2\""))
        server.enqueue(notModified("\"v2\""))
        val sync = sync()
        sync.refresh() // BadData keeps the snapshot, but OkHttp has cached the bad body with its ETag
        clock.advanceBy(Duration.ofMinutes(10))
        sync.refresh()
        server.takeRequest()
        assertEquals("\"v2\"", server.takeRequest().headers["If-None-Match"])
        assertEquals(SyncError.BadData, sync.state.value.lastError)
        assertEquals(1L, dao.lastSyncedAt().first())
        assertEquals(listOf("r-1"), dao.storedIds())
    }

    @Test
    fun aTimeoutIsANetworkErrorAndKeepsTheSnapshot() = runTest {
        seed("r-1")
        server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.Stall).build())
        val sync = sync(readTimeoutMillis = 200)
        sync.refresh()
        assertEquals(SyncError.Network, sync.state.value.lastError)
        assertEquals(listOf("r-1"), dao.storedIds())
    }

    @Test
    fun a500IsAServerErrorAndKeepsTheSnapshot() = runTest {
        seed("r-1")
        server.enqueue(status(500))
        val sync = sync()
        sync.refresh()
        assertEquals(SyncError.Server(500), sync.state.value.lastError)
        assertEquals(listOf("r-1"), dao.storedIds())
    }

    @Test
    fun recordsThatAllFailValidationAreBadDataAndKeepTheSnapshot() = runTest {
        seed("r-1")
        server.enqueue(ok("""{"restaurants": [{"name": "No id"}, {"id": "r-9", "name": " "}]}"""))
        val sync = sync()
        sync.refresh()
        assertEquals(SyncError.BadData, sync.state.value.lastError)
        assertEquals(listOf("r-1"), dao.storedIds())
    }

    @Test
    fun malformedJsonIsBadDataAndKeepsTheSnapshot() = runTest {
        seed("r-1")
        server.enqueue(ok("""{"restaurants": [{"id": "r-2""""))
        val sync = sync()
        sync.refresh()
        assertEquals(SyncError.BadData, sync.state.value.lastError)
        assertEquals(listOf("r-1"), dao.storedIds())
    }

    @Test
    fun aDatabaseFailureIsAStorageErrorAndEndsTheRefresh() = runTest {
        server.enqueue(ok(payload("r-1")))
        val sync = sync(dao = swapping { throw SQLiteFullException("database or disk is full") })
        sync.refresh() // failures arrive through the state, never as exceptions (spec §5.4)
        assertEquals(RefreshState(isRefreshing = false, lastError = SyncError.Storage), sync.state.value)
    }

    @Test
    fun aCancelledRefreshEndsWithoutAnError() = runTest {
        server.enqueue(ok(payload("r-1")))
        val swapStarted = CompletableDeferred<Unit>()
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val sync = sync(
            dao = swapping {
                swapStarted.complete(Unit)
                awaitCancellation()
            },
            scope = applicationScope
        )
        val caller = launch { sync.refresh() }
        swapStarted.await()
        applicationScope.cancel()
        caller.join()
        assertEquals(RefreshState(isRefreshing = false, lastError = null), sync.state.value)
    }

    @Test
    fun overlappingRefreshesMakeOneRequest() = runTest {
        server.enqueue(
            ok(payload("r-1")).newBuilder().headersDelay(300, TimeUnit.MILLISECONDS).build()
        )
        val sync = sync()
        List(3) { launch { sync.refresh() } }.joinAll()
        assertEquals(1, server.requestCount)
        assertEquals(listOf("r-1"), dao.storedIds())
    }

    @Test
    fun refreshIfStaleWaitsFiveMinutes() = runTest {
        repeat(2) { server.enqueue(ok(payload("r-1"))) }
        val sync = sync()
        sync.refreshIfStale() // no snapshot yet
        clock.advanceBy(Duration.ofMinutes(4))
        sync.refreshIfStale()
        assertEquals(1, server.requestCount)
        clock.advanceBy(Duration.ofMinutes(1))
        sync.refreshIfStale()
        assertEquals(2, server.requestCount)
    }
}
