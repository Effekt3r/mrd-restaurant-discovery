package com.effekt3r.mrd.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** The spacing steps (spec section 6.1). Layouts use these, never other values. */
object MrdSpacing {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 24.dp
}

/** Fields and image cards have 12 dp corners; chips and pills are fully rounded (spec section 6.1). */
object MrdShapes {
    val Card = RoundedCornerShape(12.dp)
    val Field = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(percent = 50)
}
