package com.effekt3r.mrd.feature.details

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme
import com.effekt3r.mrd.core.model.DailyWindow
import com.effekt3r.mrd.core.model.Menu
import com.effekt3r.mrd.core.model.MenuCategory
import com.effekt3r.mrd.core.model.MenuItem
import com.effekt3r.mrd.core.model.MenuOption
import com.effekt3r.mrd.core.model.MenuState
import com.effekt3r.mrd.core.model.Money
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.OpeningHours
import com.effekt3r.mrd.core.model.Rating
import com.effekt3r.mrd.core.model.Restaurant
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.SyncError
import com.effekt3r.mrd.core.model.WeekMinute
import java.time.DayOfWeek
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Details' states (spec §6.3), and the menu's own (spec §5.9), on the stateless screen. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rZA")
class RestaurantScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val now = Instant.parse("2026-11-16T10:00:00Z") // Monday 12:00 in Cape Town
    private val events = mutableListOf<String>()

    private val grill = Restaurant(
        summary = RestaurantSummary(
            id = RestaurantId("r-1001"),
            name = "Cape Town Grill",
            cuisines = listOf("Steakhouse", "Grill"),
            rating = Rating(46),
            deliveryFee = Money(0),
            etaMinutes = 32,
            imageUrl = null,
            openStatus = OpenStatus.Open(closesAt = WeekMinute.of(DayOfWeek.MONDAY, 22)),
            isFavourite = false
        ),
        openingHours = OpeningHours.from(DayOfWeek.entries.map { DailyWindow(it, 11 * 60, 22 * 60) })
    )

    private val menu = Menu(
        listOf(
            MenuCategory(
                "c-1",
                "Starters",
                listOf(
                    MenuItem(
                        "i-1",
                        "Grilled Halloumi",
                        Money(8900),
                        isAvailable = true,
                        description = "Citrus glaze",
                        imageUrl = null,
                        options = listOf(
                            MenuOption("o-1", "Extra lemon", Money(500)),
                            MenuOption("o-2", "Napkins", Money(0))
                        )
                    ),
                    MenuItem("i-2", "Rump 300 g", Money(18900), isAvailable = false, null, null, emptyList())
                )
            )
        )
    )

    private fun setScreen(state: DetailsUiState) = compose.setContent {
        MrdTheme {
            RestaurantScreen(
                state = state,
                onBack = { events += "back" },
                onFavouriteToggled = { events += "favourite" },
                onRetry = { events += "retry" },
                onRetryMenu = { events += "retry menu" }
            )
        }
    }

    private fun found(menu: MenuState, isOnline: Boolean = true) =
        DetailsUiState(RestaurantState.Found(grill), menu, isOnline, now)

    private fun scrollTo(text: String) = compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))

    @Test
    fun theRestaurantShowsItsStatsStatusAndHoursWithTodayFirstNamed() {
        setScreen(found(MenuState.Loading))
        compose.onNodeWithText("Cape Town Grill").assertExists()
        compose.onNodeWithContentDescription("Rating, 4.6").assertExists()
        compose.onNodeWithContentDescription("Delivery time, 32 minutes").assertExists()
        compose.onNodeWithContentDescription("Delivery fee, Free").assertExists()
        compose.onNodeWithText("Open · Closes 22:00").assertExists()
        compose.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasContentDescription("Today, Monday, 11:00 to 22:00"))
        compose.onNodeWithContentDescription("Tuesday, 11:00 to 22:00").assertExists()
    }

    @Test
    fun theFavouriteButtonTogglesIt() {
        setScreen(found(MenuState.Loading))
        compose.onNodeWithContentDescription("Favourite").performClick()
        assertEquals(listOf("favourite"), events)
    }

    @Test
    fun theMenuShowsSkeletonsWhileItLoads() {
        setScreen(found(MenuState.Loading))
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("Loading the menu"))
        compose.onNodeWithContentDescription("Loading the menu").assertExists()
    }

    @Test
    fun theMenuShowsPricesOptionsAndSoldOutDishes() {
        setScreen(found(MenuState.Content(menu, fetchedAt = now)))
        scrollTo("Grilled Halloumi")
        compose.onNodeWithText("R89").assertExists()
        compose.onNodeWithText("+ Extra lemon R5 · + Napkins Free").assertExists()
        scrollTo("Rump 300 g")
        compose.onNodeWithText("Sold out").assertExists()
    }

    @Test
    fun aRestaurantWithoutAMenuSaysSo() {
        setScreen(found(MenuState.NotAvailable))
        scrollTo("This restaurant hasn't shared a menu yet.")
    }

    @Test
    fun aMenuSeenBeforeShowsWhenItWasFetchedWhenItCannotRefresh() {
        val cached = MenuState.Content(menu, fetchedAt = now)
        setScreen(found(MenuState.Error(SyncError.Network, cached), isOnline = false))
        scrollTo("Grilled Halloumi")
        compose.onNodeWithText("Menu from", substring = true).assertExists()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(listOf("retry menu"), events)
    }

    @Test
    fun aMenuThatCouldNotBeSavedSaysSoNotThatItIsOffline() {
        setScreen(found(MenuState.Error(SyncError.Storage, cached = null)))
        scrollTo("Couldn't save the menu on this device. Free up some space and try again.")
    }

    @Test
    fun anUnknownIdIsNotFoundWithAWayBack() {
        setScreen(DetailsUiState(RestaurantState.NotFound, now = now))
        compose.onNodeWithText("Restaurant not found").assertExists()
        compose.onNodeWithText("See all restaurants").performClick()
        assertEquals(listOf("back"), events)
    }

    @Test
    fun aDeepLinkOfflineBeforeAnySyncSaysSoAndRetries() {
        setScreen(DetailsUiState(RestaurantState.Unavailable(SyncError.Network), isOnline = false, now = now))
        compose.onNodeWithText("You're offline").assertExists()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(listOf("retry"), events)
    }

    @Test
    fun aDeepLinkLoadingSaysSo() {
        setScreen(DetailsUiState(RestaurantState.Loading, now = now))
        compose.onNodeWithContentDescription("Loading the restaurant").assertExists()
        compose.onNodeWithContentDescription("Back").assertExists()
    }
}
