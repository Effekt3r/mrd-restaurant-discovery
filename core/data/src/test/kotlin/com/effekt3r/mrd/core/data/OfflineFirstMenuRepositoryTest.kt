package com.effekt3r.mrd.core.data

import com.effekt3r.mrd.core.data.database.MenuEntity
import com.effekt3r.mrd.core.model.MenuState
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.SyncError
import com.effekt3r.mrd.core.testing.TestClock
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineFirstMenuRepositoryTest {

    @get:Rule
    val cacheDir = TemporaryFolder()

    private val server = MockWebServer().apply { start() }
    private val database = inMemoryDatabase()
    private val clock = TestClock(Instant.parse("2026-11-16T10:42:00Z"))
    private val repository by lazy {
        OfflineFirstMenuRepository(testApi(server, cacheDir.root), database.menuDao(), testJson, clock, Dispatchers.IO)
    }
    private val id = RestaurantId("r-1")

    private val menu = """
        {"restaurant_id": "r-1", "categories": [
          {"id": "c-1", "name": "Starters", "items": [{"id": "i-1", "name": "Samoosas", "price_cents": 4500}]}]}
    """

    @After
    fun tearDown() {
        server.close()
        database.close()
    }

    @Test
    fun a200ShowsTheMenuAndCachesIt() = runTest {
        server.enqueue(ok(menu))
        val states = repository.menu(id).toList()
        assertEquals(MenuState.Loading, states.first())
        assertEquals(
            listOf("Samoosas"),
            (states.last() as MenuState.Content).menu.categories.single().items.map {
                it.name
            }
        )
        assertEquals(clock.instant(), (states.last() as MenuState.Content).fetchedAt)
        assertEquals("/v1/menus/r-1.json", server.takeRequest().target)
    }

    @Test
    fun aMenuSeenBeforeShowsOfflineWithTheError() = runTest {
        server.enqueue(ok(menu))
        val fetched = repository.menu(id).toList().last()
        server.close() // the connection is refused from now on
        assertEquals(
            listOf(fetched, MenuState.Error(SyncError.Network, fetched as MenuState.Content)),
            repository.menu(id).toList()
        )
    }

    @Test
    fun a404IsNotAvailableAndDropsTheCachedMenu() = runTest {
        server.enqueue(ok(menu))
        server.enqueue(status(404))
        repository.menu(id).toList()
        assertEquals(MenuState.NotAvailable, repository.menu(id).toList().last())
        assertNull(database.menuDao().menu("r-1"))
    }

    @Test
    fun anEmptyMenuIsNotAvailable() = runTest {
        server.enqueue(ok("""{"restaurant_id": "r-1", "categories": []}"""))
        assertEquals(listOf(MenuState.Loading, MenuState.NotAvailable), repository.menu(id).toList())
    }

    @Test
    fun badDataIsAnErrorWithoutACachedMenu() = runTest {
        server.enqueue(ok(menu.replace("\"r-1\"", "\"r-2\"")))
        assertEquals(listOf(MenuState.Loading, MenuState.Error(SyncError.BadData, null)), repository.menu(id).toList())
    }

    @Test
    fun aCachedMenuThatNoLongerDecodesIsDroppedAndFetchedAgain() = runTest {
        // As if CachedMenu had gained a field without a default since this row was written.
        database.menuDao().upsert(MenuEntity("r-1", """{"categories": [{"id": "c-1"}]}""", fetchedAt = 0))
        server.enqueue(status(503))
        server.enqueue(ok(menu))
        assertEquals(
            listOf(MenuState.Loading, MenuState.Error(SyncError.Server(503), null)),
            repository.menu(id).toList()
        )
        assertNull(database.menuDao().menu("r-1"))
        val recovered = repository.menu(id).toList().last() as MenuState.Content
        assertEquals(listOf("Samoosas"), recovered.menu.categories.single().items.map { it.name })
    }

    @Test
    fun aServerErrorKeepsTheCachedMenu() = runTest {
        server.enqueue(ok(menu))
        server.enqueue(status(503))
        val fetched = repository.menu(id).toList().last() as MenuState.Content
        assertEquals(MenuState.Error(SyncError.Server(503), fetched), repository.menu(id).toList().last())
    }
}
