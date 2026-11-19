package com.effekt3r.mrd.feature.browse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.effekt3r.mrd.core.designsystem.component.MrdFilterChip
import com.effekt3r.mrd.core.designsystem.component.MrdOfflineBanner
import com.effekt3r.mrd.core.designsystem.component.MrdSearchField
import com.effekt3r.mrd.core.designsystem.component.MrdSkeletonCard
import com.effekt3r.mrd.core.designsystem.component.MrdStateScreen
import com.effekt3r.mrd.core.designsystem.icon.MrdIcons
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme
import com.effekt3r.mrd.core.model.Money
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.Rating
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.SyncStatus
import com.effekt3r.mrd.core.model.WeekMinute
import com.effekt3r.mrd.core.ui.RestaurantCard
import com.effekt3r.mrd.core.ui.SyncProblem
import com.effekt3r.mrd.core.ui.rememberRestaurantFormatter
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.flowOf

/** Browse, with its ViewModel: the state is collected only while the screen is at least STARTED (spec §4.4). */
@Composable
internal fun BrowseScreen(onRestaurantClick: (String) -> Unit, viewModel: BrowseViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BrowseScreen(
        state = state,
        searchState = viewModel.searchState,
        restaurants = viewModel.restaurants.collectAsLazyPagingItems(),
        onOpenNowToggled = viewModel::onOpenNowToggled,
        onClearSearch = viewModel::onClearSearch,
        onRefresh = viewModel::onRefresh,
        onRestaurantClick = onRestaurantClick,
        onFavouriteToggled = viewModel::onFavouriteToggled
    )
}

/**
 * Browse (spec §6.2): a header with the title, the search field, the Open now chip and the result count, then the
 * restaurants in an adaptive grid, or whichever state replaces them. The header slides away as the list scrolls up
 * and returns as it scrolls down, so a short screen keeps room for the list. Pull to refresh works in every state.
 */
@Composable
internal fun BrowseScreen(
    state: BrowseUiState,
    searchState: TextFieldState,
    restaurants: LazyPagingItems<RestaurantSummary>,
    onOpenNowToggled: (Boolean) -> Unit,
    onClearSearch: () -> Unit,
    onRefresh: () -> Unit,
    onRestaurantClick: (String) -> Unit,
    onFavouriteToggled: (RestaurantId) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState()
) {
    ScrollToTopOnNewFilters(state, gridState)
    val header = rememberCollapsingHeaderState()
    Column(modifier.fillMaxSize().nestedScroll(header.connection)) {
        // The status bar keeps a plain surface behind its icons while the header slides away under it.
        Spacer(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.surface)
        )
        Column(header.modifier) {
            Header(state, searchState, onOpenNowToggled)
            state.banner?.let { problem -> Banner(problem, state, onRefresh) }
        }
        PullToRefreshBox(
            isRefreshing = state.syncStatus.isRefreshing && state.body != BrowseBody.FirstLoad,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f)
        ) {
            // The list, and every state screen, keeps clear of the navigation bar, the keyboard and side cutouts.
            val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            when (state.body) {
                BrowseBody.FirstLoad -> SkeletonGrid(insets)

                BrowseBody.FirstLoadError -> FirstLoadError(
                    state.problem,
                    onRefresh,
                    Modifier.windowInsetsPadding(insets)
                )

                BrowseBody.Results -> RestaurantGrid(
                    state,
                    restaurants,
                    gridState,
                    insets,
                    onRestaurantClick,
                    onFavouriteToggled
                )

                BrowseBody.NoMatches -> NoMatches(
                    state,
                    onClearSearch,
                    onTurnOffOpenNow = { onOpenNowToggled(false) },
                    modifier = Modifier.windowInsetsPadding(insets)
                )

                BrowseBody.EmptyArea -> MrdStateScreen(
                    icon = MrdIcons.Restaurant,
                    title = stringResource(R.string.browse_empty_title),
                    message = stringResource(R.string.browse_empty_message),
                    modifier = Modifier.windowInsetsPadding(insets)
                )
            }
        }
    }
}

@Composable
private fun Header(state: BrowseUiState, searchState: TextFieldState, onOpenNowToggled: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(start = MrdSpacing.Large, end = MrdSpacing.Large, top = MrdSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(MrdSpacing.Medium)
    ) {
        Text(
            text = stringResource(R.string.browse_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        MrdSearchField(searchState, placeholder = stringResource(R.string.browse_search_placeholder))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MrdSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(MrdSpacing.Small),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            MrdFilterChip(
                label = stringResource(R.string.browse_open_now),
                selected = state.openNow,
                onSelectedChange = onOpenNowToggled
            )
            // Always composed, even while the count is unknown, so TalkBack announces each new count (spec §6.5).
            Text(
                text = resultCount(state),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

@Composable
private fun resultCount(state: BrowseUiState): String {
    val count = state.resultCount
    return when {
        count == null || state.syncStatus.lastSyncedAt == null -> ""
        state.openNow -> pluralStringResource(R.plurals.browse_count_open_now, count, count)
        else -> pluralStringResource(R.plurals.browse_count_all, count, count)
    }
}

@Composable
private fun Banner(problem: SyncProblem, state: BrowseUiState, onRetry: () -> Unit) {
    val format = rememberRestaurantFormatter()
    val fetchedAt = state.syncStatus.lastSyncedAt?.let { format.fetchedAt(it, state.now, ZoneId.systemDefault()) }
    val message = when (problem) {
        SyncProblem.Offline -> stringResource(R.string.browse_banner_offline, fetchedAt.orEmpty())
        SyncProblem.CouldNotConnect -> stringResource(R.string.browse_banner_failed, fetchedAt.orEmpty())
        SyncProblem.Storage -> stringResource(R.string.browse_banner_storage)
    }
    MrdOfflineBanner(
        message = message,
        actionLabel = stringResource(R.string.browse_retry),
        onAction = onRetry,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(start = MrdSpacing.Large, end = MrdSpacing.Large, top = MrdSpacing.Medium)
    )
}

/**
 * A new search or toggle starts the list at the top; ticks and refreshes leave it where it is (spec §6.2). The last
 * filters shown are saved with the screen, so a rotation or a restored process keeps the scroll position.
 */
@Composable
private fun ScrollToTopOnNewFilters(state: BrowseUiState, gridState: LazyGridState) {
    val filters = state.query to state.openNow
    var shownFilters by rememberSaveable { mutableStateOf(filters) }
    LaunchedEffect(filters) {
        if (filters != shownFilters) {
            gridState.requestScrollToItem(0)
            shownFilters = filters
        }
    }
}

@Composable
private fun RestaurantGrid(
    state: BrowseUiState,
    restaurants: LazyPagingItems<RestaurantSummary>,
    gridState: LazyGridState,
    insets: WindowInsets,
    onRestaurantClick: (String) -> Unit,
    onFavouriteToggled: (RestaurantId) -> Unit
) {
    // Not composed until the first page arrives, so a restored scroll position isn't clamped to the top (spec §4.4).
    if (restaurants.itemCount == 0) return
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = CARD_MIN_WIDTH),
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        contentPadding = insets.add(
            WindowInsets(MrdSpacing.Large, MrdSpacing.Medium, MrdSpacing.Large, MrdSpacing.Large)
        )
            .asPaddingValues(),
        horizontalArrangement = Arrangement.spacedBy(MrdSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraLarge)
    ) {
        items(
            count = restaurants.itemCount,
            key = restaurants.itemKey { it.id.value },
            contentType = restaurants.itemContentType { RESTAURANT_CONTENT_TYPE }
        ) { index ->
            val restaurant = restaurants[index]
            if (restaurant == null) {
                MrdSkeletonCard()
            } else {
                RestaurantCard(
                    restaurant = restaurant,
                    now = state.weekMinute,
                    onClick = dropUnlessResumed { onRestaurantClick(restaurant.id.value) },
                    onToggleFavourite = { onFavouriteToggled(restaurant.id) }
                )
            }
        }
    }
}

@Composable
private fun SkeletonGrid(insets: WindowInsets) {
    val loading = stringResource(R.string.browse_loading)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = CARD_MIN_WIDTH),
        modifier = Modifier.fillMaxSize().semantics(mergeDescendants = true) { contentDescription = loading },
        contentPadding = insets.add(
            WindowInsets(MrdSpacing.Large, MrdSpacing.Medium, MrdSpacing.Large, MrdSpacing.Large)
        )
            .asPaddingValues(),
        horizontalArrangement = Arrangement.spacedBy(MrdSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraLarge),
        userScrollEnabled = false
    ) {
        items(SKELETON_COUNT) { MrdSkeletonCard() }
    }
}

@Composable
private fun FirstLoadError(problem: SyncProblem?, onRetry: () -> Unit, modifier: Modifier) {
    val (title, message) = when (problem) {
        SyncProblem.Offline -> R.string.browse_error_offline_title to R.string.browse_error_offline_message
        SyncProblem.Storage -> R.string.browse_error_storage_title to R.string.browse_error_storage_message
        SyncProblem.CouldNotConnect, null -> R.string.browse_error_failed_title to R.string.browse_error_failed_message
    }
    MrdStateScreen(
        icon = if (problem == SyncProblem.Storage) MrdIcons.Restaurant else MrdIcons.CloudOff,
        title = stringResource(title),
        message = stringResource(message),
        modifier = modifier
    ) {
        Button(onClick = onRetry) { Text(stringResource(R.string.browse_retry)) }
    }
}

@Composable
private fun NoMatches(
    state: BrowseUiState,
    onClearSearch: () -> Unit,
    onTurnOffOpenNow: () -> Unit,
    modifier: Modifier
) {
    val searching = state.query.isNotEmpty()
    MrdStateScreen(
        icon = MrdIcons.SearchOff,
        title = if (searching) {
            stringResource(R.string.browse_no_matches_title, state.query)
        } else {
            stringResource(R.string.browse_none_open_title)
        },
        message = stringResource(
            when {
                !searching -> R.string.browse_none_open_message
                state.openNow -> R.string.browse_no_matches_open_now_message
                else -> R.string.browse_no_matches_message
            }
        ),
        modifier = modifier
    ) {
        if (searching) Button(onClick = onClearSearch) { Text(stringResource(R.string.browse_clear_search)) }
        if (state.openNow) {
            if (searching) {
                // Material 3 1.4 outlines in outlineVariant, too pale on white; outline is the chip's 3:1 border.
                OutlinedButton(
                    onClick = onTurnOffOpenNow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) { Text(stringResource(R.string.browse_turn_off_open_now)) }
            } else {
                Button(onClick = onTurnOffOpenNow) { Text(stringResource(R.string.browse_turn_off_open_now)) }
            }
        }
    }
}

// One column on a phone in portrait, two or three on foldables, tablets and landscape (spec §6.2).
private val CARD_MIN_WIDTH = 300.dp
private const val SKELETON_COUNT = 4
private const val RESTAURANT_CONTENT_TYPE = "restaurant"

@MrdPreviews
@Composable
private fun BrowseScreenPreview() {
    val now = Instant.parse("2026-11-16T10:00:00Z")
    val grill = RestaurantSummary(
        id = RestaurantId("r-1001"),
        name = "Cape Town Grill",
        cuisines = listOf("Steakhouse", "Grill"),
        rating = Rating(46),
        deliveryFee = Money(1500),
        etaMinutes = 32,
        imageUrl = null,
        openStatus = OpenStatus.Open(closesAt = null),
        isFavourite = true
    )
    val cafe = grill.copy(
        id = RestaurantId("r-1171"),
        name = "Café Fynbos",
        cuisines = listOf("Café", "Breakfast"),
        rating = null,
        deliveryFee = Money(0),
        openStatus = OpenStatus.Closed(opensAt = WeekMinute.of(DayOfWeek.TUESDAY, 7, 30)),
        isFavourite = false
    )
    MrdTheme {
        Surface {
            BrowseScreen(
                state = BrowseUiState(
                    resultCount = 2,
                    syncStatus = SyncStatus(lastSyncedAt = now, isRefreshing = false, lastError = null),
                    isOnline = false,
                    now = now
                ),
                searchState = TextFieldState(),
                restaurants = flowOf(PagingData.from(listOf(grill, cafe))).collectAsLazyPagingItems(),
                onOpenNowToggled = {},
                onClearSearch = {},
                onRefresh = {},
                onRestaurantClick = {},
                onFavouriteToggled = {}
            )
        }
    }
}
