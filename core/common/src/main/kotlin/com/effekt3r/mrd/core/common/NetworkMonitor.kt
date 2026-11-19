package com.effekt3r.mrd.core.common

import kotlinx.coroutines.flow.Flow

/**
 * Whether the device has validated internet (spec §5.7). It lives here, not in :core:network, so features can pick
 * their error wording from it without depending on the networking stack.
 */
interface NetworkMonitor {
    /** A cold flow: the platform callback is registered only while it's collected. */
    val isOnline: Flow<Boolean>
}
