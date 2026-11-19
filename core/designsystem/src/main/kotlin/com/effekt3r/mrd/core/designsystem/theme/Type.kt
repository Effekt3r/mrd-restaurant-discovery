package com.effekt3r.mrd.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.effekt3r.mrd.core.designsystem.R

/**
 * Figtree, bundled rather than downloaded so text renders offline (spec section 6.1). It's one variable font with a
 * weight axis from 300 to 900. Each weight below is the same file with a different weight setting, which Android
 * applies from API 26, the app's minimum.
 */
internal val Figtree = FontFamily(
    figtree(FontWeight.Normal),
    figtree(FontWeight.Medium),
    figtree(FontWeight.SemiBold),
    figtree(FontWeight.Bold),
    figtree(FontWeight.ExtraBold)
)

// Variable-font settings are still marked experimental in Compose 1.11; Google's fonts guide opts in the same way.
@OptIn(ExperimentalTextApi::class)
private fun figtree(weight: FontWeight) = Font(
    resId = R.font.designsystem_figtree,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

/** Material's type scale in Figtree: extra-bold headings, bold titles, regular body text and semi-bold labels. */
internal val MrdTypography: Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.figtree(FontWeight.ExtraBold),
        displayMedium = displayMedium.figtree(FontWeight.ExtraBold),
        displaySmall = displaySmall.figtree(FontWeight.ExtraBold),
        headlineLarge = headlineLarge.figtree(FontWeight.ExtraBold),
        headlineMedium = headlineMedium.figtree(FontWeight.ExtraBold),
        headlineSmall = headlineSmall.figtree(FontWeight.ExtraBold),
        titleLarge = titleLarge.figtree(FontWeight.ExtraBold),
        titleMedium = titleMedium.figtree(FontWeight.Bold),
        titleSmall = titleSmall.figtree(FontWeight.Bold),
        bodyLarge = bodyLarge.figtree(FontWeight.Normal),
        bodyMedium = bodyMedium.figtree(FontWeight.Normal),
        bodySmall = bodySmall.figtree(FontWeight.Normal),
        labelLarge = labelLarge.figtree(FontWeight.SemiBold),
        labelMedium = labelMedium.figtree(FontWeight.SemiBold),
        labelSmall = labelSmall.figtree(FontWeight.SemiBold)
    )
}

private fun TextStyle.figtree(weight: FontWeight) = copy(fontFamily = Figtree, fontWeight = weight)
