package com.effekt3r.mrd.core.data.database

import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// Open now and the list's status in SQL (spec §5.6), by the same rules as openStatus() in :core:model, so paging and
// the count stay correct at 10,000 rows. `c` is the interval containing :weekMinute, if any; intervals never overlap.
private const val FROM_RESTAURANTS = """
    FROM restaurants r
    LEFT JOIN opening_intervals c
        ON c.restaurant_id = r.id AND c.start_minute <= :weekMinute AND c.end_minute > :weekMinute
"""

// The snapshot is from 0 to 30 minutes old (1,800,000 ms, exclusive). With no sync_state row it's NULL: not fresh.
private const val SNAPSHOT_FRESH = """
    (:now - (SELECT s.last_synced_at FROM sync_state s WHERE s.id = 0)) BETWEEN 0 AND 1799999
"""

private const val STATUS = """
    CASE
        WHEN r.has_hours THEN CASE WHEN c.restaurant_id IS NULL THEN 'CLOSED' ELSE 'OPEN' END
        WHEN r.is_open_snapshot IS NOT NULL AND $SNAPSHOT_FRESH
            THEN CASE WHEN r.is_open_snapshot THEN 'OPEN' ELSE 'CLOSED' END
        ELSE 'UNKNOWN'
    END
"""

// The week minute of the next change. Closed: the next interval's start, else the week's first. Open: the interval's
// end; one that runs to the end of Sunday carries on into an interval starting Monday 00:00. NULL: it never closes.
private const val NEXT_CHANGE = """
    CASE
        WHEN NOT r.has_hours THEN NULL
        WHEN c.restaurant_id IS NULL THEN COALESCE(
            (SELECT MIN(n.start_minute) FROM opening_intervals n
                WHERE n.restaurant_id = r.id AND n.start_minute > :weekMinute),
            (SELECT MIN(n.start_minute) FROM opening_intervals n WHERE n.restaurant_id = r.id))
        WHEN c.end_minute < 10080 THEN c.end_minute
        WHEN c.start_minute = 0 THEN NULL
        ELSE COALESCE(
            (SELECT w.end_minute FROM opening_intervals w WHERE w.restaurant_id = r.id AND w.start_minute = 0), 0)
    END
"""

// :query is already a search key with LIKE's wildcards escaped (likePattern).
private const val MATCHES = """
    WHERE r.name_search LIKE '%' || :query || '%' ESCAPE '\'
        AND (NOT :openNow OR $STATUS = 'OPEN')
"""

/** One restaurant in the list, with its status at the query's week minute: OPEN, CLOSED or UNKNOWN. */
internal data class RestaurantRow(
    val id: String,
    val name: String,
    val cuisines: List<String>,
    @ColumnInfo(name = "rating_tenths") val ratingTenths: Int?,
    @ColumnInfo(name = "delivery_fee_cents") val deliveryFeeCents: Long?,
    @ColumnInfo(name = "eta_minutes") val etaMinutes: Int?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "is_favourite") val isFavourite: Boolean,
    val status: String,
    @ColumnInfo(name = "next_change") val nextChange: Int?
)

internal data class RestaurantWithHours(
    @Embedded val restaurant: RestaurantEntity,
    @ColumnInfo(name = "is_favourite") val isFavourite: Boolean,
    @Relation(parentColumn = "id", entityColumn = "restaurant_id") val intervals: List<OpeningIntervalEntity>
)

@Dao
internal interface RestaurantDao {
    @Query(
        """
        SELECT r.id, r.name, r.cuisines, r.rating_tenths, r.delivery_fee_cents, r.eta_minutes, r.image_url,
            EXISTS (SELECT 1 FROM favourites f WHERE f.restaurant_id = r.id) AS is_favourite,
            $STATUS AS status, $NEXT_CHANGE AS next_change
        $FROM_RESTAURANTS $MATCHES
        ORDER BY r.position
        """
    )
    fun restaurants(query: String, openNow: Boolean, weekMinute: Int, now: Long): PagingSource<Int, RestaurantRow>

    @Query("SELECT COUNT(*) $FROM_RESTAURANTS $MATCHES")
    fun count(query: String, openNow: Boolean, weekMinute: Int, now: Long): Flow<Int>

    @Transaction
    @Query(
        """
        SELECT r.*, EXISTS (SELECT 1 FROM favourites f WHERE f.restaurant_id = r.id) AS is_favourite
        FROM restaurants r WHERE r.id = :id
        """
    )
    fun restaurant(id: String): Flow<RestaurantWithHours?>

    @Query("SELECT last_synced_at FROM sync_state WHERE id = 0")
    fun lastSyncedAt(): Flow<Long?>

    @Query("SELECT EXISTS (SELECT 1 FROM restaurants)")
    suspend fun hasRestaurants(): Boolean

    /** The ETag of the response the stored snapshot came from: null before the first snapshot, or if it had none. */
    @Query("SELECT etag FROM sync_state WHERE id = 0")
    suspend fun snapshotEtag(): String?

    @Query("UPDATE sync_state SET last_synced_at = :syncedAt WHERE id = 0")
    suspend fun setLastSyncedAt(syncedAt: Long)

    @Upsert
    suspend fun setSyncState(state: SyncStateEntity)

    @Query("DELETE FROM restaurants")
    suspend fun deleteRestaurants()

    @Insert
    suspend fun insertRestaurants(restaurants: List<RestaurantEntity>)

    @Insert
    suspend fun insertIntervals(intervals: List<OpeningIntervalEntity>)

    /**
     * Swaps in a new snapshot in one transaction (spec §5.3): readers see the old one until it commits, and [etag], the
     * ETag of the response it came from, commits with it. Deleting every restaurant (their intervals cascade) rather
     * than those missing from the snapshot avoids `NOT IN (:ids)`, which would exceed SQLite's 999-variable limit on
     * Android 10 and lower. Favourites have no foreign key, so they stay.
     */
    @Transaction
    suspend fun replaceSnapshot(
        restaurants: List<RestaurantEntity>,
        intervals: List<OpeningIntervalEntity>,
        syncedAt: Long,
        etag: String?
    ) {
        deleteRestaurants()
        insertRestaurants(restaurants)
        insertIntervals(intervals)
        setSyncState(SyncStateEntity(lastSyncedAt = syncedAt, etag = etag))
    }

    @Query("SELECT EXISTS (SELECT 1 FROM favourites WHERE restaurant_id = :id)")
    suspend fun isFavourite(id: String): Boolean

    @Insert
    suspend fun insertFavourite(favourite: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE restaurant_id = :id")
    suspend fun deleteFavourite(id: String)

    @Transaction
    suspend fun toggleFavourite(id: String, now: Long) {
        if (isFavourite(id)) deleteFavourite(id) else insertFavourite(FavouriteEntity(id, now))
    }
}
