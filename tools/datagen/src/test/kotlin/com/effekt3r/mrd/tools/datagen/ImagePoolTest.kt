package com.effekt3r.mrd.tools.datagen

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the committed image-pool.json, which the generator reads instead of the network. */
class ImagePoolTest {

    private val pool = ImagePool.load()

    @Test
    fun thePoolHasExactlyTheQueriedKeysInOrder() {
        assertEquals(ImagePoolFetcher.QUERIES.keys.sorted(), pool.photos.keys.toList())
    }

    @Test
    fun everyKeyHasAtLeastFivePhotos() {
        pool.photos.forEach { (key, photos) ->
            assertTrue("$key has ${photos.size} photos", photos.size >= ImagePoolFetcher.MIN_PHOTOS)
        }
    }

    @Test
    fun everyPhotoIsACanonicalHttpsUrlAndEachListIsSortedAndDistinct() {
        pool.photos.forEach { (key, photos) ->
            photos.forEach { assertTrue("$key: $it", ImagePoolFetcher.PHOTO_URL.matches(it)) }
            assertEquals("$key isn't sorted and distinct", photos.distinct().sorted(), photos)
        }
    }

    @Test
    fun theFileIsFormattedExactlyAsFetchImagesWritesIt() {
        val text = ImagePool.resourceText()
        assertEquals(JsonWriter.pretty(Json.parseToJsonElement(text)), text)
    }
}
