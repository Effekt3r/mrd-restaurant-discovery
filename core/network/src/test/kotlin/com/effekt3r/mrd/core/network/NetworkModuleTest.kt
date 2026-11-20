package com.effekt3r.mrd.core.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil3.request.ImageRequest
import dagger.Lazy
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import okio.ByteString.Companion.decodeBase64
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NetworkModuleTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val server = MockWebServer().apply { start() }

    @After
    fun tearDown() {
        server.close()
    }

    // Spec §5.3. The read timeout allows for the 6 MB stress file; the sync tests use their own, shorter ones.
    @Test
    fun theClientTimesOutAfter10SecondsToConnectAnd20ToRead() {
        val client = NetworkModule.okHttpClient(context)
        assertEquals(10_000, client.connectTimeoutMillis)
        assertEquals(20_000, client.readTimeoutMillis)
    }

    // Spec §5.7: photos share the client's connections, but never its HTTP cache, which holds the list's ETag.
    @Test
    fun photosShareTheClientsConnectionsButNotItsHttpCache() = runTest {
        val client = NetworkModule.okHttpClient(context)
        val loader = NetworkModule.imageLoader(context, Lazy { client })
        // A photo as TheMealDB serves it: cacheable, with an ETag.
        server.enqueue(
            MockResponse.Builder()
                .addHeader("Cache-Control", "max-age=3600")
                .addHeader("ETag", "\"photo\"")
                .addHeader("Content-Type", "image/png")
                .body(Buffer().write(ONE_PIXEL_PNG))
                .build()
        )

        loader.execute(ImageRequest.Builder(context).data(server.url("/photo.png").toString()).build())

        assertEquals("the loader fetched the photo", 1, server.requestCount)
        assertEquals("on the shared client's connections", 1, client.connectionPool.connectionCount())
        assertEquals("but kept it out of that client's HTTP cache", 0, client.cache!!.urls().asSequence().count())
    }

    private companion object {
        val ONE_PIXEL_PNG = requireNotNull(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
                .decodeBase64()
        )
    }
}
