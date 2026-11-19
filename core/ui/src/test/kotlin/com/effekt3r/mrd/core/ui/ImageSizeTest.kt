package com.effekt3r.mrd.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec §5.7: the resolver asks TheMealDB for a copy sized for where the photo is shown. */
class ImageSizeTest {

    private val photo = "https://www.themealdb.com/images/media/meals/pkopc31683207947.jpg"

    @Test
    fun aMealPhotoGetsTheSizeForWhereItIsShown() {
        assertEquals("$photo/small", sizedImageUrl(photo, ImageSize.Thumbnail))
        // A card is as wide as a phone, about 1,000 px: /large (500 px) rather than /medium (350 px), which looked soft.
        assertEquals("$photo/large", sizedImageUrl(photo, ImageSize.Card))
        // Details' header is as wide as the screen too: the full-size photo, the URL as the API sends it.
        assertEquals(photo, sizedImageUrl(photo, ImageSize.Header))
    }

    @Test
    fun anyOtherUrlIsLeftAsItIs() {
        val other = "https://images.example.com/meals/abc.jpg"
        assertEquals(other, sizedImageUrl(other, ImageSize.Card))
        // Already sized, or not a meal photo: appending again would ask for a URL that doesn't exist.
        assertEquals("$photo/small", sizedImageUrl("$photo/small", ImageSize.Card))
        val ingredient = "https://www.themealdb.com/images/ingredients/Lime.png"
        assertEquals(ingredient, sizedImageUrl(ingredient, ImageSize.Card))
    }
}
