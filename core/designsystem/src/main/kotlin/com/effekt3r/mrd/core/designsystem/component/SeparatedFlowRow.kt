package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing

/**
 * [items] in a row with a [separator] between each pair, wrapping onto more lines like a `FlowRow`. A separator never
 * starts or ends a line: when the item after it wraps, the separator is left out, so a details row at 200% font reads
 * "★ 4.6 · R12 delivery", then "Closed · Opens 07:30", with no "·" dangling at either edge.
 *
 * Items in a line are centred vertically. Separators are hidden from TalkBack, which reads the items alone.
 */
@Composable
internal fun SeparatedFlowRow(
    separator: @Composable () -> Unit,
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = MrdSpacing.ExtraSmall,
    verticalSpacing: Dp = MrdSpacing.ExtraSmall
) {
    // Each item and separator is wrapped in a Box, so each is exactly one measurable whatever its content emits.
    Layout(
        contents = listOf(
            { items.forEach { item -> Box { item() } } },
            { repeat((items.size - 1).coerceAtLeast(0)) { Box(Modifier.clearAndSetSemantics {}) { separator() } } }
        ),
        modifier = modifier
    ) { (itemMeasurables, separatorMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val itemPlaceables = itemMeasurables.map { it.measure(loose) }
        val separatorPlaceables = separatorMeasurables.map { it.measure(loose) }
        val gap = horizontalSpacing.roundToPx()
        val lineGap = verticalSpacing.roundToPx()
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity

        // Each line lists what it places, left to right: the separator before an item travels with it, unless the
        // item starts the line.
        val lines = mutableListOf(mutableListOf<Placeable>())
        var lineWidth = 0
        itemPlaceables.forEachIndexed { index, item ->
            val separator = separatorPlaceables.getOrNull(index - 1)
            val line = lines.last()
            when {
                line.isEmpty() -> lineWidth = item.width.also { line += item }

                separator != null && lineWidth + gap + separator.width + gap + item.width <= maxWidth -> {
                    line += separator
                    line += item
                    lineWidth += gap + separator.width + gap + item.width
                }

                else -> {
                    lines += mutableListOf(item)
                    lineWidth = item.width
                }
            }
        }

        val lineHeights = lines.map { line -> line.maxOfOrNull { it.height } ?: 0 }
        val width = lines.maxOf { line -> line.sumOf { it.width } + gap * (line.size - 1).coerceAtLeast(0) }
        val height = lineHeights.sum() + lineGap * (lines.size - 1)
        layout(
            width.coerceIn(constraints.minWidth, constraints.maxWidth),
            height.coerceIn(constraints.minHeight, constraints.maxHeight)
        ) {
            var y = 0
            lines.forEachIndexed { lineIndex, line ->
                var x = 0
                line.forEach { placeable ->
                    placeable.placeRelative(x, y + (lineHeights[lineIndex] - placeable.height) / 2)
                    x += placeable.width + gap
                }
                y += lineHeights[lineIndex] + lineGap
            }
        }
    }
}
