package com.effekt3r.mrd.tools.datagen

import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagePoolFetcherTest {

    @Test
    fun queriesBecomeFilterAndSearchUrls() {
        val api = "https://www.themealdb.com/api/json/v1/1"
        assertEquals("$api/filter.php?c=Beef", ImagePoolFetcher.url("c=Beef"))
        assertEquals("$api/filter.php?a=United%20States", ImagePoolFetcher.url("a=United States"))
        assertEquals("$api/search.php?s=pizza", ImagePoolFetcher.url("s=pizza"))
    }

    @Test
    fun photosAreReadFromTheResponseAndNoResultsIsAnEmptyList() {
        assertEquals(listOf(photo("a"), photo("b")), ImagePoolFetcher.photos(response(photo("a"), photo("b"))))
        assertEquals(emptyList<String>(), ImagePoolFetcher.photos("""{"meals":null}"""))
    }

    @Test
    fun thePoolIsSortedDistinctAndHoldsOnlyCanonicalPhotoUrls() {
        // Every query answers with the same photos: out of order, one twice, and three URLs the pool must reject.
        val answer = response(
            photo("e"),
            photo("c"),
            photo("a"),
            photo("d"),
            photo("b"),
            photo("a"),
            "http://www.themealdb.com/images/media/meals/f.jpg",
            photo("g") + "/medium",
            "https://www.themealdb.com/images/media/meals/h.png"
        )
        val pool = ImagePool.parse(ImagePoolFetcher.fetch { answer })
        assertEquals(ImagePoolFetcher.QUERIES.keys.sorted(), pool.photos.keys.toList())
        pool.photos.values.forEach { assertEquals(listOf("a", "b", "c", "d", "e").map(::photo), it) }
    }

    @Test
    fun aKeyWithFewerThanFivePhotosFailsTheFetchAndIsNamed() {
        val enough = response(photo("a"), photo("b"), photo("c"), photo("d"), photo("e"))
        val error = assertThrows(IllegalStateException::class.java) {
            ImagePoolFetcher.fetch { url -> if (url.endsWith("a=Greek")) response(photo("a")) else enough }
        }
        assertTrue(error.message, error.message!!.endsWith("Add a query for 'greek' (1)"))
    }

    private fun photo(name: String) = "https://www.themealdb.com/images/media/meals/$name.jpg"

    private fun response(vararg photos: String) = buildJsonObject {
        putJsonArray("meals") {
            photos.forEach { photo ->
                addJsonObject {
                    put("strMeal", "A meal")
                    put("strMealThumb", photo)
                    put("idMeal", "52772")
                }
            }
        }
    }.toString()
}
