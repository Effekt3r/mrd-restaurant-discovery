package com.effekt3r.mrd.feature.details

import androidx.lifecycle.SavedStateHandle
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
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private fun viewModel(id: String = "r-1001") = DetailsViewModel(
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
    fun aMenuThatCouldNotBeStoredIsAStorageProblem() = runTest {
        menus.states = listOf(MenuState.Error(SyncError.Storage, cached = null))
        val viewModel = viewModel()
        visible(viewModel)
        runCurrent()
        assertEquals(SyncProblem.Storage, viewModel.uiState.value.menuProblem)
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
