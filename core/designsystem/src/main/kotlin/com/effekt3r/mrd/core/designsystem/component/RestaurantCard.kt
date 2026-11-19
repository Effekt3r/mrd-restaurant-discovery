package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.R
import com.effekt3r.mrd.core.designsystem.icon.MrdIcons
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdShapes
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/**
 * A restaurant in Mr D's card style (spec section 6.2): a 16:9 photo with the ETA pill and the favourite heart, then
 * the name, the cuisines and a details row that wraps between its parts at large font sizes. A closed restaurant's
 * photo is dimmed.
 *
 * It takes display-ready text, not model types: milestone 5's `:core:ui` formats a restaurant and calls this. TalkBack
 * reads the whole card as one item, [contentDescription], with a custom action for the favourite (spec section 6.5).
 *
 * @param rating "4.6", or null for no rating yet, shown as "New".
 * @param deliveryFee "R15 delivery" or "Free delivery", or null to leave it out.
 * @param isFreeDelivery highlights [deliveryFee].
 * @param eta "25 mins", or null for no pill.
 * @param placeholderSeed picks the placeholder's colour, such as the first cuisine.
 * @param image the photo; null shows the placeholder.
 */
@Composable
fun MrdRestaurantCard(
    name: String,
    cuisines: String,
    rating: String?,
    deliveryFee: String?,
    isFreeDelivery: Boolean,
    status: String,
    statusTone: MrdStatusTone,
    eta: String?,
    isFavourite: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    onFavouriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholderSeed: String = cuisines,
    image: (@Composable () -> Unit)? = null
) {
    val favouriteAction = stringResource(
        if (isFavourite) R.string.designsystem_remove_from_favourites else R.string.designsystem_add_to_favourites
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MrdShapes.Card)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction(favouriteAction) {
                        onFavouriteClick()
                        true
                    }
                )
            },
        verticalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraSmall)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(MrdShapes.Card)) {
            Box(
                Modifier.matchParentSize().graphicsLayer {
                    alpha = if (statusTone == MrdStatusTone.Closed) CLOSED_PHOTO_ALPHA else 1f
                }
            ) {
                if (image != null) image() else MrdImagePlaceholder(placeholderSeed, Modifier.fillMaxSize())
            }
            if (eta != null) {
                MrdEtaPill(eta, Modifier.align(Alignment.BottomStart).padding(MrdSpacing.Small))
            }
            MrdFavouriteButton(
                isFavourite = isFavourite,
                onClick = onFavouriteClick,
                modifier = Modifier.align(Alignment.TopEnd).padding(MrdSpacing.ExtraSmall)
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = MrdSpacing.ExtraSmall)
        )
        Text(
            text = cuisines,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraSmall),
            verticalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraSmall),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            MrdRating(rating)
            if (deliveryFee != null) {
                Separator()
                if (isFreeDelivery) {
                    MrdTag(deliveryFee)
                } else {
                    Text(text = deliveryFee, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Separator()
            MrdStatusLabel(status, statusTone)
        }
    }
}

/** A photo placeholder in a colour picked by [seed], such as a cuisine, with a food icon. */
@Composable
fun MrdImagePlaceholder(seed: String, modifier: Modifier = Modifier) {
    val tones = MrdTheme.colors.imagePlaceholders
    Box(modifier.background(tones[seed.hashCode().mod(tones.size)]), contentAlignment = Alignment.Center) {
        Icon(
            MrdIcons.Restaurant,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MrdTheme.colors.onImagePlaceholder
        )
    }
}

@Composable
private fun Separator() {
    Text(
        text = stringResource(R.string.designsystem_separator),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private const val CLOSED_PHOTO_ALPHA = 0.45f

@MrdPreviews
@Composable
private fun MrdRestaurantCardPreview() {
    MrdTheme {
        Surface {
            Column(
                Modifier.padding(MrdSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraLarge)
            ) {
                MrdRestaurantCard(
                    name = "Cape Town Grill",
                    cuisines = "Steakhouse · Grill",
                    rating = "4.6",
                    deliveryFee = "R15 delivery",
                    isFreeDelivery = false,
                    status = "Open",
                    statusTone = MrdStatusTone.Open,
                    eta = "32 mins",
                    isFavourite = true,
                    contentDescription = "",
                    onClick = {},
                    onFavouriteClick = {}
                )
                MrdRestaurantCard(
                    name = "Kirstenbosch Coffee Bar",
                    cuisines = "Café · Breakfast",
                    rating = null,
                    deliveryFee = "Free delivery",
                    isFreeDelivery = true,
                    status = "Closed · Opens 07:30",
                    statusTone = MrdStatusTone.Closed,
                    eta = "16 mins",
                    isFavourite = false,
                    contentDescription = "",
                    onClick = {},
                    onFavouriteClick = {}
                )
            }
        }
    }
}
