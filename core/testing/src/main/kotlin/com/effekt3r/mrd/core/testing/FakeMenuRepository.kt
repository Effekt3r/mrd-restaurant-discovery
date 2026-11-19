package com.effekt3r.mrd.core.testing

import com.effekt3r.mrd.core.data.MenuRepository
import com.effekt3r.mrd.core.model.MenuState
import com.effekt3r.mrd.core.model.RestaurantId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits [states] for every request and records which restaurants were asked for. */
class FakeMenuRepository(var states: List<MenuState> = listOf(MenuState.Loading)) : MenuRepository {
    val requests = mutableListOf<RestaurantId>()

    override fun menu(id: RestaurantId): Flow<MenuState> = flow {
        requests += id
        states.forEach { emit(it) }
    }
}
