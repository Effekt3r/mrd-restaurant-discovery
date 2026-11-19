package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CircleButtonsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theFavouriteButtonPassesTheValueItTogglesTo() {
        val changes = mutableListOf<Boolean>()
        compose.setContent {
            MrdTheme { MrdFavouriteButton(isFavourite = false, onFavouriteChange = { changes += it }) }
        }
        compose.onNodeWithContentDescription("Favourite").performClick()
        assertEquals(listOf(true), changes)
    }
}
