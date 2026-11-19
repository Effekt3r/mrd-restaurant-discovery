package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RestaurantInfoTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aRatingTellsTalkBackWhatTheNumberIs() {
        compose.setContent { MrdTheme { MrdRating("4.6") } }
        compose.onNodeWithContentDescription("Rated 4.6").assertExists()
        compose.onNodeWithText("4.6").assertDoesNotExist()
    }

    @Test
    fun noRatingSaysItIsNew() {
        compose.setContent { MrdTheme { MrdRating(null) } }
        compose.onNodeWithContentDescription("New, not rated yet").assertExists()
    }
}
