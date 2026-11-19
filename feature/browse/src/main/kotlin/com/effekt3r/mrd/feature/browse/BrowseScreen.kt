package com.effekt3r.mrd.feature.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.effekt3r.mrd.core.designsystem.component.MrdFilterChip
import com.effekt3r.mrd.core.designsystem.component.MrdRestaurantCard
import com.effekt3r.mrd.core.designsystem.component.MrdSearchField
import com.effekt3r.mrd.core.designsystem.component.MrdStatusTone
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/**
 * Placeholder for Browse, restyled with the design system (milestone 4): the real header, with an inert search field
 * and an Open now chip that only toggles its look, above four sample restaurants. Photos show the placeholder until
 * images arrive. Tapping a card opens Details, and hearts toggle on this screen only. Milestone 5 replaces it.
 */
@Composable
internal fun BrowseScreen(onRestaurantClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val search = rememberTextFieldState()
    var openNow by rememberSaveable { mutableStateOf(false) }
    var favourites by rememberSaveable { mutableStateOf(emptySet<String>()) }
    Box(modifier.fillMaxSize()) {
        SampleList(
            search = search,
            openNow = openNow,
            onOpenNowChange = { openNow = it },
            favourites = favourites,
            onFavouriteClick = { id -> favourites = if (id in favourites) favourites - id else favourites + id },
            onRestaurantClick = onRestaurantClick
        )
        // The list scrolls under the transparent status bar; this strip keeps the status-bar icons on a plain surface.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Composable
private fun SampleList(
    search: TextFieldState,
    openNow: Boolean,
    onOpenNowChange: (Boolean) -> Unit,
    favourites: Set<String>,
    onFavouriteClick: (String) -> Unit,
    onRestaurantClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.safeDrawing
            .add(WindowInsets(MrdSpacing.Large, MrdSpacing.Large, MrdSpacing.Large, MrdSpacing.Large))
            .asPaddingValues(),
        horizontalArrangement = Arrangement.spacedBy(MrdSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraLarge)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(MrdSpacing.Medium)) {
                Text(
                    text = stringResource(R.string.browse_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() }
                )
                MrdSearchField(search, placeholder = stringResource(R.string.browse_search_placeholder))
                MrdFilterChip(
                    label = stringResource(R.string.browse_open_now),
                    selected = openNow,
                    onSelectedChange = onOpenNowChange
                )
            }
        }
        items(sampleRestaurants, key = { it.id }) { sample ->
            SampleRestaurantCard(
                sample = sample,
                isFavourite = sample.id in favourites,
                onClick = dropUnlessResumed { onRestaurantClick(sample.id) },
                onFavouriteClick = { onFavouriteClick(sample.id) }
            )
        }
    }
}

@Composable
private fun SampleRestaurantCard(
    sample: SampleRestaurant,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onFavouriteClick: () -> Unit
) {
    val isFree = sample.deliveryFeeCents == 0L
    val fee = if (isFree) {
        stringResource(R.string.browse_sample_free_delivery)
    } else {
        stringResource(R.string.browse_sample_delivery_fee, (sample.deliveryFeeCents / 100).toString())
    }
    val opensAt = sample.opensAt
    val description = stringResource(
        R.string.browse_sample_description,
        sample.name,
        sample.cuisines.joinToString(stringResource(R.string.browse_sample_spoken_separator)),
        sample.rating?.let { stringResource(R.string.browse_sample_rated, it) }
            ?: stringResource(R.string.browse_sample_not_rated),
        pluralStringResource(R.plurals.browse_sample_minutes, sample.etaMinutes, sample.etaMinutes),
        fee,
        opensAt?.let { stringResource(R.string.browse_sample_closed_spoken, it) }
            ?: stringResource(R.string.browse_sample_open)
    )
    MrdRestaurantCard(
        name = sample.name,
        cuisines = sample.cuisines.joinToString(stringResource(R.string.browse_sample_separator)),
        rating = sample.rating,
        deliveryFee = fee,
        isFreeDelivery = isFree,
        status = opensAt?.let { stringResource(R.string.browse_sample_closed, it) }
            ?: stringResource(R.string.browse_sample_open),
        statusTone = if (opensAt == null) MrdStatusTone.Open else MrdStatusTone.Closed,
        eta = pluralStringResource(R.plurals.browse_sample_eta, sample.etaMinutes, sample.etaMinutes),
        isFavourite = isFavourite,
        contentDescription = description,
        onClick = onClick,
        onFavouriteClick = onFavouriteClick,
        placeholderSeed = sample.cuisines.first()
    )
}

@MrdPreviews
@Composable
private fun BrowseScreenPreview() {
    MrdTheme {
        Surface {
            BrowseScreen(onRestaurantClick = {})
        }
    }
}
