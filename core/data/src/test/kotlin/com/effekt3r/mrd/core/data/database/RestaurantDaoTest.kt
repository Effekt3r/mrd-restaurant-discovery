package com.effekt3r.mrd.core.data.database

import android.database.sqlite.SQLiteConstraintException
import com.effekt3r.mrd.core.data.inMemoryDatabase
import com.effekt3r.mrd.core.data.parsedRestaurant
import com.effekt3r.mrd.core.data.storedIds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RestaurantDaoTest {

    private val database = inMemoryDatabase()
    private val dao = database.restaurantDao()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun aSwapThatFailsHalfwayLeavesThePreviousSnapshot() = runTest {
        val previous = parsedRestaurant("r-1")
        dao.replaceSnapshot(listOf(previous.toEntity(0)), previous.intervalEntities(), syncedAt = 1, etag = "\"v1\"")

        // The intervals belong to a restaurant the new snapshot doesn't have, so inserting them breaks the foreign key:
        // after the old restaurants are deleted and the new ones inserted, before the sync state is written.
        val failure = runCatching {
            dao.replaceSnapshot(
                listOf(parsedRestaurant("r-2").toEntity(0)),
                listOf(OpeningIntervalEntity("r-missing", startMinute = 0, endMinute = 10)),
                syncedAt = 2,
                etag = "\"v2\""
            )
        }.exceptionOrNull()

        assertTrue("expected a foreign-key failure, got $failure", failure is SQLiteConstraintException)
        assertEquals(listOf("r-1"), dao.storedIds())
        assertEquals(7, dao.restaurant("r-1").first()?.intervals?.size) // its cascade-deleted hours are back too
        assertEquals(1L, dao.lastSyncedAt().first())
        assertEquals("\"v1\"", dao.snapshotEtag())
    }
}
