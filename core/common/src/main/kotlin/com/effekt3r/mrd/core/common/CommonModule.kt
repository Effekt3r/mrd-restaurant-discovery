package com.effekt3r.mrd.core.common

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// Qualifiers target only @Provides functions and constructor parameters, so on a constructor property they apply to
// the parameter, where Dagger reads them.

/** For blocking I/O, such as reading a response body. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class IoDispatcher

/** For work that outlives a screen, such as a refresh other callers can join. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class ApplicationScope

/** The host app's platform bindings (spec §4.3). A host with its own provides these instead. */
@Module
@InstallIn(SingletonComponent::class)
interface CommonModule {
    @Binds
    fun minuteTicker(ticker: ClockMinuteTicker): MinuteTicker

    companion object {
        @Provides
        @IoDispatcher
        fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        @Singleton
        @ApplicationScope
        fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        @Singleton
        fun clock(): Clock = Clock.systemUTC()
    }
}
