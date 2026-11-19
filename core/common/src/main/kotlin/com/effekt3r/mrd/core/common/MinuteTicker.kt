package com.effekt3r.mrd.core.common

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** The current time, now and then at the start of every minute, so open statuses stay current (spec §5.6). */
interface MinuteTicker {
    /** A cold flow: it ticks only while it's collected. */
    fun ticks(): Flow<Instant>
}

class ClockMinuteTicker @Inject constructor(private val clock: Clock) : MinuteTicker {
    override fun ticks(): Flow<Instant> = flow {
        while (true) {
            val now = clock.instant()
            emit(now)
            delay(MILLIS_PER_MINUTE - Math.floorMod(now.toEpochMilli(), MILLIS_PER_MINUTE))
        }
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
