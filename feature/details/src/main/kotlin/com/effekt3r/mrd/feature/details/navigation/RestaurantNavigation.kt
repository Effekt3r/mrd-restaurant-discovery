package com.effekt3r.mrd.feature.details.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.effekt3r.mrd.feature.details.RestaurantScreen
import kotlinx.serialization.Serializable

/**
 * The Details destination for one restaurant. [id] is deliberately required: Navigation turns a required
 * argument into a path segment, which the mrd://restaurant/{id} deep link (milestone 6) depends on. A default
 * value would move it into the query string.
 */
@Serializable
data class RestaurantRoute(val id: String)

/** Opens Details for the restaurant with [id]. */
fun NavController.navigateToRestaurant(id: String) {
    navigate(RestaurantRoute(id))
}

/** Adds Details to the host's graph. */
fun NavGraphBuilder.restaurantScreen(onBack: () -> Unit) {
    composable<RestaurantRoute> { entry ->
        RestaurantScreen(id = entry.toRoute<RestaurantRoute>().id, onBack = onBack)
    }
}
