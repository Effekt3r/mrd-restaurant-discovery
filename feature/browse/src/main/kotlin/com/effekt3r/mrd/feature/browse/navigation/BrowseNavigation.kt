package com.effekt3r.mrd.feature.browse.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.effekt3r.mrd.feature.browse.BrowseScreen
import kotlinx.serialization.Serializable

/** The Browse destination. The host app starts its navigation graph here. */
@Serializable
data object BrowseRoute

/** Adds Browse to the host's graph. [onRestaurantClick] receives the id of the restaurant tapped. */
fun NavGraphBuilder.browseScreen(onRestaurantClick: (String) -> Unit) {
    composable<BrowseRoute> {
        BrowseScreen(onRestaurantClick = onRestaurantClick)
    }
}
