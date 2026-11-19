package com.effekt3r.mrd.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.effekt3r.mrd.core.designsystem.component.MrdBackButton
import com.effekt3r.mrd.core.designsystem.component.MrdFavouriteButton
import com.effekt3r.mrd.core.designsystem.component.MrdImagePlaceholder
import com.effekt3r.mrd.core.designsystem.component.MrdMenuCategoryHeader
import com.effekt3r.mrd.core.designsystem.component.MrdMenuItemRow
import com.effekt3r.mrd.core.designsystem.component.MrdRating
import com.effekt3r.mrd.core.designsystem.component.MrdStatusLabel
import com.effekt3r.mrd.core.designsystem.component.MrdStatusTone
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/**
 * Placeholder for Details, restyled with the design system (milestone 4): Cape Town Grill (r-1001) and a sample of its
 * menu, whichever restaurant [id] was opened; the heart toggles for this screen only. Milestone 6 replaces it with the
 * restaurant's own details and menu.
 */
@Composable
internal fun RestaurantScreen(id: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var isFavourite by rememberSaveable(id) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val headerScrolledAway by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    Box(modifier.fillMaxSize()) {
        SampleDetails(listState, isFavourite, onFavouriteClick = { isFavourite = !isFavourite }, onBack = onBack)
        // The header sits under the status bar; once it has scrolled away, a surface strip keeps the icons off the text.
        if (headerScrolledAway) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
private fun SampleDetails(
    listState: LazyListState,
    isFavourite: Boolean,
    onFavouriteClick: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            .asPaddingValues()
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(240.dp)) {
                MrdImagePlaceholder(seed = "Steakhouse", modifier = Modifier.fillMaxSize())
                MrdBackButton(
                    onClick = dropUnlessResumed { onBack() },
                    modifier = Modifier
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top + WindowInsetsSides.Start
                            )
                        )
                        .padding(MrdSpacing.Small)
                )
                MrdFavouriteButton(
                    isFavourite = isFavourite,
                    onClick = onFavouriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                        )
                        .padding(MrdSpacing.Small)
                )
            }
        }
        item {
            Column(
                modifier = Modifier.padding(MrdSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(MrdSpacing.Small)
            ) {
                Text(
                    text = stringResource(R.string.details_sample_name),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(R.string.details_sample_cuisines),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraSmall),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    MrdRating(stringResource(R.string.details_sample_rating))
                    Text(
                        text = stringResource(R.string.details_sample_eta_and_fee),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                MrdStatusLabel(stringResource(R.string.details_sample_status), MrdStatusTone.Open)
            }
        }
        item {
            Text(
                text = stringResource(R.string.details_menu),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = MrdSpacing.Large).semantics { heading() }
            )
        }
        sampleMenu.forEach { category ->
            item { MrdMenuCategoryHeader(stringResource(category.name)) }
            category.dishes.forEach { dish ->
                item {
                    val name = stringResource(dish.name)
                    MrdMenuItemRow(
                        name = name,
                        price = stringResource(dish.price),
                        description = dish.description?.let { stringResource(it) },
                        options = dish.options?.let { stringResource(it) },
                        isSoldOut = dish.isSoldOut,
                        image = if (dish.hasPhoto) photoPlaceholder(seed = name) else null
                    )
                }
            }
        }
    }
}

/** A dish's photo slot, filled with a placeholder until milestone 6 loads real photos. */
private fun photoPlaceholder(seed: String): @Composable () -> Unit = {
    MrdImagePlaceholder(seed = seed, modifier = Modifier.fillMaxSize())
}

private class SampleCategory(val name: Int, val dishes: List<SampleDish>)

private class SampleDish(
    val name: Int,
    val price: Int,
    val description: Int? = null,
    val options: Int? = null,
    val isSoldOut: Boolean = false,
    val hasPhoto: Boolean = false
)

/** Part of a steakhouse menu, in the shape of menus-design.md's generated menus. */
private val sampleMenu = listOf(
    SampleCategory(
        R.string.details_sample_starters,
        listOf(
            SampleDish(
                R.string.details_sample_halloumi,
                R.string.details_sample_halloumi_price,
                description = R.string.details_sample_halloumi_description,
                options = R.string.details_sample_halloumi_options,
                hasPhoto = true
            ),
            SampleDish(
                R.string.details_sample_livers,
                R.string.details_sample_livers_price,
                description = R.string.details_sample_livers_description
            )
        )
    ),
    SampleCategory(
        R.string.details_sample_steaks,
        listOf(
            SampleDish(
                R.string.details_sample_sirloin,
                R.string.details_sample_sirloin_price,
                description = R.string.details_sample_sirloin_description,
                options = R.string.details_sample_sirloin_options,
                hasPhoto = true
            ),
            SampleDish(R.string.details_sample_rump, R.string.details_sample_rump_price, isSoldOut = true)
        )
    ),
    SampleCategory(
        R.string.details_sample_desserts,
        listOf(
            SampleDish(
                R.string.details_sample_malva,
                R.string.details_sample_malva_price,
                description = R.string.details_sample_malva_description
            )
        )
    )
)

@MrdPreviews
@Composable
private fun RestaurantScreenPreview() {
    MrdTheme {
        Surface {
            RestaurantScreen(id = "r-1001", onBack = {})
        }
    }
}
