package com.effekt3r.mrd.feature.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt

/**
 * A header that slides away as the list scrolls up and comes back as soon as it scrolls down (Material's "enter
 * always"). The header stays whole on a tall screen, but at 200% font in landscape it would otherwise leave the list a
 * strip. Attach [connection] to the screen with `nestedScroll`, and [modifier] to the header.
 */
@Stable
internal class CollapsingHeaderState {
    // How far the header has slid up, from 0 (shown) to minus its height (gone). Read only in layout, so scrolling
    // moves the header without recomposing anything.
    private var offset by mutableFloatStateOf(0f)

    // Updated as the header is measured; a plain field, since nothing needs to recompose when it changes.
    private var height = 0

    val connection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val before = offset
            offset = (offset + available.y).coerceIn(-height.toFloat(), 0f)
            return Offset(0f, offset - before)
        }
    }

    /** Lays the header out with only its visible part's height, so the list below takes the space it gives up. */
    val modifier: Modifier = Modifier.clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        height = placeable.height
        val shift = offset.coerceAtLeast(-placeable.height.toFloat()).roundToInt()
        layout(placeable.width, placeable.height + shift) { placeable.place(0, shift) }
    }
}

@Composable
internal fun rememberCollapsingHeaderState(): CollapsingHeaderState = remember { CollapsingHeaderState() }
