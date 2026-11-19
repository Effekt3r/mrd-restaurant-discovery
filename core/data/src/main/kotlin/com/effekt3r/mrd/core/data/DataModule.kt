package com.effekt3r.mrd.core.data

import android.content.Context
import androidx.room.Room
import com.effekt3r.mrd.core.data.database.DiscoveryDatabase
import com.effekt3r.mrd.core.data.database.MenuDao
import com.effekt3r.mrd.core.data.database.RestaurantDao
import com.effekt3r.mrd.core.data.network.DiscoveryApi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    fun restaurantRepository(repository: OfflineFirstRestaurantRepository): RestaurantRepository

    @Binds
    fun menuRepository(repository: OfflineFirstMenuRepository): MenuRepository

    companion object {
        @Provides
        @Singleton
        fun database(@ApplicationContext context: Context): DiscoveryDatabase =
            Room.databaseBuilder(context, DiscoveryDatabase::class.java, "discovery.db").build()

        @Provides
        fun restaurantDao(database: DiscoveryDatabase): RestaurantDao = database.restaurantDao()

        @Provides
        fun menuDao(database: DiscoveryDatabase): MenuDao = database.menuDao()

        /** The client is created on the first call, off the main thread, not when Hilt builds the graph. */
        @Provides
        @Singleton
        fun discoveryApi(client: Lazy<OkHttpClient>): DiscoveryApi = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .callFactory { client.get().newCall(it) }
            .build()
            .create(DiscoveryApi::class.java)
    }
}
