package com.effekt3r.mrd.core.network

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.effekt3r.mrd.core.common.NetworkMonitor
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient

/**
 * The networking stack every feature shares (spec §4.3), and the image loader that uses it. A host app provides its
 * own client and loader instead, carrying its auth, logging or certificate-pinning interceptors.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface NetworkModule {
    @Binds
    fun networkMonitor(monitor: ConnectivityManagerNetworkMonitor): NetworkMonitor

    companion object {
        @Provides
        @Singleton
        fun json(): Json = Json { ignoreUnknownKeys = true }

        /**
         * A 10 MB disk cache keeps each response with its ETag, so a revalidation that finds nothing changed costs a
         * 304 of a few hundred bytes (spec §5.3). The read timeout allows for the 6 MB stress file.
         */
        @Provides
        @Singleton
        fun okHttpClient(@ApplicationContext context: Context): OkHttpClient = OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http_cache"), 10L * 1024 * 1024))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        /**
         * Coil's loader for every photo (spec §5.7), on [imageClient], with Coil's own disk cache in the app's cache
         * directory, so a photo seen before still shows offline. The client is built on the first photo request, off
         * the main thread, not when Hilt builds the graph.
         */
        @Provides
        @Singleton
        fun imageLoader(@ApplicationContext context: Context, client: Lazy<OkHttpClient>): ImageLoader =
            ImageLoader.Builder(context)
                .components { add(OkHttpNetworkFetcherFactory(callFactory = { imageClient(client.get()) })) }
                .build()

        /**
         * The shared client without its HTTP cache: the same connections, dispatcher and interceptors, but photos
         * go to Coil's disk cache only. In OkHttp's 10 MB cache, a few hundred photos would evict the restaurant
         * list's entry, whose ETag lets an unchanged refresh cost a 304 (spec §5.3).
         */
        private fun imageClient(client: OkHttpClient): OkHttpClient = client.newBuilder().cache(null).build()
    }
}
