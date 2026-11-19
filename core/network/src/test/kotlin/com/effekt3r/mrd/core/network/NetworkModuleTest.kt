package com.effekt3r.mrd.core.network

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NetworkModuleTest {

    // Spec §5.3. The read timeout allows for the 6 MB stress file; the sync tests use their own, shorter ones.
    @Test
    fun theClientTimesOutAfter10SecondsToConnectAnd20ToRead() {
        val client = NetworkModule.okHttpClient(ApplicationProvider.getApplicationContext())
        assertEquals(10_000, client.connectTimeoutMillis)
        assertEquals(20_000, client.readTimeoutMillis)
    }
}
