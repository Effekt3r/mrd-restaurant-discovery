package com.effekt3r.mrd.core.ui

import com.effekt3r.mrd.core.model.SyncError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyncProblemTest {

    @Test
    fun eachFailureIsWordedForWhatTheUserCanDo() {
        assertEquals(SyncProblem.Offline, syncProblem(SyncError.Network, isOnline = false))
        assertEquals(SyncProblem.CouldNotConnect, syncProblem(SyncError.Network, isOnline = true))
        assertEquals(SyncProblem.CouldNotConnect, syncProblem(SyncError.Server(503), isOnline = true))
        assertEquals(SyncProblem.CouldNotConnect, syncProblem(SyncError.BadData, isOnline = true))
    }

    @Test
    fun storageIsNeverWordedAsAConnectionProblem() {
        assertEquals(SyncProblem.Storage, syncProblem(SyncError.Storage, isOnline = true))
        assertEquals(SyncProblem.Storage, syncProblem(SyncError.Storage, isOnline = false))
    }

    @Test
    fun beingOfflineIsAProblemBeforeAnythingFails() {
        assertEquals(SyncProblem.Offline, syncProblem(error = null, isOnline = false))
        assertNull(syncProblem(error = null, isOnline = true))
    }
}
