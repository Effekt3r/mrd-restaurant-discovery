package com.effekt3r.mrd.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The Mr D theme: Material 3 with Mr D's colours, Figtree and the extra colours in [MrdColors]. It follows the system's
 * dark mode, as `enableEdgeToEdge()` does, so the status-bar icons always contrast with the screen behind them.
 */
@Composable
fun MrdTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMrdColors provides if (darkTheme) DarkMrdColors else LightMrdColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = MrdTypography,
            content = content
        )
    }
}

/** Mr D's extra colours; Material's are in `MaterialTheme.colorScheme`. */
object MrdTheme {
    val colors: MrdColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMrdColors.current
}
