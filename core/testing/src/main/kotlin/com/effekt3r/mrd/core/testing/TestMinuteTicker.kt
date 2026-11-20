package com.effekt3r.mrd.core.testing

import com.effekt3r.mrd.core.common.MinuteTicker
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart

/** Ticks with [clock]'s time when collection starts, and then only when a test calls [tick]. */
class TestMinuteTicker(private val clock: Clock) : MinuteTicker {
    private val ticks = MutableSharedFlow<Instant>()

    override fun ticks(): Flow<Instant> = ticks.onStart { emit(clock.instant()) }

    suspend fun tick() = ticks.emit(clock.instant())
}
