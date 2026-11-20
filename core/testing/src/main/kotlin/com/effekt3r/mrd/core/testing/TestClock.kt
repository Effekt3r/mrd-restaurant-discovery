package com.effekt3r.mrd.core.testing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock that moves only when a test moves it. */
class TestClock(private var now: Instant) : Clock() {
    fun set(instant: Instant) {
        now = instant
    }

    fun advanceBy(duration: Duration) {
        now += duration
    }

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId?): Clock = this
}
