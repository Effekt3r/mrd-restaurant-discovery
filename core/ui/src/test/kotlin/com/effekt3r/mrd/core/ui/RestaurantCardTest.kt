package com.effekt3r.mrd.core.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme
import com.effekt3r.mrd.core.model.Money
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.Rating
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.WeekMinute
import java.time.DayOfWeek.MONDAY
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class) // performCustomAccessibilityActionWithLabel
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rZA")
class RestaurantCardTest {

    @get:Rule
    val compose = createComposeRule()

    private var clicks = 0
    private var toggles = 0

    private val grill = RestaurantSummary(
        id = RestaurantId("r-1001"),
        name = "Cape Town Grill",
        cuisines = listOf("Steakhouse", "Grill"),
        rating = Rating(46),
        deliveryFee = Money(1500),
        etaMinutes = 32,
        imageUrl = null,
        openStatus = OpenStatus.Open(closesAt = WeekMinute.of(MONDAY, 22)),
        isFavourite = false
    )

    private val description =
        "Cape Town Grill. Steakhouse, Grill. Rated 4.6. 32 minutes. R15 delivery. Open, closes 22:00."

    private fun setCard() = compose.setContent {
        MrdTheme {
            RestaurantCard(
                restaurant = grill,
                now = WeekMinute.of(MONDAY, 12),
                onClick = { clicks++ },
                onToggleFavourite = { toggles++ }
            )
        }
    }

    @Test
    fun theCardIsOneTalkBackItemDescribingTheRestaurant() {
        setCard()
        compose.onNodeWithContentDescription(description).onChildren().assertCountEquals(0)
    }

    @Test
    fun tappingOpensItAndTheActionTogglesTheFavourite() {
        setCard()
        val card = compose.onNodeWithContentDescription(description)
        card.performClick()
        card.performCustomAccessibilityActionWithLabel("Add to favourites")
        assertEquals(1, clicks)
        assertEquals(1, toggles)
    }
}
