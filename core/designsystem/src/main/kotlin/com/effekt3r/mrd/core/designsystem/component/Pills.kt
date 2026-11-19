package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdShapes
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/** A filter that's on or off, such as Open now: filled with ink when on, outlined when off. Its touch target is 48 dp. */
@Composable
fun MrdFilterChip(
    label: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(MrdShapes.Pill)
            .background(if (selected) colors.primary else colors.surface)
            .border(1.dp, if (selected) colors.primary else colors.outline, MrdShapes.Pill)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = onSelectedChange)
            .padding(horizontal = MrdSpacing.Large, vertical = MrdSpacing.Small),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onPrimary else colors.onSurface
        )
    }
}

/** The white "25 mins" pill that sits on a photo's bottom-left corner. */
@Composable
fun MrdEtaPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MrdShapes.Pill)
            .padding(horizontal = MrdSpacing.Small, vertical = MrdSpacing.ExtraSmall)
    )
}

/** A highlight such as "Free delivery" or "New": ink text on sky, the only way sky appears behind text. */
@Composable
fun MrdTag(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        maxLines = 1,
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer, MrdShapes.Pill)
            .padding(horizontal = MrdSpacing.Small, vertical = 2.dp)
    )
}

@MrdPreviews
@Composable
private fun PillsPreview() {
    MrdTheme {
        Surface {
            Row(Modifier.padding(MrdSpacing.Large), verticalAlignment = Alignment.CenterVertically) {
                MrdFilterChip("Open now", selected = true, onSelectedChange = {})
                MrdFilterChip("Open now", selected = false, onSelectedChange = {})
                MrdEtaPill("25 mins", Modifier.padding(MrdSpacing.Small))
                MrdTag("Free delivery")
            }
        }
    }
}
