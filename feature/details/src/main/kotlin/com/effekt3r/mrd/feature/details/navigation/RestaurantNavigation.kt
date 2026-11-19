package com.effekt3r.mrd.feature.details.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.effekt3r.mrd.feature.details.RestaurantScreen
import kotlinx.serialization.Serializable

/**
 * The Details destination for one restaurant. [id] is deliberately required: Navigation turns a required
 * argument into a path segment, which the mrd://restaurant/{id} deep link depends on. A default value would
 * move it into the query string.
 */
@Serializable
data class RestaurantRoute(val id: String)

/** Opens Details for the restaurant with [id]. */
fun NavController.navigateToRestaurant(id: String) {
    navigate(RestaurantRoute(id))
}

/** Adds Details to the host's graph. Its ViewModel reads the restaurant's id from the route. */
fun NavGraphBuilder.restaurantScreen(onBack: () -> Unit) {
    composable<RestaurantRoute> {
        RestaurantScreen(onBack = onBack)
    }
}
