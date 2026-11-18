package com.effekt3r.mrd.feature.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Placeholder for Browse (milestone 1). It proves the navigation wiring and the edge-to-edge insets;
 * milestone 5 replaces it with the restaurant list.
 */
@Composable
internal fun BrowseScreen(onRestaurantClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(R.string.browse_title), style = MaterialTheme.typography.headlineMedium)
        Button(onClick = { onRestaurantClick(SAMPLE_RESTAURANT_ID) }) {
            Text(text = stringResource(R.string.browse_placeholder_open_sample))
        }
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
