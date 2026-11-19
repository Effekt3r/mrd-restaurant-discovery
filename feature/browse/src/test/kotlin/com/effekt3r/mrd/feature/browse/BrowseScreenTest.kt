package com.effekt3r.mrd.feature.browse

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme
import com.effekt3r.mrd.core.model.Money
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.Rating
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.SyncError
import com.effekt3r.mrd.core.model.SyncStatus
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Each of spec §6.2's states, on the stateless screen. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rZA")
class BrowseScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val now = Instant.parse("2026-11-16T10:00:00Z") // Monday 12:00 in Cape Town
    private val synced = SyncStatus(lastSyncedAt = now, isRefreshing = false, lastError = null)
    private val events = mutableListOf<String>()

    private val grill = RestaurantSummary(
        id = RestaurantId("r-1001"),
        name = "Cape Town Grill",
        cuisines = listOf("Steakhouse", "Grill"),
        rating = Rating(46),
        deliveryFee = Money(1500),
        etaMinutes = 32,
        imageUrl = null,
        openStatus = OpenStatus.Open(closesAt = null),
        isFavourite = false
    )

    private fun List<RestaurantSummary>.foundBy(query: String, openNow: Boolean = false) =
        map { BrowseListItem(it, BrowseFilters(query, openNow)) }

    private fun setScreen(state: BrowseUiState, restaurants: List<RestaurantSummary> = emptyList()) =
        compose.setContent {
            MrdTheme {
                BrowseScreen(
                    state = state,
                    searchState = TextFieldState(state.query),
                    restaurants = flowOf(PagingData.from(restaurants.foundBy(state.query, state.openNow)))
                        .collectAsLazyPagingItems(),
                    onOpenNowToggled = { events += "openNow=$it" },
                    onClearSearch = { events += "clear" },
                    onRefresh = { events += "refresh" },
                    onRestaurantClick = { events += "open $it" },
                    onFavouriteToggled = { events += "favourite ${it.value}" }
                )
            }
        }

    @Test
    fun theFirstLoadShowsSkeletonsThatTalkBackReadsOnce() {
        setScreen(BrowseUiState(syncStatus = synced.copy(lastSyncedAt = null, isRefreshing = true), now = now))
        compose.onNodeWithContentDescription("Loading restaurants").assertExists()
    }

    @Test
    fun aFirstLoadOfflineSaysSoAndRetries() {
        setScreen(
            BrowseUiState(
                syncStatus = SyncStatus(lastSyncedAt = null, isRefreshing = false, lastError = SyncError.Network),
                isOnline = false,
                now = now
            )
        )
        compose.onNodeWithText("You're offline").assertExists()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(listOf("refresh"), events)
    }

    @Test
    fun aStorageFailureIsNotWordedAsAConnectionProblem() {
        setScreen(
            BrowseUiState(
                syncStatus = SyncStatus(lastSyncedAt = null, isRefreshing = false, lastError = SyncError.Storage),
                now = now
            )
        )
        compose.onNodeWithText("Couldn't save restaurants").assertExists()
        compose.onNodeWithText("Couldn't save restaurants on this device. Free up some space and try again.")
            .assertExists()
    }

    @Test
    fun resultsShowTheCountTheCardsAndTheBannerWhenOffline() {
        setScreen(BrowseUiState(resultCount = 1, syncStatus = synced, isOnline = false, now = now), listOf(grill))
        compose.onNodeWithText("1 restaurant").assertExists()
        compose.onNode(hasContentDescription("Cape Town Grill.", substring = true)).performClick()
        compose.onNode(hasContentDescription("Cape Town Grill.", substring = true))
            .assertExists()
        compose.onNodeWithText("You're offline · Showing restaurants from", substring = true).assertExists()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(listOf("open r-1001", "refresh"), events)
    }

    @Test
    fun noMatchesNamesTheQueryAndOffersBothWaysOut() {
        setScreen(BrowseUiState(query = "xyz", openNow = true, resultCount = 0, syncStatus = synced, now = now))
        compose.onNodeWithText("No restaurants match ‘xyz’").assertExists()
        compose.onNodeWithText("0 open now").assertExists()
        compose.onNodeWithText("Open now").assertIsOn()
        compose.onNodeWithText("Clear search").performClick()
        compose.onNodeWithText("Turn off Open now").performClick()
        assertEquals(listOf("clear", "openNow=false"), events)
    }

    @Test
    fun theHeaderSlidesAwayAsTheListScrollsUpAndReturnsAsItScrollsDown() {
        val many = List(12) { grill.copy(id = RestaurantId("r-$it"), name = "Restaurant $it") }
        setScreen(BrowseUiState(resultCount = many.size, syncStatus = synced, now = now), many)
        val title = compose.onNodeWithText("Restaurants")
        title.assertIsDisplayed()
        val list = compose.onNode(hasScrollToIndexAction())
        list.performTouchInput { swipeUp() }
        title.assertIsNotDisplayed()
        list.performTouchInput { swipeDown() }
        title.assertIsDisplayed()
    }

    @Test
    fun anEmptyAreaSaysNoRestaurantsDeliverHere() {
        setScreen(BrowseUiState(resultCount = 0, syncStatus = synced, now = now))
        compose.onNodeWithText("No restaurants deliver here yet").assertExists()
    }

    // Spec §6.2: changing a filter scrolls to the top. Clearing "cafe" here: the 3 cafés are restaurants 20 to 22 of the
    // 40 that the cleared search finds.
    private val cafeIndices = 20..22
    private val all = List(40) { i ->
        val name = if (i in cafeIndices) "Café $i" else "Restaurant $i"
        grill.copy(id = RestaurantId("r-$i"), name = name)
    }
    private val cafes = all.slice(cafeIndices)
    private var state by mutableStateOf(BrowseUiState(query = "cafe", resultCount = 3, syncStatus = synced, now = now))
    private val list = MutableStateFlow(PagingData.from(cafes.foundBy("cafe")))

    @Composable
    private fun SearchScreen() = MrdTheme {
        BrowseScreen(
            state = state,
            searchState = remember { TextFieldState(state.query) },
            restaurants = list.collectAsLazyPagingItems(),
            onOpenNowToggled = {},
            onClearSearch = {},
            onRefresh = {},
            onRestaurantClick = {},
            onFavouriteToggled = {}
        )
    }

    private fun clearTheSearch() {
        state = state.copy(query = "", resultCount = null)
    }

    private fun allRestaurantsArrive() {
        list.value = PagingData.from(all.foundBy(""))
    }

    /** The index of the first card the grid shows, read from its name: "Café 20" is at index 20. */
    private fun firstShownIndex(): Int {
        val gridTop = compose.onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot.top
        return compose.onAllNodes(hasContentDescription("Steakhouse, Grill", substring = true))
            .fetchSemanticsNodes()
            .filter { it.boundsInRoot.bottom > gridTop }
            .minBy { it.boundsInRoot.top }
            .config[SemanticsProperties.ContentDescription]
            .single()
            .let { Regex("""\d+""").find(it)!!.value.toInt() }
    }

    @Test
    fun aNewSearchStartsAtTheTopWhenItsResultsArriveAfterTheNextLayout() {
        compose.setContent { SearchScreen() }
        compose.runOnIdle { clearTheSearch() }
        compose.waitForIdle() // a layout with the cafés still shown
        compose.runOnIdle { allRestaurantsArrive() }
        compose.waitForIdle()
        assertEquals(0, firstShownIndex())
    }

    @Test
    fun aNewSearchStartsAtTheTopWhenItsResultsArriveBeforeTheNextLayout() {
        compose.setContent { SearchScreen() }
        // No frames, so Paging presents the new results before the grid is laid out again.
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            clearTheSearch()
            allRestaurantsArrive()
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(0, firstShownIndex())
        // Only a new search moves the list: a later reload of the same results leaves it where the user scrolled.
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(10)
        compose.runOnIdle { list.value = PagingData.from(all.map { it.copy(isFavourite = true) }.foundBy("")) }
        compose.waitForIdle()
        assertEquals(10, firstShownIndex())
    }

    @Test
    fun aReloadOfTheSameSearchKeepsTheScrollPosition() {
        clearTheSearch()
        allRestaurantsArrive()
        compose.setContent { SearchScreen() }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(10)
        // A minute's tick or a refresh reloads the same search's results.
        compose.runOnIdle { list.value = PagingData.from(all.map { it.copy(isFavourite = true) }.foundBy("")) }
        compose.waitForIdle()
        assertEquals(10, firstShownIndex())
    }

    @Test
    fun theScrollPositionSurvivesRecreation() {
        clearTheSearch()
        allRestaurantsArrive()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { SearchScreen() }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(10)
        // A rotation, or the process restored after Android killed it: the same search, so the same position.
        restoration.emulateSavedInstanceStateRestore()
        assertEquals(10, firstShownIndex())
    }
}
