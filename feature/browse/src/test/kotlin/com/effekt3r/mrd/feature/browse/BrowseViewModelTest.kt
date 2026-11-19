package com.effekt3r.mrd.feature.browse

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.Restaurant
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.SyncError
import com.effekt3r.mrd.core.model.SyncStatus
import com.effekt3r.mrd.core.testing.FakeNetworkMonitor
import com.effekt3r.mrd.core.testing.FakeRestaurantRepository
import com.effekt3r.mrd.core.testing.MainDispatcherRule
import com.effekt3r.mrd.core.testing.TestClock
import com.effekt3r.mrd.core.testing.TestMinuteTicker
import com.effekt3r.mrd.core.ui.SyncProblem
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class) // the test scheduler's virtual time
class BrowseViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakeRestaurantRepository()
    private val network = FakeNetworkMonitor(online = true)
    private val clock = TestClock(Instant.parse("2026-11-16T10:00:00Z")) // Monday 12:00 in Cape Town
    private val ticker = TestMinuteTicker(clock)
    private val synced = SyncStatus(lastSyncedAt = clock.instant(), isRefreshing = false, lastError = null)

    private fun viewModel(saved: SavedStateHandle = SavedStateHandle()) =
        BrowseViewModel(repository, network, ticker, clock, saved)

    // TextFieldState is snapshot state: the screen's edits reach snapshotFlow once Compose applies the snapshot, which
    // a test does by hand.
    private fun BrowseViewModel.type(text: String) {
        searchState.setTextAndPlaceCursorAtEnd(text)
        Snapshot.sendApplyNotifications()
    }

    private fun TestScope.collectState(viewModel: BrowseViewModel) =
        backgroundScope.launch { viewModel.uiState.collect {} }

    private fun TestScope.collectList(viewModel: BrowseViewModel) =
        backgroundScope.launch { viewModel.restaurants.collect {} }

    @Test
    fun theSearchWaitsUntilTypingPausesFor250Ms() = runTest {
        val viewModel = viewModel()
        collectList(viewModel)
        collectState(viewModel)
        runCurrent()
        viewModel.type("caf")
        advanceTimeBy(249)
        runCurrent()
        assertEquals("", viewModel.uiState.value.query)
        advanceTimeBy(1)
        runCurrent()
        assertEquals("caf", viewModel.uiState.value.query)
        assertEquals(listOf("" to false, "caf" to false), repository.listRequests)
    }

    @Test
    fun typingQuicklyQueriesOnceAndClearingAppliesAtOnce() = runTest {
        val viewModel = viewModel()
        collectList(viewModel)
        runCurrent()
        listOf("c", "ca", "caf", "cafe").forEach {
            viewModel.type(it)
            advanceTimeBy(100)
        }
        advanceTimeBy(250)
        runCurrent()
        viewModel.onClearSearch()
        Snapshot.sendApplyNotifications()
        runCurrent()
        assertEquals(listOf("" to false, "cafe" to false, "" to false), repository.listRequests)
    }

    @Test
    fun openNowFiltersTheListAndTheCount() = runTest {
        repository.restaurants.value = listOf(restaurant("r-1", open = true), restaurant("r-2", open = false))
        repository.status.value = synced
        val viewModel = viewModel()
        collectList(viewModel)
        runCurrent()
        viewModel.uiState.test {
            assertEquals(2, expectMostRecentItem().resultCount)
            viewModel.onOpenNowToggled(true)
            // The chip changes at once; the count is unknown until the new filters' count arrives, never the old one.
            assertEquals(BrowseUiState(openNow = true, syncStatus = synced, now = clock.instant()), awaitItem())
            assertEquals(1, awaitItem().resultCount)
        }
        runCurrent()
        assertEquals(listOf("" to false, "" to true), repository.listRequests)
    }

    @Test
    fun theSearchAndTheToggleSurviveProcessDeath() = runTest {
        val first = viewModel(SavedStateHandle())
        first.type("cafe")
        first.onOpenNowToggled(true)
        runCurrent()
        // What the system restores into a new process: the handle's saved values.
        val saved = SavedStateHandle(mapOf("query" to "cafe", "openNow" to true))
        assertEquals("cafe", first.savedQuery())
        val restored = viewModel(saved)
        collectList(restored)
        runCurrent()
        assertEquals("cafe", restored.searchState.text.toString())
        assertEquals(true, restored.uiState.value.openNow)
        // The restored filters apply at once, without waiting for the debounce.
        assertEquals(listOf("cafe" to true), repository.listRequests.takeLast(1))
    }

    private fun BrowseViewModel.savedQuery(): String? {
        val handle = javaClass.getDeclaredField("savedStateHandle").apply { isAccessible = true }.get(this)
        return (handle as SavedStateHandle)[BrowseViewModel.QUERY_KEY]
    }

    @Test
    fun theFirstLoadShowsSkeletonsUntilTheSnapshotLands() = runTest {
        repository.status.value = SyncStatus(lastSyncedAt = null, isRefreshing = true, lastError = null)
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(BrowseBody.FirstLoad, expectMostRecentItem().body)
            repository.restaurants.value = listOf(restaurant("r-1"))
            repository.status.value = synced
            assertEquals(BrowseBody.Results, expectMostRecentItem().body)
        }
    }

    @Test
    fun aFailedFirstLoadIsWordedForWhatWentWrong() = runTest {
        val viewModel = viewModel()
        collectState(viewModel)
        repository.status.value = SyncStatus(lastSyncedAt = null, isRefreshing = false, lastError = SyncError.Network)
        network.setOnline(false)
        runCurrent()
        assertEquals(BrowseBody.FirstLoadError, viewModel.uiState.value.body)
        assertEquals(SyncProblem.Offline, viewModel.uiState.value.problem)
        network.setOnline(true)
        repository.status.value =
            SyncStatus(lastSyncedAt = null, isRefreshing = false, lastError = SyncError.Server(503))
        runCurrent()
        assertEquals(SyncProblem.CouldNotConnect, viewModel.uiState.value.problem)
        repository.status.value = SyncStatus(lastSyncedAt = null, isRefreshing = false, lastError = SyncError.Storage)
        runCurrent()
        assertEquals(SyncProblem.Storage, viewModel.uiState.value.problem)
        // Retry: skeletons again while it runs.
        repository.status.value = SyncStatus(lastSyncedAt = null, isRefreshing = true, lastError = SyncError.Storage)
        runCurrent()
        assertEquals(BrowseBody.FirstLoad, viewModel.uiState.value.body)
    }

    @Test
    fun cachedRestaurantsShowTheBannerWhenOfflineOrWhenTheRefreshFailed() = runTest {
        repository.restaurants.value = listOf(restaurant("r-1"))
        repository.status.value = synced
        val viewModel = viewModel()
        collectState(viewModel)
        runCurrent()
        assertNull(viewModel.uiState.value.banner)
        network.setOnline(false)
        runCurrent()
        assertEquals(BrowseBody.Results, viewModel.uiState.value.body)
        assertEquals(SyncProblem.Offline, viewModel.uiState.value.banner)
        network.setOnline(true)
        repository.status.value = synced.copy(lastError = SyncError.Network)
        runCurrent()
        assertEquals(SyncProblem.CouldNotConnect, viewModel.uiState.value.banner)
        // The repository retries on reconnecting; when that works, the banner goes (AC-7).
        repository.status.value = synced
        runCurrent()
        assertNull(viewModel.uiState.value.banner)
    }

    @Test
    fun nothingMatchingIsNoMatchesUnlessTheAreaIsEmpty() = runTest {
        repository.status.value = synced
        val viewModel = viewModel()
        collectList(viewModel)
        collectState(viewModel)
        runCurrent()
        assertEquals(BrowseBody.EmptyArea, viewModel.uiState.value.body)
        repository.restaurants.value = listOf(restaurant("r-1", open = false))
        viewModel.onOpenNowToggled(true)
        runCurrent()
        assertEquals(BrowseBody.NoMatches, viewModel.uiState.value.body)
        viewModel.onOpenNowToggled(false)
        viewModel.type("xyz")
        advanceTimeBy(250)
        runCurrent()
        assertEquals(BrowseBody.NoMatches, viewModel.uiState.value.body)
        assertEquals("xyz", viewModel.uiState.value.query)
    }

    @Test
    fun becomingVisibleRefreshesAStaleSnapshot() = runTest {
        val viewModel = viewModel()
        assertEquals(0, repository.refreshIfStaleCalls)
        val visible = collectState(viewModel)
        runCurrent()
        assertEquals(1, repository.refreshIfStaleCalls)
        // Back from the background, more than 5 seconds later: visible again.
        visible.cancel()
        advanceTimeBy(BrowseViewModel.STOP_TIMEOUT_MILLIS + 1)
        collectState(viewModel)
        runCurrent()
        assertEquals(2, repository.refreshIfStaleCalls)
    }

    @Test
    fun refreshAndFavouritesGoToTheRepository() = runTest {
        repository.restaurants.value = listOf(restaurant("r-1"))
        val viewModel = viewModel()
        viewModel.onRefresh()
        viewModel.onFavouriteToggled(RestaurantId("r-1"))
        runCurrent()
        assertEquals(1, repository.refreshCalls)
        assertEquals(true, repository.restaurants.value.single().summary.isFavourite)
    }

    @Test
    fun theListFollowsTheClockOnlyWhileTheScreenCollectsItsState() = runTest {
        // Milestone 3's review, M-5: cachedIn keeps the list collected in the background, so its ticks must stop.
        val viewModel = viewModel()
        collectList(viewModel)
        val visible = collectState(viewModel)
        runCurrent()
        clock.advanceBy(Duration.ofMinutes(1))
        ticker.tick()
        runCurrent()
        assertEquals("the first tick and a minute's", 2, repository.listTicks.size)
        assertEquals(clock.instant(), viewModel.uiState.value.now)
        // The app goes to the background: the screen stops collecting, and after 5 seconds the clock stops.
        visible.cancel()
        advanceTimeBy(BrowseViewModel.STOP_TIMEOUT_MILLIS + 1)
        clock.advanceBy(Duration.ofMinutes(1))
        ticker.tick()
        runCurrent()
        assertEquals(2, repository.listTicks.size)
        // Visible again: the clock restarts with the time now, so the list can catch up.
        collectState(viewModel)
        runCurrent()
        assertEquals(3, repository.listTicks.size)
    }

    private fun restaurant(id: String, open: Boolean = true) = Restaurant(
        summary = RestaurantSummary(
            id = RestaurantId(id),
            name = "Restaurant $id",
            cuisines = listOf("Grill"),
            rating = null,
            deliveryFee = null,
            etaMinutes = null,
            imageUrl = null,
            openStatus = if (open) OpenStatus.Open(closesAt = null) else OpenStatus.Closed(opensAt = null),
            isFavourite = false
        ),
        openingHours = null
    )
}
