package com.effekt3r.mrd.feature.browse

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    private fun setScreen(state: BrowseUiState, restaurants: List<RestaurantSummary> = emptyList()) =
        compose.setContent {
            MrdTheme {
                BrowseScreen(
                    state = state,
                    searchState = TextFieldState(state.query),
                    restaurants = flowOf(PagingData.from(restaurants)).collectAsLazyPagingItems(),
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
}
