package com.effekt3r.mrd.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.effekt3r.mrd.core.common.MinuteTicker
import com.effekt3r.mrd.core.common.NetworkMonitor
import com.effekt3r.mrd.core.data.MenuRepository
import com.effekt3r.mrd.core.data.RestaurantRepository
import com.effekt3r.mrd.core.model.MenuState
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.feature.details.navigation.RestaurantRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Details' state for the restaurant in [RestaurantRoute] (spec §4.4, §6.3): the restaurant from the list's cache, its
 * menu, connectivity for the error wording, and the time for the status line and today's hours.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val restaurantRepository: RestaurantRepository,
    menuRepository: MenuRepository,
    networkMonitor: NetworkMonitor,
    ticker: MinuteTicker,
    clock: Clock
) : ViewModel() {

    private val id = RestaurantId(savedStateHandle.toRoute<RestaurantRoute>().id)

    // Collecting the menu again retries it (MenuRepository); each retry bumps this.
    private val menuAttempts = MutableStateFlow(0)

    /**
     * The menu, fetched again on each attempt. When the connection returns, a new attempt starts unless the latest has
     * finished with the menu, or with none to show (spec §5.9), so the menu doesn't keep blaming a connection that's
     * back. That covers a fetch that has failed, and one still running, which began before the connection returned and
     * would fail after it, whether or not a cached menu is showing.
     */
    private val menu: Flow<MenuState> = channelFlow {
        var latest: MenuState = MenuState.Loading
        var fetching = false
        launch {
            // Only a change back to online counts, never the first value.
            networkMonitor.isOnline.drop(1).filter { it }.collect {
                val loaded = !fetching && (latest is MenuState.Content || latest is MenuState.NotAvailable)
                if (!loaded) menuAttempts.update { it + 1 }
            }
        }
        // collectLatest cancels the attempt that's running when a new one starts. Its block stores each state before it
        // marks the fetch finished, so the check above never sees a finished fetch with a state from before its result.
        menuAttempts.collectLatest {
            fetching = true
            menuRepository.menu(id).collect { state ->
                latest = state
                send(state)
            }
            fetching = false
        }
    }

    private val restaurant: Flow<RestaurantState> =
        combine(restaurantRepository.restaurant(id), restaurantRepository.syncStatus, ::Pair)
            .transformLatest { (restaurant, sync) ->
                when {
                    restaurant != null -> emit(RestaurantState.Found(restaurant))

                    sync.lastSyncedAt == null -> {
                        val error = sync.lastError
                        val failed = error != null && !sync.isRefreshing
                        emit(if (failed) RestaurantState.Unavailable(error) else RestaurantState.Loading)
                    }

                    // When the first snapshot lands, the snapshot's time can arrive before the restaurant's own query
                    // has run again. Asking once more keeps a restaurant that's there from flashing "not found".
                    else -> emit(
                        restaurantRepository.restaurant(id).first()?.let(RestaurantState::Found)
                            ?: RestaurantState.NotFound
                    )
                }
            }

    /**
     * The screen's state. Its upstream runs only while the screen collects it (`collectAsStateWithLifecycle`, plus 5
     * seconds for a rotation): that's when the clock ticks, the menu is fetched and retried on reconnecting, and a deep
     * link to a restaurant that isn't cached yet starts the first sync.
     */
    val uiState: StateFlow<DetailsUiState> = combine(
        restaurant,
        menu,
        networkMonitor.isOnline,
        ticker.ticks(),
        ::DetailsUiState
    )
        .onStart { viewModelScope.launch { syncIfNothingIsCached() } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            DetailsUiState(now = clock.instant())
        )

    fun onFavouriteToggled() {
        viewModelScope.launch { restaurantRepository.toggleFavourite(id) }
    }

    /** Retry for a restaurant that isn't cached: fetches the list again. */
    fun onRetry() {
        viewModelScope.launch { restaurantRepository.refresh() }
    }

    fun onRetryMenu() {
        menuAttempts.update { it + 1 }
    }

    // Spec §5.3: Details opened for an id that isn't cached, before any sync (a deep link on a fresh install), fetches
    // the list. Once a snapshot exists, a missing id is simply not found.
    private suspend fun syncIfNothingIsCached() {
        if (restaurantRepository.restaurant(id).first() == null &&
            restaurantRepository.syncStatus.first().lastSyncedAt == null
        ) {
            restaurantRepository.refresh()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
