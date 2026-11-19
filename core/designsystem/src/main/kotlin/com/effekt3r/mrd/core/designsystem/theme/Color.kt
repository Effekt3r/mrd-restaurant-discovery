package com.effekt3r.mrd.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Mr D's colours, sampled from its Play Store listing (spec section 6.1), plus the tones derived from them. Sky as text
 * on white is about 1.8:1, so sky is only ever a fill behind ink text; links use [SkyDeep], sky's hue made dark
 * enough to read. ContrastTest proves every text pair and the chip outline in both themes.
 */
internal object MrdPalette {
    val Sky = Color(0xFF6DCFF6)
    val Ink = Color(0xFF1D1D1B)
    val White = Color(0xFFFFFFFF)
    val FieldGrey = Color(0xFFE7EBEE)

    /** Sky's hue and saturation at 33% lightness: links and text buttons on white. */
    val SkyDeep = Color(0xFF0A749E)

    /** Sky's hue at 82% lightness: links on ink, in the dark theme. */
    val SkyPale = Color(0xFFADE5FB)
    val InkMuted = Color(0xFF5C6670)
    val Outline = Color(0xFF858F98)
    val Green = Color(0xFF1E7B34)
    val Red = Color(0xFFB3261E)

    // The dark theme: ink surfaces, white text.
    val InkRaised = Color(0xFF2E3134)
    val MutedOnInk = Color(0xFFB4BCC4)
    val OutlineOnInk = Color(0xFF7F8891)
    val GreenOnInk = Color(0xFF7BD992)
    val RedOnInk = Color(0xFFFF8A80)

    /** Cuisine-coloured photo placeholders: pale tones on white, deep tones on ink. */
    val PlaceholdersLight = listOf(
        Color(0xFFD3F0FC),
        Color(0xFFFCE1CF),
        Color(0xFFD5F2DF),
        Color(0xFFE6DEF8),
        Color(0xFFF5E9C9),
        Color(0xFFF9D9E0)
    )
    val PlaceholdersDark = listOf(
        Color(0xFF1F4250),
        Color(0xFF4D3324),
        Color(0xFF22432D),
        Color(0xFF3A3152),
        Color(0xFF4A4127),
        Color(0xFF4B2A33)
    )
}

/** Colours Material's [ColorScheme] has no role for. Read them through [MrdTheme.colors]. */
@Immutable
data class MrdColors(
    /** Links and text buttons. */
    val link: Color,
    /** "Open" status text. */
    val open: Color,
    /** "Closed" status text and "Sold out". */
    val closed: Color,
    /** Photo placeholders, chosen per cuisine. */
    val imagePlaceholders: List<Color>,
    /** The food icon on a photo placeholder. */
    val onImagePlaceholder: Color
)

internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = MrdPalette.Ink,
    onPrimary = MrdPalette.White,
    primaryContainer = MrdPalette.Sky,
    onPrimaryContainer = MrdPalette.Ink,
    secondary = MrdPalette.SkyDeep,
    onSecondary = MrdPalette.White,
    background = MrdPalette.White,
    onBackground = MrdPalette.Ink,
    surface = MrdPalette.White,
    onSurface = MrdPalette.Ink,
    surfaceVariant = MrdPalette.FieldGrey,
    onSurfaceVariant = MrdPalette.InkMuted,
    surfaceTint = MrdPalette.White,
    surfaceContainerLowest = MrdPalette.White,
    surfaceContainerLow = MrdPalette.White,
    surfaceContainer = MrdPalette.White,
    surfaceContainerHigh = MrdPalette.White,
    surfaceContainerHighest = MrdPalette.FieldGrey,
    inverseSurface = MrdPalette.Ink,
    inverseOnSurface = MrdPalette.White,
    inversePrimary = MrdPalette.Sky,
    error = MrdPalette.Red,
    onError = MrdPalette.White,
    outline = MrdPalette.Outline,
    outlineVariant = MrdPalette.FieldGrey
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = MrdPalette.White,
    onPrimary = MrdPalette.Ink,
    primaryContainer = MrdPalette.Sky,
    onPrimaryContainer = MrdPalette.Ink,
    secondary = MrdPalette.SkyPale,
    onSecondary = MrdPalette.Ink,
    background = MrdPalette.Ink,
    onBackground = MrdPalette.White,
    surface = MrdPalette.Ink,
    onSurface = MrdPalette.White,
    surfaceVariant = MrdPalette.InkRaised,
    onSurfaceVariant = MrdPalette.MutedOnInk,
    surfaceTint = MrdPalette.Ink,
    surfaceContainerLowest = MrdPalette.Ink,
    surfaceContainerLow = MrdPalette.Ink,
    surfaceContainer = MrdPalette.Ink,
    surfaceContainerHigh = MrdPalette.Ink,
    surfaceContainerHighest = MrdPalette.InkRaised,
    inverseSurface = MrdPalette.White,
    inverseOnSurface = MrdPalette.Ink,
    inversePrimary = MrdPalette.Ink,
    error = MrdPalette.RedOnInk,
    onError = MrdPalette.Ink,
    outline = MrdPalette.OutlineOnInk,
    outlineVariant = MrdPalette.InkRaised
)

internal val LightMrdColors = MrdColors(
    link = MrdPalette.SkyDeep,
    open = MrdPalette.Green,
    closed = MrdPalette.Red,
    imagePlaceholders = MrdPalette.PlaceholdersLight,
    onImagePlaceholder = MrdPalette.Ink.copy(alpha = 0.55f)
)

internal val DarkMrdColors = MrdColors(
    link = MrdPalette.SkyPale,
    open = MrdPalette.GreenOnInk,
    closed = MrdPalette.RedOnInk,
    imagePlaceholders = MrdPalette.PlaceholdersDark,
    onImagePlaceholder = MrdPalette.White.copy(alpha = 0.6f)
)

internal val LocalMrdColors = staticCompositionLocalOf { LightMrdColors }
