package com.effekt3r.mrd.core.designsystem.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.effekt3r.mrd.core.designsystem.R

/**
 * The app's icons: Google's Material Symbols (rounded, 24 dp, Apache 2.0), committed as vector drawables. `Icon` tints
 * them with the current content colour.
 */
object MrdIcons {
    val ArrowBack: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_arrow_back)
    val Close: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_close)
    val CloudOff: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_cloud_off)
    val Favourite: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_favourite)
    val FavouriteFilled: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_favourite_filled)
    val Restaurant: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_restaurant)
    val Search: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_search)
    val SearchOff: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_search_off)
    val StarFilled: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_star_filled)
}
