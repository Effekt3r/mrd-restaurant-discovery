package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.R
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdShapes
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/** A menu category's name, such as "Starters", marked as a heading for TalkBack. */
@Composable
fun MrdMenuCategoryHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = MrdSpacing.Large,
                end = MrdSpacing.Large,
                top = MrdSpacing.ExtraLarge,
                bottom = MrdSpacing.Small
            )
            .semantics { heading() }
    )
}

/**
 * One dish: its name, up to two lines of description, an options summary ("+ Extra lemon R5"), the price and an
 * optional 64 dp thumbnail. A sold-out dish is greyed out and says "Sold out". TalkBack reads the row as one item.
 */
@Composable
fun MrdMenuItemRow(
    name: String,
    price: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    options: String? = null,
    isSoldOut: Boolean = false,
    image: (@Composable () -> Unit)? = null
) {
    val textColor = if (isSoldOut) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = MrdSpacing.Large, vertical = MrdSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(MrdSpacing.Large)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraSmall)) {
            Text(text = name, style = MaterialTheme.typography.titleSmall, color = textColor)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (options != null) {
                Text(
                    text = options,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MrdSpacing.Small),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = price, style = MaterialTheme.typography.labelLarge, color = textColor)
                if (isSoldOut) {
                    MrdStatusLabel(stringResource(R.string.designsystem_sold_out), MrdStatusTone.Closed)
                }
            }
        }
        if (image != null) {
            Box(Modifier.size(64.dp).clip(MrdShapes.Card).graphicsLayer { alpha = if (isSoldOut) 0.45f else 1f }) {
                image()
            }
        }
    }
}

@MrdPreviews
@Composable
private fun MenuPreview() {
    MrdTheme {
        Surface {
            Column {
                MrdMenuCategoryHeader("Starters")
                MrdMenuItemRow(
                    name = "Grilled halloumi",
                    price = "R69",
                    description = "Citrus glaze, micro herbs",
                    options = "+ Extra lemon R5",
                    image = { MrdImagePlaceholder("Starters", Modifier.fillMaxSize()) }
                )
                MrdMenuItemRow(name = "Rump 250 g", price = "R189", isSoldOut = true)
            }
        }
    }
}
