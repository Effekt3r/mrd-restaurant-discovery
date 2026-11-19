package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalTestApi::class) // performCustomAccessibilityActionWithLabel
@RunWith(RobolectricTestRunner::class)
class RestaurantCardTest {

    @get:Rule
    val compose = createComposeRule()

    private val changes = mutableListOf<Boolean>()

    private fun setCard(isFavourite: Boolean) = compose.setContent {
        MrdTheme {
            MrdRestaurantCard(
                name = "Cape Town Grill",
                cuisines = "Steakhouse · Grill",
                rating = "4.6",
                deliveryFee = "R15 delivery",
                isFreeDelivery = false,
                status = "Open",
                statusTone = MrdStatusTone.Open,
                eta = "32 mins",
                isFavourite = isFavourite,
                contentDescription = "Cape Town Grill. Open.",
                onClick = {},
                onFavouriteChange = { changes += it }
            )
        }
    }

    @Test
    fun theCardIsOneTalkBackItemThatSaysItIsAFavourite() {
        setCard(isFavourite = true)
        val card = compose.onNodeWithContentDescription("Cape Town Grill. Open.")
        card.onChildren().assertCountEquals(0)
        card.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "In favourites"))
    }

    @Test
    fun theFavouriteActionPassesTheValueItTogglesTo() {
        setCard(isFavourite = true)
        compose.onNodeWithContentDescription("Cape Town Grill. Open.")
            .performCustomAccessibilityActionWithLabel("Remove from favourites")
        assertEquals(listOf(false), changes)
    }
}
