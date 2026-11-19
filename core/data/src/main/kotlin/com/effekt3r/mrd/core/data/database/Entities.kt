package com.effekt3r.mrd.core.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One restaurant of the current snapshot (spec §5.5). [position] keeps the server's order. */
@Entity(tableName = "restaurants", indices = [Index("position")])
internal data class RestaurantEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "name_search") val nameSearch: String,
    val cuisines: List<String>,
    @ColumnInfo(name = "rating_tenths") val ratingTenths: Int?,
    @ColumnInfo(name = "delivery_fee_cents") val deliveryFeeCents: Long?,
    @ColumnInfo(name = "eta_minutes") val etaMinutes: Int?,
    @ColumnInfo(name = "is_open_snapshot") val isOpenSnapshot: Boolean?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "has_hours") val hasHours: Boolean,
    val position: Int
)

/** An opening interval [start_minute, end_minute) in week minutes (spec §5.6). */
@Entity(
    tableName = "opening_intervals",
    primaryKeys = ["restaurant_id", "start_minute"],
    foreignKeys = [
        ForeignKey(
            entity = RestaurantEntity::class,
            parentColumns = ["id"],
            childColumns = ["restaurant_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
internal data class OpeningIntervalEntity(
    @ColumnInfo(name = "restaurant_id") val restaurantId: String,
    @ColumnInfo(name = "start_minute") val startMinute: Int,
    @ColumnInfo(name = "end_minute") val endMinute: Int
)

/** No foreign key, so a favourite survives snapshot swaps and a restaurant that's briefly missing (spec §5.8). */
@Entity(tableName = "favourites")
internal data class FavouriteEntity(
    @PrimaryKey @ColumnInfo(name = "restaurant_id") val restaurantId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

/** A single row (id 0): when the snapshot was last fetched or confirmed unchanged, in epoch milliseconds. */
@Entity(tableName = "sync_state")
internal data class SyncStateEntity(
    @PrimaryKey val id: Int = 0,
    @ColumnInfo(name = "last_synced_at") val lastSyncedAt: Long
)

/** The validated menu of one restaurant, as JSON of [CachedMenu], so a menu seen before works offline (spec §5.9). */
@Entity(tableName = "menus")
internal data class MenuEntity(
    @PrimaryKey @ColumnInfo(name = "restaurant_id") val restaurantId: String,
    @ColumnInfo(name = "menu_json") val menuJson: String,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long
)
