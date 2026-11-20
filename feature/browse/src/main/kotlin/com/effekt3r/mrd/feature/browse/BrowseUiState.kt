package com.effekt3r.mrd.feature.browse

import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.SyncStatus
import com.effekt3r.mrd.core.model.WeekMinute
import com.effekt3r.mrd.core.ui.SyncProblem
import com.effekt3r.mrd.core.ui.syncProblem
import java.time.Instant

/**
 * Everything Browse shows apart from the paged list itself (spec §6.2).
 *
 * @param query the search the list shows: the field's text, trimmed and debounced.
 * @param resultCount how many restaurants match; null until it's known.
 * @param now the current time, which moves on every minute while the screen is visible.
 */
data class BrowseUiState(
    val query: String = "",
    val openNow: Boolean = false,
    val resultCount: Int? = null,
    val syncStatus: SyncStatus = SyncStatus(lastSyncedAt = null, isRefreshing = false, lastError = null),
    val isOnline: Boolean = true,
    val now: Instant = Instant.EPOCH
) {
    /** The current minute in Cape Town, for the cards' "Opens tomorrow 11:00". */
    val weekMinute: WeekMinute get() = WeekMinute.at(now)

    /** What fills the screen under the header (spec §6.2's state table). */
    val body: BrowseBody
        get() = when {
            syncStatus.lastSyncedAt == null && syncStatus.lastError != null && !syncStatus.isRefreshing ->
                BrowseBody.FirstLoadError

            syncStatus.lastSyncedAt == null -> BrowseBody.FirstLoad

            resultCount == null || resultCount > 0 -> BrowseBody.Results

            query.isNotEmpty() || openNow -> BrowseBody.NoMatches

            else -> BrowseBody.EmptyArea
        }

    /** Why the list can't be refreshed, for the full-screen error and the banner; null when nothing's wrong. */
    val problem: SyncProblem? get() = syncProblem(syncStatus.lastError, isOnline)

    /** The banner over cached restaurants: shown when offline, or when the last refresh failed. */
    val banner: SyncProblem? get() = problem.takeIf { syncStatus.lastSyncedAt != null }
}

/** What Browse's list is filtered by: the search, trimmed and debounced, and the Open now toggle. */
data class BrowseFilters(val query: String = "", val openNow: Boolean = false)

/**
 * A restaurant in Browse's list, with the filters that found it. Each new search or toggle shows a new list, which
 * starts at the top (spec §6.2), and the grid tells the lists apart by these filters.
 */
data class BrowseListItem(val restaurant: RestaurantSummary, val filters: BrowseFilters)

enum class BrowseBody {
    /** No snapshot yet, and the first refresh is running: skeleton cards. */
    FirstLoad,

    /** No snapshot, and the first refresh failed: a full-screen error with Retry. */
    FirstLoadError,

    /** The grid. */
    Results,

    /** The filters match nothing: Clear search, and Turn off Open now when it's on. */
    NoMatches,

    /** The snapshot has no restaurants at all. */
    EmptyArea
}
