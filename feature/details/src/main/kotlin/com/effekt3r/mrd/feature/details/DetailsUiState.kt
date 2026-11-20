package com.effekt3r.mrd.feature.details

import com.effekt3r.mrd.core.model.MenuState
import com.effekt3r.mrd.core.model.Restaurant
import com.effekt3r.mrd.core.model.SyncError
import com.effekt3r.mrd.core.model.WeekMinute
import com.effekt3r.mrd.core.ui.SyncProblem
import com.effekt3r.mrd.core.ui.syncProblem
import java.time.Instant

/**
 * Everything Details shows (spec §6.3): the restaurant, from the list's cache, and its menu, which loads on its own
 * and never holds the restaurant up.
 *
 * @param now the current time, which moves on every minute while the screen is visible.
 */
data class DetailsUiState(
    val restaurant: RestaurantState = RestaurantState.Loading,
    val menu: MenuState = MenuState.Loading,
    val isOnline: Boolean = true,
    val now: Instant = Instant.EPOCH
) {
    /** The current minute in Cape Town: the status line's next change, and which day of the hours is today. */
    val weekMinute: WeekMinute get() = WeekMinute.at(now)

    /** Why the restaurant can't be shown, when it isn't cached and couldn't be fetched. */
    val restaurantProblem: SyncProblem?
        get() = (restaurant as? RestaurantState.Unavailable)?.let { syncProblem(it.error, isOnline) }

    /** Why the menu couldn't be fetched, when it failed. */
    val menuProblem: SyncProblem? get() = (menu as? MenuState.Error)?.let { syncProblem(it.error, isOnline) }
}

sealed interface RestaurantState {
    /** Not cached yet, and the first sync is still running: a deep link on a fresh install (spec §5.3). */
    data object Loading : RestaurantState

    data class Found(val restaurant: Restaurant) : RestaurantState

    /** The snapshot has no restaurant with this id (AC-12). */
    data object NotFound : RestaurantState

    /** Not cached, and the sync that would fetch it failed, as when a deep link opens offline (AC-12). */
    data class Unavailable(val error: SyncError) : RestaurantState
}
