package com.effekt3r.mrd.core.data

import androidx.paging.PagingSource
import com.effekt3r.mrd.core.data.database.intervalEntities
import com.effekt3r.mrd.core.data.database.toEntity
import com.effekt3r.mrd.core.data.database.toModel
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.OpeningHours
import com.effekt3r.mrd.core.model.WeekMinute
import com.effekt3r.mrd.core.model.openStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Spec §5.6: the list's status and Open now come from SQL, and Details' from Kotlin. Every boundary case must give the
 * expected status both ways, and SQL's Open now filter must keep exactly the restaurants it shows as Open.
 */
@RunWith(RobolectricTestRunner::class)
class OpenStatusAgreementTest {

    private val database = inMemoryDatabase()
    private val dao = database.restaurantDao()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun kotlinAndSqlAgreeOnEveryBoundaryCase() = runTest {
        val failures = openStatusCases.flatMap { case ->
            val now = case.at.instant()
            val snapshotAt = now - case.snapshotAge
            val kotlin = openStatus(OpeningHours.from(case.windows), case.openSnapshot, snapshotAt, now)

            val restaurant = parsedRestaurant("r-1", windows = case.windows, isOpen = case.openSnapshot)
            dao.replaceSnapshot(
                listOf(restaurant.toEntity(0)),
                restaurant.intervalEntities(),
                snapshotAt.toEpochMilli()
            )
            val weekMinute = WeekMinute.at(now).value
            val page = dao.restaurants("", openNow = false, weekMinute, now.toEpochMilli())
                .load(PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false))
            val sql = (page as PagingSource.LoadResult.Page).data.single().toModel().openStatus
            val keptByOpenNow = dao.count("", openNow = true, weekMinute, now.toEpochMilli()).first() == 1

            listOfNotNull(
                "${case.name}: Kotlin gives $kotlin, expected ${case.expected}".takeIf { kotlin != case.expected },
                "${case.name}: SQL gives $sql, expected ${case.expected}".takeIf { sql != case.expected },
                "${case.name}: Open now ${if (keptByOpenNow) "keeps" else "drops"} it"
                    .takeIf { keptByOpenNow != (case.expected is OpenStatus.Open) }
            )
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
