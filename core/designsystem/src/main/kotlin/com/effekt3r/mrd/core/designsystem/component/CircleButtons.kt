package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.R
import com.effekt3r.mrd.core.designsystem.icon.MrdIcons
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/** A heart in a white circle, for photos. A 48 dp toggle whose state TalkBack reads (spec section 6.5). */
@Composable
fun MrdFavouriteButton(isFavourite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.designsystem_favourite)
    val state = stringResource(
        if (isFavourite) R.string.designsystem_favourite_on else R.string.designsystem_favourite_off
    )
    CircleButton(
        icon = if (isFavourite) MrdIcons.FavouriteFilled else MrdIcons.Favourite,
        modifier = modifier
            .clip(CircleShape)
            .toggleable(value = isFavourite, role = Role.Checkbox, onValueChange = { onClick() })
            .semantics {
                contentDescription = label
                stateDescription = state
            }
    )
}

/** A back arrow in a white circle, for screens that open over a photo. */
@Composable
fun MrdBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.designsystem_back)
    CircleButton(
        icon = MrdIcons.ArrowBack,
        modifier = modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
    )
}

@Composable
private fun CircleButton(icon: ImageVector, modifier: Modifier) {
    Box(modifier = modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@MrdPreviews
@Composable
private fun CircleButtonsPreview() {
    MrdTheme {
        Row(
            Modifier.background(MrdTheme.colors.imagePlaceholders[0]).padding(MrdSpacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MrdBackButton(onClick = {})
            MrdFavouriteButton(isFavourite = false, onClick = {})
            MrdFavouriteButton(isFavourite = true, onClick = {})
            Text("48 dp targets", Modifier.padding(MrdSpacing.Small))
        }
    }
}
