package com.effekt3r.mrd.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.effekt3r.mrd.core.designsystem.component.MrdImagePlaceholder

/**
 * The sizes a photo is shown at, and the copy TheMealDB serves for each (spec §5.7). Measured across the 159 photos in
 * `restaurants.json` on 2026-11-19, a photo is 700 px wide, `/large` 500 px, `/medium` 350 px and `/small` 150 px.
 * For 17 of them TheMealDB has the copies the other way round: `/large` is 350 px and `/medium` the full 700 px.
 */
enum class ImageSize(internal val suffix: String) {
    /** A dish's 64 dp thumbnail on Details' menu: `/small`. */
    Thumbnail("/small"),

    /**
     * A card's photo on Browse: `/large`. A card is as wide as a phone, about 1,000 px, where `/medium` looked soft;
     * `/large` costs about 30 KB more a card.
     */
    Card("/large"),

    /** The photo across the top of Details, as wide as the screen: the full-size photo, the URL as the API sends it. */
    Header("")
}

// A meal photo as the API sends it. Only these have sized copies; an ingredient image, or a photo already sized,
// doesn't.
private val MEAL_PHOTO = Regex("""https://www\.themealdb\.com/images/media/meals/[^/?#]+\.(?:jpe?g|png)""")

/**
 * [url] at [size]: TheMealDB serves a smaller copy of a meal photo at its URL plus `/small`, `/medium` or `/large`, and
 * the full-size photo at the URL itself. This stands in for an image CDN's resize parameters; any other URL is
 * returned as it is.
 */
fun sizedImageUrl(url: String, size: ImageSize): String = if (MEAL_PHOTO.matches(url)) url + size.suffix else url

/**
 * A restaurant's or dish's photo at [size], filling [modifier]'s bounds, cropped. The design system's placeholder, in
 * [placeholderSeed]'s colour, sits underneath, so it shows while the photo loads, when it fails, and offline when it
 * was never cached; a null [url] shows only the placeholder. Photos are decorative: the text beside them says what
 * they show.
 */
@Composable
fun RestaurantImage(url: String?, placeholderSeed: String, size: ImageSize, modifier: Modifier = Modifier) {
    Box(modifier) {
        MrdImagePlaceholder(placeholderSeed, Modifier.matchParentSize())
        if (url != null) {
            val context = LocalPlatformContext.current
            val request = remember(context, url, size) {
                ImageRequest.Builder(context).data(sizedImageUrl(url, size)).crossfade(true).build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
