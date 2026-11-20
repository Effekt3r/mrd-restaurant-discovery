package com.effekt3r.mrd.feature.details

import androidx.lifecycle.SavedStateHandle
import com.effekt3r.mrd.core.data.MenuRepository
import com.effekt3r.mrd.core.data.RestaurantRepository
import com.effekt3r.mrd.core.model.Menu
import com.effekt3r.mrd.core.model.MenuCategory
import com.effekt3r.mrd.core.model.MenuItem
import com.effekt3r.mrd.core.model.MenuState
import com.effekt3r.mrd.core.model.Money
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.Restaurant
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.SyncError
import com.effekt3r.mrd.core.model.SyncStatus
import com.effekt3r.mrd.core.testing.FakeMenuRepository
import com.effekt3r.mrd.core.testing.FakeNetworkMonitor
import com.effekt3r.mrd.core.testing.FakeRestaurantRepository
import com.effekt3r.mrd.core.testing.MainDispatcherRule
import com.effekt3r.mrd.core.testing.TestClock
import com.effekt3r.mrd.core.testing.TestMinuteTicker
import com.effekt3r.mrd.core.ui.SyncProblem
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric: the route is read from SavedStateHandle with Navigation's toRoute(), which uses Android's Bundle.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DetailsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val restaurants = FakeRestaurantRepository()
    private val menus = FakeMenuRepository()
    private val network = FakeNetworkMonitor(online = true)
    private val clock = TestClock(Instant.parse("2026-11-16T10:00:00Z"))
    private val synced = SyncStatus(lastSyncedAt = clock.instant(), isRefreshing = false, lastError = null)

    private fun viewModel(id: String = "r-1001", menus: MenuRepository = this.menus) = DetailsViewModel(
        SavedStateHandle(mapOf("id" to id)),
        restaurants,
        menus,
        network,
        TestMinuteTicker(clock),
        clock
    )

    private fun TestScope.visible(viewModel: DetailsViewModel) = backgroundScope.launch { viewModel.uiState.collect {} }

    @Test
    fun aCachedRestaurantShowsAtOnceWithoutASync() = runTest {
        restaurants.restaurants.value = listOf(grill)
        restaurants.status.value = synced
        val viewModel = viewModel()
        visible(viewModel)
        runCurrent()
        assertEquals(RestaurantState.Found(grill), viewModel.uiState.value.restaurant)
        assertEquals(0, restaurants.refreshCalls)
    }

    @Test
    fun anIdTheSnapshotDoesNotHaveIsNotFound() = runTest {
        restaurants.restaurants.value = listOf(grill)
        restaurants.status.value = synced
        val viewModel = viewModel(id = "r-9999")
        visible(viewModel)
        runCurrent()
        assertEquals(RestaurantState.NotFound, viewModel.uiState.value.restaurant)
        assertEquals("a snapshot exists, so there's nothing to fetch", 0, restaurants.refreshCalls)
    }

    @Test
    fun aDeepLinkBeforeAnySyncLoadsWhileTheFirstSyncRuns() = runTest {
        val viewModel = viewModel()
        visible(viewModel)
        runCurrent()
        assertEquals(RestaurantState.Loading, viewModel.uiState.value.restaurant)
        assertEquals(1, restaurants.refreshCalls)
        restaurants.restaurants.value = listOf(grill)
        restaurants.status.value = synced
        runCurrent()
        assertEquals(RestaurantState.Found(grill), viewModel.uiState.value.restaurant)
    }

    @Test
    fun aRestaurantIsNotCalledMissingWhileItsQueryCatchesUpWithTheFirstSnapshot() = runTest {
        val database = LaggingRestaurantRepository(restaurants)
        val viewModel = DetailsViewModel(
            SavedStateHandle(mapOf("id" to "r-1001")),
            database,
            menus,
            network,
            TestMinuteTicker(clock),
            clock
        )
        val shown = mutableListOf<RestaurantState>()
        backgroundScope.launch { viewModel.uiState.map { it.restaurant }.distinctUntilChanged().toList(shown) }
        runCurrent()
        // The first snapshot lands. The sync status reports it before the restaurant's own query has run again.
        restaurants.restaurants.value = listOf(grill)
        restaurants.status.value = synced
        runCurrent()
        database.invalidate()
        runCurrent()
        assertEquals(listOf(RestaurantState.Loading, RestaurantState.Found(grill)), shown)
    }

    @Test
    fun aDeepLinkOfflineBeforeAnySyncIsAnOfflineErrorWithRetry() = runTest {
        network.setOnline(false)
        val viewModel = viewModel()
        visible(viewModel)
        restaurants.status.value = SyncStatus(lastSyncedAt = null, isRefreshing = false, lastError = SyncError.Network)
        runCurrent()
        assertEquals(RestaurantState.Unavailable(SyncError.Network), viewModel.uiState.value.restaurant)
        assertEquals(SyncProblem.Offline, viewModel.uiState.value.restaurantProblem)
        viewModel.onRetry()
        runCurrent()
        assertEquals(2, restaurants.refreshCalls)
    }

    @Test
    fun theFavouriteTogglesThroughTheRepositoryBrowseReadsToo() = runTest {
        restaurants.restaurants.value = listOf(grill)
        restaurants.status.value = synced
        val viewModel = viewModel()
        visible(viewModel)
        viewModel.onFavouriteToggled()
        runCurrent()
        val shown = viewModel.uiState.value.restaurant as RestaurantState.Found
        assertEquals(true, shown.restaurant.summary.isFavourite)
        assertEquals(true, restaurants.restaurants.value.single().summary.isFavourite)
    }

    @Test
    fun theMenuLoadsOnItsOwn() = runTest {
        val content = MenuState.Content(menu, fetchedAt = clock.instant())
        menus.states = listOf(MenuState.Loading, content)
        val viewModel = viewModel()
        visible(viewModel)
        runCurrent()
        assertEquals(content, viewModel.uiState.value.menu)
        assertEquals(
            "the restaurant isn't held up by the menu",
            RestaurantState.Loading,
            viewModel.uiState.value.restaurant
        )
        assertEquals(listOf(RestaurantId("r-1001")), menus.requests)
    }

    @Test
    fun aMissingMenuIsNotAvailable() = runTest {
        menus.states = listOf(MenuState.Loading, MenuState.NotAvailable)
        val viewModel = viewModel()
        visible(viewModel)
        runCurrent()
        assertEquals(MenuState.NotAvailable, viewModel.uiState.value.menu)
    }

    @Test
    fun aFailedMenuKeepsTheMenuSeenBeforeAndRetries() = runTest {
        val cached = MenuState.Content(menu, fetchedAt = clock.instant())
        menus.states = listOf(cached, MenuState.Error(SyncError.Network, cached))
        network.setOnline(false)
        val viewModel = viewModel()
        visible(viewModel)
        runCurrent()
        assertEquals(MenuState.Error(SyncError.Network, cached), viewModel.uiState.value.menu)
        assertEquals(SyncProblem.Offline, viewModel.uiState.value.menuProblem)
        menus.states = listOf(MenuState.Loading, cached)
        viewModel.onRetryMenu()
        runCurrent()
        assertEquals(cached, viewModel.uiState.value.menu)
        assertEquals(2, menus.requests.size)
    }

    @Test
    fun aFailedMenuLoadsAgainWhenTheConnectionReturns() = runTest {
        menus.states = listOf(MenuState.Loading, MenuState.Error(SyncError.Network, cached = null))
        network.setOnline(false)
        val viewModel = viewModel()
        visible(viewModel)
        runCurrent()
        assertEquals(SyncProblem.Offline, viewModel.uiState.value.menuProblem)
        // Back online: the menu loads by itself, rather than blaming a connection that's back.
        val content = MenuState.Content(menu, fetchedAt = clock.instant())
        menus.states = listOf(MenuState.Loading, content)
        network.setOnline(true)
        runCurrent()
        assertEquals(content, viewModel.uiState.value.menu)
        // A menu that loaded isn't fetched again when the connection next returns, as the list isn't.
        network.setOnline(false)
        runCurrent()
        network.setOnline(true)
        runCurrent()
        assertEquals(2, menus.requests.size)
    }

    @Test
    fun aMenuStillLoadingWhenTheConnectionReturnsLoadsByItself() = runTest {
        val content = MenuState.Content(menu, fetchedAt = clock.instant())
        val menus = HeldMenuRepository(shownFirst = MenuState.Loading, fetched = content)
        network.setOnline(false)
        val viewModel = viewModel(menus = menus)
        visible(viewModel)
        runCurrent()
        assertEquals(MenuState.Loading, viewModel.uiState.value.menu)
        // The connection returns while the fetch that started offline is still running, and that fetch then fails.
        network.setOnline(true)
        runCurrent()
        menus.failTheFirstFetch(MenuState.Error(SyncError.Network, cached = null))
        runCurrent()
        assertEquals(content, viewModel.uiState.value.menu)
    }

    @Test
    fun aCachedMenuStillRefreshingWhenTheConnectionReturnsRefreshesByItself() = runTest {
        val cached = MenuState.Content(menu, fetchedAt = clock.instant().minus(Duration.ofDays(1)))
        val fresh = MenuState.Content(menu, fetchedAt = clock.instant())
        val menus = HeldMenuRepository(shownFirst = cached, fetched = fresh)
        network.setOnline(false)
        val viewModel = viewModel(menus = menus)
        visible(viewModel)
        runCurrent()
        assertEquals(cached, viewModel.uiState.value.menu)
        // The cached menu shows, but its refresh is what's still running when the connection returns.
        network.setOnline(true)
        runCurrent()
        menus.failTheFirstFetch(MenuState.Error(SyncError.Network, cached))
        runCurrent()
        assertEquals(fresh, viewModel.uiState.value.menu)
    }

    @Test
    fun aMenuThatCouldNotBeStoredIsAStorageProblem() = runTest {
        menus.states = listOf(MenuState.Error(SyncError.Storage, cached = null))
        val viewModel = viewModel()
        visible(viewModel)
        runCurrent()
        assertEquals(SyncProblem.Storage, viewModel.uiState.value.menuProblem)
    }

    /**
     * Room's timing: a new query reads the database as it is, but a running query emits again only once its
     * invalidation has been processed, which can be after the sync status has moved on.
     */
    private class LaggingRestaurantRepository(private val database: FakeRestaurantRepository) :
        RestaurantRepository by database {
        private val invalidations = MutableSharedFlow<Unit>()

        suspend fun invalidate() = invalidations.emit(Unit)

        override fun restaurant(id: RestaurantId): Flow<Restaurant?> = flow {
            emit(database.restaurant(id).first())
            invalidations.collect { emit(database.restaurant(id).first()) }
        }
    }

    /**
     * A fetch that started offline and is still running when the connection returns, as a request on a connection
     * that's gone waits for its timeout. Every request shows [shownFirst]; the first then waits until the test fails
     * it, and any later one gets [fetched] at once.
     */
    private class HeldMenuRepository(private val shownFirst: MenuState, private val fetched: MenuState) :
        MenuRepository {
        private val firstFetch = CompletableDeferred<MenuState>()
        private var requests = 0

        fun failTheFirstFetch(error: MenuState.Error) = firstFetch.complete(error)

        override fun menu(id: RestaurantId): Flow<MenuState> = flow {
            val request = ++requests
            emit(shownFirst)
            emit(if (request == 1) firstFetch.await() else fetched)
        }
    }

    private val grill = Restaurant(
        summary = RestaurantSummary(
            id = RestaurantId("r-1001"),
            name = "Cape Town Grill",
            cuisines = listOf("Steakhouse", "Grill"),
            rating = null,
            deliveryFee = Money(1500),
            etaMinutes = 32,
            imageUrl = null,
            openStatus = OpenStatus.Open(closesAt = null),
            isFavourite = false
        ),
        openingHours = null
    )

    private val menu = Menu(
        listOf(
            MenuCategory(
                id = "c-1",
                name = "Starters",
                items = listOf(MenuItem("i-1", "Grilled Halloumi", Money(8900), true, null, null, emptyList()))
            )
        )
    )
}
