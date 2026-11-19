package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.effekt3r.mrd.core.designsystem.icon.MrdIcons
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdShapes
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/**
 * The banner over cached content when the last refresh failed ("You're offline · Showing restaurants from 10:42"),
 * with an action such as Retry. TalkBack announces it when it appears.
 */
@Composable
fun MrdOfflineBanner(message: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.primaryContainer, MrdShapes.Field)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = MrdSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(MrdIcons.CloudOff, contentDescription = null, tint = colors.onPrimaryContainer)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onPrimaryContainer,
            modifier = Modifier.weight(1f).padding(horizontal = MrdSpacing.Small, vertical = MrdSpacing.Medium)
        )
        TextButton(
            onClick = onAction,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.onPrimaryContainer)
        ) {
            Text(actionLabel)
        }
    }
}

@MrdPreviews
@Composable
private fun MrdOfflineBannerPreview() {
    MrdTheme {
        Surface {
            MrdOfflineBanner(
                message = "You're offline · Showing restaurants from 10:42",
                actionLabel = "Retry",
                onAction = {},
                modifier = Modifier.padding(MrdSpacing.Large)
            )
        }
    }
}
