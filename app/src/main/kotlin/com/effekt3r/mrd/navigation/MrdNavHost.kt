package com.effekt3r.mrd.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.effekt3r.mrd.feature.browse.navigation.BrowseRoute
import com.effekt3r.mrd.feature.browse.navigation.browseScreen
import com.effekt3r.mrd.feature.details.navigation.navigateToRestaurant
import com.effekt3r.mrd.feature.details.navigation.restaurantScreen

/**
 * The app's navigation graph. Features don't know about each other; this is the only place where
 * Browse is connected to Details.
 */
@Composable
fun MrdNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = BrowseRoute, modifier = modifier) {
        browseScreen(onRestaurantClick = navController::navigateToRestaurant)
        restaurantScreen(onBack = navController::navigateUp)
    }
}
