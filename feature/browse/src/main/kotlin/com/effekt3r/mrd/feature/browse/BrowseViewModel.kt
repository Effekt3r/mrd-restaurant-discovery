package com.effekt3r.mrd.feature.browse

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.effekt3r.mrd.core.common.MinuteTicker
import com.effekt3r.mrd.core.common.NetworkMonitor
import com.effekt3r.mrd.core.data.RestaurantRepository
import com.effekt3r.mrd.core.model.RestaurantId
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Browse's state (spec §4.4): the search field, the Open now toggle, the paged list, its count and the sync status.
 * The search text and the toggle are kept in [SavedStateHandle], so they survive process death (AC-13).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repository: RestaurantRepository,
    networkMonitor: NetworkMonitor,
    ticker: MinuteTicker,
    clock: Clock,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** The search field. The screen edits it; every change is copied to [SavedStateHandle]. */
    val searchState: TextFieldState = TextFieldState(initialText = savedStateHandle[QUERY_KEY] ?: "")

    // The filters the list and the count use. The query waits until typing pauses (spec §4.4), but an empty one, as
    // Clear search makes it, applies at once. The restored filters are the initial value, so they don't wait either.
    private val filters: StateFlow<BrowseFilters> = combine(
        snapshotFlow { searchState.text.toString().trim() }
            .debounce { query -> if (query.isEmpty()) 0L else SEARCH_DEBOUNCE_MILLIS },
        savedStateHandle.getStateFlow(OPEN_NOW_KEY, false),
        ::BrowseFilters
    ).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        BrowseFilters(searchState.text.toString().trim(), savedStateHandle[OPEN_NOW_KEY] ?: false)
    )

    // The list's minute ticks. cachedIn keeps the paged flow collected for as long as this ViewModel lives, in the
    // background too, so the list can't stop a ticker of its own. The ticks come from the screen's clock in [uiState]
    // instead, which runs only while the screen collects it (spec §5.6).
    private val listTicks = MutableSharedFlow<Instant>()

    /**
     * The restaurants matching the filters, a page at a time; they reload in place when a status changes. Each carries
     * the filters that found it, so the screen can tell a new search's results from a reload of the same ones.
     */
    val restaurants: Flow<PagingData<BrowseListItem>> = filters
        .flatMapLatest { filters ->
            repository.restaurants(filters.query, filters.openNow, listTicks)
                .map { page -> page.map { BrowseListItem(it, filters) } }
        }
        .cachedIn(viewModelScope)

    /**
     * Everything else on the screen. Its upstream runs only while the screen collects it, which
     * `collectAsStateWithLifecycle` does while the screen is at least STARTED, plus 5 seconds to ride out a rotation
     * (spec §4.4). That's when the screen's clock ticks, when a failed refresh is retried on reconnecting, and when
     * becoming visible refreshes a stale snapshot (spec §5.3).
     */
    val uiState: StateFlow<BrowseUiState> = combine(
        filters,
        // Each count comes with the filters it counted, so a new filter never shows the old count, even for a frame.
        filters.flatMapLatest { filters -> repository.count(filters.query, filters.openNow).map { filters to it } },
        repository.syncStatus,
        networkMonitor.isOnline,
        ticker.ticks().onEach { listTicks.emit(it) }
    ) { filters, (countedFilters, count), syncStatus, isOnline, now ->
        val resultCount = count.takeIf { countedFilters == filters }
        BrowseUiState(filters.query, filters.openNow, resultCount, syncStatus, isOnline, now)
    }
        .onStart { viewModelScope.launch { repository.refreshIfStale() } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            BrowseUiState(query = filters.value.query, openNow = filters.value.openNow, now = clock.instant())
        )

    init {
        viewModelScope.launch {
            snapshotFlow { searchState.text.toString() }.collect { savedStateHandle[QUERY_KEY] = it }
        }
    }

    fun onOpenNowToggled(openNow: Boolean) {
        savedStateHandle[OPEN_NOW_KEY] = openNow
    }

    fun onClearSearch() {
        searchState.clearText()
    }

    /** Pull-to-refresh, and Retry on the banner or the error screen. Overlapping calls make one request. */
    fun onRefresh() {
        viewModelScope.launch { repository.refresh() }
    }

    fun onFavouriteToggled(id: RestaurantId) {
        viewModelScope.launch { repository.toggleFavourite(id) }
    }

    internal companion object {
        const val QUERY_KEY = "query"
        const val OPEN_NOW_KEY = "openNow"

        /** At most 250 ms, so the list follows typing without querying on every key (AC-2). */
        const val SEARCH_DEBOUNCE_MILLIS = 250L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
