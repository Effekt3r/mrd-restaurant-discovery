package com.effekt3r.mrd.feature.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed

/**
 * Placeholder for Browse (milestone 1): one sample restaurant that opens Details when tapped, which proves the
 * navigation wiring and the edge-to-edge insets. Milestone 5 replaces it with the real restaurant list.
 */
@Composable
internal fun BrowseScreen(onRestaurantClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        Text(
            text = stringResource(R.string.browse_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        ListItem(
            headlineContent = { Text(text = stringResource(R.string.browse_sample_restaurant_name)) },
            supportingContent = { Text(text = stringResource(R.string.browse_sample_restaurant_cuisines)) },
            modifier = Modifier.clickable(onClick = dropUnlessResumed { onRestaurantClick(SAMPLE_RESTAURANT_ID) })
        )
    }
}

private const val SAMPLE_RESTAURANT_ID = "r-1001"

@Preview(showBackground = true)
@Composable
private fun BrowseScreenPreview() {
    MaterialTheme {
        BrowseScreen(onRestaurantClick = {})
    }
}
