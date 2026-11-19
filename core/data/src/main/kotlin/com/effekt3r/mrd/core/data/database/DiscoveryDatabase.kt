package com.effekt3r.mrd.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/** `discovery.db` (spec §5.5). Each version's schema is exported to core/data/schemas/. */
@Database(
    entities = [
        RestaurantEntity::class,
        OpeningIntervalEntity::class,
        FavouriteEntity::class,
        SyncStateEntity::class,
        MenuEntity::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
internal abstract class DiscoveryDatabase : RoomDatabase() {
    abstract fun restaurantDao(): RestaurantDao

    abstract fun menuDao(): MenuDao
}
