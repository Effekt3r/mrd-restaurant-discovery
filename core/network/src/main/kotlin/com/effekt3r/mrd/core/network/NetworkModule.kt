package com.effekt3r.mrd.core.network

import android.content.Context
import com.effekt3r.mrd.core.common.NetworkMonitor
import dagger.Binds
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
 * The networking stack every feature shares (spec §4.3). A host app provides its own client instead, carrying its
 * auth, logging or certificate-pinning interceptors.
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
    }
}
