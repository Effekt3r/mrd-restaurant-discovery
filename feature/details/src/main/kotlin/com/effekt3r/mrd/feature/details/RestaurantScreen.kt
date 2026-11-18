package com.effekt3r.mrd.feature.details

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

/** Placeholder for Details (milestone 1); milestone 6 replaces it. */
@Composable
internal fun RestaurantScreen(id: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(R.string.details_title, id), style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onBack) {
            Text(text = stringResource(R.string.details_back))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RestaurantScreenPreview() {
    MaterialTheme {
        RestaurantScreen(id = "r-1001", onBack = {})
    }
}
