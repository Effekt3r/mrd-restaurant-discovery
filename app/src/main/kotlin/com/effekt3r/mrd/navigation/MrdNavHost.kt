package com.effekt3r.mrd.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.effekt3r.mrd.feature.browse.navigation.BrowseRoute
import com.effekt3r.mrd.feature.browse.navigation.browseScreen
import com.effekt3r.mrd.feature.details.navigation.navigateToRestaurant
import com.effekt3r.mrd.feature.details.navigation.restaurantScreen

/**
 * The app's navigation graph. Features don't know about each other; this is the only place where
 * Browse is connected to Details.
 */
@Composable
fun MrdNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = BrowseRoute, modifier = modifier) {
        mrdDestinations(navController)
    }
}

/** Browse, the start destination, and Details, with its deep link. */
internal fun NavGraphBuilder.mrdDestinations(navController: NavController) {
    browseScreen(onRestaurantClick = navController::navigateToRestaurant)
    restaurantScreen(onBack = navController::navigateUp)
}
