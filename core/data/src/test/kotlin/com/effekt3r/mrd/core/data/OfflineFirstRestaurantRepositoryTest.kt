package com.effekt3r.mrd.core.data

import androidx.paging.testing.asSnapshot
import com.effekt3r.mrd.core.data.database.intervalEntities
import com.effekt3r.mrd.core.data.database.toEntity
import com.effekt3r.mrd.core.data.network.ParsedRestaurant
import com.effekt3r.mrd.core.data.sync.RestaurantSync
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.SyncError
import com.effekt3r.mrd.core.testing.FakeNetworkMonitor
import com.effekt3r.mrd.core.testing.TestClock
import com.effekt3r.mrd.core.testing.TestMinuteTicker
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstRestaurantRepositoryTest {

    @get:Rule
    val cacheDir = TemporaryFolder()

    private val server = MockWebServer().apply { start() }
    private val database = inMemoryDatabase()
    private val dao = database.restaurantDao()
    private val clock = TestClock(at(MONDAY, 21, 59).instant())
    private val ticker = TestMinuteTicker(clock)
    private val network = FakeNetworkMonitor(online = true)

    @After
    fun tearDown() {
        server.close()
        database.close()
    }

    private fun TestScope.repository() = OfflineFirstRestaurantRepository(
        dao = dao,
        sync = RestaurantSync(
            testApi(server, cacheDir.root),
            dao,
            testJson,
            clock,
            Dispatchers.IO,
            backgroundScope
        ),
        ticker = ticker,
        networkMonitor = network,
        clock = clock
    )

    private suspend fun seed(vararg restaurants: ParsedRestaurant) = dao.replaceSnapshot(
        restaurants.mapIndexed { position, it -> it.toEntity(position) },
        restaurants.flatMap { it.intervalEntities() },
        syncedAt = clock.millis()
    )

    @Test
    fun aTickReloadsTheListInPlaceWithTheNewStatus() = runTest {
        // Open 11:00 to 22:00. At 21:59 Open now lists it; at 22:00 it doesn't, once the minute ticks.
        seed(parsedRestaurant("r-1"))
        val openNow = repository().restaurants(query = "", openNow = true)
        val withoutATick = openNow.asSnapshot { clock.set(at(MONDAY, 22).instant()) }
        clock.set(at(MONDAY, 21, 59).instant())
        val afterATick = openNow.asSnapshot {
            clock.set(at(MONDAY, 22).instant())
            ticker.tick()
        }
        assertEquals(listOf("r-1"), withoutATick.map { it.id.value })
        assertEquals(emptyList<String>(), afterATick.map { it.id.value })
    }

    @Test
    fun theListShowsEachStatusWithItsNextChange() = runTest {
        seed(parsedRestaurant("r-1"), parsedRestaurant("r-2", windows = daily(hm(22), hm(23))))
        val statuses = repository().restaurants("", openNow = false).asSnapshot().map { it.openStatus }
        assertEquals(listOf(OpenStatus.Open(at(MONDAY, 22)), OpenStatus.Closed(at(MONDAY, 22))), statuses)
    }

    @Test
    fun searchIgnoresCaseAndAccentsAndMatchesWildcardsLiterally() = runTest {
        seed(
            parsedRestaurant("r-1", name = "Café Fynbos"),
            parsedRestaurant("r-2", name = "100% Plant Kitchen"),
            parsedRestaurant("r-3", name = "Cape  Town Grill"),
            parsedRestaurant("r-4", name = "Plant_Based")
        )
        val repository = repository()
        suspend fun search(query: String) = repository.restaurants(query, openNow = false).asSnapshot().map { it.name }
        assertEquals(listOf("Café Fynbos"), search("CAFE"))
        assertEquals(listOf("100% Plant Kitchen"), search("0%"))
        assertEquals(listOf("Plant_Based"), search("t_b"))
        assertEquals(listOf("Cape  Town Grill"), search("  cape town "))
        assertEquals(emptyList<String>(), search("%%"))
        assertEquals(1, repository.count("cafe", openNow = false).first())
    }

    @Test
    fun theCountFollowsTheClock() = runTest {
        seed(parsedRestaurant("r-1"), parsedRestaurant("r-2", windows = daily(hm(8), hm(22, 30))))
        val counts = repository().count("", openNow = true)
            .onEach {
                clock.set(at(MONDAY, 22, 15).instant())
                ticker.tick()
            }
            .take(2)
            .toList()
        assertEquals(listOf(2, 1), counts)
    }

    @Test
    fun detailsFollowTheClockAndHaveTheHours() = runTest {
        seed(parsedRestaurant("r-1"))
        val restaurant = repository().restaurant(RestaurantId("r-1"))
        assertEquals(7, restaurant.first()?.openingHours?.intervals?.size)
        val statuses = restaurant.map { it?.summary?.openStatus }
            .onEach {
                clock.set(at(MONDAY, 22).instant())
                ticker.tick()
            }
            .take(2)
            .toList()
        assertEquals(listOf(OpenStatus.Open(at(MONDAY, 22)), OpenStatus.Closed(at(TUESDAY, 11))), statuses)
    }

    @Test
    fun favouritesSurviveASnapshotSwap() = runTest {
        seed(parsedRestaurant("r-1"))
        val repository = repository()
        repository.toggleFavourite(RestaurantId("r-1"))
        seed(parsedRestaurant("r-2"))
        seed(parsedRestaurant("r-1"), parsedRestaurant("r-2"))
        assertEquals(true, repository.restaurant(RestaurantId("r-1")).first()?.summary?.isFavourite)
        assertEquals(listOf(true, false), repository.restaurants("", false).asSnapshot().map { it.isFavourite })
        repository.toggleFavourite(RestaurantId("r-1"))
        assertEquals(false, repository.restaurant(RestaurantId("r-1")).first()?.summary?.isFavourite)
    }

    @Test
    fun aFailedRefreshIsRetriedWhenTheConnectionReturns() = runTest {
        server.enqueue(status(500))
        server.enqueue(ok(payload("r-1")))
        network.setOnline(false)
        val repository = repository()
        backgroundScope.launch { repository.syncStatus.collect {} }
        repository.refresh()
        assertEquals(SyncError.Server(500), repository.syncStatus.first().lastError)
        network.setOnline(true)
        val status = repository.syncStatus.first { it.lastSyncedAt != null && !it.isRefreshing }
        assertEquals(null, status.lastError)
        assertEquals(Instant.ofEpochMilli(clock.millis()), status.lastSyncedAt)
        assertEquals(2, server.requestCount)
    }
}
