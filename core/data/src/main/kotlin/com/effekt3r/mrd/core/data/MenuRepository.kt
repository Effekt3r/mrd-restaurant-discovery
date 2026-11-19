package com.effekt3r.mrd.core.data

import com.effekt3r.mrd.core.model.MenuState
import com.effekt3r.mrd.core.model.RestaurantId
import kotlinx.coroutines.flow.Flow

/** Restaurants' menus (spec §5.9), fetched when Details asks and cached for offline use. */
interface MenuRepository {
    /**
     * The cached menu straight away if there is one, else Loading; then the fetch's result. Collect again to retry.
     * Failures arrive as [MenuState.Error], never as exceptions: a database failure is `SyncError.Storage`, and when the
     * cache can't even be read, that error is the only state.
     */
    fun menu(id: RestaurantId): Flow<MenuState>
}
