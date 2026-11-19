package com.effekt3r.mrd.core.common

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class) // the test scheduler's virtual time
class ClockMinuteTickerTest {

    // Follows the test's virtual time, so delay() and the clock move together.
    private fun TestScope.clockFrom(start: String) = object : Clock() {
        override fun instant(): Instant = Instant.parse(start).plusMillis(testScheduler.currentTime)

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?): Clock = this
    }

    @Test
    fun ticksNowThenAtTheStartOfEveryMinute() = runTest {
        val ticks = ClockMinuteTicker(clockFrom("2026-11-16T10:00:42.500Z")).ticks().take(3).toList()
        assertEquals(
            listOf("2026-11-16T10:00:42.500Z", "2026-11-16T10:01:00Z", "2026-11-16T10:02:00Z").map(Instant::parse),
            ticks
        )
    }

    @Test
    fun aTickOnTheBoundaryWaitsAWholeMinute() = runTest {
        val ticks = ClockMinuteTicker(clockFrom("2026-11-16T10:00:00Z")).ticks().take(2).toList()
        assertEquals(listOf("2026-11-16T10:00:00Z", "2026-11-16T10:01:00Z").map(Instant::parse), ticks)
    }
}
