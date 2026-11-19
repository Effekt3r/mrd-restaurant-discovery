package com.effekt3r.mrd.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Previews a composable at the smallest supported width (360 dp) in light, dark and at 200% font scale. */
@Preview(name = "Light", widthDp = 360, showBackground = true)
@Preview(name = "Dark", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "200% font", widthDp = 360, showBackground = true, fontScale = 2f)
annotation class MrdPreviews
