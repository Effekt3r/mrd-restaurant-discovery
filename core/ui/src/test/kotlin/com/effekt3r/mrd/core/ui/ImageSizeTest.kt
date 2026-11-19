package com.effekt3r.mrd.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec §5.7: the resolver asks TheMealDB for a copy sized for where the photo is shown. */
class ImageSizeTest {

    private val photo = "https://www.themealdb.com/images/media/meals/pkopc31683207947.jpg"

    @Test
    fun aMealPhotoGetsTheSizeForWhereItIsShown() {
        assertEquals("$photo/small", sizedImageUrl(photo, ImageSize.Thumbnail))
        // Cards and Details' header are as wide as a phone, about 1,000 px: the full-size photo (700 px), the URL as the
        // API sends it. TheMealDB's /large is 350 px for some photos, so only the full size is sharp on every card.
        assertEquals(photo, sizedImageUrl(photo, ImageSize.Card))
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
