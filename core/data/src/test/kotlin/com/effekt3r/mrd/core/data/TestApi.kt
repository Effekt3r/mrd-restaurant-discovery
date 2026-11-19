package com.effekt3r.mrd.core.data

import com.effekt3r.mrd.core.data.network.DiscoveryApi
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit

internal val testJson = Json { ignoreUnknownKeys = true }

/** The production API interface against [server], with an OkHttp disk cache in [cacheDir] so ETags work. */
internal fun testApi(server: MockWebServer, cacheDir: File, readTimeoutMillis: Long = 5_000): DiscoveryApi =
    Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(
            OkHttpClient.Builder()
                .cache(Cache(cacheDir, 1024 * 1024))
                .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
                .build()
        )
        .build()
        .create(DiscoveryApi::class.java)

/** A 200 as GitHub serves it: cacheable for 5 minutes, with an ETag. */
internal fun ok(body: String, etag: String = "\"v1\"") = MockResponse.Builder()
    .code(200)
    .addHeader("Cache-Control", "max-age=300")
    .addHeader("ETag", etag)
    .body(body)
    .build()

internal fun status(code: Int) = MockResponse.Builder().code(code).build()

/** A payload of restaurants open 11:00 to 22:00 every Monday. */
internal fun payload(vararg ids: String) = ids.joinToString(prefix = """{"restaurants": [""", postfix = "]}") {
    """{"id": "$it", "name": "Restaurant $it", "opening_hours": [{"day": "MONDAY", "opens": "11:00", "closes": "22:00"}]}"""
}
