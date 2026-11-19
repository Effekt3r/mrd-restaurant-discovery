package com.effekt3r.mrd.feature.details.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
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

/**
 * Adds Details to the host's graph, with its deep link, mrd://restaurant/{id} (spec §4.5). Navigation builds the
 * deep link's back stack from the graph, so Browse, the start destination, sits underneath. The host declares the
 * matching intent filter, since it owns the app's external entry points. Details' ViewModel reads the id from the
 * route.
 */
fun NavGraphBuilder.restaurantScreen(onBack: () -> Unit) {
    composable<RestaurantRoute>(deepLinks = listOf(navDeepLink<RestaurantRoute>(basePath = DEEP_LINK_BASE))) {
        RestaurantScreen(onBack = onBack)
    }
}

private const val DEEP_LINK_BASE = "mrd://restaurant"
