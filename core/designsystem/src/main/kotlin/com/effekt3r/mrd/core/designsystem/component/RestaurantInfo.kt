package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.R
import com.effekt3r.mrd.core.designsystem.icon.MrdIcons
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/** How a status reads: green for open, red for closed, muted when the app can't tell (spec section 5.6). */
enum class MrdStatusTone { Open, Closed, Unknown }

/** A restaurant's status, such as "Open" or "Closed · Opens 11:00", in its tone's colour. */
@Composable
fun MrdStatusLabel(text: String, tone: MrdStatusTone, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = when (tone) {
            MrdStatusTone.Open -> MrdTheme.colors.open
            MrdStatusTone.Closed -> MrdTheme.colors.closed
            MrdStatusTone.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier
    )
}

/**
 * A star and the rating ("4.6"), or a "New" tag when [rating] is null. TalkBack reads "Rated 4.6", or "New, not rated
 * yet", rather than a bare number.
 */
@Composable
fun MrdRating(rating: String?, modifier: Modifier = Modifier) {
    if (rating == null) {
        val spoken = stringResource(R.string.designsystem_rating_new_spoken)
        MrdTag(
            stringResource(R.string.designsystem_rating_new),
            modifier.clearAndSetSemantics { contentDescription = spoken }
        )
        return
    }
    val spoken = stringResource(R.string.designsystem_rated, rating)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(MrdIcons.StarFilled, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(text = rating, style = MaterialTheme.typography.labelLarge)
    }
}

@MrdPreviews
@Composable
private fun RestaurantInfoPreview() {
    MrdTheme {
        Surface {
            Column(Modifier.padding(MrdSpacing.Large), verticalArrangement = Arrangement.spacedBy(MrdSpacing.Small)) {
                MrdRating("4.6")
                MrdRating(null)
                MrdStatusLabel("Open", MrdStatusTone.Open)
                MrdStatusLabel("Closed · Opens 11:00", MrdStatusTone.Closed)
                MrdStatusLabel("Hours unknown", MrdStatusTone.Unknown)
            }
        }
    }
}
