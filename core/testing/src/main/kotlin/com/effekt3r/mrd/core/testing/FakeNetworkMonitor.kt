package com.effekt3r.mrd.core.testing

import com.effekt3r.mrd.core.common.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNetworkMonitor(online: Boolean = true) : NetworkMonitor {
    private val state = MutableStateFlow(online)

    override val isOnline: Flow<Boolean> = state

    fun setOnline(online: Boolean) {
        state.value = online
    }
}
