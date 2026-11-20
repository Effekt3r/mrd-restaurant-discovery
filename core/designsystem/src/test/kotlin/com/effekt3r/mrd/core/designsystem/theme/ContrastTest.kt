package com.effekt3r.mrd.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.2 contrast in both themes: at least 4.5:1 for every text colour on the background the design system puts it
 * on (spec section 6.5), and 3:1 for the outline that marks a chip that's off (non-text contrast).
 */
class ContrastTest {

    @Test
    fun textMeetsAaInTheLightTheme() = assertTextContrast(LightColorScheme, LightMrdColors)

    @Test
    fun textMeetsAaInTheDarkTheme() = assertTextContrast(DarkColorScheme, DarkMrdColors)

    @Test
    fun theOffChipOutlineIsVisibleInBothThemes() {
        assertTrue(contrast(LightColorScheme.outline, LightColorScheme.surface) >= 3.0)
        assertTrue(contrast(DarkColorScheme.outline, DarkColorScheme.surface) >= 3.0)
    }

    @Test
    fun skyFailsAsTextOnWhiteSoItIsOnlyEverAFill() {
        // Spec section 6.1: about 1.8:1. That's why no text colour in either scheme is sky.
        assertTrue(contrast(MrdPalette.Sky, MrdPalette.White) < 2.0)
    }

    private fun assertTextContrast(scheme: ColorScheme, mrd: MrdColors) {
        val pairs = mapOf(
            "text on the screen" to (scheme.onSurface to scheme.surface),
            "secondary text on the screen" to (scheme.onSurfaceVariant to scheme.surface),
            "text in the search field" to (scheme.onSurface to scheme.surfaceVariant),
            "the search field's placeholder" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
            "a chip that's on, and buttons" to (scheme.onPrimary to scheme.primary),
            "tags and the offline banner (sky)" to (scheme.onPrimaryContainer to scheme.primaryContainer),
            "errors" to (scheme.onError to scheme.error),
            "error containers" to (scheme.onErrorContainer to scheme.errorContainer),
            "tonal buttons and stock chips (secondary containers)" to
                (scheme.onSecondaryContainer to scheme.secondaryContainer),
            "tertiary accents" to (scheme.onTertiary to scheme.tertiary),
            "tertiary containers" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
            "primary fixed" to (scheme.onPrimaryFixed to scheme.primaryFixed),
            "primary fixed, variant on dim" to (scheme.onPrimaryFixedVariant to scheme.primaryFixedDim),
            "secondary fixed" to (scheme.onSecondaryFixed to scheme.secondaryFixed),
            "secondary fixed, variant on dim" to (scheme.onSecondaryFixedVariant to scheme.secondaryFixedDim),
            "tertiary fixed" to (scheme.onTertiaryFixed to scheme.tertiaryFixed),
            "tertiary fixed, variant on dim" to (scheme.onTertiaryFixedVariant to scheme.tertiaryFixedDim),
            "links" to (mrd.link to scheme.surface),
            "Open" to (mrd.open to scheme.surface),
            "Closed and Sold out" to (mrd.closed to scheme.surface)
        )
        val failures = pairs.mapNotNull { (use, colours) ->
            val ratio = contrast(colours.first, colours.second)
            if (ratio < 4.5) "$use: ${"%.2f".format(ratio)}:1" else null
        }
        assertTrue("Text below WCAG AA's 4.5:1:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    /** WCAG 2.2's contrast ratio, from 1:1 to 21:1. */
    private fun contrast(a: Color, b: Color): Double {
        val (lighter, darker) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun linear(channel: Float): Double =
            if (channel <= 0.04045f) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }
}
